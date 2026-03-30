package com.hl.hlaicodemother.controller;

import com.hl.hlaicodemother.annotation.AuthCheck;
import com.hl.hlaicodemother.common.BaseResponse;
import com.hl.hlaicodemother.common.ResultUtils;
import com.hl.hlaicodemother.constant.UserConstant;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.vo.ChatHistoryVO;
import com.hl.hlaicodemother.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import com.hl.hlaicodemother.model.entity.ChatHistory;
import com.hl.hlaicodemother.service.ChatHistoryService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页获取应用的对话历史记录。
     * @param appId
     * @param pageSize
     * @param lastCreatTime
     * @param request
     * @return
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistoryVO>> listChatHistory(@PathVariable Long appId,
                                                           @RequestParam(defaultValue = "10") int pageSize,
                                                           @RequestParam(required = false) LocalDateTime lastCreatTime,
                                                           HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistoryVO> chatHistoryPage =
                chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreatTime, loginUser);
        return ResultUtils.success(chatHistoryPage);
    }

    /**
     * 导出并下载应用的对话历史 Markdown 文档。
     * @param appId
     * @param request
     * @return
     */
    @GetMapping("/app/{appId}/export/md")
    public ResponseEntity<org.springframework.core.io.Resource> downloadChatHistoryMd(@PathVariable Long appId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        File mdFile = chatHistoryService.exportChatHistoryToMd(appId, loginUser);
        org.springframework.core.io.Resource resource = new FileSystemResource(mdFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(mdFile.getName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .contentLength(mdFile.length())
                .body(resource);
    }

    /**
     * 分页获取所有对话历史记录（管理员接口）。
     * @param chatHistoryQueryRequest
     * @return
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(pageNum, pageSize), chatHistoryService.getQueryWrapper(chatHistoryQueryRequest));
        return ResultUtils.success(chatHistoryPage);
    }
}
