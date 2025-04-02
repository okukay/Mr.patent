package com.d208.mr_patent_backend.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Table(name = "chat_room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@IdClass(ChatRoomId.class)

public class ChatRoom {
    @Id
    @Column(length = 36)
    private String roomId; // UUID 값을 준다

    @Id
    private Integer userId;
    private Integer receiverId;
    private Integer status;
    private Integer unreadCount;
    private String sessionId;

    @Column(columnDefinition = "TEXT")
    private String lastMessage;
    private LocalDateTime lastTimestamp;
    private LocalDateTime created;
    private LocalDateTime updated;

}
