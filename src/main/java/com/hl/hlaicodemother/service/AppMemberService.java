package com.hl.hlaicodemother.service;

import com.hl.hlaicodemother.model.dto.appMember.AppMemberQueryRequest;
import com.mybatisflex.core.service.IService;
import com.hl.hlaicodemother.model.entity.AppMember;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.vo.AppMemberVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 应用成员表 服务层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
public interface AppMemberService extends IService<AppMember> {

    /**
     * 创建应用时初始化 owner 成员记录
     *
     * @param appId 应用 id
     * @param userId 用户 id
     * @return 是否成功
     */
    boolean addOwnerMember(Long appId, Long userId);

    /**
     * 邀请应用成员
     *
     * @param appId 应用 id
     * @param inviteUserId 被邀请用户 id
     * @param memberRole 成员角色
     * @param loginUser 当前登录用户
     * @return 成员记录 id
     */
    Long inviteMember(Long appId, Long inviteUserId, String memberRole, User loginUser);

    /**
     * 接受应用邀请
     *
     * @param appId 应用 id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean acceptInvite(Long appId, User loginUser);

    /**
     * 更新应用成员角色
     *
     * @param appId 应用 id
     * @param userId 用户 id
     * @param memberRole 成员角色
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean updateMemberRole(Long appId, Long userId, String memberRole, User loginUser);

    /**
     * 根据 id 移除成员
     *
     * @param id 成员记录 id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean removeMemberById(Long id, User loginUser);

    /**
     * 分页查询应用成员
     *
     * @param appMemberQueryRequest 查询请求
     * @param loginUser 当前登录用户
     * @return 分页结果
     */
    Page<AppMember> listAppMemberByPage(AppMemberQueryRequest appMemberQueryRequest, User loginUser);

    /**
     * 分页查询应用成员视图
     *
     * @param appMemberQueryRequest 查询请求
     * @param loginUser 当前登录用户
     * @return 分页结果
     */
    Page<AppMemberVO> listAppMemberVOByPage(AppMemberQueryRequest appMemberQueryRequest, User loginUser);

    /**
     * 构造查询条件
     *
     * @param appMemberQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppMemberQueryRequest appMemberQueryRequest);

    /**
     * 根据应用和用户获取成员关系
     *
     * @param appId 应用 id
     * @param userId 用户 id
     * @return 成员关系
     */
    AppMember getAppMember(Long appId, Long userId);

    /**
     * 根据用户 id 获取可见应用 id 列表
     *
     * @param userId 用户 id
     * @return 应用 id 列表
     */
    List<Long> listVisibleAppIdsByUserId(Long userId);

    /**
     * 批量查询用户在多个应用中的成员关系
     *
     * @param appIds 应用 id 集合
     * @param userId 用户 id
     * @return 应用 id 到成员关系的映射
     */
    Map<Long, AppMember> getAppMemberMap(Collection<Long> appIds, Long userId);

    /**
     * 校验是否为应用有效成员
     *
     * @param appId 应用 id
     * @param userId 用户 id
     * @return 是否为已加入成员
     */
    boolean isActiveMember(Long appId, Long userId);
}
