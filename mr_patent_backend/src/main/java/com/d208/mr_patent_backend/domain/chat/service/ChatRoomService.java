package com.d208.mr_patent_backend.domain.chat.service;

import com.d208.mr_patent_backend.domain.chat.dto.ChatListDto;
import com.d208.mr_patent_backend.domain.chat.dto.ChatRoomCreateRequest;
import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import com.d208.mr_patent_backend.domain.chat.repository.ChatRoomRepository;
import com.d208.mr_patent_backend.domain.s3.service.S3Service;
import com.d208.mr_patent_backend.domain.user.entity.Expert;
import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.ExpertRepository;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final ExpertRepository expertRepository;


    // 채팅방 생성(2개)
    @Transactional
    public String createChatRoom(ChatRoomCreateRequest request ) {

        Instant now = Instant.now();
        Integer userId = request.getUserId();
        Integer receiverId = request.getReceiverId();

        Optional<ChatRoom> existing = chatRoomRepository.findByUserIdAndReceiverId(userId, receiverId);

        if (existing.isPresent()) {
            return existing.get().getRoomId(); // 기존 roomId 반환
        }

        String roomId = UUID.randomUUID().toString();


        // 유저용 채팅방
        ChatRoom userRoom = ChatRoom.builder()
                .roomId(roomId)
                .userId(userId)
                .receiverId(receiverId)
                .expertId(receiverId)
                .lastMessage(null)
                .unreadCount(0)
                .status(0)
                .created(now)
                .updated(now)
                .build();

        // 변리사용 채팅방
        ChatRoom receiverRoom = ChatRoom.builder()
                .roomId(roomId)
                .userId(receiverId)
                .receiverId(userId)
                .lastMessage(null)
                .unreadCount(0)
                .status(0)
                .created(now)
                .updated(now)
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
                .map(room -> {
                    //  상대방 정보 조회
                    User receiver = userRepository.findById(room.getReceiverId())
                            .orElseThrow(() -> new RuntimeException("상대방 정보 없음"));

                    //  Presigned URL 생성
                    String downUrl = null;
                    String userImage = receiver.getUserImage();
                    if (userImage != null && !userImage.isBlank()) {
                        downUrl = s3Service.generatePresignedDownloadUrl(userImage);
                    }
//

                    // receiverId를 가지고 expert인지 확인하고 expertId 반환
                    Integer receiverId = room.getReceiverId();
                    Expert expert = expertRepository.findByUser_UserId(receiverId); //메세지 보낸userId로 expert 인지 확인
                    Integer expertId = expert != null ? expert.getExpertId() : -1; // 맞으면 expertId 추출 아니면 null

//

                    return ChatListDto.builder()
                            .userId(room.getUserId())         // 로그인한 사용자 ID
                            .expertId(expertId)    // 전문가 ID (사용한다면)
                            .roomId(room.getRoomId())
                            .receiverId(room.getReceiverId()) // 상대방 ID
                            .unreadCount(room.getUnreadCount())
                            .userName(receiver.getUserName()) // 상대방 이름
                            .userImage(downUrl)              // S3 이미지 Presigned URL
                            .lastMessage(room.getLastMessage())
                            .lastTimestamp(room.getLastTimestamp())
                            .build();
                })
                .collect(Collectors.toList());
    }
}