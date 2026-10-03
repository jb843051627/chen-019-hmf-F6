package com.fc.v2.hmf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fc.v2.model.auto.THmfAcctCard;
import com.fc.v2.model.auto.THmfAcctStepLog;
import com.fc.v2.model.auto.THmfArch;
import com.fc.v2.model.custom.HouseholdAmend;
import com.fc.v2.model.custom.HouseholdFound;
import com.fc.v2.model.custom.HouseholdOpen;
import com.fc.v2.model.custom.HouseholdRequired;
import com.fc.v2.model.custom.HouseholdRoster;
import com.fc.v2.model.custom.HouseholdStep;
import com.fc.v2.model.custom.HouseholdVerdict;
import com.fc.v2.service.impl.THmfAcctCardServiceImpl;

/**
 * 《立户口径》逐条核对：
 * 一户一档不两处挂、编号系统发、按类别凭据、三条界压线取上层、档与钱同一回折、
 * 步痕重放定当前步、不跨步、退回旧拨留痕、已核对锁码冻住即尽头、
 * 改面/换册另起新版旧版沉历史层、校验码各版各码、冻住名额空出、迁出册不收新户旧户仍在册、
 * 名录三笔同源、查户三码片段且只摆本级、旧簿迁入补痕、代号户号一对只一张。
 */
class HouseholdRegisterTest {

    private static final int PENDING = 0;
    private static final int CHECKED = 1;
    private static final int FROZEN = 2;

    private static final int LAYER_CUR = 0;
    private static final int LAYER_HIS = 1;

    private THmfAcctCardServiceImpl service;
    private FakeAcctCardMapper cards;
    private FakeAcctStepLogMapper logs;
    private FakeArchMapper archs;

    private long clock = 1_700_000_000_000L;

    @BeforeEach
    void setUp() throws Exception {
        this.service = new THmfAcctCardServiceImpl() {
            @Override
            protected Date nowTime() {
                return new Date(clock);
            }
        };
        this.cards = new FakeAcctCardMapper();
        this.logs = new FakeAcctStepLogMapper();
        this.archs = new FakeArchMapper();
        inject("baseMapper", cards);
        inject("hmfAcctStepLogMapper", logs);
        inject("hmfArchMapper", archs);
        // 在册商品房 JZ00；在册保障房 JZA；在册直管公房 JZP；已迁出商品房 JZ01
        arch(0L, "JZ00", "梧桐里苑", "商品房", 0);
        arch(2L, "JZA", "滨河北里", "保障性住房", 0);
        arch(3L, "JZP", "直管老楼", "直管公房", 0);
        arch(1L, "JZ01", "已迁项目", "商品房", 1);
    }

    private void inject(String name, Object value) throws Exception {
        Field f;
        try {
            f = THmfAcctCardServiceImpl.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            f = THmfAcctCardServiceImpl.class.getSuperclass().getDeclaredField(name);
        }
        f.setAccessible(true);
        f.set(service, value);
    }

    private void arch(long id, String no, String name, String type, int status) {
        THmfArch a = new THmfArch();
        a.setId(id);
        a.setSiteNo(no);
        a.setSiteName(name);
        a.setSiteType(type);
        a.setRoadName("澜川市—河湾区—长街上");
        a.setTh1Max(new BigDecimal("90.00"));
        a.setTh2Max(new BigDecimal("144.00"));
        a.setTh3Max(new BigDecimal("300.00"));
        a.setStatus(status);
        a.setDelFlag(0);
        archs.insert(a);
    }

    private Date at(long dayOffset) {
        return new Date(clock + dayOffset * 24L * 60 * 60 * 1000);
    }

    private HouseholdOpen open(String siteNo, String door, String qty) {
        HouseholdOpen o = new HouseholdOpen();
        o.setSiteNo(siteNo);
        o.setDoorNo(door);
        o.setQty(new BigDecimal(qty));
        o.setPaperCertNo("YS-PRE-001");
        o.setPaperRefNo("CH-REPORT-001");
        o.setWriteStart(at(0));
        o.setWriteEnd(at(1));
        o.setOperator("柜员甲");
        o.setNow(at(0));
        return o;
    }

    private HouseholdStep step(Long id, int action) {
        HouseholdStep s = new HouseholdStep();
        s.setCardId(id);
        s.setAction(action);
        s.setOperator("柜员甲");
        s.setNow(new Date(clock));
        return s;
    }

