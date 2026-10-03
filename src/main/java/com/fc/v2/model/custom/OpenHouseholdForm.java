package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 立户递单意图。
 *
 * 户号、证载号由系统发出——本单里没有这两栏，递进来也不入库；
 * 项目类别照所属底册落，不由人填；收费档次与应缴额由系统同一回算出，同样不收手填。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class OpenHouseholdForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 所属底册代号（户归哪本底册只凭代号认，名字读起来像不算数） */
    private String siteNo;

    /** 门牌 */
    private String doorNo;

    /** 本户建筑面积（平方米）；零与负数不收 */
    private BigDecimal qty;

    /** 起写时刻 */
    private Date writeStart;

    /** 止写时刻 */
    private Date writeEnd;

    /** 起写晚于止写时，须显式确认才收；无此确认整单退回 */
    private Boolean confirmWriteOrder;

    /** 商品房：预售证载号 */
    private String permitNo;

    /** 商品房：测绘报告编号 */
    private String surveyNo;

    /** 保障性住房：配租配售文号 */
    private String allotNo;

    /** 保障性住房：承办科室 */
    private String hostDept;

    /** 直管公房：产权代管单位 */
    private String ownerUnit;

    /** 直管公房：拨交文据 */
    private String grantDoc;

    /** 经手人；不带由入口按当前登录人续名 */
    private String operator;

    /** 随笔记事 */
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

    public Boolean getConfirmWriteOrder() {
        return confirmWriteOrder;
    }

    public void setConfirmWriteOrder(Boolean confirmWriteOrder) {
        this.confirmWriteOrder = confirmWriteOrder;
    }

    public String getPermitNo() {
        return permitNo;
    }

    public void setPermitNo(String permitNo) {
        this.permitNo = permitNo;
    }

    public String getSurveyNo() {
        return surveyNo;
    }

    public void setSurveyNo(String surveyNo) {
        this.surveyNo = surveyNo;
    }

    public String getAllotNo() {
        return allotNo;
    }

    public void setAllotNo(String allotNo) {
        this.allotNo = allotNo;
    }

    public String getHostDept() {
        return hostDept;
    }

    public void setHostDept(String hostDept) {
        this.hostDept = hostDept;
    }

    public String getOwnerUnit() {
        return ownerUnit;
    }

    public void setOwnerUnit(String ownerUnit) {
        this.ownerUnit = ownerUnit;
    }

    public String getGrantDoc() {
        return grantDoc;
    }

    public void setGrantDoc(String grantDoc) {
        this.grantDoc = grantDoc;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
