package com.d208.mr_patent_backend.global.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class FirebaseConfig {

    @Value("${spring.firebase.key-path}") //application.yml에 정의된 값을 주입 받을 수 있게함
    String fcmKeyPath;

    private static boolean isFirebaseInitialized = false;
    //의존성 주입이 끝난 후에 자동으로 실행되는 메서드
    //즉 서버가 실행되면 이 메서드가 한 번 실행되며 Firebase를 초기화 할거임
    @PostConstruct
    public void getFcmCredential(){
        try {
            InputStream refreshToken = new ClassPathResource(fcmKeyPath).getInputStream();
            FirebaseOptions options = FirebaseOptions.builder() //Firebase Admin SDK에서 사용할 인증 정보 설정
                    .setCredentials(GoogleCredentials.fromStream(refreshToken)).build();


            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                isFirebaseInitialized = true;
                log.info("FCM 초기화 완료");
            } else {
                isFirebaseInitialized = true;
                log.info("FCM은 이미 초기화되어 있음");
            }
        } catch (IOException e) {
            log.error("Fcm 연결 오류 {}", e.getMessage());
            isFirebaseInitialized = false;
        }
    }   public static boolean isFirebaseInitialized() {
        return isFirebaseInitialized;
    }
}