    private HouseholdStep fwd(Long id) {
        clock += 24L * 60 * 60 * 1000;
        return step(id, 1);
    }

    private HouseholdStep back(Long id) {
        clock += 24L * 60 * 60 * 1000;
        return step(id, -1);
    }

    // ---------------------------------------------------------------- 折档与发号

    @Test
    void open_issuesNumbers_andFoldsGradeWithBoundaryTakingUpper() {
        assertGrade("80.00", 1, "4800.00");
        assertGrade("90.00", 2, "7200.00");   // 恰好压低界，算跨过去取上一层
        assertGrade("143.99", 2, "11519.20");
        assertGrade("144.00", 3, "14400.00"); // 压中界取上层
        assertGrade("299.99", 3, "29999.00");
        assertGrade("300.00", 4, "36000.00"); // 压高界取顶层
        assertGrade("301.00", 4, "36120.00");
    }

    private void assertGrade(String qty, int grade, String fine) {
        HouseholdVerdict v = service.openHousehold(open("JZ00", "门" + qty, qty));
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, v.getCode(), v.getMessage());
        THmfAcctCard c = v.getCard();
        assertEquals(grade, c.getGradeLevel().intValue());
        assertEquals(0, new BigDecimal(fine).compareTo(c.getFineAmt()));
        assertEquals(0, c.getFineAmt().compareTo(c.getQty().multiply(c.getRateStd())),
                "应缴额与档次必须同一回算的数");
        assertTrue(c.getBillNo().startsWith("JZ00-H"), "户号系统发出：" + c.getBillNo());
        assertTrue(c.getCertNo().startsWith("YS-"), "证载号系统发出");
        assertEquals(c.getBillNo(), c.getChainNo());
        assertEquals(1, c.getNodeNo().intValue());
        assertEquals(LAYER_CUR, c.getLayerNo().intValue());
        assertEquals(PENDING, c.getStatus().intValue());
    }

    @Test
    void open_twoHouseholds_getDistinctSystemNumbers() {
        THmfAcctCard a = service.openHousehold(open("JZ00", "3-201", "80")).getCard();
        THmfAcctCard b = service.openHousehold(open("JZ00", "3-202", "80")).getCard();
        assertFalse(a.getBillNo().equals(b.getBillNo()));
        assertFalse(a.getCertNo().equals(b.getCertNo()));
    }

    // ---------------------------------------------------------------- 进门校验

    @Test
    void open_humanDraftedNumbers_rejected_andNamed() {
        HouseholdOpen o = open("JZ00", "3-201", "80");
        o.setBillNo("SELF-MADE-1");
        HouseholdVerdict v = service.openHousehold(o);
        assertEquals(HouseholdVerdict.CODE_REJECTED, v.getCode());
        assertTrue(v.getMessage().contains("户号"));
        assertEquals(0, cards.all().size(), "人拟号不入库");

        HouseholdOpen o2 = open("JZ00", "3-201", "80");
        o2.setCertNo("SELF-CERT");
        v = service.openHousehold(o2);
        assertEquals(HouseholdVerdict.CODE_REJECTED, v.getCode());
        assertTrue(v.getMessage().contains("证载号"));
        assertEquals(0, cards.all().size());
    }

    @Test
    void open_missingFields_namedInReceipt() {
        HouseholdOpen blankSite = open("JZ00", "3-201", "80");
        blankSite.setSiteNo("  ");
        assertTrue(service.openHousehold(blankSite).getMessage().contains("底册代号"));

        HouseholdOpen unknown = open("JZ99", "3-201", "80");
        HouseholdVerdict v = service.openHousehold(unknown);
        assertEquals(HouseholdVerdict.CODE_REJECTED, v.getCode(), "名字像不算数，只认代号");
        assertTrue(v.getMessage().contains("底册代号"));

        HouseholdOpen noDoor = open("JZ00", "  ", "80");
        assertTrue(service.openHousehold(noDoor).getMessage().contains("门牌"));

        HouseholdOpen zero = open("JZ00", "3-201", "0");
        assertTrue(service.openHousehold(zero).getMessage().contains("建筑面积"));
        HouseholdOpen neg = open("JZ00", "3-201", "-5");
        assertEquals(HouseholdVerdict.CODE_REJECTED, service.openHousehold(neg).getCode());

        HouseholdOpen noPaper1 = open("JZ00", "3-201", "80");
        noPaper1.setPaperCertNo(" ");
        assertTrue(service.openHousehold(noPaper1).getMessage().contains("预售证载号"));
        HouseholdOpen noPaper2 = open("JZ00", "3-201", "80");
        noPaper2.setPaperRefNo(null);
        assertTrue(service.openHousehold(noPaper2).getMessage().contains("测绘报告编号"));

        HouseholdOpen badDates = open("JZ00", "3-201", "80");
        badDates.setWriteStart(at(5));
        badDates.setWriteEnd(at(4));
        assertTrue(service.openHousehold(badDates).getMessage().contains("起写"));

        assertEquals(0, cards.all().size(), "全部拒收，一笔不入库");
    }

    @Test
    void requiredFields_followProjectCategory() {
        HouseholdRequired c = service.requiredFields("商品房");
        assertEquals(Arrays.asList("预售证载号", "测绘报告编号"), c.getLabels());
        assertEquals(Arrays.asList("配租配售文号", "承办科室"),
                service.requiredFields("保障性住房").getLabels());
        assertEquals(Arrays.asList("产权代管单位", "拨交文据"),
                service.requiredFields("直管公房").getLabels());
        assertNull(service.requiredFields("别的类"));
        assertNull(service.requiredFields(null));
    }

    @Test
    void open_eachCategory_requiresItsOwnPapers() {
        HouseholdOpen aff = open("JZA", "A-1", "80");
        aff.setPaperCertNo("PZ-2026-9");
        aff.setPaperRefNo("保障科");
        HouseholdVerdict va = service.openHousehold(aff);
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, va.getCode(), va.getMessage());

        HouseholdOpen affBad = open("JZA", "A-2", "80");
        affBad.setPaperCertNo("YS-PRE-001");
        affBad.setPaperRefNo(null);
        assertTrue(service.openHousehold(affBad).getMessage().contains("承办科室"));

        HouseholdOpen pub = open("JZP", "P-1", "80");
        pub.setPaperCertNo("房管中心");
        pub.setPaperRefNo("BJ-2026-3");
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, service.openHousehold(pub).getCode());

        HouseholdOpen pubBad = open("JZP", "P-2", "80");
        pubBad.setPaperCertNo(null);
        pubBad.setPaperRefNo("BJ");
        assertTrue(service.openHousehold(pubBad).getMessage().contains("产权代管单位"));
    }

    @Test
    void open_ontoMovedArch_rejected_butOldHouseholdsRemain() {
        // 早前挂在迁出册底下的户仍旧在册、查得到（直接造一张旧户挂着）
        THmfAcctCard old = legacyCard("JZ01", "JZ01-H0001", "旧门1", "80.00", CHECKED);
        HouseholdOpen o = open("JZ01", "新门1", "80");
        HouseholdVerdict v = service.openHousehold(o);
        assertEquals(HouseholdVerdict.CODE_ARCH_GONE, v.getCode());
        assertEquals(1, cards.all().size(), "迁出册不收新户，新户没挂上去");
        // 旧户仍在册：查得到、名录仍数它
        List<HouseholdFound> hits = service.searchHouseholds("旧门", Collections.singletonList("JZ01"));
        assertEquals(1, hits.size());
        assertEquals(1, service.rosterOf(Collections.singletonList("JZ01")).getCurrentCount());
    }

    // ---------------------------------------------------------------- 一户一在办单

    @Test
    void sameHousehold_secondSheetBlockedWhileRunning_evenAcrossWindows() {
        Long id = service.openHousehold(open("JZ00", "3-201", "80")).getCard().getId();
        // 两个窗口同日各报一遍
        HouseholdVerdict again = service.openHousehold(open("JZ00", "3-201", "95"));
        assertEquals(HouseholdVerdict.CODE_DUP, again.getCode());
        assertEquals(1, cards.all().size(), "库里仍旧只那一张");
        // 核对过后仍在走（已核对未冻住），换人重报也不许叠
        service.pushHousehold(fwd(id));
        assertEquals(HouseholdVerdict.CODE_DUP, service.openHousehold(
                withOperator(open("JZ00", "3-201", "95"), "柜员乙")).getCode());
    }

    private HouseholdOpen withOperator(HouseholdOpen o, String op) {
        o.setOperator(op);
        return o;
    }

    @Test
    void afterFrozen_slotFreed_sameHouseholdOpensNextVersion() {
        THmfAcctCard first = service.openHousehold(open("JZ00", "3-201", "80")).getCard();
        Long id = first.getId();
        service.pushHousehold(fwd(id));   // 已核对
        service.pushHousehold(fwd(id));   // 已冻住
        assertEquals(0, service.rosterOf(Collections.singletonList("JZ00")).getActiveCount());

        HouseholdVerdict v = service.openHousehold(open("JZ00", "3-201", "100"));
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, v.getCode(), "冻住占过的名额空出来，同户接得上新单");
        THmfAcctCard second = v.getCard();
        assertEquals(first.getBillNo(), second.getBillNo(), "同一户号续下一张");
        assertEquals(first.getCertNo(), second.getCertNo());
        assertEquals(2, second.getNodeNo().intValue());
        assertEquals(LAYER_CUR, second.getLayerNo().intValue());
        assertEquals(PENDING, second.getStatus().intValue());
        // 旧那张沉历史层，不销毁
        THmfAcctCard oldRow = cards.selectById(id);
        assertEquals(LAYER_HIS, oldRow.getLayerNo().intValue());
        assertEquals(FROZEN, oldRow.getStatus().intValue(), "旧数据原样留着");
    }

    // ---------------------------------------------------------------- 迈步状态机

    @Test
    void chain_pendingCheckedFrozen_lockCodeAtChecked() {
        Long id = service.openHousehold(open("JZ00", "3-201", "80")).getCard().getId();
        assertNull(cards.selectById(id).getContent(), "待核对还没有校验码");

        HouseholdVerdict c = service.pushHousehold(fwd(id));
        assertEquals(CHECKED, c.getStep());
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, c.getCode());
        String code = cards.selectById(id).getContent();
        assertNotNull(code);
        assertEquals(16, code.length());

        HouseholdVerdict f = service.pushHousehold(fwd(id));
        assertEquals(FROZEN, f.getStep());

        // 冻住即尽头：推不动、退不回、改不动、删不走（服务口子）
        assertEquals(HouseholdVerdict.CODE_FROZEN, service.pushHousehold(fwd(id)).getCode());
        assertEquals(HouseholdVerdict.CODE_FROZEN, service.pushHousehold(back(id)).getCode());
        HouseholdAmend a = new HouseholdAmend();
        a.setCardId(id);
        a.setQty(new BigDecimal("120"));
        a.setOperator("柜员甲");
        assertEquals(HouseholdVerdict.CODE_FROZEN, service.amendArea(a).getCode());
    }

    @Test
    void noStepJumping_andSameStepSecondTimeStays() {
        Long id = service.openHousehold(open("JZ00", "3-201", "80")).getCard().getId();
        HouseholdStep jump = step(id, 1);
        jump.setExpectStep(FROZEN);
        assertEquals(HouseholdVerdict.CODE_STAY, service.pushHousehold(jump).getCode(), "步子不得跨");

        service.pushHousehold(fwd(id)); // 已核对
        HouseholdStep same = step(id, 1);
        same.setExpectStep(CHECKED);
        assertEquals(HouseholdVerdict.CODE_STAY, service.pushHousehold(same).getCode(), "同步第二遍不另起一行");

        // 待核对退无可退（新单）；已核对可退一步
        Long id2 = service.openHousehold(open("JZ00", "3-202", "80")).getCard().getId();
        assertEquals(HouseholdVerdict.CODE_STAY, service.pushHousehold(back(id2)).getCode());
    }

    @Test
    void rollbackOneStep_oldRoundKept_newRoundRestarts() {
        Long id = service.openHousehold(open("JZ00", "3-201", "80")).getCard().getId();
        service.pushHousehold(fwd(id)); // 进已核对 round1
        String code = cards.selectById(id).getContent();

        HouseholdVerdict back = service.pushHousehold(back(id));
        assertEquals(PENDING, back.getStep());
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, back.getCode());

        List<THmfAcctStepLog> cardLogs = logsOf(id);
        assertEquals(2, cardLogs.size(), "过口一行 + 退回一行，旧记录原样留着");
        assertEquals(0, cardLogs.get(0).getAction().intValue(), "头一行是进已核对的过口");
        assertEquals(CHECKED, cardLogs.get(0).getStepNo().intValue());
        // 退回行记从已核对(1)收回，拨次为旧拨 1
        assertEquals(1, cardLogs.get(1).getAction().intValue());
        assertEquals(CHECKED, cardLogs.get(1).getStepNo().intValue());
        assertEquals(1, cardLogs.get(1).getRoundNo().intValue());

        // 重新核对：新一拨过口（round2），上一趟的行不与后补的笔混；码仍是同一把锁
        HouseholdVerdict recheck = service.pushHousehold(fwd(id));
        assertEquals(CHECKED, recheck.getStep());
        assertEquals(code, cards.selectById(id).getContent(), "同一版重走，锁码不变");
        THmfAcctStepLog secondPass = logsOf(id).get(2);
        assertEquals(2, secondPass.getRoundNo().intValue(), "重走另起一拨");
    }

    @Test
    void currentStepAlwaysReplayed_paperStatusIgnored() {
        Long id = service.openHousehold(open("JZ00", "3-201", "80")).getCard().getId();
        service.pushHousehold(fwd(id)); // 真在已核对
        // 屏上/纸面被人手拨成冻住
        THmfAcctCard tampered = cards.selectById(id);
        tampered.setStatus(FROZEN);
        cards.updateById(tampered);

        // 往后退：系统照步痕重放认出还在已核对，允许收回待核对
        HouseholdVerdict v = service.pushHousehold(back(id));
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, v.getCode());
        assertEquals(PENDING, v.getStep(), "纸档不算数，步痕说了算");
        assertEquals(PENDING, cards.selectById(id).getStatus().intValue(), "镜像被校正");
    }

    // ---------------------------------------------------------------- 改面 / 换册

    @Test
    void amendArea_raisesNewVersion_recomputesGradeAndCode_oldVersionSinks() {
        THmfAcctCard v1 = service.openHousehold(open("JZ00", "3-201", "80")).getCard();
        service.pushHousehold(fwd(v1.getId())); // v1 已核对锁码
        String code1 = v1.getContent();

        HouseholdAmend a = new HouseholdAmend();
        a.setCardId(v1.getId());
        a.setQty(new BigDecimal("150")); // 跨过中界，落到第3层
        a.setOperator("柜员甲");
        HouseholdVerdict v = service.amendArea(a);
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, v.getCode(), v.getMessage());
        THmfAcctCard v2 = v.getCard();
        assertEquals(2, v2.getNodeNo().intValue());
        assertEquals(3, v2.getGradeLevel().intValue());
        assertEquals(0, new BigDecimal("15000.00").compareTo(v2.getFineAmt()));
        assertEquals(LAYER_CUR, v2.getLayerNo().intValue());
        assertNull(v2.getContent(), "新版未核对，码空着待重算");
        assertEquals(v1.getBillNo(), v2.getBillNo());
        assertEquals(LAYER_HIS, cards.selectById(v1.getId()).getLayerNo().intValue(), "旧版不露不毁");

        // 名录只露新版
        List<THmfAcctCard> current = service.historyOf(v1.getBillNo());
        assertEquals(2, current.size());
        service.pushHousehold(fwd(v2.getId()));
        String code2 = cards.selectById(v2.getId()).getContent();
        assertFalse(code1.equals(code2), "新版新码、旧版旧码，两版共用一个码即为错");
        assertEquals(code1, cards.selectById(v1.getId()).getContent(), "旧版旧码原样留着");
    }

    @Test
    void amend_rejectsZeroQty_noChange_andRequiresRunning() {
        THmfAcctCard v1 = service.openHousehold(open("JZ00", "3-201", "80")).getCard();
        HouseholdAmend zero = new HouseholdAmend();
        zero.setCardId(v1.getId());
        zero.setQty(new BigDecimal("0"));
        assertEquals(HouseholdVerdict.CODE_REJECTED, service.amendArea(zero).getCode());

        HouseholdAmend none = new HouseholdAmend();
        none.setCardId(v1.getId());
        assertEquals(HouseholdVerdict.CODE_REJECTED, service.amendArea(none).getCode());
        assertEquals(HouseholdVerdict.CODE_REJECTED, service.amendBook(none).getCode());

        service.pushHousehold(fwd(v1.getId()));
        service.pushHousehold(fwd(v1.getId())); // 冻住
        HouseholdAmend a = new HouseholdAmend();
        a.setCardId(v1.getId());
        a.setQty(new BigDecimal("120"));
        assertEquals(HouseholdVerdict.CODE_FROZEN, service.amendArea(a).getCode());
    }

    @Test
    void amendBook_reFoldsByNewArch_requiresNewCategoryPapers() {
        THmfAcctCard v1 = service.openHousehold(open("JZ00", "3-201", "301")).getCard(); // 商品房第4层
        // 换去保障房：新类别凭据缺样先拒
        HouseholdAmend bad = new HouseholdAmend();
        bad.setCardId(v1.getId());
        bad.setSiteNo("JZA");
        bad.setPaperCertNo(null);
        bad.setPaperRefNo(null);
        HouseholdVerdict rv = service.amendBook(bad);
        assertEquals(HouseholdVerdict.CODE_REJECTED, rv.getCode());
        assertTrue(rv.getMessage().contains("配租配售文号"));

        HouseholdAmend ok = new HouseholdAmend();
        ok.setCardId(v1.getId());
        ok.setSiteNo("JZA");
        ok.setPaperCertNo("PZ-9");
        ok.setPaperRefNo("保障科");
        ok.setOperator("柜员甲");
        HouseholdVerdict v = service.amendBook(ok);
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, v.getCode(), v.getMessage());
        THmfAcctCard v2 = v.getCard();
        assertEquals("JZA", v2.getSiteNo());
        assertEquals("保障性住房", v2.getSiteType());
        assertEquals(4, v2.getGradeLevel().intValue());
        assertEquals(0, new BigDecimal("36120.00").compareTo(v2.getFineAmt()));
        assertEquals(2, v2.getNodeNo().intValue());
        // 旧版仍挂在 JZ00 历史层
        assertEquals("JZ00", cards.selectById(v1.getId()).getSiteNo());
        assertEquals(LAYER_HIS, cards.selectById(v1.getId()).getLayerNo().intValue());

        // 换去已迁出册：挂不上去
        HouseholdAmend gone = new HouseholdAmend();
        gone.setCardId(v2.getId());
        gone.setSiteNo("JZ01");
        gone.setPaperCertNo("X");
        gone.setPaperRefNo("Y");
        assertEquals(HouseholdVerdict.CODE_ARCH_GONE, service.amendBook(gone).getCode());
    }

    @Test
    void revertVersion_onlySwapsThatVersion_oldDoesNotOverwriteNew() {
        THmfAcctCard v1 = service.openHousehold(open("JZ00", "3-201", "80")).getCard();
        service.pushHousehold(fwd(v1.getId()));
        String code1 = cards.selectById(v1.getId()).getContent();
        HouseholdAmend a = new HouseholdAmend();
        a.setCardId(v1.getId());
        a.setQty(new BigDecimal("150"));
        THmfAcctCard v2 = service.amendArea(a).getCard();

        HouseholdVerdict back = service.revertVersion(v1.getId(), "柜员甲");
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, back.getCode());
        assertEquals(LAYER_CUR, cards.selectById(v1.getId()).getLayerNo().intValue());
        assertEquals(LAYER_HIS, cards.selectById(v2.getId()).getLayerNo().intValue());
        assertEquals(code1, cards.selectById(v1.getId()).getContent(), "旧版旧码不被覆");
        assertEquals(2, service.historyOf(v1.getBillNo()).size(), "两张都还在，旧不覆新");
    }

    // ---------------------------------------------------------------- 名录三笔

    @Test
    void roster_countsFromSameCounting_openPlusOne_freezeMinusOne() {
        String site = "JZ00";
        THmfAcctCard a = service.openHousehold(open(site, "3-201", "80")).getCard();
        service.openHousehold(open(site, "3-202", "80"));
        HouseholdRoster r = service.rosterOf(Collections.singletonList(site));
        assertEquals(2, r.getActiveCount());
        assertEquals(0, r.getFrozenCount());
        assertEquals(2, r.getCurrentCount());
        assertTrue(r.isMatched());

        service.pushHousehold(fwd(a.getId()));
        service.pushHousehold(fwd(a.getId())); // a 冻住：在册减一
        r = service.rosterOf(Collections.singletonList(site));
        assertEquals(1, r.getActiveCount());
        assertEquals(1, r.getFrozenCount());
        assertEquals(2, r.getCurrentCount(), "在册+冻住=现行，同一回点算");
    }

    // ---------------------------------------------------------------- 查户

    @Test
    void search_byBillDoorCert_prefixAndFragment_bringsSiteAndGrade() {
        THmfAcctCard a = service.openHousehold(open("JZ00", "梧桐里3-201", "80")).getCard();
        service.openHousehold(open("JZ00", "别的路9-1", "80"));

        // 户号从头匹配
        List<HouseholdFound> byBill = service.searchHouseholds(a.getBillNo().substring(0, 6),
                Collections.singletonList("JZ00"));
        assertEquals(2, byBill.size());
        // 门牌中间片段匹配
        List<HouseholdFound> byDoor = service.searchHouseholds("3-20", Collections.singletonList("JZ00"));
        assertEquals(1, byDoor.size());
        HouseholdFound hit = byDoor.get(0);
        assertEquals(a.getBillNo(), hit.getBillNo());
        assertEquals("JZ00", hit.getSiteNo(), "命中直接带出所属底册");
        assertEquals("梧桐里苑", hit.getSiteName());
        assertEquals(1, hit.getGradeLevel().intValue(), "带出所在层数，不用再抄");
        assertEquals(PENDING, hit.getStep().intValue());
        // 证载号匹配
        assertEquals(1, service.searchHouseholds(a.getCertNo(), Collections.singletonList("JZ00")).size());
    }

    @Test
    void search_onlyShowsCurrentLevel_noFakeRows_currentVersionsOnly() {
        THmfAcctCard a = service.openHousehold(open("JZ00", "3-201", "80")).getCard();
        service.pushHousehold(fwd(a.getId()));
        HouseholdAmend am = new HouseholdAmend();
        am.setCardId(a.getId());
        am.setQty(new BigDecimal("150"));
        service.amendArea(am); // v1 沉历史层，v2 现行；同户号同门牌

        // 只摆现行那一张，旧版不与新版同屏供挑
        List<HouseholdFound> hits = service.searchHouseholds("3-201", Collections.singletonList("JZ00"));
        assertEquals(1, hits.size());
        assertEquals(2, hits.get(0).getNodeNo().intValue());

        // 别级的户不同屏：JZ99 范围给空集合 / 别的代号都摆不出
        assertEquals(0, service.searchHouseholds("3-201", Collections.singletonList("JZA")).size());
        assertEquals(0, service.searchHouseholds("3-201", Collections.emptyList()).size());
        assertEquals(0, service.searchHouseholds("库里没有的号", Collections.singletonList("JZ00")).size(),
                "不造没立过的户");
    }

    // ---------------------------------------------------------------- 纸簿旧单迁入

    @Test
    void legacy_pendingEmptyFields_doesNotBreak_treatedRunning() {
        THmfAcctCard paper = new THmfAcctCard();
        paper.setBillNo("FH-OLD-1");
        paper.setSiteNo("JZ00");
        paper.setDoorNo("老门1");
        paper.setQty(new BigDecimal("80.00"));
        paper.setPaperCertNo("老预售号");
        paper.setPaperRefNo("老测绘号");
        // nodeNo / content / status 全空
        HouseholdVerdict v = service.importLegacy(paper, "迁入员");
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, v.getCode(), v.getMessage());
        THmfAcctCard c = v.getCard();
        assertEquals(1, c.getNodeNo().intValue(), "版次序补上");
        assertEquals(PENDING, v.getStep(), "步数空着认在走，不误当已停");
        assertEquals(0, logsOf(c.getId()).size(), "待核对的旧单无过口痕，但不报错");
        assertNull(c.getContent(), "待核对未到锁码，码仍空，走到已核对再锁");
        assertEquals(0, new BigDecimal("4800.00").compareTo(c.getFineAmt()), "档/钱按现界重折，治老钱旧病");
        // 名录把它当在走户
        assertEquals(1, service.rosterOf(Collections.singletonList("JZ00")).getActiveCount());

        // 接着可以正常迈步（一套流程接得住）
        assertEquals(CHECKED, service.pushHousehold(fwd(c.getId())).getStep());
    }

    @Test
    void legacy_checkedAndFrozen_backfilledStepsAndCode() {
        THmfAcctCard checked = paper("FH-OLD-2", "JZ00", "老门2", "95", CHECKED);
        HouseholdVerdict vc = service.importLegacy(checked, "迁入员");
        assertEquals(CHECKED, vc.getStep(), "纸上已核对，补痕后认在已核对（在走，不是冻住）");
        THmfAcctCard cc = vc.getCard();
        List<THmfAcctStepLog> lc = logsOf(cc.getId());
        assertEquals(1, lc.size());
        assertEquals(CHECKED, lc.get(0).getStepNo().intValue());
        assertNotNull(cc.getContent(), "已核对的旧单补锁码");
        // 已核对可退回，说明被认作在走而非冻住
        assertEquals(PENDING, service.pushHousehold(back(cc.getId())).getStep());

        THmfAcctCard frozen = paper("FH-OLD-3", "JZ00", "老门3", "301", FROZEN);
        THmfAcctCard fc = service.importLegacy(frozen, "迁入员").getCard();
        List<THmfAcctStepLog> lf = logsOf(fc.getId());
        assertEquals(2, lf.size(), "核对、冻住两道痕都补齐");
        assertEquals(FROZEN, fc.getStatus().intValue());
        assertEquals(HouseholdVerdict.CODE_FROZEN, service.pushHousehold(fwd(fc.getId())).getCode(),
                "冻住的旧单仍封尽");
        // 老门2 已退回待核对（在册在走），老门3 冻住：三笔同源
        HouseholdRoster r = service.rosterOf(Collections.singletonList("JZ00"));
        assertEquals(1, r.getActiveCount());
        assertEquals(1, r.getFrozenCount());
        assertEquals(2, r.getCurrentCount());
    }

    @Test
    void legacy_dupPairOnlyOne_andMovedOrMissingRejected() {
        THmfAcctCard p1 = paper("FH-DUP", "JZ00", "老门4", "80", PENDING);
        assertEquals(HouseholdVerdict.CODE_ACCEPTED, service.importLegacy(p1, "迁入员").getCode());
        THmfAcctCard p2 = paper("FH-DUP", "JZ00", "老门4", "80", PENDING);
        HouseholdVerdict again = service.importLegacy(p2, "迁入员");
        assertEquals(HouseholdVerdict.CODE_DUP, again.getCode(), "代号+户号一对只一张，不分先后");

        THmfAcctCard gone = paper("FH-G", "JZ01", "老门5", "80", PENDING);
        assertEquals(HouseholdVerdict.CODE_ARCH_GONE, service.importLegacy(gone, "迁入员").getCode());
        THmfAcctCard nosite = paper("FH-N", "  ", "老门6", "80", PENDING);
        assertEquals(HouseholdVerdict.CODE_REJECTED, service.importLegacy(nosite, "迁入员").getCode());
    }

    private THmfAcctCard paper(String bill, String site, String door, String qty, int status) {
        THmfAcctCard p = new THmfAcctCard();
        p.setBillNo(bill);
        p.setSiteNo(site);
        p.setDoorNo(door);
        p.setQty(new BigDecimal(qty));
        p.setPaperCertNo("纸面一");
        p.setPaperRefNo("纸面二");
        p.setStatus(status);
        return p;
    }

    private THmfAcctCard legacyCard(String site, String bill, String door, String qty, int status) {
        THmfAcctCard c = new THmfAcctCard();
        c.setBillNo(bill);
        c.setCertNo("YS-LEGACY-" + bill);
        c.setChainNo(bill);
        c.setDoorNo(door);
        c.setNodeNo(1);
        c.setLayerNo(LAYER_CUR);
        c.setSiteNo(site);
        c.setSiteType("商品房");
        c.setQty(new BigDecimal(qty));
        c.setFineAmt(new BigDecimal("1.00"));
        c.setGradeLevel(1);
        c.setStatus(status);
        c.setDelFlag(0);
        cards.insert(c);
        return c;
    }

    private List<THmfAcctStepLog> logsOf(Long cardId) {
        List<THmfAcctStepLog> all = logs.all();
        java.util.List<THmfAcctStepLog> out = new java.util.ArrayList<>();
        for (THmfAcctStepLog l : all) {
            if (cardId.equals(l.getCardId())) {
                out.add(l);
            }
        }
        out.sort((x, y) -> {
            int c = x.getLogTime().compareTo(y.getLogTime());
            return c != 0 ? c : Long.compare(x.getId(), y.getId());
        });
        return out;
    }
}
