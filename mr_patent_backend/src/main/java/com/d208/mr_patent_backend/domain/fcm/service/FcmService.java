package com.d208.mr_patent_backend.domain.fcm.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmService {

    public void sendMessageToToken(String targetToken, String title, String body, Map<String, String> data) {
        Notification notification = Notification.builder() // 화면에 보이는 알림 내용 (푸시 상단에 뜨는 제목/내용)
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder() //Firebase로 전송할 전체 메시지 구조
                .setToken(targetToken)
                .setNotification(notification)
                .putAllData(data)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message); //Firebase 서버에 메시지를 전송
            System.out.println("FCM 메시지 전송 성공: " + response);
            System.out.println(");

        } catch (FirebaseMessagingException e) {
            System.out.println("FCM 메시지 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
