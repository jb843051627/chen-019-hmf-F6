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

    /** 分户账立户单号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "分户账立户单号")
    private String billNo;

    /** 本单版次序 */
    @TableField("node_no")
    @ApiModelProperty(value = "本单版次序")
    private Integer nodeNo;

    /** 所属分户底册 */
    @TableField("site_id")
    @ApiModelProperty(value = "所属分户底册")
    private Integer siteId;

    /** 所属底册代号 */
    @TableField("site_no")
    @ApiModelProperty(value = "所属底册代号")
    private String siteNo;

    /** 门牌 */
    @TableField("door_no")
    @ApiModelProperty(value = "门牌")
    private String doorNo;

    /** 随附证载号（系统发出） */
    @TableField("cert_no")
    @ApiModelProperty(value = "随附证载号（系统发出）")
    private String certNo;

    /** 项目类别镜像（商品房/保障性住房/直管公房，照底册落） */
    @TableField("site_type")
    @ApiModelProperty(value = "项目类别镜像")
    private String siteType;

    /** 商品房：预售证载号 */
    @TableField("permit_no")
    @ApiModelProperty(value = "商品房：预售证载号")
    private String permitNo;

    /** 商品房：测绘报告编号 */
    @TableField("survey_no")
    @ApiModelProperty(value = "商品房：测绘报告编号")
    private String surveyNo;

    /** 保障性住房：配租配售文号 */
    @TableField("allot_no")
    @ApiModelProperty(value = "保障性住房：配租配售文号")
    private String allotNo;

    /** 保障性住房：承办科室 */
    @TableField("host_dept")
    @ApiModelProperty(value = "保障性住房：承办科室")
    private String hostDept;

    /** 直管公房：产权代管单位 */
    @TableField("owner_unit")
    @ApiModelProperty(value = "直管公房：产权代管单位")
    private String ownerUnit;

    /** 直管公房：拨交文据 */
    @TableField("grant_doc")
    @ApiModelProperty(value = "直管公房：拨交文据")
    private String grantDoc;

    /** 起写时刻（不得晚于止写） */
    @TableField("write_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "起写时刻")
    private Date writeStart;

    /** 止写时刻（早于起写须先确认才收） */
    @TableField("write_end")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "止写时刻")
    private Date writeEnd;

    /** 本户建筑面积(平方米) */
    @TableField("qty")
    @ApiModelProperty(value = "本户建筑面积(平方米)")
    private BigDecimal qty;

    /** 本户应缴额(元，与档次同一回算出) */
    @TableField("fine_amt")
    @ApiModelProperty(value = "本户应缴额(元)")
    private BigDecimal fineAmt;

    /** 本档每平米标准（随档次取） */
    @TableField("unit_price")
    @ApiModelProperty(value = "本档每平米标准")
    private BigDecimal unitPrice;

    /** 校验码（走到已核对锁死；换版重算） */
    @TableField("content")
    @ApiModelProperty(value = "校验码（走到已核对锁死；换版重算）")
    private String content;

    /** 面积折到的收费档次 */
    @TableField("grade_level")
    @ApiModelProperty(value = "面积折到的收费档次")
    private Integer gradeLevel;

    /** 所在层 0现行 1历史层 */
    @TableField("layer")
    @ApiModelProperty(value = "所在层 0现行 1历史层")
    private Integer layer;

    /** 就地打住 0在走 1已打住 */
    @TableField("halt_flag")
    @ApiModelProperty(value = "就地打住 0在走 1已打住")
    private Integer haltFlag;

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

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public String getPermitNo() {
        return permitNo;
    }

    public void setPermitNo(String permitNo) {
        this.permitNo = permitNo;
    }

    public String getSurveyNo() {
        return surveyNo;
    }

    public void setSurveyNo(String surveyNo) {
        this.surveyNo = surveyNo;
    }

    public String getAllotNo() {
        return allotNo;
    }

    public void setAllotNo(String allotNo) {
        this.allotNo = allotNo;
    }

    public String getHostDept() {
        return hostDept;
    }

    public void setHostDept(String hostDept) {
        this.hostDept = hostDept;
    }

    public String getOwnerUnit() {
        return ownerUnit;
    }

    public void setOwnerUnit(String ownerUnit) {
        this.ownerUnit = ownerUnit;
    }

    public String getGrantDoc() {
        return grantDoc;
    }

    public void setGrantDoc(String grantDoc) {
        this.grantDoc = grantDoc;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getLayer() {
        return layer;
    }

    public void setLayer(Integer layer) {
        this.layer = layer;
    }

    public Integer getHaltFlag() {
        return haltFlag;
    }

    public void setHaltFlag(Integer haltFlag) {
        this.haltFlag = haltFlag;
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
