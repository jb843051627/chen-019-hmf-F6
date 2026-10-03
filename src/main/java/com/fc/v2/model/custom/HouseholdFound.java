package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 查户命中的一条：直接带出所属底册与所在收费档次，不用人再抄第二遍。
 * 只可能出自库里真有的现行户——不造没立过的户，不抹在场的版次。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HouseholdFound implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 系统发出的户号 */
    private String billNo;

    /** 系统发出的证载号 */
    private String certNo;

    /** 门牌 */
    private String doorNo;

    /** 所属底册代号 */
    private String siteNo;

    /** 底册（项目）全称 */
    private String siteName;

    /** 项目类别 */
    private String siteType;

    /** 版次序 */
    private Integer nodeNo;

    /** 建筑面积 */
    private BigDecimal qty;

    /** 收费档次（所在层数） */
    private Integer gradeLevel;

    /** 应缴额 */
    private BigDecimal fineAmt;

    /** 当前步 0待核对 1已核对 2已冻住 */
    private Integer step;

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

    public String getDoorNo() {
        return doorNo;
    }

    public void setDoorNo(String doorNo) {
        this.doorNo = doorNo;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public BigDecimal getFineAmt() {
        return fineAmt;
    }

    public void setFineAmt(BigDecimal fineAmt) {
        this.fineAmt = fineAmt;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }
}
