package com.d208.mr_patent_backend.domain.fcm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer userId;

    @Column(nullable = false, length = 512)
    private String token;

    private LocalDateTime updatedAt;

}
