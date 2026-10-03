package com.fc.v2.controller.admin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.THmfAcctCard;
import com.fc.v2.model.custom.AcctCardReceipt;
import com.fc.v2.model.custom.AcctCardVerdict;
import com.fc.v2.model.custom.AcctFieldSpec;
import com.fc.v2.model.custom.AcctCardRoster;
import com.fc.v2.model.custom.HouseholdHit;
import com.fc.v2.model.custom.OpenHouseholdForm;
import com.fc.v2.model.custom.ReviseHouseholdForm;
import com.fc.v2.service.ITHmfAcctCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 分户账立户单 Controller（一户一张档：立户唯一保存入口、步子唯一推进入口）。
 *
 * 合规不合规、处在哪一档、还能不能进退、必填哪几栏、名录进几个数，
 * 全由服务层回话，本层不留第二条判路；户号、证载号页面没有填写栏，由系统发出。
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "分户账立户单")
@Controller
@RequestMapping("/HmfAcctCardController")
public class HmfAcctCardController extends BaseController {

    private final String prefix = "admin/hmfAcctCard";

    @Autowired
    private ITHmfAcctCardService hmfAcctCardService;

    @ApiOperation(value = "户档台账跳转", notes = "户档台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("hmf:hmfAcctCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "分户账立户单名录查询", action = "list")
    @ApiOperation(value = "本级名录（只摆现行真户，三数同源）", notes = "本级名录")
    @GetMapping("/roster")
    @RequiresPermissions("hmf:hmfAcctCard:list")
    @ResponseBody
    public AjaxResult roster(String siteNo) {
        AcctCardRoster roster = hmfAcctCardService.rosterOf(siteNo);
        return AjaxResult.successData(200, roster);
    }

    @Log(title = "分户账立户单分页查询", action = "list")
    @ApiOperation(value = "分页查询（现行层真户，不铺空行）", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("hmf:hmfAcctCard:list")
    @ResponseBody
    public ResultTable list(THmfAcctCard record) {
        // 一屏只摆库里真有的现行户：不补空行，也不把在场的旧版次抹掉（旧版另在历史层翻）。
        // 行数直接取服务层同回点出的名册，不经手第二条点数。
        AcctCardRoster roster = hmfAcctCardService.rosterOf(record.getSiteNo());
        return pageTable(roster.getRows(), roster.getRows().size());
    }

    @Log(title = "分户账立户单必填栏回话", action = "required")
    @ApiOperation(value = "按项目类别回话必填哪几栏", notes = "必填栏回话")
    @GetMapping("/required")
    @RequiresPermissions("hmf:hmfAcctCard:view")
    @ResponseBody
    public AjaxResult required(String siteType) {
        AcctFieldSpec spec = hmfAcctCardService.requiredFields(siteType);
        return spec == null ? AjaxResult.error("项目类别空着或不认得，先把类别落定")
                : AjaxResult.successData(200, spec);
    }

    @Log(title = "分户账立户单立户", action = "open")
    @ApiOperation(value = "立户（户号/证载号系统发出，缺栏逐栏点名）", notes = "立户")
    @PostMapping("/open")
    @RequiresPermissions("hmf:hmfAcctCard:add")
    @ResponseBody
    public AjaxResult open(OpenHouseholdForm form) {
        AcctCardReceipt receipt = hmfAcctCardService.openHousehold(form);
        if (receipt.getCode() == AcctCardReceipt.CODE_REJECTED) {
            return AjaxResult.error(receipt.getMessage()).put("missingFields", receipt.getMissingFields());
        }
        return AjaxResult.success(200, receipt.getMessage(), receipt);
    }

    @Log(title = "分户账立户单换版", action = "revise")
    @ApiOperation(value = "建面改了/换底册：新版立起旧版沉历史层", notes = "换版重算")
    @PostMapping("/revise")
    @RequiresPermissions("hmf:hmfAcctCard:add")
    @ResponseBody
    public AjaxResult revise(ReviseHouseholdForm form) {
        AcctCardReceipt receipt = hmfAcctCardService.reviseHousehold(form);
        if (receipt.getCode() == AcctCardReceipt.CODE_REJECTED) {
            return AjaxResult.error(receipt.getMessage()).put("missingFields", receipt.getMissingFields());
        }
        return AjaxResult.success(200, receipt.getMessage(), receipt);
    }

    @Log(title = "分户账立户单历史翻版", action = "history")
    @ApiOperation(value = "翻历史层（只摆旧版，不改动）", notes = "历史层")
    @GetMapping("/history")
    @RequiresPermissions("hmf:hmfAcctCard:list")
    @ResponseBody
    public AjaxResult history(String billNo) {
        if (billNo == null || billNo.trim().isEmpty()) {
            return AjaxResult.error("给个户号才翻得到它的旧版");
        }
        java.util.List<com.fc.v2.model.auto.THmfAcctCard> versions = hmfAcctCardService
                .selectTHmfAcctCardList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<
                        com.fc.v2.model.auto.THmfAcctCard>()
                        .eq("bill_no", billNo.trim()).eq("del_flag", 0)
                        .orderByDesc("node_no"));
        return AjaxResult.successData(200, versions);
    }

