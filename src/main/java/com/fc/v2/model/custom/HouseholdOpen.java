package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 立户意图：窗口递进来的立户单内容。
 *
 * 户号（billNo）、证载号（certNo）两处编号不由人报——即便递进来也不入库，
 * 由服务层退回并点名；这两个字段只为认出“人自家拟的号”而留。
 * 收费档次、应缴额同样不在这里：三条界折档、按每平米标准折钱都由服务层同一回算出。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HouseholdOpen implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 所属底册代号（挂靠只认代号，名字读起来像不算数） */
    private String siteNo;

    /** 门牌（同册同门牌再报一遍只认头一张） */
    private String doorNo;

    /** 本户建筑面积（平方米，必须大于 0） */
    private BigDecimal qty;

    /** 纸面凭据一：商品房=预售证载号；保障房=配租配售文号；直管公房=产权代管单位 */
    private String paperCertNo;

    /** 纸面凭据二：商品房=测绘报告编号；保障房=承办科室；直管公房=拨交文据 */
    private String paperRefNo;

    /** 起写那一格（不得晚于止写格） */
    private Date writeStart;

    /** 止写那一格 */
    private Date writeEnd;

    /** 人自家拟的户号：不许有，递进来即整单退回并点名 */
    private String billNo;

    /** 人自家拟的证载号：不许有，递进来即整单退回并点名 */
    private String certNo;

    /** 经办人；不带则按当前登录人续名 */
    private String operator;

    /** 落库时刻，缺省取当前时刻（测试可定点） */
    private Date now;

    /** 备注 */
    private String remark;

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getDoorNo() {
        return doorNo;
    }

    public void setDoorNo(String doorNo) {
        this.doorNo = doorNo;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
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

    public Date getWriteStart() {
        return writeStart;
    }

    public void setWriteStart(Date writeStart) {
        this.writeStart = writeStart;
    }

    public Date getWriteEnd() {
        return writeEnd;
    }

    public void setWriteEnd(Date writeEnd) {
        this.writeEnd = writeEnd;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
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
