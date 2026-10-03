package com.fc.v2.model.custom;

import java.io.Serializable;

import com.fc.v2.model.auto.THmfAcctCard;

/**
 * 立户单推进/退回/打住的唯一回话。
 *
 * 当前停在哪一步，由服务层照步子留痕数已完成的步数得出——
 * 屏上填的档、经办人记的档都不算数。步子不得跨；冻住即尽头，添不进、改不动、撤不掉。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class AcctCardVerdict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 往前挪了一步（待核对→已核对→已冻住） */
    public static final int CODE_MOVED = 1;
    /** 步子不过（跨步、冻住后再推、材料不合等），停在原地，账不动 */
    public static final int CODE_STAY = 0;
    /** 退回一步重来：旧一趟记录留存，不与后补笔混摆 */
    public static final int CODE_BACK = 2;
    /** 已就地打住：单不进不退，在走名额腾出 */
    public static final int CODE_HALTED = 3;

    /** 系统照留痕数出来的当前步（0待核对 1已核对 2已冻住，权威值） */
    private int step;

    /** 回话情形码，见本类常量 */
    private int code;

    /** 给柜员看的那句话 */
    private String message;

    /** 最新单据（status 为镜像，与本回话的权威步同源写出） */
    private THmfAcctCard card;

    public AcctCardVerdict() {
    }

    public AcctCardVerdict(int step, int code, String message, THmfAcctCard card) {
        this.step = step;
        this.code = code;
        this.message = message;
        this.card = card;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public THmfAcctCard getCard() {
        return card;
    }

    public void setCard(THmfAcctCard card) {
        this.card = card;
    }
}
