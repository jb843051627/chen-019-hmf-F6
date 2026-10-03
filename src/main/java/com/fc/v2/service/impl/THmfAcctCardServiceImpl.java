package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.mapper.auto.THmfAcctCardLogMapper;
import com.fc.v2.mapper.auto.THmfAcctCardMapper;
import com.fc.v2.mapper.auto.THmfArchMapper;
import com.fc.v2.model.auto.THmfAcctCard;
import com.fc.v2.model.auto.THmfAcctCardLog;
import com.fc.v2.model.auto.THmfArch;
import com.fc.v2.model.custom.AcctCardReceipt;
import com.fc.v2.model.custom.AcctCardVerdict;
import com.fc.v2.model.custom.AcctFieldSpec;
import com.fc.v2.model.custom.AcctCardRoster;
import com.fc.v2.model.custom.HouseholdHit;
import com.fc.v2.model.custom.OpenHouseholdForm;
import com.fc.v2.model.custom.ReviseHouseholdForm;
import com.fc.v2.service.ITHmfAcctCardService;
import com.fc.v2.shiro.util.ShiroUtils;

/**
 * 分户账立户单 Service业务层处理。
 *
 * 三句话总口径：一户一张档，一张档不两处挂，一册底册不两家项目共用。
 * 合规判定、折档算钱、步子进退、版次沉底、册首点数、找户过滤，只在这里发生；
 * 控制器与前端拿不到第二条判路。旧的增删改查签名原样留着（早年迁库要用），
 * 立户这一路另走 openHousehold / reviseHousehold。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class THmfAcctCardServiceImpl
        extends ServiceImpl<THmfAcctCardMapper, THmfAcctCard> implements ITHmfAcctCardService {

    /** 步子：待核对 */
    private static final int STEP_WAIT = 0;
    /** 步子：已核对 */
    private static final int STEP_CHECKED = 1;
    /** 步子：已冻住（尽头） */
    private static final int STEP_FROZEN = 2;

    /** 所在层：现行（名录只露这一层） */
    private static final int LAYER_CURRENT = 0;
    /** 所在层：历史层（旧版沉底，只供翻） */
    private static final int LAYER_HISTORY = 1;

    /** 就地打住：在走 */
    private static final int HALT_NO = 0;
    /** 就地打住：已打住（不进不退，名额空出） */
    private static final int HALT_YES = 1;

    /** 留痕动作：推进 */
    private static final int LOG_PUSH = 0;
    /** 留痕动作：退回 */
    private static final int LOG_BACK = 1;
    /** 留痕动作：打住 */
    private static final int LOG_HALT = 2;

    /** 项目类别：商品房 */
    private static final String TYPE_COMMODITY = "商品房";
    /** 项目类别：保障性住房 */
    private static final String TYPE_AFFORDABLE = "保障性住房";
    /** 项目类别：直管公房 */
    private static final String TYPE_PUBLIC = "直管公房";

    /** 底册情形：已迁出（不再收新户，老户仍在册可查） */
    private static final int ARCH_MOVED = 1;

    /** 各档每平米标准（档次1..4对应）。应缴额＝标准×建面，同一回折出，不由人报。 */
    private static final BigDecimal[] UNIT_PRICE_BY_GRADE = {
            null,
            new BigDecimal("50.00"),
            new BigDecimal("70.00"),
            new BigDecimal("90.00"),
            new BigDecimal("110.00"),
    };

    /** 系统发号用自占序号 */
    private static final AtomicLong NODE_SEQ = new AtomicLong(0);

    @javax.annotation.Resource
    private THmfArchMapper hmfArchMapper;

    @javax.annotation.Resource
    private THmfAcctCardLogMapper hmfAcctCardLogMapper;

    // ---------------------------------------------------------------- 旧签名（原样留）

    @Override
    public THmfAcctCard selectTHmfAcctCardById(Long id) {
        if (id == null) {
            return null;
        }
        List<THmfAcctCard> list = this.baseMapper.selectList(new QueryWrapper<THmfAcctCard>()
                .eq("id", id).eq("del_flag", 0));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<THmfAcctCard> selectTHmfAcctCardList(Wrapper<THmfAcctCard> queryWrapper) {
        // 旧读法签名不动：照调用方给的条件查（早年迁库的列表也要点得出来）。
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public int insertTHmfAcctCard(THmfAcctCard record) {
        // 旧签名只供早年纸质单搬迁一条路用，不做新立户的判路。
        if (record == null) {
            return 0;
        }
        if (record.getDelFlag() == null) {
            record.setDelFlag(0);
        }
        if (record.getLayer() == null) {
            record.setLayer(LAYER_CURRENT);
        }
        if (record.getHaltFlag() == null) {
            record.setHaltFlag(HALT_NO);
        }
        int n = this.baseMapper.insert(record);
        // 纸质旧单迁入：誊有步数的，按步数补迁入留痕当重放基线，
        // 此后这张单的当前步照样只照留痕数；步数空着的不补（按还在走的待核对认）。
        Integer legacyStep = record.getStatus();
        if (legacyStep != null && legacyStep > 0) {
            String mover = isBlank(record.getCreateBy()) ? "纸质旧单迁入" : record.getCreateBy();
            for (int step = STEP_CHECKED; step <= Math.min(legacyStep, STEP_FROZEN); step++) {
                writeLog(record, step, LOG_PUSH, 1, mover, "纸质旧单迁入补痕",
                        record.getCreateTime() == null ? new Date() : record.getCreateTime());
            }
        }
        return n;
    }

    @Override
    public int updateTHmfAcctCard(THmfAcctCard record) {
        if (record == null || record.getId() == null) {
            return 0;
        }
        THmfAcctCard store = selectTHmfAcctCardById(record.getId());
        if (store == null) {
            return 0;
        }
        // 冻住即尽头：旧签名也覆不上去。
        if (isFrozen(store)) {
            return 0;
        }
        record.setUpdateTime(new Date());
        return this.baseMapper.updateById(record);
    }

    @Override
    public int deleteTHmfAcctCardByIds(String ids) {
        if (isBlank(ids)) {
            return 0;
        }
        int n = 0;
        for (String id : ids.split(",")) {
            if (!isBlank(id.trim())) {
                n += deleteTHmfAcctCardById(Long.valueOf(id.trim()));
            }
        }
        return n;
    }

    @Override
    public int deleteTHmfAcctCardById(Long id) {
        // 冻住的整张撤不掉；其余只打删除标记，不真抹（旧单也要查得到）。
        THmfAcctCard store = selectTHmfAcctCardById(id);
        if (store == null || isFrozen(store)) {
            return 0;
        }
        THmfAcctCard tomb = new THmfAcctCard();
        tomb.setId(id);
        tomb.setDelFlag(1);
        tomb.setUpdateTime(new Date());
        return this.baseMapper.updateById(tomb);
    }

    // ---------------------------------------------------------------- 立户（唯一保存方法）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcctCardReceipt openHousehold(OpenHouseholdForm form) {
        AcctCardReceipt receipt = new AcctCardReceipt();
        if (form == null) {
            receipt.setCode(AcctCardReceipt.CODE_REJECTED);
            receipt.getMissingFields().add("form");
            receipt.setMessage("空单不收");
            return receipt;
        }

        // 户归哪本底册只凭代号认：先按代号把底册取出来，名字像不算数。
        THmfArch arch = archByCode(form.getSiteNo());
        List<String> missing = validateFields(form.getSiteNo(), arch, form.getDoorNo(), form.getQty(),
                form.getWriteStart(), form.getWriteEnd(), form.getConfirmWriteOrder(),
                form.getPermitNo(), form.getSurveyNo(), form.getAllotNo(), form.getHostDept(),
                form.getOwnerUnit(), form.getGrantDoc());
        if (!missing.isEmpty()) {
            receipt.setCode(AcctCardReceipt.CODE_REJECTED);
            receipt.setMissingFields(missing);
            receipt.setMessage("户进不了库，缺栏/不合规栏：" + String.join("、", missing));
            receipt.setSiteNo(form.getSiteNo());
            writeRosterCounts(receipt, form.getSiteNo());
            return receipt;
        }

        // 一户一张档：同本底册同门牌（同一户）只认一个户号、一张在场。
        // 前一张在走（含已打住）或已冻住，都不许再发新户号两套在场；接续一律走换版：
        // 同户号立新版，旧版整版沉底。两个窗口同日各报、换人重报也撞在这里。
        THmfAcctCard present = cardAtDoor(form.getSiteNo().trim(), form.getDoorNo().trim(), null);
        if (present != null) {
            receipt.setCode(AcctCardReceipt.CODE_REJECTED);
            receipt.getMissingFields().add("billNo");
            receipt.setMessage("该门牌户已在户（户号" + present.getBillNo()
                    + "）：在走的不许叠第二张；已打住/已冻住的接续请走换版，同户号另起新版，不另发新号");
            receipt.setSiteNo(form.getSiteNo().trim());
            writeRosterCounts(receipt, form.getSiteNo().trim());
            return receipt;
        }

        THmfAcctCard card = new THmfAcctCard();
        // 户号、证载号两处编号一律系统发出，表单上根本没有这两栏，人手拟的递不进来。
        String billNo = issueNumber("FH");
        String certNo = issueNumber("CZ");
        card.setBillNo(billNo);
        card.setCertNo(certNo);
        card.setNodeNo(1);
        card.setSiteId(arch.getId() == null ? null : arch.getId().intValue());
        card.setSiteNo(arch.getSiteNo().trim());
        card.setSiteType(arch.getSiteType() == null ? null : arch.getSiteType().trim());
        card.setDoorNo(form.getDoorNo().trim());
        card.setWriteStart(form.getWriteStart());
        card.setWriteEnd(form.getWriteEnd());
        card.setPermitNo(trimOrNull(form.getPermitNo()));
        card.setSurveyNo(trimOrNull(form.getSurveyNo()));
        card.setAllotNo(trimOrNull(form.getAllotNo()));
        card.setHostDept(trimOrNull(form.getHostDept()));
        card.setOwnerUnit(trimOrNull(form.getOwnerUnit()));
        card.setGrantDoc(trimOrNull(form.getGrantDoc()));
        card.setRemark(form.getRemark());

        // 刚立户即重算时点之一：档次、每平米标准、应缴额同一回落卡，档次栏只读不写。
        applyGradeAndAmount(card, form.getQty(), arch);

        card.setContent(checksumOf(card));
        card.setStatus(STEP_WAIT);
        card.setLayer(LAYER_CURRENT);
        card.setHaltFlag(HALT_NO);
        card.setDelFlag(0);
        this.baseMapper.insert(card);

        // 立成一户加一：册首三笔数与逐户点算在同一回统计里写出。
        receipt.setCode(AcctCardReceipt.CODE_OPENED);
        receipt.setCard(card);
        receipt.setMessage("已立户，户号与证载号由系统发出，待核对");
        writeRosterCounts(receipt, card.getSiteNo());
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcctCardReceipt reviseHousehold(ReviseHouseholdForm form) {
        AcctCardReceipt receipt = new AcctCardReceipt();
        if (form == null || form.getId() == null) {
            receipt.setCode(AcctCardReceipt.CODE_REJECTED);
            receipt.getMissingFields().add("id");
            receipt.setMessage("没指到要改的那张现行单");
            return receipt;
        }
        THmfAcctCard old = selectTHmfAcctCardById(form.getId());
        // 名录只露现行版，只能从现行版另起新版；历史层的旧版不许拿来再改。
        if (old == null || !intEquals(old.getLayer(), LAYER_CURRENT)) {
            receipt.setCode(AcctCardReceipt.CODE_REJECTED);
            receipt.getMissingFields().add("id");
            receipt.setMessage("指不到现行的那张单，另起新版无从接");
            return receipt;
        }

        String targetSiteNo = isBlank(form.getTargetSiteNo()) ? old.getSiteNo() : form.getTargetSiteNo().trim();
        THmfArch arch = archByCode(targetSiteNo);
        String doorNo = isBlank(form.getDoorNo()) ? old.getDoorNo() : form.getDoorNo().trim();
        BigDecimal qty = form.getQty() == null ? old.getQty() : form.getQty();
        Date writeStart = form.getWriteStart() == null ? old.getWriteStart() : form.getWriteStart();
        Date writeEnd = form.getWriteEnd() == null ? old.getWriteEnd() : form.getWriteEnd();

        List<String> missing = validateFields(targetSiteNo, arch, doorNo, qty, writeStart, writeEnd,
                Boolean.TRUE.equals(form.getConfirmWriteOrder()) ? Boolean.TRUE : null,
                firstNonBlank(form.getPermitNo(), old.getPermitNo()),
                firstNonBlank(form.getSurveyNo(), old.getSurveyNo()),
                firstNonBlank(form.getAllotNo(), old.getAllotNo()),
                firstNonBlank(form.getHostDept(), old.getHostDept()),
                firstNonBlank(form.getOwnerUnit(), old.getOwnerUnit()),
                firstNonBlank(form.getGrantDoc(), old.getGrantDoc()));
        if (!missing.isEmpty()) {
            receipt.setCode(AcctCardReceipt.CODE_REJECTED);
            receipt.setMissingFields(missing);
            receipt.setMessage("新版立不起来，缺栏/不合规栏：" + String.join("、", missing));
            receipt.setSiteNo(targetSiteNo);
            writeRosterCounts(receipt, old.getSiteNo());
            return receipt;
        }

        // 换底册或改门牌，不能把户挂到别户已在占的门牌上（一张档不两处挂、一户一张档）。
        THmfAcctCard clash = cardAtDoor(targetSiteNo, doorNo, old.getId());
        if (clash != null) {
            receipt.setCode(AcctCardReceipt.CODE_REJECTED);
            receipt.getMissingFields().add("doorNo.occupied");
            receipt.setMessage(targetSiteNo + " 该门牌已挂在户号" + clash.getBillNo()
                    + " 名下，一张档不能两处挂");
            receipt.setSiteNo(targetSiteNo);
            writeRosterCounts(receipt, old.getSiteNo());
            return receipt;
        }

        // 新版立起：户号、证载号随户走不换；版次序取这一户各版最大值+1；
        // 旧版整版沉历史层，不覆一个字（真要退回旧版也只换回那版，旧数据永远不覆新的）。
        int nextNode = maxNodeNo(old.getBillNo(), old.getNodeNo()) + 1;
        old.setLayer(LAYER_HISTORY);
        this.baseMapper.updateById(old);

        THmfAcctCard card = new THmfAcctCard();
        card.setBillNo(old.getBillNo());
        card.setCertNo(old.getCertNo());
        card.setNodeNo(nextNode);
        card.setSiteId(arch.getId() == null ? null : arch.getId().intValue());
        card.setSiteNo(arch.getSiteNo().trim());
        card.setSiteType(arch.getSiteType() == null ? null : arch.getSiteType().trim());
        card.setDoorNo(doorNo);
        card.setWriteStart(writeStart);
        card.setWriteEnd(writeEnd);
        card.setPermitNo(trimOrNull(firstNonBlank(form.getPermitNo(), old.getPermitNo())));
        card.setSurveyNo(trimOrNull(firstNonBlank(form.getSurveyNo(), old.getSurveyNo())));
        card.setAllotNo(trimOrNull(firstNonBlank(form.getAllotNo(), old.getAllotNo())));
        card.setHostDept(trimOrNull(firstNonBlank(form.getHostDept(), old.getHostDept())));
        card.setOwnerUnit(trimOrNull(firstNonBlank(form.getOwnerUnit(), old.getOwnerUnit())));
        card.setGrantDoc(trimOrNull(firstNonBlank(form.getGrantDoc(), old.getGrantDoc())));
        card.setRemark(form.getRemark());

        // 建面改了、换底册，都是重算时点：新档新钱同一回折。
        applyGradeAndAmount(card, qty, arch);

        // 版次一换校验码重算：新版新码、旧版旧码（码里含版次序与数，两版不可能同码）。
        card.setContent(checksumOf(card));
        card.setStatus(STEP_WAIT);
        card.setLayer(LAYER_CURRENT);
        card.setHaltFlag(HALT_NO);
        card.setDelFlag(0);
        this.baseMapper.insert(card);

        receipt.setCode(AcctCardReceipt.CODE_REVISED);
        receipt.setCard(card);
        receipt.setMessage("新版已立（第" + nextNode + "版），旧版整版沉历史层，只可翻不可覆");
        writeRosterCounts(receipt, card.getSiteNo());
        return receipt;
    }

    // ---------------------------------------------------------------- 版本换回

    @Override
    @Transactional(rollbackFor = Exception.class)
    public THmfAcctCard restoreHouseholdVersion(Long historyId, String operator, String comment) {
        if (historyId == null) {
            return null;
        }
        THmfAcctCard target = selectTHmfAcctCardById(historyId);
        // 只换回历史层里的旧版；拿现行版来换没意义。
        if (target == null || !intEquals(target.getLayer(), LAYER_HISTORY) || isBlank(target.getBillNo())) {
            return null;
        }
        // 找这一户（同户号，跨册）当前在场的新版。
        THmfAcctCard present = null;
        for (THmfAcctCard c : allLiveCards()) {
            if (intEquals(c.getLayer(), LAYER_CURRENT) && target.getBillNo().equals(c.getBillNo())) {
                present = c;
                break;
            }
        }
        if (present == null) {
            return null;
        }
        // 在场版已冻住即尽头，不往旧版倒；已打住的也不进不退。两种都要动只能另起新版。
        if (isFrozen(present) || intEquals(present.getHaltFlag(), HALT_YES)) {
            return null;
        }
        // 只换回那一版：旧版版次序、数、校验码、步子一律原样；在场新版不覆一字，整版沉底留着。
        present.setLayer(LAYER_HISTORY);
        this.baseMapper.updateById(present);
        target.setLayer(LAYER_CURRENT);
        this.baseMapper.updateById(target);
        return target;
    }

    // ---------------------------------------------------------------- 步子推进/退回/打住

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcctCardVerdict pushCard(Long id, String operator, String comment) {
        THmfAcctCard card = operable(id);
        if (card == null) {
            return null;
        }
        if (intEquals(card.getHaltFlag(), HALT_YES)) {
            return new AcctCardVerdict(currentStep(card), AcctCardVerdict.CODE_STAY,
                    "这张已就地打住，不进不退；要动只能另起一张", card);
        }
        int cur = currentStep(card);
        if (cur >= STEP_FROZEN) {
            return new AcctCardVerdict(STEP_FROZEN, AcctCardVerdict.CODE_STAY,
                    "已冻住即尽头：添不进、改不动、整张撤不掉", card);
        }
        // 步子不得跨：只许迈紧挨的下一步。旧纸单缺建面/档次的，先补齐（另起新版）才核得过。
        if (cur == STEP_WAIT && (card.getQty() == null || card.getGradeLevel() == null
                || card.getFineAmt() == null)) {
            return new AcctCardVerdict(cur, AcctCardVerdict.CODE_STAY,
                    "建面、档次、应缴额没齐，核不过；旧单请先订正另起新版", card);
        }
        int target = cur + 1;

        // 先验锁码再落痕：码与单面数对不上即判被人动过，哪一步都不放行，也不许先混一笔痕进去。
        String digest = checksumOf(card);
        if (isBlank(card.getContent())) {
            // 早年旧单没码的，推进时当场定稿，不挡路。
            card.setContent(digest);
        } else if (!card.getContent().equals(digest)) {
            throw new IllegalStateException("校验码与单面数对不上，这张被动过：" + card.getBillNo());
        }

        int round = roundOf(card.getId(), target);
        writeLog(card, target, LOG_PUSH, round, operator(operator), comment, new Date());

        card.setStatus(target);
        this.baseMapper.updateById(card);
        String msg = target == STEP_FROZEN ? "已冻住，这张卡到了尽头，名额空出" : "已核对，校验码锁死";
        return new AcctCardVerdict(target, AcctCardVerdict.CODE_MOVED, msg, card);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcctCardVerdict rollbackCard(Long id, String operator, String comment) {
        THmfAcctCard card = operable(id);
        if (card == null) {
            return null;
        }
        if (intEquals(card.getHaltFlag(), HALT_YES)) {
            return new AcctCardVerdict(currentStep(card), AcctCardVerdict.CODE_STAY,
                    "已打住的单退不动", card);
        }
        int cur = currentStep(card);
        if (cur >= STEP_FROZEN) {
            return new AcctCardVerdict(STEP_FROZEN, AcctCardVerdict.CODE_STAY,
                    "冻住是尽头，退不回去", card);
        }
        if (cur <= STEP_WAIT) {
            return new AcctCardVerdict(STEP_WAIT, AcctCardVerdict.CODE_STAY,
                    "头一步之前无处可退", card);
        }
        // 退回本身逐笔留痕：旧一趟记录原样留着，后补那笔另起一趟，不混摆。
        int round = roundOf(card.getId(), cur);
        writeLog(card, cur, LOG_BACK, round, operator(operator), comment, new Date());

        int back = cur - 1;
        card.setStatus(back);
        this.baseMapper.updateById(card);
        return new AcctCardVerdict(back, AcctCardVerdict.CODE_BACK,
                "退回一步重来，前一趟的记录留底不抹", card);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AcctCardVerdict haltCard(Long id, String operator, String comment) {
        THmfAcctCard card = operable(id);
        if (card == null) {
            return null;
        }
        int cur = currentStep(card);
        if (cur >= STEP_FROZEN) {
            return new AcctCardVerdict(STEP_FROZEN, AcctCardVerdict.CODE_STAY,
                    "已冻住的单不必再打住（名额本已空出）", card);
        }
        if (intEquals(card.getHaltFlag(), HALT_YES)) {
            return new AcctCardVerdict(cur, AcctCardVerdict.CODE_STAY, "这张已经打住了", card);
        }
        // 就地打住：单不进不退，但占着的在走名额当场空出，同户号另立新单接得上。
        writeLog(card, cur, LOG_HALT, roundOf(card.getId(), cur), operator(operator), comment, new Date());
        card.setHaltFlag(HALT_YES);
        this.baseMapper.updateById(card);
        return new AcctCardVerdict(cur, AcctCardVerdict.CODE_HALTED,
                "已就地打住，单不进不退，在走名额空出", card);
    }

    // ---------------------------------------------------------------- 必填栏回话

    @Override
    public AcctFieldSpec requiredFields(String siteType) {
        if (TYPE_COMMODITY.equals(siteType)) {
            return new AcctFieldSpec(TYPE_COMMODITY,
                    new String[] {"permitNo", "surveyNo"},
                    new String[] {"预售证载号", "测绘报告编号"});
        }
        if (TYPE_AFFORDABLE.equals(siteType)) {
            return new AcctFieldSpec(TYPE_AFFORDABLE,
                    new String[] {"allotNo", "hostDept"},
                    new String[] {"配租配售文号", "承办科室"});
        }
        if (TYPE_PUBLIC.equals(siteType)) {
            return new AcctFieldSpec(TYPE_PUBLIC,
                    new String[] {"ownerUnit", "grantDoc"},
                    new String[] {"产权代管单位", "拨交文据"});
        }
        // 类别空着/不认得：不回一套瞎格子。
        return null;
    }

    // ---------------------------------------------------------------- 本级名录（三数同源）

    @Override
    public AcctCardRoster rosterOf(String siteNo) {
        AcctCardRoster roster = new AcctCardRoster();
        roster.setSiteNo(siteNo);
        if (isBlank(siteNo)) {
            return roster;
        }
        // 只摆库里真有的现行户：历史层旧版不进名录，空着也不铺假行。
        List<THmfAcctCard> currents = currentCardsOf(siteNo.trim());
        int opened = 0;
        int frozen = 0;
        int pointed = 0;
        for (THmfAcctCard c : currents) {
            opened++;
            if (isFrozen(c)) {
                frozen++;
            } else {
                // 当场逐户点：在走的、打住的都还算在册（只有冻住减一）。
                pointed++;
            }
        }
        roster.setRows(currents);
        roster.setOpenedCount(opened);
        roster.setFrozenCount(frozen);
        roster.setRosterCount(opened - frozen);
        roster.setPointCount(pointed);
        if (roster.getRosterCount() != pointed) {
            // 册首数与逐户点算出自同一次计算，理论上不可能岔；岔了即账坏，宁可不回。
            throw new IllegalStateException("名录册首数与逐户点算对不上：" + siteNo);
        }
        return roster;
    }

    // ---------------------------------------------------------------- 找户

    @Override
    public List<HouseholdHit> findHousehold(String keyword, List<String> visibleSiteNos) {
        if (isBlank(keyword) || visibleSiteNos == null || visibleSiteNos.isEmpty()) {
            // 本级名下一个没有（或无权看任何一册）就空着，不铺假行。
            return new ArrayList<>();
        }
        String kw = keyword.trim();
        // 只在本级有权看的底册里找，别级的户不同屏供人挑。
        Map<String, THmfArch> archMap = new HashMap<>();
        for (String code : visibleSiteNos) {
            if (!isBlank(code)) {
                THmfArch a = archByCode(code);
                if (a != null) {
                    archMap.put(a.getSiteNo().trim(), a);
                }
            }
        }
        if (archMap.isEmpty()) {
            return new ArrayList<>();
        }

        List<HouseholdHit> hits = new ArrayList<>();
        for (THmfAcctCard c : allLiveCards()) {
            if (!intEquals(c.getLayer(), LAYER_CURRENT) || !archMap.containsKey(c.getSiteNo())) {
                continue;
            }
            // 户号、门牌、证载号三处都开；从头匹配与中间片段匹配都给得出。
            if (contains(c.getBillNo(), kw) || contains(c.getDoorNo(), kw) || contains(c.getCertNo(), kw)) {
                HouseholdHit hit = new HouseholdHit();
                hit.setSiteNo(c.getSiteNo());
                THmfArch a = archMap.get(c.getSiteNo());
                hit.setSiteName(a == null ? null : a.getSiteName());
                hit.setBillNo(c.getBillNo());
                hit.setDoorNo(c.getDoorNo());
                hit.setCertNo(c.getCertNo());
                hit.setGradeLevel(c.getGradeLevel());
                hit.setStatus(currentStep(c));
                hits.add(hit);
            }
        }
        // 出现的前提是库里真有这一户：不造、不抹旧版（旧版在历史层，本来就不上这屏）。
        return hits;
    }

    // ---------------------------------------------------------------- 内部口径

    /** 按底册代号取在册/已迁出底册（代号才是凭据）；查无、已删回 null。 */
    private THmfArch archByCode(String siteNo) {
        if (isBlank(siteNo)) {
            return null;
        }
        List<THmfArch> list = this.hmfArchMapper.selectList(new QueryWrapper<THmfArch>()
                .eq("site_no", siteNo.trim()).eq("del_flag", 0));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按项目类别点必填格。回执缺栏照这里逐栏点名；类别空着当场退回。
     * 共性格：代号、门牌、建面、起写、止写；起写晚于止写须显式确认。
     */
    private List<String> validateFields(String siteNo, THmfArch arch, String doorNo, BigDecimal qty,
                                        Date writeStart, Date writeEnd, Boolean orderConfirmed,
                                        String permitNo, String surveyNo, String allotNo, String hostDept,
                                        String ownerUnit, String grantDoc) {
        List<String> missing = new ArrayList<>();
        if (isBlank(siteNo)) {
            missing.add("siteNo");
        }
        if (arch == null) {
            missing.add("siteNo");
        } else {
            // 已迁走的底册不再收新户：早前挂的老户仍在册可查，新户挂不上去。
            if (intEquals(arch.getStatus(), ARCH_MOVED)) {
                missing.add("siteNo.archived");
            }
            if (isBlank(arch.getSiteType())) {
                // 类别空着，户进不了库。
                missing.add("siteType");
            }
        }
        if (isBlank(doorNo)) {
            missing.add("doorNo");
        }
        if (qty == null) {
            missing.add("qty");
        } else if (qty.signum() <= 0) {
            // 建面填零或负数不收。
            missing.add("qty.nonPositive");
        }
        if (writeStart == null) {
            missing.add("writeStart");
        }
        if (writeEnd == null) {
            missing.add("writeEnd");
        }
        if (writeStart != null && writeEnd != null && writeStart.after(writeEnd)
                && !Boolean.TRUE.equals(orderConfirmed)) {
            // 起写比止写还晚，得先确认才收。
            missing.add("writeOrderConfirm");
        }

        String type = arch == null || arch.getSiteType() == null ? null : arch.getSiteType().trim();
        if (TYPE_COMMODITY.equals(type)) {
            if (isBlank(permitNo)) {
                missing.add("permitNo");      // 商品房交预售证载号
            }
            if (isBlank(surveyNo)) {
                missing.add("surveyNo");      // 与测绘报告编号
            }
        } else if (TYPE_AFFORDABLE.equals(type)) {
            if (isBlank(allotNo)) {
                missing.add("allotNo");       // 保障房交配租配售文号
            }
            if (isBlank(hostDept)) {
                missing.add("hostDept");      // 与承办科室
            }
        } else if (TYPE_PUBLIC.equals(type)) {
            if (isBlank(ownerUnit)) {
                missing.add("ownerUnit");     // 直管公房交产权代管单位
            }
            if (isBlank(grantDoc)) {
                missing.add("grantDoc");      // 与拨交文据
            }
        } else if (type != null) {
            missing.add("siteType.unknown");
        }
        return missing;
    }

    /**
     * 三条界由低往高排，拿建面往上靠：够不着低界落头一层；够到低界没到中界落上一层；
     * 够到中界没到高界再上一层；够得上高界落顶。恰好压界算跨过去，取上面一层。
     * 档次、每平米标准、应缴额在这同一回算里一起落卡。
     */
    private void applyGradeAndAmount(THmfAcctCard card, BigDecimal qty, THmfArch arch) {
        card.setQty(qty.setScale(2, RoundingMode.HALF_UP));
        int grade = 1;
        if (arch.getTh1Max() != null && qty.compareTo(arch.getTh1Max()) >= 0) {
            grade = 2;
        }
        if (arch.getTh2Max() != null && qty.compareTo(arch.getTh2Max()) >= 0) {
            grade = 3;
        }
        if (arch.getTh3Max() != null && qty.compareTo(arch.getTh3Max()) >= 0) {
            grade = 4;
        }
        card.setGradeLevel(grade);
        BigDecimal unitPrice = UNIT_PRICE_BY_GRADE[grade];
        card.setUnitPrice(unitPrice);
        card.setFineAmt(unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP));
    }

    /** 同底册同门牌现行层那张（一个门牌只认一张在场）；excludeId 给了则跳过它自己。 */
    private THmfAcctCard cardAtDoor(String siteNo, String doorNo, Long excludeId) {
        for (THmfAcctCard c : currentCardsOf(siteNo)) {
            if (excludeId != null && excludeId.equals(c.getId())) {
                continue;
            }
            if (doorNo.equals(c.getDoorNo() == null ? null : c.getDoorNo().trim())) {
                return c;
            }
        }
        return null;
    }

    private List<THmfAcctCard> currentCardsOf(String siteNo) {
        List<THmfAcctCard> out = new ArrayList<>();
        for (THmfAcctCard c : allLiveCards()) {
            if (intEquals(c.getLayer(), LAYER_CURRENT) && siteNo.equals(c.getSiteNo())) {
                out.add(c);
            }
        }
        return out;
    }

    private List<THmfAcctCard> allLiveCards() {
        return this.baseMapper.selectList(new QueryWrapper<THmfAcctCard>().eq("del_flag", 0));
    }

    private List<THmfAcctCardLog> logsOf(Long cardId) {
        List<THmfAcctCardLog> logs = this.hmfAcctCardLogMapper.selectList(
                new QueryWrapper<THmfAcctCardLog>()
                        .eq("card_id", cardId).eq("del_flag", 0)
                        .orderByAsc("log_time").orderByAsc("id"));
        return logs == null ? Collections.emptyList() : logs;
    }

    /**
     * 当前停在哪一步：照步子留痕按时间序重放数出来（推进+1、退回-1、打住不挪步）。
     * 屏上填的档、经办人记的档一概不算。早年纸质单：步数空着的无留痕，按还在
     * 走的待核对起步，绝不误当成已经停了的；誊有步数的迁入时补痕，重放落在现状。
     */
    private int currentStep(THmfAcctCard card) {
        // 只照留痕重放，卡面 status 只是镜像——屏上填的、经手人记的一概不算。
        // 旧纸单步数空着：无留痕，从头档待核对起步（还在走，不是停了的）；
        // 誊有步数的：迁入时已按步数补过迁入留痕，重放自然落在迁来的那一步。
        int step = STEP_WAIT;
        for (THmfAcctCardLog log : logsOf(card.getId())) {
            if (intEquals(log.getAction(), LOG_PUSH)) {
                step++;
            } else if (intEquals(log.getAction(), LOG_BACK)) {
                step--;
            }
        }
        return step;
    }

    /** 落某一步是第几趟：从这一步每退回过一趟，再来就加一拨，旧趟留存不混。 */
    private int roundOf(Long cardId, int targetStep) {
        int round = 1;
        for (THmfAcctCardLog log : logsOf(cardId)) {
            if (intEquals(log.getAction(), LOG_BACK) && intEquals(log.getStepNo(), targetStep)) {
                round++;
            }
        }
        return round;
    }

    private void writeLog(THmfAcctCard card, int stepNo, int action, int round,
                          String operator, String comment, Date now) {
        THmfAcctCardLog log = new THmfAcctCardLog();
        log.setCardId(card.getId());
        log.setSiteNo(card.getSiteNo());
        log.setBillNo(card.getBillNo());
        log.setNodeNo(card.getNodeNo());
        log.setStepNo(stepNo);
        log.setAction(action);
        log.setRoundNo(round);
        log.setOperator(operator);
        log.setLogTime(now);
        log.setRemark(comment);
        log.setDelFlag(0);
        this.hmfAcctCardLogMapper.insert(log);
    }

    /** 取现行层那张可操作的单；历史层、已删、没有都回 null。 */
    private THmfAcctCard operable(Long id) {
        THmfAcctCard card = selectTHmfAcctCardById(id);
        if (card == null || !intEquals(card.getLayer(), LAYER_CURRENT)) {
            return null;
        }
        return card;
    }

    private void writeRosterCounts(AcctCardReceipt receipt, String siteNo) {
        if (isBlank(siteNo)) {
            return;
        }
        AcctCardRoster roster = rosterOf(siteNo.trim());
        receipt.setSiteNo(roster.getSiteNo());
        receipt.setOpenedCount(roster.getOpenedCount());
        receipt.setFrozenCount(roster.getFrozenCount());
        receipt.setRosterCount(roster.getRosterCount());
        receipt.setPointCount(roster.getPointCount());
    }

    /** 这一户各版版次序最大值：户号随户走、换册也不换号，故跨册只按户号数（旧纸单空版次序按0）。 */
    private int maxNodeNo(String billNo, Integer fallback) {
        int max = fallback == null ? 0 : fallback;
        for (THmfAcctCard c : allLiveCards()) {
            if (billNo != null && billNo.equals(c.getBillNo())
                    && c.getNodeNo() != null && c.getNodeNo() > max) {
                max = c.getNodeNo();
            }
        }
        return max;
    }

    /** 校验码：户号、版次序、门牌、证载号、建面、档次、应缴额同一串摘要；换版即换码。 */
    private String checksumOf(THmfAcctCard card) {
        String raw = join("|",
                card.getSiteNo(), card.getBillNo(), String.valueOf(card.getNodeNo()),
                card.getDoorNo(), card.getCertNo(),
                card.getQty() == null ? "" : card.getQty().toPlainString(),
                card.getGradeLevel() == null ? "" : String.valueOf(card.getGradeLevel()),
                card.getFineAmt() == null ? "" : card.getFineAmt().toPlainString());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (Exception e) {
            throw new IllegalStateException("校验码算不出来", e);
        }
    }

    /** 系统发号：前缀+时刻+自占序号，全局不重（重复再取）。 */
    private String issueNumber(String prefix) {
        String no;
        do {
            String stamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
            no = prefix + stamp + String.format("%04d", NODE_SEQ.incrementAndGet() % 10000);
        } while (numberExists(no));
        return no;
    }

    private boolean numberExists(String no) {
        for (THmfAcctCard c : allLiveCards()) {
            if (no.equals(c.getBillNo()) || no.equals(c.getCertNo())) {
                return true;
            }
        }
        return false;
    }

    private boolean isFrozen(THmfAcctCard c) {
        // 冻住与否也照留痕重放，不看卡面镜像（屏上拨成冻住不算）。
        return currentStep(c) == STEP_FROZEN;
    }

    private String operator(String operator) {
        return isBlank(operator) ? ShiroUtils.getLoginName() : operator.trim();
    }

    private static boolean intEquals(Integer a, int b) {
        return a != null && a == b;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimOrNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : b;
    }

    private static boolean contains(String haystack, String kw) {
        return haystack != null && haystack.contains(kw);
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(parts[i] == null ? "" : parts[i]);
        }
        return sb.toString();
    }
}
