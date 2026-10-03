package com.fc.v2.hmf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.THmfAcctCard;
import com.fc.v2.model.auto.THmfAcctCardLog;
import com.fc.v2.model.auto.THmfArch;
import com.fc.v2.model.custom.AcctCardReceipt;
import com.fc.v2.model.custom.AcctCardRoster;
import com.fc.v2.model.custom.AcctCardVerdict;
import com.fc.v2.model.custom.AcctFieldSpec;
import com.fc.v2.model.custom.HouseholdHit;
import com.fc.v2.model.custom.OpenHouseholdForm;
import com.fc.v2.model.custom.ReviseHouseholdForm;
import com.fc.v2.service.impl.THmfAcctCardServiceImpl;

/**
 * 《立户口径》逐条核对：
 * 一户一张档一档不两处挂、系统发号不收自拟、类别随附凭据点齐、迁出册不收新户老户在册、
 * 零负面积/起止写确认、三线折档压线跨上与应缴额同回算、一户只容一张在走（冻住/打住才腾名）、
 * 版次沉底新码、核对锁码防改、步子照留痕数不看屏填、跨步不认、退回旧趟留存新趟另计、
 * 冻住尽头改撤都不动、册首三数同回且与逐户点相等、找户三栏片段且只摆本级、旧空单不报错不误停。
 */
class AcctCardHouseholdTest {

    private static final int WAIT = 0;
    private static final int CHECKED = 1;
    private static final int FROZEN = 2;

    private THmfAcctCardServiceImpl service;
    private FakeAcctCardMapper cards;
    private FakeAcctCardLogMapper logs;
    private FakeArchMapper archs;

    @BeforeEach
    void setUp() throws Exception {
        this.service = new THmfAcctCardServiceImpl();
        this.cards = new FakeAcctCardMapper();
        this.logs = new FakeAcctCardLogMapper();
        this.archs = new FakeArchMapper();
        inject("baseMapper", cards);
        inject("hmfAcctCardLogMapper", logs);
        inject("hmfArchMapper", archs);

        arch("JZ00", "梧桐里苑", "商品房", 0, "90", "144", "300");
        arch("JZ01", "滨河北里（已迁出）", "保障性住房", 1, "90", "144", "300");
        arch("JZ02", "安泰保障房", "保障性住房", 0, "60", "120", "200");
        arch("JZ03", "旧城直管公房", "直管公房", 0, "50", "100", "180");
    }

