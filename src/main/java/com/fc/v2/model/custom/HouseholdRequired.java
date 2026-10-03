package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 按项目类别当场回话的必填栏：该不该交、交哪几样跟着类别走。
 * 接口层与前端都不留第二条绕开服务层的判路，栏单以本回话为准。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HouseholdRequired implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 项目类别：商品房 / 保障性住房 / 直管公房 */
    private String siteType;

    /** 必填栏名（与立户意图字段同名，供前端逐栏点名） */
    private List<String> fields = new ArrayList<>();

    /** 必填栏的给人看的名目（与 fields 同序） */
    private List<String> labels = new ArrayList<>();

    public HouseholdRequired() {
    }

    public HouseholdRequired(String siteType, List<String> fields, List<String> labels) {
        this.siteType = siteType;
        this.fields = fields;
        this.labels = labels;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }
}
