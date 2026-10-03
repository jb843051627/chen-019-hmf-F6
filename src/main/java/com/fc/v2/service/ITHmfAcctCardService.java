package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.THmfAcctCard;
import com.fc.v2.model.custom.HouseholdAmend;
import com.fc.v2.model.custom.HouseholdFound;
import com.fc.v2.model.custom.HouseholdOpen;
import com.fc.v2.model.custom.HouseholdRequired;
import com.fc.v2.model.custom.HouseholdRoster;
import com.fc.v2.model.custom.HouseholdStep;
import com.fc.v2.model.custom.HouseholdVerdict;

/**
 * 分户账立户单 Service接口
 *
 * 旧读法与增删改签名原样留着；今年钉死的立户口径全部另开口子（openHousehold 一族），
 * 合规不合规、眼下处在哪一步、能不能再迈/退、必填哪几栏，都只由服务层这几条方法回话，
 * 接口层与前端不留第二条绕开它的判路。
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITHmfAcctCardService {

    /** 按主键查询 */
    THmfAcctCard selectTHmfAcctCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<THmfAcctCard> selectTHmfAcctCardList(Wrapper<THmfAcctCard> queryWrapper);

    /** 新增（旧签名原样留着；新立户一律走 {@link #openHousehold}，此口不再作合规判路） */
    int insertTHmfAcctCard(THmfAcctCard record);

    /** 修改（旧签名原样留着；改面/换册/迈步一律走服务层新口子，此口不作合规判路） */
    int updateTHmfAcctCard(THmfAcctCard record);

    /** 批量删除（旧签名原样留着；冻住的单删不掉，要动只能另起一张） */
    int deleteTHmfAcctCardByIds(String ids);

    /** 按主键删除（旧签名原样留着） */
    int deleteTHmfAcctCardById(Long id);

    // ================================================================ 今年钉死的立户口径

    /**
     * 按项目类别当场回话必填哪几栏（该不该交、交哪几样跟着类别走）。
     * 商品房：预售证载号 + 测绘报告编号；保障性住房：配租配售文号 + 承办科室；
     * 直管公房：产权代管单位 + 拨交文据。类别不认得时回 null。
     */
    HouseholdRequired requiredFields(String siteType);

    /**
     * 立户（今年从这一步管起来）：一户一张档、一张档不两处挂、一册底册不两家项目共用。
     * 户号与证载号一律系统发出，人自家拟的号递进来不入库并点名；类别空、或选了商品房却
     * 没交预售证载号等凭据缺样，户进不了库，回执点名缺哪一栏；建面零/负、应缴相关口径不
     * 正、起写格晚于止写格，先确认才收；挂靠只认底册代号，已迁出的底册不再收新户；
     * 同册同门牌（同户）头一张还在走时第二张叠不上。收费档次与应缴额由系统同一回折出落卡。
     */
    HouseholdVerdict openHousehold(HouseholdOpen open);

    /**
     * 唯一迈步入口（往前核对/冻住、往后退回都走这里）：
     * 当前停在哪一步由系统照步痕重放数出，屏上填的、经办人记的都不算数；步子不得跨；
     * 未到冻住可退回一步重来，上一趟记录原样留着不与后补的笔混；推进到冻住即到尽头，
     * 添不进、改不动、整张撤不掉。走到已核对即锁死校验码，此后谁也覆不上去。
     */
    HouseholdVerdict pushHousehold(HouseholdStep step);

    /**
     * 建面改了：旧版转历史层（名录不露、不销毁），另起新版，版次序+1、校验码重算，
     * 收费档次与应缴额照新面同一回重折。三个重算时点之一。只许对现行未冻住的版次办。
     */
    HouseholdVerdict amendArea(HouseholdAmend amend);

    /**
     * 换挂靠的底册：按新底册三条界与类别重新折档、重新验凭据、重算应缴额，同样另起新版。
     * 三个重算时点之一。新底册已迁出则挂不上去。
     */
    HouseholdVerdict amendBook(HouseholdAmend amend);

    /**
     * 查户口：户号、门牌、证载号三处都开，从头匹配与中间片段匹配都给得出；
     * 命中条直接带出所属底册与所在档次。只摆本级（siteNos 圈定）有权看的现行户，
     * 别级的不同屏供挑；本级一个没有就空着，不铺假行；库里没有就不造。
     */
    List<HouseholdFound> searchHouseholds(String keyword, List<String> siteNos);

    /**
     * 本级名录三笔：在册（在走）、冻住、现行，由同一场逐户点算写出（在册+冻住=现行）。
     * 屏上那三笔是不是同一回数出来的，以此回话为准；不补空行、不抹在场旧版次。
     */
    HouseholdRoster rosterOf(List<String> siteNos);

    /** 翻某一户（按系统户号）的历次版次：名录只露现行，旧版在此翻得到，按版次序升序。 */
    List<THmfAcctCard> historyOf(String billNo);

    /**
     * 真要退回旧版：只把指定旧版换回现行（该版重新露于名录），当下现行版沉回历史层，
     * 旧数据不覆新的；冻结态/校验码各版各码不串。冻住过的版不许再换回。
     */
    HouseholdVerdict revertVersion(Long cardId, String operator);

    /**
     * 早年纸簿旧单迁入：步数、版次序、校验码空着的旧单这一套流程也接得住——
     * 补版次序、重算校验码、照纸上的进展补起步痕（在走的旧单不能误当成已停的），
     * 不重发户号（沿用纸上的号，登记为系统号）。缺挂靠底册或底册已迁出的另回话拒收。
     */
    HouseholdVerdict importLegacy(THmfAcctCard paper, String operator);
}
