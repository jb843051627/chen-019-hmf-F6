package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 改立户单意图：建面改了、换挂靠的底册，都是重算的时点。
 *
 * 改法不是覆写旧单——新版立起来（版次序+1、校验码重算、从待核对重新走），
 * 旧版既不留名名录也不销毁，整版沉到历史层等着被翻。冻住的单要动也只能走这里另起一版。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class ReviseHouseholdForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 现行那张单的主键（名录只露现行版，改的就是它背后这一户） */
    private Long id;

    /** 新挂靠的底册代号；给了且与旧的不同就算换底册（只认代号，新底册须在册） */
    private String targetSiteNo;

    /** 新门牌号（门牌订正随新版走） */
    private String doorNo;

    /** 新建筑面积；零与负数不收 */
    private BigDecimal qty;

    /** 起写时刻 */
    private Date writeStart;

    /** 止写时刻 */
    private Date writeEnd;

    /** 起写晚于止写时须显式确认 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTargetSiteNo() {
        return targetSiteNo;
    }

    public void setTargetSiteNo(String targetSiteNo) {
        this.targetSiteNo = targetSiteNo;
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
