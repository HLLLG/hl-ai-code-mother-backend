package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.dto.appMember.AppMemberQueryRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.AppMember;
import com.hl.hlaicodemother.mapper.AppMemberMapper;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.AppMemberRoleEnum;
import com.hl.hlaicodemother.model.enums.AppMemberStatusEnum;
import com.hl.hlaicodemother.model.vo.AppMemberVO;
import com.hl.hlaicodemother.model.vo.UserVO;
import com.hl.hlaicodemother.service.AppMemberService;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用成员表 服务层实现。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Service
public class AppMemberServiceImpl extends ServiceImpl<AppMemberMapper, AppMember> implements AppMemberService {

    @Resource
    @Lazy
    private AppService appService;

    @Resource
    private UserService userService;

    @Override
    public boolean addOwnerMember(Long appId, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不合法");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        LocalDateTime now = LocalDateTime.now();
        AppMember oldAppMember = getAppMember(appId, userId);
        if (oldAppMember != null) {
            AppMember updateAppMember = new AppMember();
            updateAppMember.setId(oldAppMember.getId());
            updateAppMember.setMemberRole(AppMemberRoleEnum.OWNER.getValue());
            updateAppMember.setMemberStatus(AppMemberStatusEnum.ACTIVE.getValue());
            updateAppMember.setInvitedBy(userId);
            updateAppMember.setLastActiveTime(now);
            return this.updateById(updateAppMember);
        }
        AppMember appMember = AppMember.builder()
                .appId(appId)
                .userId(userId)
                .memberRole(AppMemberRoleEnum.OWNER.getValue())
                .memberStatus(AppMemberStatusEnum.ACTIVE.getValue())
                .invitedBy(userId)
                .lastActiveTime(now)
                .build();
        return this.save(appMember);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long inviteMember(Long appId, Long inviteUserId, String memberRole, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不合法");
        ThrowUtils.throwIf(inviteUserId == null || inviteUserId <= 0, ErrorCode.PARAMS_ERROR, "被邀请用户 id 不合法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        checkAppOwner(app, loginUser);
        User inviteUser = userService.getById(inviteUserId);
        ThrowUtils.throwIf(inviteUser == null, ErrorCode.NOT_FOUND_ERROR, "被邀请用户不存在");
        String validMemberRole = validInviteMemberRole(memberRole);
        AppMember oldAppMember = getAppMember(appId, inviteUserId);
        if (oldAppMember != null) {
            Integer oldStatus = oldAppMember.getMemberStatus();
            if (AppMemberStatusEnum.ACTIVE.getValue().equals(oldStatus)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户已是应用成员");
            }
            if (AppMemberStatusEnum.PENDING.getValue().equals(oldStatus)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户已在待接受列表中");
            }
            AppMember updateAppMember = new AppMember();
            updateAppMember.setId(oldAppMember.getId());
            updateAppMember.setMemberRole(validMemberRole);
            updateAppMember.setMemberStatus(AppMemberStatusEnum.PENDING.getValue());
            updateAppMember.setInvitedBy(loginUser.getId());
            updateAppMember.setLastActiveTime(null);
            boolean result = this.updateById(updateAppMember);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "邀请成员失败");
            return oldAppMember.getId();
        }
        AppMember appMember = AppMember.builder()
                .appId(appId)
                .userId(inviteUserId)
                .memberRole(validMemberRole)
                .memberStatus(AppMemberStatusEnum.PENDING.getValue())
                .invitedBy(loginUser.getId())
                .build();
        boolean result = this.save(appMember);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "邀请成员失败");
        return appMember.getId();
    }

