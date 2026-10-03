package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.THmfAcctCard;
import com.fc.v2.model.custom.AcctCardReceipt;
import com.fc.v2.model.custom.AcctCardVerdict;
import com.fc.v2.model.custom.AcctFieldSpec;
import com.fc.v2.model.custom.AcctCardRoster;
import com.fc.v2.model.custom.HouseholdHit;
import com.fc.v2.model.custom.OpenHouseholdForm;
import com.fc.v2.model.custom.ReviseHouseholdForm;

/**
 * 分户账立户单 Service接口。
 *
 * 口径（三句话）：一户一张档，一张档不两处挂，一册底册不两家项目共用。
 * 合不合规、处在哪一档、还能不能迈一步或退一步、必填哪几栏，全由本接口这些方法说了算；
 * 接口层与前端不留第二条绕开的判路。名录进几个数、屏上三笔是否同一回数，
 * 同样以保存方法 {@link #openHousehold} 的回写为准。
 *
 * 旧的增删改查签名原样留着（兼容早年迁库的读法与调用）；另想一种立户走法，
 * 另起 {@link #openHousehold} 这个同义方法去接，不改旧签名。
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITHmfAcctCardService {

    /** 按主键查询（旧读法，签名不动；现行层与历史层都查得到） */
    THmfAcctCard selectTHmfAcctCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理；旧读法签名不动） */
    List<THmfAcctCard> selectTHmfAcctCardList(Wrapper<THmfAcctCard> queryWrapper);

    /** 新增（旧签名保留，新立户请走 {@link #openHousehold}） */
    int insertTHmfAcctCard(THmfAcctCard record);

    /** 修改（旧签名保留；改面积/换底册请走 {@link #reviseHousehold} 另起新版） */
    int updateTHmfAcctCard(THmfAcctCard record);

    /** 批量删除（旧签名保留；冻住的单整张撤不掉） */
    int deleteTHmfAcctCardByIds(String ids);

    /** 按主键删除（旧签名保留；冻住的单整张撤不掉） */
    int deleteTHmfAcctCardById(Long id);

    /**
     * 立户（保存方法，唯一判路）。
     * 户号、证载号由系统发出，人手自拟的不收；类别照底册落，随附凭据按类别点齐；
     * 建面零/负、应缴负数、起写晚于止写又没确认的，整户退回，回执逐栏点名；
     * 已迁出底册不收新户；同底册同户号在走的单只容一张。
     * 收费档次与应缴额同一回算落卡（档次栏只读），回执三笔册首数同回写出。
     */
    AcctCardReceipt openHousehold(OpenHouseholdForm form);

    /**
     * 建面改了／换挂靠底册：不覆旧单，新版立起（版次序+1、校验码重算、从待核对重走），
     * 旧版整版沉历史层，名录只露新版。新版不合规则旧版原样不动。
     */
    AcctCardReceipt reviseHousehold(ReviseHouseholdForm form);

    /**
     * 翻历史层：把某一旧版原样换回为在场版。只换回那一版（版次序、数、校验码都不动），
     * 当前在场的新版不被覆掉，一并沉历史层留着。目标不是历史层或户已冻住在场，回 null。
     */
    THmfAcctCard restoreHouseholdVersion(Long historyId, String operator, String comment);

    /**
     * 单子往前走唯一入口：步子不得跨；当前步由系统照步子留痕数，不听屏上填的档。
     * 推进到已核对即锁死校验码；推进到冻住即尽头（添不进、改不动、撤不掉），并腾出在走名额。
     */
    AcctCardVerdict pushCard(Long id, String operator, String comment);

    /**
     * 还没到冻住，前面的核对可退回一步重来：旧一趟留痕原样留着，不与后补笔摆在一起。
     */
    AcctCardVerdict rollbackCard(Long id, String operator, String comment);

    /**
     * 就地打住：单不进不退，占着的在走名额当场空出（同户号可另立新单）；要动只能另起。
     */
    AcctCardVerdict haltCard(Long id, String operator, String comment);

    /** 某类项目立户必填哪几栏，由服务层按类别当场回话；类别给不出回话 null。 */
    AcctFieldSpec requiredFields(String siteType);

    /** 本级名录：只摆真有的现行户，不铺空行；三笔数与逐户点算同回给出。 */
    AcctCardRoster rosterOf(String siteNo);

    /**
     * 找户：户号、门牌、证载号三处任一处从头或片段命中；命中直带所属底册与所在档次。
     * 只摆本级（visibleSiteNos）有权看的，别级不同屏；本级一个没有就空着，不造假行。
     * visibleSiteNos 为空表示一张都不许看（同样空表）。
     */
    List<HouseholdHit> findHousehold(String keyword, List<String> visibleSiteNos);
}
