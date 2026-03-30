package com.hl.hlaicodemother.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.annotation.AuthCheck;
import com.hl.hlaicodemother.common.BaseResponse;
import com.hl.hlaicodemother.common.DeleteRequest;
import com.hl.hlaicodemother.common.ResultUtils;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.constant.UserConstant;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.dto.app.AppAdminUpdateRequest;
import com.hl.hlaicodemother.model.dto.app.AppQueryRequest;
import com.hl.hlaicodemother.model.dto.app.AppUpdateRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.AppMember;
import com.hl.hlaicodemother.model.entity.AppVersion;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.vo.AppVO;
import com.hl.hlaicodemother.service.AppMemberService;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.AppVersionService;
import com.hl.hlaicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    private static final long USER_PAGE_MAX_SIZE = 20;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private AppMemberService appMemberService;

    /**
     * 与AI模型对话，生成代码
     * @param appId
     * @param message
     * @param request
     * @return
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,@RequestParam String message,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务层方法，获取生成结果
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        // 对数据块进行封装，防止前端空白丢失
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    // 将 wrapper 转换为 JSON 字符串
                    String jsonStr = JSONUtil.toJsonStr(wrapper);
                    // 使用ServerSentEvent格式封装数据块，前端可以直接解析
                    return ServerSentEvent.<String>builder()
                            .data(jsonStr)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 手动停止 AI 生成代码
     *
     * @param appId 应用 id
     * @param request 请求
     * @return 是否已发送停止信号
     */
    @PostMapping("/chat/gen/code/stop")
    public BaseResponse<Boolean> stopChatToGenCode(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        User loginUser = userService.getLoginUser(request);
        boolean stopped = appService.stopChatToGenCode(appId, loginUser);
        return ResultUtils.success(stopped);
    }

    /**
     * 部署应用
     * @param appId
     * @param request
     * @return
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务层方法，部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 创建应用
     *
     * @param appAddRequest 创建请求
     * @param request       请求
     * @return 应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务层方法，创建应用
        Long appId = appService.addApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
    }

    @PostMapping("/update/app/version")
    public BaseResponse<Boolean> updateAppVersion(@RequestParam Long appId, @RequestParam Integer version) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        ThrowUtils.throwIf(version == null || version <= 0, ErrorCode.PARAMS_ERROR, "版本号不合法");
        // 获取应用
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 获取版本信息
        AppVersion appVersion = appVersionService.getOne(new QueryWrapper()
                .eq(AppVersion::getAppId, appId)
                .eq(AppVersion::getVersion, version));
        ThrowUtils.throwIf(appVersion == null, ErrorCode.NOT_FOUND_ERROR, "版本不存在");
        // 更新版本号
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setCurrentVersion(version);
        updateApp.setCurrentVersionId(appVersion.getId());
        updateApp.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用版本更新失败");
        return ResultUtils.success(result);
    }

    /**
     * 用户编辑自己的应用（当前仅支持修改名称）
     *
     * @param appUpdateRequest 编辑请求
     * @param request          请求
     * @return 是否成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null || appUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 校验应用是否存在且属于用户
        App oldApp = appService.getById(appUpdateRequest.getId());
        appService.checkAppOwner(oldApp, loginUser);
        // 构造更新对象
        App app = new App();
        app.setId(appUpdateRequest.getId());
        app.setAppName(appUpdateRequest.getAppName());
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        // 校验应用是否合法
        appService.validApp(app, false);
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用更新失败");
        return ResultUtils.success(result);
    }

    /**
     * 用户删除自己的应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 校验应用是否存在且属于用户或管理员
        App oldApp = appService.getById(deleteRequest.getId());
        appService.checkAppOwner(oldApp, loginUser);
        boolean result = appService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用删除失败");
        return ResultUtils.success(result);
    }


    /**
     * 获取应用版本数量
     *
     * @param appId 应用 id
     * @return 版本数量
     */
    @GetMapping("/get/version/count")
    public BaseResponse<Integer> getAppVersionCount(@RequestParam Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        appService.checkAppViewAuth(app, loginUser);
        int versionCount = (int) appVersionService.count(new QueryWrapper().eq(AppVersion::getAppId, appId));
        return ResultUtils.success(versionCount);
    }

    /**
     * 根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVoById(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        appService.checkAppViewAuth(app, loginUser);
        // 获取封装类（包含用户信息）
        AppVO appVO = appService.getAppVO(app);
        fillMyMemberInfo(List.of(appVO), loginUser);
        return ResultUtils.success(appVO);
    }

    /**
     * 分页获取当前用户可查看的应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        ThrowUtils.throwIf(appQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR, "每页最多20条");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        Set<Long> visibleAppIdSet = new LinkedHashSet<>();
        List<App> ownerAppList = appService.list(new QueryWrapper().eq("userId", loginUser.getId()));
        ownerAppList.forEach(app -> visibleAppIdSet.add(app.getId()));
        visibleAppIdSet.addAll(appMemberService.listVisibleAppIdsByUserId(loginUser.getId()));
        if (visibleAppIdSet.isEmpty()) {
            return ResultUtils.success(new Page<>(appQueryRequest.getPageNum(), appQueryRequest.getPageSize(), 0));
        }
        appQueryRequest.setUserId(null);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest)
                .in("id", visibleAppIdSet);
        Page<App> appPage = appService.page(new Page<>(appQueryRequest.getPageNum(), appQueryRequest.getPageSize()),
                queryWrapper);
        Page<AppVO> appVOPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        fillMyMemberInfo(appVOList, loginUser);
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }


    /**
     * 分页获取精选的应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        ThrowUtils.throwIf(appQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR, "每页最多20条");
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        Page<App> appPage = appService.page(new Page<>(appQueryRequest.getPageNum(), appQueryRequest.getPageSize()),
                appService.getQueryWrapper(appQueryRequest));
        Page<AppVO> appVOPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        appVOPage.setRecords(appService.getAppVOList(appPage.getRecords()));
        return ResultUtils.success(appVOPage);
    }


    /**
     * 管理员删除应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 是否成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest,
                                                  HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        // 校验应用是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用删除失败");
        return ResultUtils.success(result);
    }

    /**
     * 管理编辑应用
     *
     * @param appAdminUpdateRequest 编辑请求
     * @param request               请求
     * @return 是否成功
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest,
                                                  HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null || appAdminUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 校验应用是否存在
        App oldApp = appService.getById(appAdminUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 构造更新对象
        App app = new App();
        BeanUtils.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        // 校验应用是否合法
        appService.validApp(app, false);
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用更新失败");
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页获取应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 根据 id 获取应用详情(管理员)
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    public BaseResponse<AppVO> getAppByIdByAdmin(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 填充当前用户在应用中的成员信息
     * 将应用列表中的成员角色和状态信息补充到 AppVO 对象中
     *
     * @param appVOList 应用 VO 对象列表
     * @param loginUser 当前登录用户
     */
    private void fillMyMemberInfo(List<AppVO> appVOList, User loginUser) {
        // 参数有效性校验
        if (appVOList == null || appVOList.isEmpty() || loginUser == null) {
            return;
        }
        // 提取所有应用的 ID 集合
        Set<Long> appIdSet = appVOList.stream().map(AppVO::getId).collect(Collectors.toSet());
        // 批量查询当前用户在这些应用中的成员信息
        Map<Long, AppMember> appMemberMap = appMemberService.getAppMemberMap(appIdSet, loginUser.getId());
        // 遍历应用列表，为每个应用填充成员信息
        appVOList.forEach(appVO -> {
            // 跳过用户自己创建的应用
            if (loginUser.getId().equals(appVO.getUserId())) {
                return;
            }
            // 从成员信息映射表中获取当前应用对应的成员信息
            AppMember appMember = appMemberMap.get(appVO.getId());
            // 如果存在成员信息，则设置到 AppVO 对象中
            if (appMember != null) {
                appVO.setMyMemberRole(appMember.getMemberRole());
                appVO.setMyMemberStatus(appMember.getMemberStatus());
            }
        });
    }
}
