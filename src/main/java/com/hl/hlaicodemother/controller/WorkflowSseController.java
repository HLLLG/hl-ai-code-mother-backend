package com.hl.hlaicodemother.controller;

import com.hl.hlaicodemother.langgraph4j.CodeGenWorkflow;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * 工作流sse控制器
 */
@RestController
@RequestMapping("/workflow")
@Slf4j
public class WorkflowSseController {

    /**
     * 同步执行工作流
     */
    @GetMapping("/execute")
    public WorkflowContext executeWorkflow(@RequestParam String prompt) {
        log.info("收到同步工作流执行请求：{}", prompt);
        // 创建工作流
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        // 执行工作流
        return workflow.executeWorkflow(prompt);
    }

    /**
     * 流式执行工作流
     */
    @GetMapping(value = "/execute-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeWorkflowWithFlux(@RequestParam String prompt) {
        log.info("收到 Flux 工作流执行请求：{}", prompt);
        // 创建工作流
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        // 执行工作流
        return workflow.executeWorkflowWithFlux(prompt);
    }

    /**
     * SSE 执行工作流
     */
    @GetMapping(value = "/execute-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflowWithSse(@RequestParam String prompt) {
        log.info("收到 SSE 工作流执行请求：{}", prompt);
        // 创建工作流
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        // 执行工作流
        return workflow.executeWorkflowWithSse(prompt);
    }
}
