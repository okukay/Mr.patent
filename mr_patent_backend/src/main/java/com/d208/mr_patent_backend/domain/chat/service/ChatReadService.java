package com.d208.mr_patent_backend.domain.chat.service;

import com.d208.mr_patent_backend.domain.chat.entity.ChatMessage;
import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import com.d208.mr_patent_backend.domain.chat.repository.ChatMessageRepository;
import com.d208.mr_patent_backend.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatReadService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 채팅방에 사용자가 입장했을 때 호출됨
     * 상대방이 보낸 메시지 중 안읽은 메시지를 읽음 처리하고,
     * 상대방의 unreadCount를 0으로 초기화함
     */
    @Transactional
    public void handleUserEntered(String roomId, Integer userId) {

        //  내 row 조회
        ChatRoom myRoom = chatRoomRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        // 내 row가 아닌 상대방의 row만 처리
        // 내가 보낸 메시지는 내가 읽어도 read 처리 X
        for (ChatRoom room : chatRoomRepository.findByRoomId(roomId)) {
            if (!room.getUserId().equals(userId)) {
                // 3. 상대방이 보낸 메시지 중 읽지 않은 것만 가져오기
                List<ChatMessage> unreadMessages = chatMessageRepository.findByRoomIdAndUserIdAndReadFalse(
                        roomId, room.getUserId()
                );

                //  메시지 읽음 처리 true 로 변경
                for (ChatMessage msg : unreadMessages) {
                    msg.setRead(true);
                }
                chatMessageRepository.saveAll(unreadMessages);

                //  내 unreadCount = 0 처리 (내가 읽었으니까)
                myRoom.setUnreadCount(0);
                chatRoomRepository.save(myRoom);

                break;
            }
        }
    }
}
