package com.d208.mr_patent_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_email", length = 50, nullable = false, unique = true)
    private String userEmail;

    @Column(name = "user_pw", length = 255, nullable = false)  // 암호화를 위해 길이 늘림
    private String userPw;

    @Column(name = "user_name", length = 25, nullable = false)
    private String userName;

    @Column(name = "user_image", length = 255, nullable = false)
    private String userImage = "기본 이미지 경로";  // 기본값 설정

    @Column(name = "user_role", nullable = false)
    private Integer userRole = 0;  // 기본값: 일반회원

    @Column(name = "user_refresh_token", length = 300)
    private String userRefreshToken;

    @Column(name = "user_created_at", nullable = false)
    private LocalDateTime userCreatedAt;

    @Column(name = "user_updated_at")
    private LocalDateTime userUpdatedAt;

    @PrePersist
    public void prePersist() {
        this.userCreatedAt = LocalDateTime.now();
        this.userUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.userUpdatedAt = LocalDateTime.now();
    }
}
