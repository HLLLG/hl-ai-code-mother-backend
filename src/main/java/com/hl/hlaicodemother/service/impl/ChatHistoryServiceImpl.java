package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.exception.BusinessException;
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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.markdown.MarkdownRenderer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

    private static final DateTimeFormatter CHAT_HISTORY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    public int loadChatHistoryToMemory(long appId, int maxCount, MessageWindowChatMemory chatMemory) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            // 查询对话历史记录
            List<ChatHistory> chatHistories = this.list(queryWrapper);
            if (CollUtil.isEmpty(chatHistories)) {
                return 0;
            }
            // 将查询结果倒序，以便按照时间顺序加载到内存中
            chatHistories = chatHistories.reversed();
            int loadCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory chatHistory : chatHistories) {
                if (chatHistory.getMessageType().equals(ChatHistoryMessageTypeEnum.USER.getValue())) {
                    chatMemory.add(new UserMessage(chatHistory.getMessage()));
                } else if (chatHistory.getMessageType().equals(ChatHistoryMessageTypeEnum.AI.getValue())) {
                    chatMemory.add(new AiMessage(chatHistory.getMessage()));
                }
                loadCount++;
            }
            log.info("成功为 appId：{} 加载了 {} 条历史对话",  appId, loadCount);
            return loadCount;
        } catch (Exception e) {
            // 记录日志
            log.error("加载对话历史到内存失败, appId: {}, error: {}", appId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public File exportChatHistoryToMd(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        appService.checkAppOwner(app, loginUser);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId)
                .orderBy(ChatHistory::getCreateTime, false);
        List<ChatHistory> chatHistories = this.list(queryWrapper);
        if (CollUtil.isNotEmpty(chatHistories)) {
            chatHistories = chatHistories.reversed();
        }
        try {
            File outputDir = FileUtil.mkdir(AppConstant.CHAT_HISTORY_MD_DIR);
            File mdFile = new File(outputDir, appId + ".md");
            String mdContent = buildChatHistoryMarkdown(appId, chatHistories);
            FileUtil.writeString(mdContent, mdFile, StandardCharsets.UTF_8);
            log.info("应用对话历史 Markdown 导出完成, appId: {}, filePath: {}, messageCount: {}",
                    appId, mdFile.getAbsolutePath(), CollUtil.size(chatHistories));
            return mdFile;
        } catch (Exception e) {
            log.error("应用对话历史 Markdown 导出失败, appId: {}", appId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "导出对话历史 Markdown 失败");
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("id", chatHistoryQueryRequest.getId())
                .eq("appId", chatHistoryQueryRequest.getAppId())
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

    private String buildChatHistoryMarkdown(Long appId, List<ChatHistory> chatHistories) {
        Document document = new Document();
        Heading title = new Heading();
        title.setLevel(1);
        title.appendChild(new Text(appId + " 应用的对话历史"));
        document.appendChild(title);

        Paragraph summary = new Paragraph();
        summary.appendChild(new Text("导出时间：" + LocalDateTime.now().format(CHAT_HISTORY_TIME_FORMATTER)));
        summary.appendChild(new SoftLineBreak());
        summary.appendChild(new Text("消息总数：" + CollUtil.size(chatHistories)));
        document.appendChild(summary);

        if (CollUtil.isEmpty(chatHistories)) {
            Paragraph emptyParagraph = new Paragraph();
            emptyParagraph.appendChild(new Text("暂无对话历史"));
            document.appendChild(emptyParagraph);
            return MarkdownRenderer.builder().build().render(document);
        }

        Parser parser = Parser.builder().build();
        int index = 1;
        for (ChatHistory chatHistory : chatHistories) {
            document.appendChild(new ThematicBreak());

            Heading messageTitle = new Heading();
            messageTitle.setLevel(2);
            messageTitle.appendChild(new Text("第 " + index + " 条 · " + getMessageTypeText(chatHistory.getMessageType())));
            document.appendChild(messageTitle);

            BulletList bulletList = new BulletList();
            bulletList.appendChild(createListItem("时间：" + formatCreateTime(chatHistory.getCreateTime())));
            if (chatHistory.getUserId() != null) {
                bulletList.appendChild(createListItem("用户 ID：" + chatHistory.getUserId()));
            }
            bulletList.appendChild(createListItem("消息类型：" + getMessageTypeText(chatHistory.getMessageType())));
            document.appendChild(bulletList);

            appendMarkdownChildren(document, parser.parse(StrUtil.blankToDefault(chatHistory.getMessage(), "（空消息）")));
            index++;
        }
        return MarkdownRenderer.builder().build().render(document);
    }

    private ListItem createListItem(String text) {
        ListItem listItem = new ListItem();
        Paragraph paragraph = new Paragraph();
        paragraph.appendChild(new Text(text));
        listItem.appendChild(paragraph);
        return listItem;
    }

    private void appendMarkdownChildren(Node parent, Node markdownDocument) {
        Node child = markdownDocument.getFirstChild();
        if (child == null) {
            Paragraph paragraph = new Paragraph();
            paragraph.appendChild(new Text("（空消息）"));
            parent.appendChild(paragraph);
            return;
        }
        while (child != null) {
            Node next = child.getNext();
            child.unlink();
            parent.appendChild(child);
            child = next;
        }
    }

    private String getMessageTypeText(String messageType) {
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        return messageTypeEnum != null ? messageTypeEnum.getText() : StrUtil.blankToDefault(messageType, "未知");
    }

    private String formatCreateTime(LocalDateTime createTime) {
        return createTime == null ? "未知时间" : createTime.format(CHAT_HISTORY_TIME_FORMATTER);
    }
}
