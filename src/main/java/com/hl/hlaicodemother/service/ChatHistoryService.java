package com.hl.hlaicodemother.service;

import com.hl.hlaicodemother.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.hl.hlaicodemother.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.hl.hlaicodemother.model.entity.ChatHistory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {


    /**
     * 添加对话消息。
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);


    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastedCreateTime,
                                               User loginUser);


    /**
     * 获取查询条件包装器。
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

}

