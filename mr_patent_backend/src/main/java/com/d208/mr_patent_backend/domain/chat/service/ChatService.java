package com.d208.mr_patent_backend.domain.chat.service;
import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import com.d208.mr_patent_backend.domain.chat.dto.ChatMessageDto;
import com.d208.mr_patent_backend.domain.chat.entity.ChatMessage;
import com.d208.mr_patent_backend.domain.chat.repository.ChatMessageRepository;
import com.d208.mr_patent_backend.domain.chat.repository.ChatRoomRepository;
import com.d208.mr_patent_backend.domain.s3.service.S3Service;
import com.d208.mr_patent_backend.domain.user.entity.Expert;
import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.ExpertRepository;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SseService sseService;
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;



    //메세지 저장
    @Transactional
    public ChatMessageDto saveMessage(ChatMessageDto dto) {
        Instant now = Instant.now();

        String type = dto.getMessageType();
        //"TEXT"|"PDF"|"WORD"|"IMAGE"

        if (type != null) {
            switch (type) {
                case "IMAGE":
                    dto.setMessage("사진을 보냈습니다.");
                    break;
                case "WORD":
                case "PDF":
                    dto.setMessage("파일을 보냈습니다.");
                    break;
            }
        }
        ChatMessage message = ChatMessage.builder()
                .chatId(dto.getChatId())
                .roomId(dto.getRoomId())
                .userId(dto.getUserId())
                .receiverId(dto.getReceiverId())
                .message(dto.getMessage())
                .timestamp(now)
                .read(dto.isRead()) // 클라이언트가 보내주는데로 0 or 1 로 저장
                .type("CHAT")
                .messageType(dto.getMessageType())
                .fileUrl(dto.getFileUrl())
                .fileName(dto.getFileName())
                .build();

        chatMessageRepository.save(message);
        System.out.println(" 메시지 DB 저장 완료: " + dto.getMessage());


        // 3. ChatRoom row 가져오기
        ChatRoom senderRoom = chatRoomRepository.findByRoomIdAndUserId(dto.getRoomId(), dto.getUserId())
                .orElseThrow(() -> new RuntimeException("보낸 사람 채팅방이 없습니다."));

        ChatRoom receiverRoom = chatRoomRepository.findByRoomIdAndUserId(dto.getRoomId(), dto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("받는 사람 채팅방이 없습니다."));

        // 4. senderRoom 업데이트
        senderRoom.setLastMessage(message.getMessage());
        senderRoom.setLastTimestamp(now);
        senderRoom.setUpdated(now);

        // 5. receiverRoom 업데이트
        receiverRoom.setLastMessage(message.getMessage());
        receiverRoom.setLastTimestamp(now);
        if (!dto.isRead()) {
            receiverRoom.setUnreadCount(receiverRoom.getUnreadCount() + 1);
        }
        receiverRoom.setUpdated(now);

        // 6. 저장
        chatRoomRepository.save(senderRoom);
        chatRoomRepository.save(receiverRoom);

        System.out.println("채팅방 메타데이터 업데이트 완료");

        // 상대방 오프라인일 경우 -> sse연결되어있다면 -> sse전송
        if (!dto.isRead()) {
            if(sseService.isConnected(dto.getReceiverId())) {
                System.out.println("sse 연결확인");

                User user = userRepository.findById(dto.getUserId())
                        .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

                Expert expert = expertRepository.findByUser_UserId(dto.getUserId()); //메세지 보낸userId로 expert 인지 확인
                Integer expertId = expert != null ? expert.getExpertId() : -1; // 맞으면 expertId 추출 아니면 null

                // SSE 전송 로직 추가
                sseService.sendToUser(dto.getReceiverId(), Map.of(
                        "userId",dto.getUserId(),
                        "expertId", expertId,
                        "roomId", dto.getRoomId(),
                        "unreadCount", receiverRoom.getUnreadCount(),
                        "lastMessage", dto.getMessage(),
                        "userName" ,user.getUserName(),
                        "userImage", user.getUserImage(),
                        "receiverId", dto.getReceiverId(),
                        "timestamp", now

                ));
                System.out.println("sse 메세지 전송 완료");
            }

        }
        return ChatMessageDto.builder()
                .chatId(message.getChatId())
                .roomId(message.getRoomId())
                .userId(message.getUserId())
                .receiverId(message.getReceiverId())
                .message(message.getMessage())
                .timeStamp(message.getTimestamp())
                .read(message.isRead())
                .type(message.getType())
                .messageType(message.getMessageType())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .build();
    }


    // 대화내용 불러오기 (무한 스크롤)
    public List<ChatMessageDto> getMessages(String roomId, Long lastMessageId, int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "chatId"));

        List<ChatMessage> messages;

        if (lastMessageId == null) {
            // 처음 입장: 최신 메시지부터 size개 조회
            messages = chatMessageRepository.findByRoomIdOrderByChatIdDesc(roomId, pageable);
        } else {
            // 무한스크롤: 마지막 메시지 이전 메시지 size개 조회
            messages = chatMessageRepository.findByRoomIdAndChatIdLessThanOrderByChatIdDesc(roomId, lastMessageId, pageable);
        }

        List<ChatMessageDto> result = new ArrayList<>();
        for (ChatMessage entity : messages) {
            //  첨부 파일이 있는 경우 Presigned URL 발급
//            if (entity.getFileName() != null && (entity.getFileUrl() == null)) {
            if (entity.getFileName() != null) {
                String newUrl = s3Service.generatePresignedDownloadUrl(entity.getFileName());
                entity.setFileUrl(newUrl);

            }
            ChatMessageDto dto = ChatMessageDto.fromEntity(entity);
            result.add(dto);
        }

        return result;
    }

}