package com.d208.mr_patent_backend.domain.chat.service;

import com.d208.mr_patent_backend.domain.chat.dto.ChatListDto;
import com.d208.mr_patent_backend.domain.chat.dto.ChatRoomCreateRequest;
import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import com.d208.mr_patent_backend.domain.chat.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    // 채팅방 생성(2개)
    @Transactional
    public String createChatRoom(ChatRoomCreateRequest request ) {

        Integer userId = request.getUserId();
        Integer receiverId = request.getReceiverId();

        Optional<ChatRoom> existing = chatRoomRepository.findByUserIdAndReceiverId(userId, receiverId);

        if (existing.isPresent()) {
            return existing.get().getRoomId(); // 기존 roomId 반환
        }

        String roomId = UUID.randomUUID().toString();


        // 사용자 A용
        ChatRoom userRoom = ChatRoom.builder()
                .roomId(roomId)
                .userId(userId)
                .receiverId(receiverId)
                .lastMessage(null)
                .unreadCount(0)
                .status(0)
                .expertName(request.getExpertName())
                .expertImage(request.getExpertImage())
                .created(LocalDateTime.now())
                .updated(LocalDateTime.now())
                .build();

        // 사용자 B용
        ChatRoom receiverRoom = ChatRoom.builder()
                .roomId(roomId)
                .userId(receiverId)
                .receiverId(userId)
                .lastMessage(null)
                .unreadCount(0)
                .status(0)
                .expertName(request.getExpertName())
                .expertImage(request.getExpertImage())
                .created(LocalDateTime.now())
                .updated(LocalDateTime.now())
                .build();

        chatRoomRepository.save(userRoom);
        chatRoomRepository.save(receiverRoom);

        return roomId; // 클라이언트에게 전달
    }


    // userId에 따른 채팅방 목록 조회
    public List<ChatListDto> getChatRoomsByUserId(Integer userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserIdAndLastMessageIsNotNull(userId);

        //(리스트 조회한걸 -> Dto 형식으로 변환)
        //room은 chatRooms 리스트 안의 각각의 요소
        return chatRooms.stream()
                .map(room -> ChatListDto.builder()
                        .roomId(room.getRoomId())
                        .userId(room.getUserId())
                        .expertName(room.getExpertName())
                        .expertImage(room.getExpertImage())
                        .unreadCount(room.getUnreadCount())
                        .lastMessage(room.getLastMessage())
                        .receiverId(room.getReceiverId())
                        .lastTimestamp(room.getLastTimestamp())
                        .build())
                .collect(Collectors.toList());
        }
}