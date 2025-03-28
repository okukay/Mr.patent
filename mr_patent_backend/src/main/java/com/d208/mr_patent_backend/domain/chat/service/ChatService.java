package com.d208.mr_patent_backend.domain.chat.service;
import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import com.d208.mr_patent_backend.domain.chat.dto.ChatMessageDto;
import com.d208.mr_patent_backend.domain.chat.entity.ChatMessage;
import com.d208.mr_patent_backend.domain.chat.repository.ChatMessageRepository;
import com.d208.mr_patent_backend.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, ChatRoomRepository chatRoomRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomRepository = chatRoomRepository;
    }
    @Transactional
    public void saveMessage(ChatMessageDto dto) {
        LocalDateTime now = dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now();

        ChatMessage message = ChatMessage.builder()
                .roomId(dto.getRoomId())
                .userId(dto.getUserId()) // 보낸 사람
                .message(dto.getMessage())
                .timestamp(now)
                .read(dto.isRead()) // 클라이언트가 보내주는데로 0 or 1 로 저장
                .type("CHAT")
                .build();
                     //메세지 타입
        chatMessageRepository.save(message);
        System.out.println(" 메시지 DB 저장 완료: " + dto.getMessage());


        // 3. ChatRoom row 가져오기
        ChatRoom senderRoom = chatRoomRepository.findByRoomIdAndUserId(dto.getRoomId(), dto.getUserId())
                .orElseThrow(() -> new RuntimeException("보낸 사람 채팅방이 없습니다."));

        ChatRoom receiverRoom = chatRoomRepository.findByRoomIdAndUserId(dto.getRoomId(), dto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("받는 사람 채팅방이 없습니다."));

        // 4. senderRoom 업데이트
        senderRoom.setLastMessage(dto.getMessage());
        senderRoom.setLastTimestamp(now);
        senderRoom.setUpdated(now);

        // 5. receiverRoom 업데이트
        receiverRoom.setLastMessage(dto.getMessage());
        receiverRoom.setLastTimestamp(now);
        receiverRoom.setUnreadCount(receiverRoom.getUnreadCount() + 1); // 안읽은 메시지 1 증가
        receiverRoom.setUpdated(now);

        // 6. 저장
        chatRoomRepository.save(senderRoom);
        chatRoomRepository.save(receiverRoom);

        System.out.println("채팅방 메타데이터 업데이트 완료");
    }

    public List<ChatMessageDto> getMessages(String roomId, Long lastMessageId, int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "chatId"));

        if (lastMessageId == null) {
            // 처음 입장: 최신 메시지부터 size개 조회
            return chatMessageRepository
                    .findByRoomIdOrderByChatIdDesc(roomId, pageable)
                    .stream()   //stream 형태로 변환( map 하기 위해서 )
                    .map(ChatMessageDto::fromEntity) //엔티티 -> dto 변환
                    .collect(Collectors.toList());  // collect 이용해서 다시 list로 합침
        } else {
            // 이전 메시지 불러오기 (무한스크롤)
            return chatMessageRepository
                    .findByRoomIdAndChatIdLessThanOrderByChatIdDesc(roomId, lastMessageId, pageable)
                    .stream()
                    .map(ChatMessageDto::fromEntity)
                    .collect(Collectors.toList());
        }
    }
}