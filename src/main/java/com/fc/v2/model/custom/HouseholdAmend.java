package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 更正意图：建面动了、或挂靠底册换了，都在这一个口子办。
 *
 * 这不是在旧单上改字：旧版转历史层（既不留名于名录、也不销毁），另起新版，
 * 版次序+1、校验码重算；收费档次与应缴额照新面/新界同一回重折。
 * 只许对现行、且尚未冻住的版次办；已冻住的单到了尽头，要动只能另起一张。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HouseholdAmend implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 现行版次行主键 */
    private Long cardId;

    /** 新建筑面积（null 表示面不动，只换册；给值须大于 0） */
    private BigDecimal qty;

    /** 新挂靠底册代号（null/空 表示册不动，只改面） */
    private String siteNo;

    /** 换册时按新底册类别补的纸面凭据一（同册改面不用带） */
    private String paperCertNo;

    /** 换册时按新底册类别补的纸面凭据二（同册改面不用带） */
    private String paperRefNo;

    /** 经办人；不带则按当前登录人续名 */
    private String operator;

    /** 落笔时刻，缺省取当前时刻（测试可定点） */
    private Date now;

    /** 备注 */
    private String remark;

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getPaperCertNo() {
        return paperCertNo;
    }

    public void setPaperCertNo(String paperCertNo) {
        this.paperCertNo = paperCertNo;
    }

    public String getPaperRefNo() {
        return paperRefNo;
    }

    public void setPaperRefNo(String paperRefNo) {
        this.paperRefNo = paperRefNo;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getNow() {
        return now;
    }

    public void setNow(Date now) {
        this.now = now;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
