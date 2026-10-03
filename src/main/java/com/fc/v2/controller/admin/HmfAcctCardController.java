package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.THmfAcctCard;
import com.fc.v2.service.ITHmfAcctCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 分户账立户单 Controller（只读台账）
 *
 * 今年从立户这一步管起来后，写操作一律走 {@link HmfHouseholdController} 的服务口子：
 * 立户 /hmfHousehold/open、迈步 /push、改面 /amendArea、换册 /amendBook。
 * 本处不再挂新增、修改、删除入口——通用增删改已在服务层封死，接口层没有第二条路可绕。
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

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("hmf:hmfAcctCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "分户账立户单现行台账", action = "list")
    @ApiOperation(value = "分页查询（只摆现行层真有的户，不补空行；旧版走版本历史翻）", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("hmf:hmfAcctCard:list")
    @ResponseBody
    public ResultTable list(THmfAcctCard record) {
        QueryWrapper<THmfAcctCard> queryWrapper = new QueryWrapper<THmfAcctCard>()
                .eq("del_flag", 0)
                .eq("layer_no", 0)
                .orderByDesc("create_time");
        startPage();
        com.github.pagehelper.PageInfo<THmfAcctCard> page =
                new com.github.pagehelper.PageInfo<THmfAcctCard>(hmfAcctCardService.selectTHmfAcctCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }
}
