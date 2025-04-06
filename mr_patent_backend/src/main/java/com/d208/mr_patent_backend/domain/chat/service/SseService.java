package com.d208.mr_patent_backend.domain.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SseService {
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); // 1개의 스레드로 예약 작업 실행

    // SSE 연결 확인( true or false)
    public boolean isConnected(Integer userId) {
        return emitters.containsKey(userId);
    }


    // sse 연결
    public SseEmitter subscribe(Integer userId) { //새로운 SseEmitter 생성
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간 유효// 유효시간을 꼭 설정해야하는지?//다른페이지로 갔을때 자동으로 해제가 되는지?
        emitters.put(userId, emitter); //emitter를 만들어서 eemitters에 넣음
        System.out.println( userId + "emitter 생성 완료");

        // 연결 끊겼을 때 제거
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            System.out.println("emitter 연결해제(화면이동)");
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            System.out.println("emitter 연결해제 타임아웃");
        });

        emitter.onError(e -> {
            emitters.remove(userId);
            System.out.println("emitter 연결해제 에러발생");
        });
//        emitter.onCompletion(() -> emitters.remove(userId));
//        emitter.onTimeout(() -> emitters.remove(userId));
//        emitter.onError(e -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(Map.of("status", "connected")));
            System.out.println("✅ 초기 연결 메시지 전송 완료");
        } catch (IOException e) {
            e.printStackTrace();
            emitter.completeWithError(e);
        }

        System.out.println("연결시 첫번쨰확인 메세지 전송완료");
//        scheduler.schedule(() -> {
//            try {
//                if (emitters.containsKey(userId)) { // 여전히 연결된 상태일 때만 전송
//                    emitter.send(SseEmitter.event()
//                            .name("chat-update")
//                            .data(Map.of(
//                                    "roomId", "8224c9c2-424f-45e4-8d27-93d21b9bcabd",
//                                    "userName","수닝모",
//                                    "lastMessage", "test",
//                                    "unreadCount", 1
//                            )));
//                    System.out.println("🕒 5초 후 추가 메시지 전송 완료");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                emitter.completeWithError(e);
//            }
//        }, 5, TimeUnit.SECONDS);
//
//        System.out.println("연결시 모든 확인 메시지 전송 완료");

        return emitter;
    }

    // sse 메세지 전송
    public void sendToUser(Integer userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("chat-update")
                        .data(data));

            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
}
