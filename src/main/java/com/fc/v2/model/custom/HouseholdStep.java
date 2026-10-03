package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.Date;

/**
 * 迈步意图：一张立户单往前核对/冻住，或往后退回一步，都走这一个口子。
 * 动作只两种（1往前、-1往后），不靠屏上带的步号猜方向；步号只当意图。
 *
 * @author fuce
 * @date 2026-10-03
 */
public class HouseholdStep implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 要迈步的立户单（现行版次行）主键 */
    private Long cardId;

    /** 动作：1往前（缺省）；-1往后（一次只收一步，且只在未冻住时收得回） */
    private Integer action;

    /** 意向步号（屏上带出的只作意图，不作凭据；权威步数听服务层回话） */
    private Integer expectStep;

    /** 经办人；不带则按当前登录人续名 */
    private String operator;

    /** 落笔时刻，缺省取当前时刻（测试可定点） */
    private Date now;

    /** 随笔记事（退回事由等） */
    private String remark;

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public Integer getExpectStep() {
        return expectStep;
    }

    public void setExpectStep(Integer expectStep) {
        this.expectStep = expectStep;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getNow() {
        return now;
    }

    public void setNow(Date now) {
        this.now = now;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
