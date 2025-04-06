package com.d208.mr_patent_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import com.d208.mr_patent_backend.domain.category.entity.ExpertCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expert")
@Getter
@Setter
@NoArgsConstructor
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_id")
    private Integer expertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expert_identification", length = 255, nullable = false)
    private String expertIdentification;

    @Column(name = "expert_description", length = 500, nullable = false)
    private String expertDescription;

    @Column(name = "expert_address", length = 30, nullable = false)
    private String expertAddress;

    @Column(name = "expert_phone", length = 15, nullable = false)
    private String expertPhone;

    @Column(name = "expert_get_date", nullable = false)
    private LocalDate expertGetDate;

    @Column(name = "expert_license", length = 255, nullable = false)
    private String expertLicense;

    @Column(name = "expert_status", nullable = false)
    private Integer expertStatus = 0;  // 기본값: 미승인

    @Column(name = "expert_created_at", nullable = false)
    private LocalDateTime expertCreatedAt;

    @Column(name = "expert_updated_at")
    private LocalDateTime expertUpdatedAt;

    @OneToMany(mappedBy = "expert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpertCategory> expertCategory = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.expertCreatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.expertUpdatedAt = LocalDateTime.now();
    }

    // 연관관계 편의 메서드
    public void addExpertCategory(ExpertCategory expertCategory) {
        this.expertCategory.add(expertCategory);
        expertCategory.setExpert(this);
    }
}