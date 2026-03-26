package com.hl.hlaicodemother.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.hl.hlaicodemother.model.entity.AppVersion;
import com.hl.hlaicodemother.service.AppVersionService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 应用版本表 控制层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@RestController
@RequestMapping("/appVersion")
public class AppVersionController {

    @Autowired
    private AppVersionService appVersionService;

    /**
     * 保存应用版本表。
     *
     * @param appVersion 应用版本表
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody AppVersion appVersion) {
        return appVersionService.save(appVersion);
    }

    /**
     * 根据主键删除应用版本表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return appVersionService.removeById(id);
    }

    /**
     * 根据主键更新应用版本表。
     *
     * @param appVersion 应用版本表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody AppVersion appVersion) {
        return appVersionService.updateById(appVersion);
    }

    /**
     * 查询所有应用版本表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<AppVersion> list() {
        return appVersionService.list();
    }

    /**
     * 根据主键获取应用版本表。
     *
     * @param id 应用版本表主键
     * @return 应用版本表详情
     */
    @GetMapping("getInfo/{id}")
    public AppVersion getInfo(@PathVariable Long id) {
        return appVersionService.getById(id);
    }

    /**
     * 分页查询应用版本表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<AppVersion> page(Page<AppVersion> page) {
        return appVersionService.page(page);
    }

}
