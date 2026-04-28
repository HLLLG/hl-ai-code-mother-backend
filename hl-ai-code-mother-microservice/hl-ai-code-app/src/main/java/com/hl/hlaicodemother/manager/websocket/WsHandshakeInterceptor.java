package com.hl.hlaicodemother.manager.websocket;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.innerservice.InnerUserService;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.AppMember;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.service.AppMemberService;
import com.hl.hlaicodemother.service.AppService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private AppService appService;

    @Resource
    private AppMemberService appMemberService;

    /**
     * WebSocket 握手前拦截处理，执行完整的权限验证流程
     *
     * <p>该方法在 WebSocket 握手请求建立连接之前执行，负责验证客户端请求的合法性。</p>
     *
     * <p>验证流程包括：</p>
     * <ul>
     *     <li>校验请求类型是否为 Servlet 请求</li>
     *     <li>校验应用参数是否存在且格式正确</li>
     *     <li>校验用户登录状态</li>
     *     <li>校验应用是否存在</li>
     *     <li>校验用户是否为应用成员</li>
     *     <li>校验用户权限（必须为活跃成员且非访客角色）</li>
     * </ul>
     *
     * <p>验证通过时会将 appId、用户信息、成员信息存入 attributes 供后续使用。</p>
     *
     * @param request WebSocket 握手请求，包含客户端发送的 HTTP 请求信息
     * @param response WebSocket 握手响应，可用于设置响应头等信息
     * @param wsHandler WebSocket 处理器，用于处理后续的 WebSocket 通信
     * @param attributes 握手属性映射表，用于在握手过程中传递数据到 WebSocket 会话
     * @return 验证通过返回 true 允许握手，验证失败返回 false 拒绝握手并中断连接
     * @throws Exception 握手处理过程中可能抛出的异常
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 校验请求类型，仅支持 Servlet 请求
        if (!(request instanceof ServletServerHttpRequest)) {
            log.error("非 Servlet 请求，拒绝握手");
            return false;
        }

        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

        // 获取并校验应用参数
        String appId = servletRequest.getParameter("appId");
        if (StrUtil.isBlank(appId)) {
            log.error("缺少应用参数，拒绝握手");
            return false;
        }

        // 解析应用 ID 为 Long 类型，防止非法格式
        Long appIdLong;
        try {
            appIdLong = Long.valueOf(appId);
        } catch (NumberFormatException e) {
            log.error("非法的应用参数格式：{}", appId);
            return false;
        }

        // 获取并校验用户登录状态
        User loginUser = InnerUserService.getLoginUser(servletRequest);
        if (loginUser == null) {
            log.error("用户未登录，拒绝握手");
            return false;
        }

        // 校验应用是否存在
        App app = appService.getById(appId);
        if (app == null) {
            log.error("应用不存在，拒绝握手，应用 ID: {}", appId);
            return false;
        }

        // 校验用户应用成员关系
        AppMember appMember = appMemberService.getAppMember(app.getId(), loginUser.getId());
        if (appMember == null) {
            log.error("用户未加入应用，拒绝握手，应用 ID: {}, 用户 ID: {}", appId, loginUser.getId());
            return false;
        }

        // 校验用户权限：应用成员均可接入围观协作，进入对话状态会在消息处理阶段再做 owner/editor 校验
        if (!appMemberService.isActiveMember(app.getId(), loginUser.getId())) {
            log.error("用户不是活跃应用成员，拒绝握手，应用 ID: {}, 用户 ID: {}",
                    appId, loginUser.getId());
            return false;
        }

        // 设置握手属性，供后续 WebSocket 会话使用
        attributes.put("appId", appIdLong);
        attributes.put("user", loginUser);
        attributes.put("appMember", appMember);

        log.info("用户 {} 握手成功，应用 {}", loginUser.getUserName(), app.getAppName());
        return true;
    }



    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}
