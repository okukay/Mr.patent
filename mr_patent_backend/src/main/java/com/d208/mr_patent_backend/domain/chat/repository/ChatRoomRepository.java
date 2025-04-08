package com.d208.mr_patent_backend.domain.chat.repository;

import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {
//    List<ChatRoom> findByUserId(Integer userId);
    List<ChatRoom> findByUserIdAndLastMessageIsNotNull(Integer userId);
    Optional<ChatRoom> findByRoomIdAndUserId(String roomId, Integer userId);
    Optional<ChatRoom> findByUserIdAndReceiverId(Integer userId, Integer receiverId);
    Optional<ChatRoom> findBySessionId(String sessionId);
    List<ChatRoom> findByRoomId(String roomId);
    List<ChatRoom> findBySessionIdIsNotNull();

}
