package com.d208.mr_patent_backend.domain.chat.repository;

import com.d208.mr_patent_backend.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    List<ChatMessage> findByRoomIdOrderByTimestampAsc(String roomId); // 채팅방Id

    Page<ChatMessage> findByRoomIdOrderByChatIdDesc(String roomId, Pageable pageable);

    // 무한 스크롤: 특정 메시지 ID 이전 메시지들 최신순 조회
    List<ChatMessage> findByRoomIdAndChatIdLessThanOrderByChatIdDesc(String roomId, Long chatId, Pageable pageable);

    // roomId 중에서 userId가 보낸 메시지 중에 안읽은 메세지만 가져오가
    List<ChatMessage> findByRoomIdAndUserIdAndReadFalse(String roomId, Integer userId);

}

