package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.mapper.ChatHistoryMapper;
import com.hl.hlaicodemother.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.ChatHistory;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(message == null || message.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(messageType == null || messageType.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 校验消息类型是否合法
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型不合法");
        // 创建对话历史对象
        ChatHistory chatHistory = ChatHistory.builder()
                .message(message)
                .messageType(messageType)
                .appId(appId)
                .userId(userId)
                .build();
        // 保存对话历史
        return this.save(chatHistory);
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize >= 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR, "登录用户不能为空");
        // 获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 校验应用ID和用户ID是否匹配
        appService.checkAppOwner(app, loginUser);
        // 构建查询条件
        ChatHistoryQueryRequest chatHistoryQueryRequest = new ChatHistoryQueryRequest();
        chatHistoryQueryRequest.setAppId(appId);
        chatHistoryQueryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(chatHistoryQueryRequest);
        // 分页查询对话历史
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", chatHistoryQueryRequest.getId())
                .eq("app_id", chatHistoryQueryRequest.getAppId())
                .like("message", chatHistoryQueryRequest.getMessage())
                .eq("messageType", chatHistoryQueryRequest.getMessageType())
                .eq("userId", chatHistoryQueryRequest.getUserId())
                .lt("createTime", chatHistoryQueryRequest.getLastCreateTime());
        if (StrUtil.isNotBlank(chatHistoryQueryRequest.getSortField())) {
            queryWrapper.orderBy(chatHistoryQueryRequest.getSortField(),
                    "ascend".equals(chatHistoryQueryRequest.getSortOrder()));
        } else {
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }
}
