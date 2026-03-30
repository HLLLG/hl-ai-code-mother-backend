package com.hl.hlaicodemother.controller;

import com.hl.hlaicodemother.common.BaseResponse;
import com.hl.hlaicodemother.common.DeleteRequest;
import com.hl.hlaicodemother.common.ResultUtils;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.dto.appMember.AppMemberAcceptRequest;
import com.hl.hlaicodemother.model.dto.appMember.AppMemberInviteRequest;
import com.hl.hlaicodemother.model.dto.appMember.AppMemberQueryRequest;
import com.hl.hlaicodemother.model.dto.appMember.AppMemberRoleUpdateRequest;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.vo.AppMemberVO;
import com.hl.hlaicodemother.service.AppMemberService;
import com.hl.hlaicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应用成员表 控制层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@RestController
@RequestMapping("/appMember")
public class AppMemberController {

    @Resource
    private AppMemberService appMemberService;

    @Resource
    private UserService userService;

    /**
     * 邀请应用成员。
     *
     * @param appMemberInviteRequest 邀请请求
     * @param request 请求
     * @return 成员记录 id
     */
    @PostMapping("/invite")
    public BaseResponse<Long> inviteMember(@RequestBody AppMemberInviteRequest appMemberInviteRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(appMemberInviteRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long appMemberId = appMemberService.inviteMember(appMemberInviteRequest.getAppId(),
                appMemberInviteRequest.getUserId(), appMemberInviteRequest.getMemberRole(), loginUser);
        return ResultUtils.success(appMemberId);
    }

    /**
     * 接受应用成员邀请。
     *
     * @param appMemberAcceptRequest 接受请求
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/accept")
    public BaseResponse<Boolean> acceptInvite(@RequestBody AppMemberAcceptRequest appMemberAcceptRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(appMemberAcceptRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = appMemberService.acceptInvite(appMemberAcceptRequest.getAppId(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 更新应用成员角色。
     *
     * @param appMemberRoleUpdateRequest 角色更新请求
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/update/role")
    public BaseResponse<Boolean> updateMemberRole(@RequestBody AppMemberRoleUpdateRequest appMemberRoleUpdateRequest,
                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(appMemberRoleUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = appMemberService.updateMemberRole(appMemberRoleUpdateRequest.getAppId(),
                appMemberRoleUpdateRequest.getUserId(),
                appMemberRoleUpdateRequest.getMemberRole(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 移除应用成员。
     *
     * @param deleteRequest 删除请求
     * @param request 请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> removeMember(@RequestBody DeleteRequest deleteRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = appMemberService.removeMemberById(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询应用成员。
     *
     * @param appMemberQueryRequest 查询请求
     * @param request 请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<AppMemberVO>> listAppMemberByPage(@RequestBody AppMemberQueryRequest appMemberQueryRequest,
                                                               HttpServletRequest request) {
        ThrowUtils.throwIf(appMemberQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<AppMemberVO> appMemberPage = appMemberService.listAppMemberVOByPage(appMemberQueryRequest, loginUser);
        return ResultUtils.success(appMemberPage);
    }
}
