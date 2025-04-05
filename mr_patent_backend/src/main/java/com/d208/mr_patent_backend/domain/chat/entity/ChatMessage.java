package com.d208.mr_patent_backend.domain.chat.entity;

//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer chatId;

    private String roomId;
    private Integer userId;
    private Integer receiverId;

    @Column(columnDefinition = "TEXT")
    private String message;
    private Instant timestamp;

    @Column(name = "is_read")
    private boolean read; // DB에는 TINYINT형식으로 저장됨 (0 = false ,1 = true)
    private String type;                //메세지 타입
    private String messageType;

    @Column(length = 2000)
    private String fileUrl;             // 다운로드 url
    private String fileName;            // 파일이름


}

