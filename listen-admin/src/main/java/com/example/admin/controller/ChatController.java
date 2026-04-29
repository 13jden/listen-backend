package com.example.admin.controller;

import com.example.wx.Agent.ChatService;
import com.example.wx.Agent.dto.ChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "AI聊天接口")
@RestController
@RequestMapping("chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @Operation(summary = "流式聊天", description = "发送消息并接收AI流式响应")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@Parameter(description = "聊天请求") @RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }

    @Operation(summary = "测试接口")
    @GetMapping("/test")
    public String test() {
        return "test";
    }
}
