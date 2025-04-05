package com.d208.mr_patent_backend.domain.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

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
                    .data("connected"));
        }
        catch (IOException e) {
            e.printStackTrace();
            emitter.completeWithError(e);
        }
        System.out.println("연결시 확인 메세지 전송완료");
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
