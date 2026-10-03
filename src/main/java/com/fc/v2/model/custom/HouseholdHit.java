package com.fc.v2.model.custom;

import java.io.Serializable;

/**
 * 找户口命中一条：户号、门牌、证载号三处任一处从头或中间片段命中即给出，
 * 命中直接带出所属底册代号与所在收费档次，不用人再抄第二遍。
 *
 * 出现的前提是库里真有这一户：不造没立过的户，不把在场的旧版次抹掉——
 * 名录露的是现行版，找户命中也只带现行那版。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HouseholdHit implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 所属底册代号 */
    private String siteNo;

    /** 底册（项目）全称，命中行顺带带出 */
    private String siteName;

    /** 户号 */
    private String billNo;

    /** 门牌 */
    private String doorNo;

    /** 证载号 */
    private String certNo;

    /** 所在收费档次（折层结果，只读） */
    private Integer gradeLevel;

    /** 当前步 0待核对 1已核对 2已冻住 */
    private Integer status;

    public HouseholdHit() {
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

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getDoorNo() {
        return doorNo;
    }

    public void setDoorNo(String doorNo) {
        this.doorNo = doorNo;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
