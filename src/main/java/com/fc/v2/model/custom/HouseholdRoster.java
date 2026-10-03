package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.List;

/**
 * 本级名录回话：本级在册几户、冻住几户、现行几户，三笔由同一场逐户点算写出，
 * 与保存方法回写同源；不另存一份会被人手拨偏的数。
 *
 * 在册（在走）：立成一户加一，冻住一户减一；现行=在册+冻住，即现行层逐户点数。
 * 本级名下一个没有也照实回零，不铺假行。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HouseholdRoster implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 点算覆盖的本级底册代号（本级有权看的那几本） */
    private List<String> siteNos;

    /** 在册（在走、未冻住）户数：立成加一、冻住减一 */
    private int activeCount;

    /** 已冻住户数 */
    private int frozenCount;

    /** 现行层户数（=在册+冻住，逐户点出来的那一笔） */
    private int currentCount;

    /** 三笔是否接得平（在册+冻住=现行），同一回算出 */
    private boolean matched;

    public List<String> getSiteNos() {
        return siteNos;
    }

    public void setSiteNos(List<String> siteNos) {
        this.siteNos = siteNos;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public int getFrozenCount() {
        return frozenCount;
    }

    public void setFrozenCount(int frozenCount) {
        this.frozenCount = frozenCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }
}
