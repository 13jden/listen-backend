package com.example.admin.controller;

import com.example.admin.service.ChatService;
import com.example.admin.service.dto.ChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }
}
