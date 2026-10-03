package com.fc.v2.controller.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.THmfAcctCard;
import com.fc.v2.model.custom.HouseholdAmend;
import com.fc.v2.model.custom.HouseholdFound;
import com.fc.v2.model.custom.HouseholdOpen;
import com.fc.v2.model.custom.HouseholdRequired;
import com.fc.v2.model.custom.HouseholdRoster;
import com.fc.v2.model.custom.HouseholdStep;
import com.fc.v2.model.custom.HouseholdVerdict;
import com.fc.v2.service.ITHmfAcctCardService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 分户账立户 Controller（今年从立户这一步管起来）
 *
 * 合不合规、处在哪一步、能不能再迈/退、必填哪几栏，全由服务层那几条方法回话；
 * 本层只收意图、转回话，不留第二条判路。查户/名录只摆本级（本级页绑定的底册代号）
 * 名下的现行户，本级一个没有就空着，不铺假行。
 *
 * @author fuce
 * @date 2026-10-03
 */
@Api(value = "分户账立户")
@Controller
@RequestMapping("/hmfHousehold")
public class HmfHouseholdController extends BaseController {

    private final String prefix = "admin/hmfHousehold";

    @Autowired
    private ITHmfAcctCardService hmfAcctCardService;

    @ApiOperation(value = "立户台账跳转", notes = "立户台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("hmf:hmfHousehold:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "分户账立户·必填栏回话", action = "required")
    @ApiOperation(value = "按项目类别回话必填哪几栏", notes = "必填栏由服务层按类别当场回话")
    @GetMapping("/required")
    @RequiresPermissions("hmf:hmfHousehold:view")
    @ResponseBody
    public AjaxResult required(String siteType) {
        HouseholdRequired required = hmfAcctCardService.requiredFields(siteType);
        return required == null
                ? AjaxResult.error("项目类别空着或不在三类排法里（商品房/保障性住房/直管公房）")
                : AjaxResult.successData(200, required);
    }

    @Log(title = "分户账立户·立户", action = "open")
    @ApiOperation(value = "立户（户号证载号系统发出，凭据按类别，档次应缴系统折）", notes = "立户")
    @PostMapping("/open")
    @RequiresPermissions("hmf:hmfHousehold:open")
    @ResponseBody
    public AjaxResult open(HouseholdOpen open) {
        HouseholdVerdict v = hmfAcctCardService.openHousehold(open);
        return toVerdict(v);
    }

    @Log(title = "分户账立户·迈步", action = "push")
    @ApiOperation(value = "唯一迈步入口（核对/冻住/退回都走这里）", notes = "迈步")
    @PostMapping("/push")
    @RequiresPermissions("hmf:hmfHousehold:push")
    @ResponseBody
    public AjaxResult push(HouseholdStep step) {
        HouseholdVerdict v = hmfAcctCardService.pushHousehold(step);
        if (v == null) {
            return AjaxResult.error("单据不存在");
        }
        return toVerdict(v);
    }

    @Log(title = "分户账立户·更正建面", action = "amendArea")
    @ApiOperation(value = "建面改了（另起新版，旧版沉历史层）", notes = "更正建面")
    @PostMapping("/amendArea")
    @RequiresPermissions("hmf:hmfHousehold:amend")
    @ResponseBody
    public AjaxResult amendArea(HouseholdAmend amend) {
        return toVerdict(hmfAcctCardService.amendArea(amend));
    }

    @Log(title = "分户账立户·换底册", action = "amendBook")
    @ApiOperation(value = "换挂靠底册（按新界重折，另起新版）", notes = "换底册")
    @PostMapping("/amendBook")
    @RequiresPermissions("hmf:hmfHousehold:amend")
    @ResponseBody
    public AjaxResult amendBook(HouseholdAmend amend) {
        return toVerdict(hmfAcctCardService.amendBook(amend));
    }

    @Log(title = "分户账立户·查户", action = "search")
    @ApiOperation(value = "查户（户号/门牌/证载号，从头与片段匹配，只摆本级现行户）", notes = "查户")
    @GetMapping("/search")
    @RequiresPermissions("hmf:hmfHousehold:view")
    @ResponseBody
    public AjaxResult search(String keyword, @RequestParam(value = "siteNos", required = false) String siteNos) {
        // 本级范围：本级页绑定的底册代号；一个不传就一个不摆（默认不给看，别级绝不同屏供挑）。
        List<HouseholdFound> hits = hmfAcctCardService.searchHouseholds(keyword, split(siteNos));
        return AjaxResult.successData(200, hits);
    }

    @Log(title = "分户账立户·本级名录", action = "roster")
    @ApiOperation(value = "本级名录三笔（同一回点算）", notes = "本级名录")
    @GetMapping("/roster")
    @RequiresPermissions("hmf:hmfHousehold:view")
    @ResponseBody
    public AjaxResult roster(@RequestParam(value = "siteNos", required = false) String siteNos) {
        HouseholdRoster roster = hmfAcctCardService.rosterOf(split(siteNos));
        return AjaxResult.successData(200, roster);
    }

    @Log(title = "分户账立户·翻旧版", action = "history")
    @ApiOperation(value = "翻某户历次版次", notes = "版本历史")
    @GetMapping("/history")
    @RequiresPermissions("hmf:hmfHousehold:view")
    @ResponseBody
    public AjaxResult history(String billNo) {
        List<THmfAcctCard> versions = hmfAcctCardService.historyOf(billNo);
        return AjaxResult.successData(200, versions);
    }

    @Log(title = "分户账立户·退回旧版", action = "revert")
    @ApiOperation(value = "只换回那一版，旧数据不覆新", notes = "退回旧版")
    @PostMapping("/revert")
    @RequiresPermissions("hmf:hmfHousehold:amend")
    @ResponseBody
    public AjaxResult revert(Long cardId) {
        return toVerdict(hmfAcctCardService.revertVersion(cardId, null));
    }

    @Log(title = "分户账立户·纸簿旧单迁入", action = "importLegacy")
    @ApiOperation(value = "步数/版次序/校验码空着的旧单也接得住", notes = "旧单迁入")
    @PostMapping("/importLegacy")
    @RequiresPermissions("hmf:hmfHousehold:open")
    @ResponseBody
    public AjaxResult importLegacy(THmfAcctCard paper) {
        return toVerdict(hmfAcctCardService.importLegacy(paper, null));
    }

    // ---------------------------------------------------------------- 回话转换

    private AjaxResult toVerdict(HouseholdVerdict v) {
        if (v == null) {
            return AjaxResult.error("单据不存在");
        }
        String msg = v.getMessage() + "（当前步：" + stepText(v.getStep()) + "）";
        if (v.getCode() == HouseholdVerdict.CODE_ACCEPTED) {
            return AjaxResult.success(200, msg, v.getCard());
        }
        return AjaxResult.error(msg)
                .put("data", v.getCard())
                .put("step", v.getStep())
                .put("verdictCode", v.getCode());
    }

    private String stepText(int step) {
        switch (step) {
            case 1: return "已核对";
            case 2: return "已冻住";
            default: return "待核对";
        }
    }

    private List<String> split(String siteNos) {
        List<String> out = new ArrayList<>();
        if (siteNos == null || siteNos.trim().isEmpty()) {
            return out;
        }
        for (String s : Arrays.asList(siteNos.split(","))) {
            if (s != null && !s.trim().isEmpty() && !out.contains(s.trim())) {
                out.add(s.trim());
            }
        }
        return out;
    }
}
