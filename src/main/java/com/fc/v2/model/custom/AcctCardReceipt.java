package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.THmfAcctCard;

/**
 * 立户保存方法的唯一回话（立户、换版重算都走它）。
 *
 * 本级名录进几个数全在本回执里、同一回计算给出：
 * 立成一户加一、冻住一户减一，册首记的在册数要与当场逐户点出来的相等，
 * 不许分两回读，更不许页面自己点。缺格整户退回，回执逐栏点名。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class AcctCardReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 头一遍立户：收下 */
    public static final int CODE_OPENED = 1;
    /** 新版立起、旧版整版沉底 */
    public static final int CODE_REVISED = 2;
    /** 不合规：整户不收，一个字不入库，缺栏逐栏点名 */
    public static final int CODE_REJECTED = 3;

    /** 回话情形码，见本类常量 */
    private int code;

    /** 缺的/不合规的栏（页面栏号），整户退回时逐栏点名，不拿一句话搪塞 */
    private List<String> missingFields = new ArrayList<>();

    /** 给柜员看的那句话 */
    private String message;

    /** 落定的那张现行单（系统发出的户号、证载号、算定的档次与应缴额都在上面） */
    private THmfAcctCard card;

    /** 本级底册代号 */
    private String siteNo;

    /** 本级累计立成户数（现行层逐户点，版本不重复计） */
    private int openedCount;

    /** 本级已冻住户数（冻住即从在走名册减一） */
    private int frozenCount;

    /** 名录在册户数＝立成−冻住（册首数） */
    private int rosterCount;

    /** 当场逐户点出来的非冻住户数；与 rosterCount 同一回计算，必须相等 */
    private int pointCount;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public THmfAcctCard getCard() {
        return card;
    }

    public void setCard(THmfAcctCard card) {
        this.card = card;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public int getOpenedCount() {
        return openedCount;
    }

    public void setOpenedCount(int openedCount) {
        this.openedCount = openedCount;
    }

    public int getFrozenCount() {
        return frozenCount;
    }

    public void setFrozenCount(int frozenCount) {
        this.frozenCount = frozenCount;
    }

    public int getRosterCount() {
        return rosterCount;
    }

    public void setRosterCount(int rosterCount) {
        this.rosterCount = rosterCount;
    }

    public int getPointCount() {
        return pointCount;
    }

    public void setPointCount(int pointCount) {
        this.pointCount = pointCount;
    }
}
