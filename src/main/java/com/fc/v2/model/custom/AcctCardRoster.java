package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.THmfAcctCard;

/**
 * 本级名录回话：只摆库里真有的现行户，不补空行，也不把在场的旧版次抹掉（旧版在历史层，另查）。
 *
 * 三笔数——立成、冻住、在册——与逐户当场点出的数同一回计算给出；
 * 这与立户保存回执走的是同一条点户口径，屏上自点的数不作数。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class AcctCardRoster implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本级底册代号 */
    private String siteNo;

    /** 累计立成户数（现行层，一户只计最新版一次） */
    private int openedCount;

    /** 已冻住户数 */
    private int frozenCount;

    /** 在册户数＝立成−冻住（立成加一、冻住减一） */
    private int rosterCount;

    /** 当场逐户点出的非冻住户数，与 rosterCount 同回，相等才作数 */
    private int pointCount;

    /** 现行户逐户列（真有才摆，空就空着，不铺假行；不含历史层旧版） */
    private List<THmfAcctCard> rows = new ArrayList<>();

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

    public List<THmfAcctCard> getRows() {
        return rows;
    }

    public void setRows(List<THmfAcctCard> rows) {
        this.rows = rows;
    }
}