    @Log(title = "分户账立户单换回旧版", action = "restore")
    @ApiOperation(value = "把旧版原样换回在场（只换那一版，不覆新版）", notes = "换回旧版")
    @PostMapping("/restore")
    @RequiresPermissions("hmf:hmfAcctCard:edit")
    @ResponseBody
    public AjaxResult restore(Long id, String remark) {
        com.fc.v2.model.auto.THmfAcctCard card =
                hmfAcctCardService.restoreHouseholdVersion(id, null, remark);
        return card == null
                ? AjaxResult.error("目标不是可换回的历史旧版，或在场新版已冻住（冻住不往回翻）")
                : AjaxResult.success("已换回第" + card.getNodeNo() + "版，原在场新版整版沉底未覆");
    }

    @Log(title = "分户账立户单推进", action = "push")
    @ApiOperation(value = "往前一步（步子听服务层，跨步不认）", notes = "推进")
    @PostMapping("/push")
    @RequiresPermissions("hmf:hmfAcctCard:edit")
    @ResponseBody
    public AjaxResult push(Long id, String remark) {
        return verdict(hmfAcctCardService.pushCard(id, null, remark));
    }

    @Log(title = "分户账立户单退回", action = "rollback")
    @ApiOperation(value = "退回一步重来（旧趟留痕不抹）", notes = "退回")
    @PostMapping("/rollback")
    @RequiresPermissions("hmf:hmfAcctCard:edit")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark) {
        return verdict(hmfAcctCardService.rollbackCard(id, null, remark));
    }

    @Log(title = "分户账立户单打住", action = "halt")
    @ApiOperation(value = "就地打住（不进不退，名额空出）", notes = "打住")
    @PostMapping("/halt")
    @RequiresPermissions("hmf:hmfAcctCard:edit")
    @ResponseBody
    public AjaxResult halt(Long id, String remark) {
        return verdict(hmfAcctCardService.haltCard(id, null, remark));
    }

    @Log(title = "分户账立户单找户", action = "find")
    @ApiOperation(value = "户号/门牌/证载号片段找户（只摆本级有权看的）", notes = "找户")
    @GetMapping("/find")
    @RequiresPermissions("hmf:hmfAcctCard:list")
    @ResponseBody
    public AjaxResult find(String keyword, String siteNos) {
        // 本级有权看哪几册由调用侧按层级给出；一个不给就空着，别级的户不同屏供人挑。
        List<String> visible = splitSiteNos(siteNos);
        List<HouseholdHit> hits = hmfAcctCardService.findHousehold(keyword, visible);
        return AjaxResult.successData(200, hits);
    }

    private AjaxResult verdict(AcctCardVerdict v) {
        if (v == null) {
            return AjaxResult.error("查不到现行的那张单");
        }
        if (v.getCode() == AcctCardVerdict.CODE_STAY) {
            return AjaxResult.error(v.getMessage() + "（当前步：" + v.getStep() + "）")
                    .put("data", v.getCard()).put("step", v.getStep());
        }
        return AjaxResult.success(200, v.getMessage() + "（当前步：" + v.getStep() + "）", v.getCard())
                .put("step", v.getStep());
    }

    private List<String> splitSiteNos(String siteNos) {
        if (siteNos == null || siteNos.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(siteNos.split(","));
    }
}
