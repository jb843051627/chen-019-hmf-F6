package com.fc.v2.model.custom;

import java.io.Serializable;

import com.fc.v2.model.auto.THmfAcctCard;

/**
 * 立户/迈步/更正各口子的唯一回话：收没收、停在哪一步、缺哪一栏，由服务层这一口定。
 * 屏上填的步号与此回话对不齐时，以此回话为准。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HouseholdVerdict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 收下了：立户落库／往前挪了一步／退回一步／更正起了新版 */
    public static final int CODE_ACCEPTED = 1;
    /** 门槛不过：缺栏、编号人报、面为零负、额为负、起止格倒置、凭据缺样等，整张账不动 */
    public static final int CODE_REJECTED = 0;
    /** 同一册户号（或同门牌）头一张还在走，第二张叠不上去；同号再报库中仍只那一张 */
    public static final int CODE_DUP = 2;
    /** 已冻住：到了尽头，添不进、改不动、撤不掉，要动只能另起一张 */
    public static final int CODE_FROZEN = 3;
    /** 挂靠的底册已迁出：不再收新户，挂不上去（早前挂的户仍在册、仍查得到） */
    public static final int CODE_ARCH_GONE = 4;
    /** 步子没挪：想跨步、同一步第二遍、待核对还往回退，都停在原步不添痕 */
    public static final int CODE_STAY = 5;

    /** 系统照步痕重放数出来的当前步（0待核对 1已核对 2已冻住） */
    private int step;

    /** 回话情形码，见本类常量 */
    private int code;

    /** 给柜员看的那句话（拒收时点名缺哪一栏） */
    private String message;

    /** 最新现行版次行（step 为镜像，与本回话同源写出）；拒收时可能为 null */
    private THmfAcctCard card;

    public HouseholdVerdict() {
    }

    public HouseholdVerdict(int step, int code, String message, THmfAcctCard card) {
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
