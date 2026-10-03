package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 分户账立户单对象 t_hmf_acct_card
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_hmf_acct_card")
@ApiModel(value = "THmfAcctCard", description = "分户账立户单")
public class THmfAcctCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 分户账号（系统发出，人自家拟的号不入库） */
    @TableField("bill_no")
    @ApiModelProperty(value = "分户账号")
    private String billNo;

    /** 系统发出的证载号（商品房为预售证载号） */
    @TableField("cert_no")
    @ApiModelProperty(value = "证载号（系统发出）")
    private String certNo;

    /** 链号：同一户历次版次共用一串，名录按链只露现行版 */
    @TableField("chain_no")
    @ApiModelProperty(value = "链号（同户各版共用）")
    private String chainNo;

    /** 门牌（查户口三码之一） */
    @TableField("door_no")
    @ApiModelProperty(value = "门牌")
    private String doorNo;

    /** 本单版次序（首版1，换版+1） */
    @TableField("node_no")
    @ApiModelProperty(value = "本单版次序")
    private Integer nodeNo;

    /** 所在层 0现行 1历史（旧版沉底只供翻） */
    @TableField("layer_no")
    @ApiModelProperty(value = "所在层 0现行 1历史")
    private Integer layerNo;

    /** 所属分户底册 */
    @TableField("site_id")
    @ApiModelProperty(value = "所属分户底册")
    private Integer siteId;

    /** 所属底册代号 */
    @TableField("site_no")
    @ApiModelProperty(value = "所属底册代号")
    private String siteNo;

    /** 项目类别镜像（商品房/保障性住房/直管公房，立户时落定） */
    @TableField("site_type")
    @ApiModelProperty(value = "项目类别镜像")
    private String siteType;

    /** 本户建筑面积(平方米，>0) */
    @TableField("qty")
    @ApiModelProperty(value = "本户建筑面积(平方米)")
    private BigDecimal qty;

    /** 本户应缴额(元，与档次同一回折算) */
    @TableField("fine_amt")
    @ApiModelProperty(value = "本户应缴额(元)")
    private BigDecimal fineAmt;

    /** 纸面凭据一（按类别：预售证载号/配租配售文号/产权代管单位） */
    @TableField("paper_cert_no")
    @ApiModelProperty(value = "纸面凭据一")
    private String paperCertNo;

    /** 纸面凭据二（按类别：测绘报告编号/承办科室/拨交文据） */
    @TableField("paper_ref_no")
    @ApiModelProperty(value = "纸面凭据二")
    private String paperRefNo;

    /** 起写那一格（不得晚于止写格） */
    @TableField("write_start")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "起写格")
    private Date writeStart;

    /** 止写那一格 */
    @TableField("write_end")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "止写格")
    private Date writeEnd;

    /** 校验码（走到已核对锁死；换版重算，新版新码旧版旧码） */
    @TableField("content")
    @ApiModelProperty(value = "校验码（随版次序走）")
    private String content;

    /** 按三条界折出的收费档次 1..4（只读不写，系统折出） */
    @TableField("grade_level")
    @ApiModelProperty(value = "面积折到的收费档次")
    private Integer gradeLevel;

    /** 折档所用每平米标准（应缴额=建面×标准，同一回算出） */
    @TableField("rate_std")
    @ApiModelProperty(value = "每平米收费标准")
    private BigDecimal rateStd;

    /** 进展 0待核对 1已核对 2已冻住 */
    @TableField("status")
    @ApiModelProperty(value = "进展 0待核对 1已核对 2已冻住")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getChainNo() {
        return chainNo;
    }

    public void setChainNo(String chainNo) {
        this.chainNo = chainNo;
    }

    public String getDoorNo() {
        return doorNo;
    }

    public void setDoorNo(String doorNo) {
        this.doorNo = doorNo;
    }

    public Integer getLayerNo() {
        return layerNo;
    }

    public void setLayerNo(Integer layerNo) {
        this.layerNo = layerNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getSiteId() {
        return siteId;
    }

    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public String getPaperCertNo() {
        return paperCertNo;
    }

    public void setPaperCertNo(String paperCertNo) {
        this.paperCertNo = paperCertNo;
    }

    public String getPaperRefNo() {
        return paperRefNo;
    }

    public void setPaperRefNo(String paperRefNo) {
        this.paperRefNo = paperRefNo;
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

    public BigDecimal getRateStd() {
        return rateStd;
    }

    public void setRateStd(BigDecimal rateStd) {
        this.rateStd = rateStd;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getFineAmt() {
        return fineAmt;
    }

    public void setFineAmt(BigDecimal fineAmt) {
        this.fineAmt = fineAmt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
