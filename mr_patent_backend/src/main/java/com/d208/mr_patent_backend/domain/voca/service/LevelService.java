package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.domain.voca.dto.level.LevelDTO;
import com.d208.mr_patent_backend.domain.voca.entity.UserLevel;
import com.d208.mr_patent_backend.domain.voca.repository.UserLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final UserRepository userRepository;
    private final UserLevelRepository userLevelRepository;

    @Transactional
    public List<LevelDTO> getUserLevels(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<UserLevel> userLevels = getUserLevelsOrCreateInitial(user);

        createMissingLevels(user, userLevels);

        List<LevelDTO> result = buildAllLevels(user, userLevels);

        // 로깅 추가
        for (LevelDTO level : result) {
            System.out.println("Level " + level.getLevel_id() +
                    ": Best Score = " + level.getBest_score() +
                    ", Accessible = " + level.is_accessible() +
                    ", Passed = " + level.is_passed());
        }

        return result;
    }

    /**
     * 사용자의 기존 레벨을 조회하거나 초기 레벨 생성
     */
    private List<UserLevel> getUserLevelsOrCreateInitial(User user) {
        List<UserLevel> userLevels = userLevelRepository.findByUserOrderByLevelNumberAsc(user);

        if (userLevels.isEmpty()) {
            UserLevel level1 = createInitialLevel(user);
            userLevels.add(level1);
        }

        return userLevels;
    }

    /**
     * 누락된 접근 가능한 레벨 생성
     */
    private void createMissingLevels(User user, List<UserLevel> userLevels) {
        for (byte i = 1; i <= 5; i++) {
            final byte levelNumber = i;
            boolean levelExists = userLevels.stream()
                    .anyMatch(level -> level.getLevelNumber() == levelNumber);

            if (!levelExists) {
                // 레벨 1은 항상 생성 가능
                // 다른 레벨은 이전 레벨 접근 가능 여부 확인
                if (levelNumber == 1 || isLevelAccessible(user, levelNumber)) {
                    UserLevel newLevel = UserLevel.builder()
                            .user(user)
                            .levelNumber(levelNumber)
                            .bestScore(levelNumber == 1 ? (byte) 0 : (byte) 100)
                            .isPassed((byte) 0)
                            .build();
                    UserLevel savedLevel = userLevelRepository.save(newLevel);
                    userLevels.add(savedLevel);
                }
            }
        }
    }

    /**
     * 모든 레벨에 대한 DTO 생성
     */
    private List<LevelDTO> buildAllLevels(User user, List<UserLevel> userLevels) {
        userLevels.sort((a, b) -> a.getLevelNumber() - b.getLevelNumber());

        List<LevelDTO> allLevels = new ArrayList<>();
        for (byte i = 1; i <= 5; i++) {
            final byte levelNumber = i;
            UserLevel existingLevel = userLevels.stream()
                    .filter(level -> level.getLevelNumber() == levelNumber)
                    .findFirst()
                    .orElse(null);

            if (existingLevel != null) {
                System.out.println("Existing level " + levelNumber + ": " + existingLevel.getBestScore());
            }

            if (existingLevel != null) {
                byte bestScore = adjustBestScore(existingLevel, levelNumber);
                LevelDTO dto = convertToLevelDTO(existingLevel);
                dto.setBest_score(bestScore);
                allLevels.add(dto);
            } else {
                allLevels.add(createDefaultLevelDTO(levelNumber));
            }
        }

        return allLevels;
    }

    /**
     * 최고 점수 조정 (레벨 1은 초기 100점, 다른 레벨은 0~10점 범위로 제한)
     */
    private byte adjustBestScore(UserLevel userLevel, byte levelNumber) {
        // 레벨 1은 실제 최고 점수 반환
        if (levelNumber == 1) {
            return userLevel.getBestScore();
        }

        // 접근 가능하지 않은 레벨은 100점
        if (!isLevelAccessible(userLevel.getUser(), levelNumber)) {
            return 100;
        }

        // 접근 가능한 레벨은 실제 최고 점수 반환, 점수 없으면 100점
        return userLevel.getBestScore() > 0 ? userLevel.getBestScore() : 100;
    }


    /**`
     * 기본 레벨 DTO 생성 (접근 불가능한 레벨)
     */
    private LevelDTO createDefaultLevelDTO(byte levelNumber) {
        return LevelDTO.builder()
                .level_id(levelNumber)
                .level_name("Lv." + levelNumber)
                .is_accessible(false)
                .best_score((byte) 100)
                .is_passed(false)
                .build();
    }

    /**
     * 사용자 이메일로 사용자 찾기
     */
    public User findUserByEmail(String userEmail) {
        return userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 초기 레벨 1 생성
     */
    @Transactional
    public UserLevel createInitialLevel(User user) {
        UserLevel level1 = UserLevel.builder()
                .user(user)
                .levelNumber((byte) 1)
                .bestScore((byte) 0)
                .isPassed((byte) 0)
                .build();
        return userLevelRepository.save(level1);
    }

    /**
     * UserLevel 엔티티를 LevelDTO로 변환
     */
    private LevelDTO convertToLevelDTO(UserLevel userLevel) {
        boolean isCurrentLevelPassed = userLevel.getIsPassed() == 1;
        boolean isAccessible = isLevelAccessible(userLevel.getUser(), userLevel.getLevelNumber());

        return LevelDTO.builder()
                .level_id(userLevel.getLevelNumber())
                .level_name("Lv." + userLevel.getLevelNumber())
                .is_accessible(isAccessible)
                .best_score(userLevel.getBestScore())
                .is_passed(isCurrentLevelPassed)
                .build();
    }

    /**
     * 특정 레벨이 없으면 생성
     */
    @Transactional
    public UserLevel getOrCreateUserLevel(User user, Byte levelId) {
        return userLevelRepository.findByUserAndLevelNumber(user, levelId)
                .orElseGet(() -> {
                    UserLevel newLevel = UserLevel.builder()
                            .user(user)
                            .levelNumber(levelId)
                            .bestScore((byte) 0)
                            .isPassed((byte) 0)
                            .build();
                    return userLevelRepository.save(newLevel);
                });
    }

    /**
     * 특정 레벨에 접근 가능한지 확인
     */
    public boolean isLevelAccessible(User user, Byte levelNumber) {
        // 레벨 1은 항상 접근 가능
        if (levelNumber == 1) {
            return true;
        }

        Optional<UserLevel> previousLevelOpt = userLevelRepository.findByUserAndLevelNumber(user, (byte)(levelNumber - 1));

        if (previousLevelOpt.isEmpty()) {
            System.out.println("Previous level not found for level " + levelNumber);
            return false;
        }

        UserLevel previousLevel = previousLevelOpt.get();
        System.out.println("Previous level " + (levelNumber-1) + " best score: " + previousLevel.getBestScore());

        return previousLevel.getBestScore() >= 8;
    }

    /**
     * 레벨 통과 처리 및 다음 레벨 생성
     */
    @Transactional
    public void processLevelPass(User user, Byte levelId, UserLevel userLevel) {
        // 퀴즈를 통과했을 때
        userLevel.pass(); // 통과 상태로 변경
        userLevelRepository.save(userLevel); // 명시적으로 저장

        // 다음 레벨 생성 (최대 레벨 5까지)
        if (levelId < 5) {
            createNextLevel(user, levelId);
        }
    }

    /**
     * 다음 레벨 생성
     */
    private void createNextLevel(User user, Byte currentLevel) {
        Byte nextLevel = (byte) (currentLevel + 1);
        if (!userLevelRepository.existsByUserAndLevelNumberAndIsPassed(user, nextLevel, (byte) 0) &&
                !userLevelRepository.existsByUserAndLevelNumberAndIsPassed(user, nextLevel, (byte) 1)) {
            UserLevel newLevel = UserLevel.builder()
                    .user(user)
                    .levelNumber(nextLevel)
                    .bestScore((byte) 0)
                    .isPassed((byte) 0)
                    .build();
            userLevelRepository.save(newLevel);
        }
    }
}