    private void inject(String name, Object value) throws Exception {
        Class<?> c = THmfAcctCardServiceImpl.class;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(service, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private void arch(String code, String name, String type, int status, String t1, String t2, String t3) {
        THmfArch a = new THmfArch();
        a.setSiteNo(code);
        a.setSiteName(name);
        a.setSiteType(type);
        a.setStatus(status);
        a.setTh1Max(new BigDecimal(t1));
        a.setTh2Max(new BigDecimal(t2));
        a.setTh3Max(new BigDecimal(t3));
        a.setDelFlag(0);
        archs.insert(a);
    }

    private Date t(long ms) {
        return new Date(ms);
    }

    private OpenHouseholdForm form(String siteNo, String door, String qty) {
        OpenHouseholdForm f = new OpenHouseholdForm();
        f.setSiteNo(siteNo);
        f.setDoorNo(door);
        f.setQty(new BigDecimal(qty));
        f.setWriteStart(t(1_000_000));
        f.setWriteEnd(t(2_000_000));
        f.setPermitNo("YS-" + door);
        f.setSurveyNo("CH-" + door);
        return f;
    }

    private AcctCardReceipt open(String door, String qty) {
        return service.openHousehold(form("JZ00", door, qty));
    }

    // ---- 系统发号；档次/应缴额同一回算 ----

    @Test
    void open_issuesBillAndCertNumbersAndComputesGradeAndAmountInOnePass() {
        AcctCardReceipt rc = open("3-402", "100");
        assertEquals(AcctCardReceipt.CODE_OPENED, rc.getCode());
        THmfAcctCard c = rc.getCard();
        assertNotNull(c.getId());
        assertTrue(c.getBillNo().startsWith("FH"), "户号系统发出");
        assertTrue(c.getCertNo().startsWith("CZ"), "证载号系统发出");
        assertEquals(1, c.getNodeNo());
        assertEquals("JZ00", c.getSiteNo());
        assertEquals("商品房", c.getSiteType());
        assertEquals(2, c.getGradeLevel(), "100㎡：够到低界90没到中界144，落第二层");
        assertEquals(0, new BigDecimal("70.00").compareTo(c.getUnitPrice()));
        assertEquals(0, new BigDecimal("7000.00").compareTo(c.getFineAmt()),
                "应缴额＝本档每平米标准×建面，与档次同一回算");
        assertEquals(WAIT, c.getStatus());
        assertNotNull(c.getContent());
        assertEquals(16, c.getContent().length());
        // 册首三数与逐户点同回给出
        assertEquals(1, rc.getOpenedCount());
        assertEquals(0, rc.getFrozenCount());
        assertEquals(1, rc.getRosterCount());
        assertEquals(1, rc.getPointCount());
    }

    @Test
    void open_formHasNoSlotForBillNoCertNoGradeOrFineAmount() {
        // 不留口子：人自家拟的号、手报的档次和钱，表单上根本没有这几栏。
        assertThrows(NoSuchFieldException.class,
                () -> OpenHouseholdForm.class.getDeclaredField("billNo"));
        assertThrows(NoSuchFieldException.class,
                () -> OpenHouseholdForm.class.getDeclaredField("fineAmt"));
        assertThrows(NoSuchFieldException.class,
                () -> OpenHouseholdForm.class.getDeclaredField("gradeLevel"));
    }

    @Test
    void gradeBoundaries_boundaryCountsAsCrossedToUpperGrade() {
        // 90/144/300，恰好压界算跨过去取上面一层
        assertEquals(1, open("d01", "89.99").getCard().getGradeLevel());
        assertEquals(2, open("d02", "90.00").getCard().getGradeLevel());
        assertEquals(2, open("d03", "143.99").getCard().getGradeLevel());
        assertEquals(3, open("d04", "144.00").getCard().getGradeLevel());
        assertEquals(3, open("d05", "299.99").getCard().getGradeLevel());
        assertEquals(4, open("d06", "300.00").getCard().getGradeLevel());
        assertEquals(4, open("d07", "500").getCard().getGradeLevel());
        // 头一层：50×89.99＝4499.50
        assertEquals(0, new BigDecimal("4499.50").compareTo(open("d08", "89.99").getCard().getFineAmt()));
    }

    // ---- 缺栏逐栏点名 ----

    @Test
    void reject_pointsAtEveryMissingFieldByCategory() {
        AcctCardReceipt rc = service.openHousehold(new OpenHouseholdForm());
        assertEquals(AcctCardReceipt.CODE_REJECTED, rc.getCode());
        assertTrue(rc.getMissingFields().contains("siteNo"));
        assertTrue(rc.getMissingFields().contains("doorNo"));
        assertTrue(rc.getMissingFields().contains("qty"));
        assertTrue(rc.getMissingFields().contains("writeStart"));
        assertTrue(rc.getMissingFields().contains("writeEnd"));
        assertTrue(rc.getMessage().contains("siteNo"), "回执点名哪一栏缺");

        // 类别空着（不认得的代号）：户进不了库
        OpenHouseholdForm badCode = form("JZ99", "d1", "100");
        assertEquals(AcctCardReceipt.CODE_REJECTED, service.openHousehold(badCode).getCode());

        // 选了商品房却没交预售证载号
        OpenHouseholdForm noPermit = form("JZ00", "d2", "100");
        noPermit.setPermitNo(null);
        AcctCardReceipt r2 = service.openHousehold(noPermit);
        assertTrue(r2.getMissingFields().contains("permitNo"));
        noPermit.setPermitNo("YS-1");
        noPermit.setSurveyNo(null);
        assertTrue(service.openHousehold(noPermit).getMissingFields().contains("surveyNo"));
    }

    @Test
    void categorySpecificMaterials_affordableAndPublic() {
        OpenHouseholdForm af = form("JZ02", "af1", "100");
        af.setPermitNo(null);
        af.setSurveyNo(null);
        // 保障房要的是配租配售文号与承办科室
        assertTrue(service.openHousehold(af).getMissingFields().contains("allotNo"));
        af.setAllotNo("PZ-1");
        af.setHostDept("住房保障科");
        assertEquals(AcctCardReceipt.CODE_OPENED, service.openHousehold(af).getCode());

        OpenHouseholdForm pf = form("JZ03", "pf1", "80");
        pf.setPermitNo(null);
        pf.setSurveyNo(null);
        // 直管公房要产权代管单位与拨交文据
        assertTrue(service.openHousehold(pf).getMissingFields().contains("ownerUnit"));
        pf.setOwnerUnit("房管一所");
        pf.setGrantDoc("BJ-9");
        assertEquals(AcctCardReceipt.CODE_OPENED, service.openHousehold(pf).getCode());
    }

    @Test
    void requiredFields_answeredByTypePerCall() {
        assertEquals(2, service.requiredFields("商品房").getRequiredFields().length);
        assertEquals("allotNo", service.requiredFields("保障性住房").getRequiredFields()[0]);
        assertEquals("ownerUnit", service.requiredFields("直管公房").getRequiredFields()[0]);
        assertNull(service.requiredFields(null));
        assertNull(service.requiredFields("写字楼"));
    }

    @Test
    void reject_zeroOrNegativeArea() {
        assertTrue(service.openHousehold(form("JZ00", "z1", "0")).getMissingFields().contains("qty.nonPositive"));
        assertTrue(service.openHousehold(form("JZ00", "z2", "-13")).getMissingFields().contains("qty.nonPositive"));
    }

    @Test
    void writeStartAfterEnd_needsExplicitConfirm() {
        OpenHouseholdForm f = form("JZ00", "w1", "100");
        f.setWriteStart(t(5_000_000));
        f.setWriteEnd(t(2_000_000));
        assertTrue(service.openHousehold(f).getMissingFields().contains("writeOrderConfirm"));
        f.setConfirmWriteOrder(true);
        assertEquals(AcctCardReceipt.CODE_OPENED, service.openHousehold(f).getCode());
    }

    // ---- 迁出底册：不收新户，老户在册 ----

    @Test
    void archivedArch_rejectsNewHouseholdButLegacyHousesStayOnRoster() {
        AcctFieldSpec spec = service.requiredFields("保障性住房");
        assertNotNull(spec);
        OpenHouseholdForm f = form("JZ01", "old-1", "80");
        f.setPermitNo(null);
        f.setSurveyNo(null);
        f.setAllotNo("PZ-OLD");
        f.setHostDept("保障科");
        AcctCardReceipt rc = service.openHousehold(f);
        assertEquals(AcctCardReceipt.CODE_REJECTED, rc.getCode());
        assertTrue(rc.getMissingFields().contains("siteNo.archived"), "已迁走的底册挂不上去");

        // 早前挂在它底下的旧户仍旧查得到、仍旧算在册
        THmfAcctCard legacy = new THmfAcctCard();
        legacy.setSiteNo("JZ01");
        legacy.setBillNo("FH-OLD-1");
        legacy.setDoorNo("old-1");
        legacy.setQty(new BigDecimal("80"));
        service.insertTHmfAcctCard(legacy);
        AcctCardRoster roster = service.rosterOf("JZ01");
        assertEquals(1, roster.getOpenedCount(), "底册走了户没跟着没");
        assertEquals(1, roster.getRows().size());
    }

    // ---- 一户只容一张在走 ----

    @Test
    void sameHouseholdSecondCardRejected_frozenHouseholdContinuesByRevisionSameBillNo() {
        AcctCardReceipt first = open("dup-1", "100");
        assertEquals(AcctCardReceipt.CODE_OPENED, first.getCode());
        // 两个窗口各报一遍
        assertEquals(AcctCardReceipt.CODE_REJECTED, open("dup-1", "120").getCode());
        // 不同门牌各立各的
        assertEquals(AcctCardReceipt.CODE_OPENED, open("dup-2", "120").getCode());

        // 头一张走到冻住，名额空出；同门牌接续也不另发新户号（防两套在场），走换版同号接
        Long id = first.getCard().getId();
        assertEquals(CHECKED, service.pushCard(id, "甲", null).getStep());
        assertEquals(FROZEN, service.pushCard(id, "甲", null).getStep());
        assertEquals(AcctCardReceipt.CODE_REJECTED, open("dup-1", "130").getCode());

        ReviseHouseholdForm rf = new ReviseHouseholdForm();
        rf.setId(id);
        rf.setQty(new BigDecimal("130"));
        rf.setPermitNo("YS-dup-1");
        rf.setSurveyNo("CH-dup-1");
        AcctCardReceipt again = service.reviseHousehold(rf);
        assertEquals(AcctCardReceipt.CODE_REVISED, again.getCode());
        assertEquals(first.getCard().getBillNo(), again.getCard().getBillNo(), "同户号接续");
        assertNotEquals(first.getCard().getId(), again.getCard().getId());
    }

    @Test
    void halt_freesTheWalkingSlotWithoutMovingTheCard() {
        AcctCardReceipt first = open("h1", "100");
        Long id = first.getCard().getId();
        AcctCardVerdict v = service.haltCard(id, "乙", "材料待补");
        assertEquals(AcctCardVerdict.CODE_HALTED, v.getCode());
        assertEquals(WAIT, v.getStep(), "打住不挪步");
        // 打住后不进不退
        assertEquals(AcctCardVerdict.CODE_STAY, service.pushCard(id, "乙", null).getCode());
        assertEquals(AcctCardVerdict.CODE_STAY, service.rollbackCard(id, "乙", null).getCode());
        // 要动只能另起一张：同门牌立户仍不许另发新号，走换版另起新版（名额接得上）
        assertEquals(AcctCardReceipt.CODE_REJECTED, open("h1", "110").getCode());
        ReviseHouseholdForm rf = new ReviseHouseholdForm();
        rf.setId(id);
        rf.setQty(new BigDecimal("110"));
        rf.setPermitNo("YS-h1");
        rf.setSurveyNo("CH-h1");
        AcctCardReceipt next = service.reviseHousehold(rf);
        assertEquals(AcctCardReceipt.CODE_REVISED, next.getCode());
        assertEquals(WAIT, next.getCard().getStatus(), "新版从待核对接着走");
        // 名录只露现行新版一张在册，旧打住那张整版沉底，不重复计
        assertEquals(1, service.rosterOf("JZ00").getRosterCount());
    }

    // ---- 步子：照留痕数，跨步不认，退回旧趟留存 ----

    @Test
    void push_walksStepsAndLocksChecksumAtChecked_freezesAtEnd() {
        Long id = open("p1", "100").getCard().getId();
        THmfAcctCard stored = service.selectTHmfAcctCardById(id);
        String codeAtWait = stored.getContent();

        AcctCardVerdict v1 = service.pushCard(id, "甲", null);
        assertEquals(CHECKED, v1.getStep());
        assertEquals(AcctCardVerdict.CODE_MOVED, v1.getCode());
        assertEquals(codeAtWait, service.selectTHmfAcctCardById(id).getContent(), "码随立户定稿，核对只锁不改");

        // 到冻住即尽头
        assertEquals(FROZEN, service.pushCard(id, "甲", null).getStep());
        assertEquals(AcctCardVerdict.CODE_STAY, service.pushCard(id, "甲", null).getCode(), "冻住后添不进");
        assertEquals(AcctCardVerdict.CODE_STAY, service.rollbackCard(id, "甲", null).getCode(), "冻住退不回");
        assertEquals(0, service.updateTHmfAcctCard(stored), "冻住字改不动");
        assertEquals(0, service.deleteTHmfAcctCardById(id), "冻住整张撤不掉");

        AcctCardRoster roster = service.rosterOf("JZ00");
        assertEquals(1, roster.getOpenedCount());
        assertEquals(1, roster.getFrozenCount());
        assertEquals(0, roster.getRosterCount(), "冻住一户减一");
        assertEquals(0, roster.getPointCount(), "册首数与当场逐户点出的相等，同回计算");
    }

    @Test
    void currentStep_replayedFromLogs_screenFilledStatusIgnored() {
        Long id = open("p2", "100").getCard().getId();
        service.pushCard(id, "甲", null);
        // 屏上/经办人把档直接拨成冻住：系统不认，照留痕数还停在已核对
        THmfAcctCard tampered = service.selectTHmfAcctCardById(id);
        tampered.setStatus(FROZEN);
        cards.updateById(tampered);
        AcctCardVerdict v = service.pushCard(id, "甲", null);
        assertEquals(FROZEN, v.getStep(), "照留痕重放：从已核对再迈一步才到冻住");
    }

    @Test
    void lockedChecksum_blocksPushWhenCardNumbersWereTampered() {
        Long id = open("p3", "100").getCard().getId();
        service.pushCard(id, "甲", null); // 已核对，锁死
        THmfAcctCard c = service.selectTHmfAcctCardById(id);
        c.setQty(new BigDecimal("129.00"));    // 偷改面积，档次/钱/码全对不上
        cards.updateById(c);
        assertThrows(IllegalStateException.class, () -> service.pushCard(id, "甲", null),
                "锁死后谁也覆不上数；硬推当场识破");
    }

    @Test
    void rollback_keepsOldRoundAndStartsNewOne() {
        Long id = open("p4", "100").getCard().getId();
        service.pushCard(id, "甲", null); // 推进到已核对（第1趟）
        AcctCardVerdict back = service.rollbackCard(id, "甲", "拿错测绘稿");
        assertEquals(WAIT, back.getStep());
        assertEquals(AcctCardVerdict.CODE_BACK, back.getCode());
        service.pushCard(id, "甲", null); // 重走到已核对（第2趟）

        List<THmfAcctCardLog> trail = logs.all().stream()
                .filter(l -> id.equals(l.getCardId())).collect(Collectors.toList());
        List<THmfAcctCardLog> pushesToChecked = trail.stream()
                .filter(l -> l.getStepNo() == CHECKED && l.getAction() == 0).collect(Collectors.toList());
        assertEquals(2, pushesToChecked.size());
        assertEquals(1, pushesToChecked.get(0).getRoundNo(), "旧一趟留着");
        assertEquals(2, pushesToChecked.get(1).getRoundNo(), "后补的另起一趟，不摆在一起");
        assertEquals("拿错测绘稿", trail.stream().filter(l -> l.getAction() == 1)
                .findFirst().orElseThrow(AssertionError::new).getRemark());
        service.rollbackCard(id, "甲", null); // 再退回头一步待核对
        // 头一步之前无处可退
        assertEquals(AcctCardVerdict.CODE_STAY, service.rollbackCard(id, "甲", null).getCode());
    }

    // ---- 版次：旧版沉底，新码不共码 ----

    @Test
    void revise_standsNewVersionSinksOld_andRecomputesCodeAndAmount() {
        AcctCardReceipt first = open("v1-door", "90");
        THmfAcctCard v1 = first.getCard();
        assertEquals(2, v1.getGradeLevel());
        assertEquals(0, new BigDecimal("6300.00").compareTo(v1.getFineAmt()));

        ReviseHouseholdForm rf = new ReviseHouseholdForm();
        rf.setId(v1.getId());
        rf.setQty(new BigDecimal("200")); // 测绘更正后的新数
        rf.setPermitNo("YS-v1-door");
        rf.setSurveyNo("CH-v1-door");
        AcctCardReceipt rr = service.reviseHousehold(rf);
        assertEquals(AcctCardReceipt.CODE_REVISED, rr.getCode());
        THmfAcctCard v2 = rr.getCard();
        assertEquals(2, v2.getNodeNo());
        assertEquals(v1.getBillNo(), v2.getBillNo(), "户号随户走");
        assertEquals(v1.getCertNo(), v2.getCertNo());
        assertEquals(3, v2.getGradeLevel(), "面积动过层数跟着动");
        assertEquals(0, new BigDecimal("18000.00").compareTo(v2.getFineAmt()), "钱也同一回重算");
        assertNotEquals(v1.getContent(), v2.getContent(), "新版新码、旧版旧码，不共用一个码");

        // 名录只露最新版；旧版在历史层翻得到
        assertEquals(1, service.rosterOf("JZ00").getRows().size());
        THmfAcctCard old = service.selectTHmfAcctCardById(v1.getId());
        assertEquals(1, old.getLayer());
        assertEquals(v1.getContent(), old.getContent(), "旧数据一个字没被覆");

        // 新版不合规则旧版原样不动（这次 v2 已现行，改它）
        ReviseHouseholdForm bad = new ReviseHouseholdForm();
        bad.setId(v2.getId());
        bad.setQty(BigDecimal.ZERO);
        AcctCardReceipt rejected = service.reviseHousehold(bad);
        assertEquals(AcctCardReceipt.CODE_REJECTED, rejected.getCode());
        assertEquals(0, service.selectTHmfAcctCardById(v2.getId()).getLayer());
    }

    @Test
    void revise_canChangeArch_butNewArchMustExistAndBeActive() {        AcctCardReceipt first = open("mv-door", "100");
        ReviseHouseholdForm rf = new ReviseHouseholdForm();
        rf.setId(first.getCard().getId());
        rf.setTargetSiteNo("JZ01"); // 已迁出，挂不上去
        rf.setAllotNo("PZ-1");
        rf.setHostDept("保障科");
        assertTrue(service.reviseHousehold(rf).getMissingFields().contains("siteNo.archived"));

        rf.setTargetSiteNo("JZ02"); // 在册保障房，材料按新册类别交
        AcctCardReceipt moved = service.reviseHousehold(rf);
        assertEquals(AcctCardReceipt.CODE_REVISED, moved.getCode());
        assertEquals("JZ02", moved.getCard().getSiteNo());
        assertEquals("保障性住房", moved.getCard().getSiteType());
        // 新册三线 60/120/200：100㎡ 落第二层
        assertEquals(2, moved.getCard().getGradeLevel());
    }

    @Test
    void revise_allowedOnFrozenCard_byStandingANewVersion() {
        AcctCardReceipt first = open("fz-door", "90");
        Long id = first.getCard().getId();
        service.pushCard(id, "甲", null);
        service.pushCard(id, "甲", null); // 冻住
        ReviseHouseholdForm rf = new ReviseHouseholdForm();
        rf.setId(id);
        rf.setQty(new BigDecimal("144"));
        rf.setPermitNo("YS-fz-door");
        rf.setSurveyNo("CH-fz-door");
        AcctCardReceipt rr = service.reviseHousehold(rf);
        assertEquals(AcctCardReceipt.CODE_REVISED, rr.getCode());
        assertEquals(2, rr.getCard().getNodeNo());
        assertEquals(WAIT, rr.getCard().getStatus(), "新版从待核对重新走");
    }

    @Test
    void restoreVersion_swapsBackOnlyThatVersionWithoutOverwritingPresent() {
        AcctCardReceipt first = open("rs-door", "90");
        THmfAcctCard v1 = first.getCard();
        ReviseHouseholdForm rf = new ReviseHouseholdForm();
        rf.setId(v1.getId());
        rf.setQty(new BigDecimal("200"));
        rf.setPermitNo("YS-rs-door");
        rf.setSurveyNo("CH-rs-door");
        THmfAcctCard v2 = service.reviseHousehold(rf).getCard();
        assertEquals(1, v1.getLayer());
        assertEquals(0, v2.getLayer());

        // 只换回旧那一版：数、版次序、校验码原样；新版不被覆，整版沉底
        THmfAcctCard back = service.restoreHouseholdVersion(v1.getId(), "甲", null);
        assertNotNull(back);
        assertEquals(v1.getId(), back.getId());
        assertEquals(0, back.getLayer());
        assertEquals(1, back.getNodeNo());
        assertEquals(2, back.getGradeLevel(), "换回的是旧版那套数（90㎡→二层）");
        assertEquals(v1.getContent(), back.getContent());
        THmfAcctCard sunk = service.selectTHmfAcctCardById(v2.getId());
        assertEquals(1, sunk.getLayer());
        assertEquals(3, sunk.getGradeLevel(), "新版原封不动，没被旧数据覆");
        // 名录只露换回后的一张
        assertEquals(1, service.rosterOf("JZ00").getRows().size());

        // 不是历史层的换不回；在场冻住/打住的不往回翻
        assertNull(service.restoreHouseholdVersion(back.getId(), "甲", null));
        service.pushCard(back.getId(), "甲", null);
        service.pushCard(back.getId(), "甲", null); // 在场的换回版冻住
        assertNull(service.restoreHouseholdVersion(sunk.getId(), "甲", null));
    }

    // ---- 找户：三栏、片段、只摆本级、不造假 ----

    @Test
    void find_byBillDoorOrCert_prefixOrFragment_withinVisibleArchivesOnly() {
        AcctCardReceipt r = open("长街12号3-402", "100");
        String bill = r.getCard().getBillNo();
        String cert = r.getCard().getCertNo();

        // 户号从头匹配
        List<HouseholdHit> h1 = service.findHousehold(bill.substring(0, 6), Arrays.asList("JZ00"));
        assertEquals(1, h1.size());
        assertEquals("JZ00", h1.get(0).getSiteNo());
        assertEquals("梧桐里苑", h1.get(0).getSiteName());
        assertEquals(2, h1.get(0).getGradeLevel(), "命中直带所在层数");

        // 门牌中间片段
        assertEquals(1, service.findHousehold("3-40", Arrays.asList("JZ00")).size());
        // 证载号片段
        assertEquals(1, service.findHousehold(cert.substring(cert.length() - 5),
                Arrays.asList("JZ00")).size());
        // 别级的户不同屏
        assertEquals(0, service.findHousehold(bill.substring(0, 6), Arrays.asList("JZ01")).size());
        // 本级一个没有就空着；无权看任何一册也空着；不造假行
        assertEquals(0, service.findHousehold("根本没有这一户", Arrays.asList("JZ00")).size());
        assertEquals(0, service.findHousehold(bill, Collections.emptyList()).size());
        assertEquals(0, service.findHousehold(null, Arrays.asList("JZ00")).size());
    }

    @Test
    void find_showsCurrentVersionOnly_notSunkHistory() {
        AcctCardReceipt first = open("his-door", "90");
        String bill = first.getCard().getBillNo();
        ReviseHouseholdForm rf = new ReviseHouseholdForm();
        rf.setId(first.getCard().getId());
        rf.setQty(new BigDecimal("200"));
        rf.setPermitNo("YS-his-door");
        rf.setSurveyNo("CH-his-door");
        service.reviseHousehold(rf);

        List<HouseholdHit> hits = service.findHousehold(bill, Arrays.asList("JZ00"));
        assertEquals(1, hits.size(), "旧版在历史层，不上找户屏，但没被抹掉（库里仍在）");
        assertEquals(3, hits.get(0).getGradeLevel(), "带出的是现行新版");
        assertEquals(2, cards.all().stream().filter(c -> bill.equals(c.getBillNo())).count(),
                "旧版仍在库里，只是沉底");
    }

    // ---- 早年纸质旧单：步数/版次序/校验码空着也接得住 ----

    @Test
    void legacyCards_nullStepNodeChecksum_doNotBlowUpAndAreNotMistakenForStopped() {
        THmfAcctCard walking = new THmfAcctCard();
        walking.setSiteNo("JZ00");
        walking.setBillNo("FH-OLD-WALK");
        walking.setDoorNo("legacy-walk");
        walking.setQty(new BigDecimal("100"));
        walking.setGradeLevel(2);
        walking.setFineAmt(new BigDecimal("7000"));
        // status / nodeNo / content / layer 全空
        service.insertTHmfAcctCard(walking);

        THmfAcctCard checked = new THmfAcctCard();
        checked.setSiteNo("JZ00");
        checked.setBillNo("FH-OLD-CHK");
        checked.setDoorNo("legacy-chk");
        checked.setQty(new BigDecimal("100"));
        checked.setGradeLevel(2);
        checked.setFineAmt(new BigDecimal("7000"));
        checked.setStatus(CHECKED); // 迁来现状：已核对
        service.insertTHmfAcctCard(checked);

        // 在走的旧单查得到、算在册，没被当成停了的
        AcctCardRoster roster = service.rosterOf("JZ00");
        assertEquals(2, roster.getRosterCount());

        // 步子/档次没齐的旧单，核不过但不报错，提示订正另起新版
        THmfAcctCard bare = new THmfAcctCard();
        bare.setSiteNo("JZ00");
        bare.setBillNo("FH-OLD-BARE");
        bare.setDoorNo("legacy-bare");
        service.insertTHmfAcctCard(bare);
        AcctCardVerdict stay = service.pushCard(bare.getId(), "甲", null);
        assertEquals(AcctCardVerdict.CODE_STAY, stay.getCode());
        assertEquals(WAIT, stay.getStep());

        // 已核对的旧单，以迁来现状为基线，再迈一步到冻住；没码当场补定
        AcctCardVerdict frozen = service.pushCard(checked.getId(), "甲", null);
        assertEquals(FROZEN, frozen.getStep());
        assertNotNull(service.selectTHmfAcctCardById(checked.getId()).getContent());
    }
}
