package com.fc.v2.model.custom;

import java.io.Serializable;

/**
 * 必填栏规格：服务层按项目类别当场回话，页面照此摆格、照此点名，不自带第二套判法。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class AcctFieldSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 项目类别：商品房／保障性住房／直管公房 */
    private String siteType;

    /** 该类别要交的随附凭据栏位名（页面栏号，回执缺栏照此点名） */
    private String[] requiredFields;

    /** 各栏中文句（给柜员看） */
    private String[] requiredLabels;

    public AcctFieldSpec() {
    }

    public AcctFieldSpec(String siteType, String[] requiredFields, String[] requiredLabels) {
        this.siteType = siteType;
        this.requiredFields = requiredFields;
        this.requiredLabels = requiredLabels;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public String[] getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(String[] requiredFields) {
        this.requiredFields = requiredFields;
    }

    public String[] getRequiredLabels() {
        return requiredLabels;
    }

    public void setRequiredLabels(String[] requiredLabels) {
        this.requiredLabels = requiredLabels;
    }
}
