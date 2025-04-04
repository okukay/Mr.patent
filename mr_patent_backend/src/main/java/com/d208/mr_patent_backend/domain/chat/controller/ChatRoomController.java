package com.d208.mr_patent_backend.domain.chat.controller;

import com.d208.mr_patent_backend.domain.chat.dto.ChatListDto;
import com.d208.mr_patent_backend.domain.chat.dto.ChatMessageDto;
import com.d208.mr_patent_backend.domain.chat.dto.ChatRoomCreateRequest;
import com.d208.mr_patent_backend.domain.chat.service.ChatRoomService;


import com.d208.mr_patent_backend.domain.chat.service.ChatService;
import com.d208.mr_patent_backend.domain.chat.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Tag(name = "채팅방 API", description = "채팅방 생성/목록/대화내용 불러오기 ")
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final SseService sseService;


    @Operation(summary = "채팅방 목록 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getChatRoomsByUserId(@PathVariable Integer userId) {
        List<ChatListDto> chatRooms = chatRoomService.getChatRoomsByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("data", chatRooms);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "채팅방 생성")
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createChatRoom(@RequestBody ChatRoomCreateRequest request) {
        String roomId = chatRoomService.createChatRoom(request);
        Map<String, Object> response = new HashMap<>();
        response.put("data", roomId);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "대화 내용 불러오기")
    @GetMapping("/message/{roomId}")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable String roomId,
            @RequestParam(required = false) Long lastMessageId // 파라미터 없어도 괜찮음(첫 대화 불러오기 때문에)
    ) {
        int size = 10;
        List<ChatMessageDto> messages = chatService.getMessages(roomId, lastMessageId, size);
        Map<String, Object> response = new HashMap<>();
        response.put("data", messages);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "SSE 연결")
    @GetMapping("/subscribe/{userId}")
    public SseEmitter subscribe(@PathVariable Integer userId) {
        return sseService.subscribe(userId);
    }
}