    @Override
    public boolean acceptInvite(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不合法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        AppMember appMember = getAppMember(appId, loginUser.getId());
        ThrowUtils.throwIf(appMember == null, ErrorCode.NOT_FOUND_ERROR, "邀请记录不存在");
        ThrowUtils.throwIf(!AppMemberStatusEnum.PENDING.getValue().equals(appMember.getMemberStatus()),
                ErrorCode.OPERATION_ERROR, "当前邀请状态不可接受");
        AppMember updateAppMember = new AppMember();
        updateAppMember.setId(appMember.getId());
        updateAppMember.setMemberStatus(AppMemberStatusEnum.ACTIVE.getValue());
        updateAppMember.setLastActiveTime(LocalDateTime.now());
        return this.updateById(updateAppMember);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMemberRole(Long appId, Long userId, String memberRole, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不合法");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        checkAppOwner(app, loginUser);
        AppMember appMember = getAppMember(appId, userId);
        ThrowUtils.throwIf(appMember == null, ErrorCode.NOT_FOUND_ERROR, "应用成员不存在");
        ThrowUtils.throwIf(app.getUserId().equals(userId), ErrorCode.OPERATION_ERROR, "应用 owner 角色不可修改");
        ThrowUtils.throwIf(!AppMemberStatusEnum.ACTIVE.getValue().equals(appMember.getMemberStatus()),
                ErrorCode.OPERATION_ERROR, "只能修改已加入成员的角色");
        AppMemberRoleEnum memberRoleEnum = AppMemberRoleEnum.getEnumByValue(memberRole);
        ThrowUtils.throwIf(memberRoleEnum == null, ErrorCode.PARAMS_ERROR, "成员角色不合法");
        ThrowUtils.throwIf(AppMemberRoleEnum.OWNER.equals(memberRoleEnum), ErrorCode.OPERATION_ERROR,
                "应用 owner 唯一且不可通过该接口修改");
        AppMember updateAppMember = new AppMember();
        updateAppMember.setId(appMember.getId());
        updateAppMember.setMemberRole(memberRoleEnum.getValue());
        return this.updateById(updateAppMember);
    }

    @Override
    public boolean removeMemberById(Long id, User loginUser) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "成员 id 不合法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        AppMember appMember = this.getById(id);
        ThrowUtils.throwIf(appMember == null, ErrorCode.NOT_FOUND_ERROR, "应用成员不存在");
        App app = appService.getById(appMember.getAppId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        checkAppOwner(app, loginUser);
        ThrowUtils.throwIf(appMember.getUserId().equals(app.getUserId()), ErrorCode.OPERATION_ERROR, "owner 不能被移除");
        AppMember updateAppMember = new AppMember();
        updateAppMember.setId(appMember.getId());
        updateAppMember.setMemberStatus(AppMemberStatusEnum.REMOVED.getValue());
        return this.updateById(updateAppMember);
    }

    @Override
    public Page<AppMember> listAppMemberByPage(AppMemberQueryRequest appMemberQueryRequest, User loginUser) {
        ThrowUtils.throwIf(appMemberQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        ThrowUtils.throwIf(appMemberQueryRequest.getAppId() == null || appMemberQueryRequest.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用 id 不合法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appMemberQueryRequest.getAppId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        checkCanViewAppMembers(app, loginUser);
        return this.page(Page.of(appMemberQueryRequest.getPageNum(), appMemberQueryRequest.getPageSize()),
                getQueryWrapper(appMemberQueryRequest));
    }

    @Override
    public Page<AppMemberVO> listAppMemberVOByPage(AppMemberQueryRequest appMemberQueryRequest, User loginUser) {
        Page<AppMember> appMemberPage = listAppMemberByPage(appMemberQueryRequest, loginUser);
        Page<AppMemberVO> appMemberVOPage = new Page<>(appMemberPage.getPageNumber(), appMemberPage.getPageSize(),
                appMemberPage.getTotalRow());
        appMemberVOPage.setRecords(getAppMemberVOList(appMemberPage.getRecords()));
        return appMemberVOPage;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppMemberQueryRequest appMemberQueryRequest) {
        ThrowUtils.throwIf(appMemberQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", appMemberQueryRequest.getId())
                .eq("appId", appMemberQueryRequest.getAppId())
                .eq("userId", appMemberQueryRequest.getUserId())
                .eq("memberRole", appMemberQueryRequest.getMemberRole())
                .eq("memberStatus", appMemberQueryRequest.getMemberStatus())
                .eq("invitedBy", appMemberQueryRequest.getInvitedBy());
        if (StrUtil.isNotBlank(appMemberQueryRequest.getSortField())) {
            queryWrapper.orderBy(appMemberQueryRequest.getSortField(),
                    "ascend".equals(appMemberQueryRequest.getSortOrder()));
        } else {
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    @Override
    public AppMember getAppMember(Long appId, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 不合法");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        return this.getOne(QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId));
    }

    @Override
    public List<Long> listVisibleAppIdsByUserId(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        return this.list(QueryWrapper.create()
                        .eq("userId", userId))
                .stream()
                .filter(appMember -> AppMemberStatusEnum.PENDING.getValue().equals(appMember.getMemberStatus())
                        || AppMemberStatusEnum.ACTIVE.getValue().equals(appMember.getMemberStatus()))
                .map(AppMember::getAppId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, AppMember> getAppMemberMap(Collection<Long> appIds, Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 不合法");
        if (appIds == null || appIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return this.list(QueryWrapper.create()
                        .in("appId", appIds)
                        .eq("userId", userId))
                .stream()
                .collect(Collectors.toMap(AppMember::getAppId, appMember -> appMember, (left, right) -> left));
    }

    @Override
    public boolean isActiveMember(Long appId, Long userId) {
        AppMember appMember = getAppMember(appId, userId);
        return appMember != null && AppMemberStatusEnum.ACTIVE.getValue().equals(appMember.getMemberStatus());
    }

    private void checkAppOwner(App app, User loginUser) {
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NOT_AUTH_ERROR, "只有应用 owner 才能操作成员");
    }

    private void checkCanViewAppMembers(App app, User loginUser) {
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        if (app.getUserId().equals(loginUser.getId())) {
            return;
        }
        ThrowUtils.throwIf(!isActiveMember(app.getId(), loginUser.getId()),
                ErrorCode.NOT_AUTH_ERROR, "无权查看应用成员");
    }

    private String validInviteMemberRole(String memberRole) {
        String validMemberRole = StrUtil.blankToDefault(memberRole, AppMemberRoleEnum.EDITOR.getValue());
        AppMemberRoleEnum memberRoleEnum = AppMemberRoleEnum.getEnumByValue(validMemberRole);
        ThrowUtils.throwIf(memberRoleEnum == null, ErrorCode.PARAMS_ERROR, "成员角色不合法");
        ThrowUtils.throwIf(AppMemberRoleEnum.OWNER.equals(memberRoleEnum), ErrorCode.PARAMS_ERROR,
                "邀请成员时不支持直接指定 owner 角色");
        return validMemberRole;
    }

    private List<AppMemberVO> getAppMemberVOList(List<AppMember> appMemberList) {
        if (appMemberList == null || appMemberList.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIdSet = appMemberList.stream().map(AppMember::getUserId).collect(Collectors.toSet());
        List<User> userList = userService.listByIds(userIdSet);
        Map<Long, UserVO> userVOMap = new HashMap<>();
        if (userList != null && !userList.isEmpty()) {
            userVOMap = userList.stream().collect(Collectors.toMap(User::getId, userService::getUserVO));
        }
        Map<Long, UserVO> finalUserVOMap = userVOMap;
        return appMemberList.stream().map(appMember -> {
            AppMemberVO appMemberVO = new AppMemberVO();
            BeanUtils.copyProperties(appMember, appMemberVO);
            appMemberVO.setUser(finalUserVOMap.get(appMember.getUserId()));
            return appMemberVO;
        }).collect(Collectors.toList());
    }

}
