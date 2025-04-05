package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.domain.voca.dto.level.LevelDTO;
import com.d208.mr_patent_backend.domain.voca.entity.UserLevel;
import com.d208.mr_patent_backend.domain.voca.repository.UserLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final UserRepository userRepository;
    private final UserLevelRepository userLevelRepository;

    @Transactional
    public List<LevelDTO> getUserLevels(String userEmail) {
        User user = findUserByEmail(userEmail);

        // 사용자의 모든 레벨 조회 또는 초기 설정
        List<UserLevel> userLevels = getUserLevelsOrCreateAll(user);

        // 모든 레벨에 대한 DTO 생성
        List<LevelDTO> result = buildLevelDTOs(userLevels);

        // 로깅 추가
        for (LevelDTO level : result) {
            System.out.println("Level " + level.getLevel_id() +
                    ": Best Score = " + level.getBest_score() +
                    ", Accessible = " + level.isAccessible() +
                    ", Passed = " + level.isPassed());
        }

        return result;
    }

    // 2. 사용자의 모든 레벨 조회 또는 모든 레벨(1~5) 생성
    private List<UserLevel> getUserLevelsOrCreateAll(User user) {
        List<UserLevel> userLevels = userLevelRepository.findByUserOrderByLevelNumberAsc(user);

        if (userLevels.isEmpty()) {
            // 사용자의 레벨이 없으면 모든 레벨(1~5) 생성
            userLevels = createAllLevels(user);
        } else if (userLevels.size() < 5) {
            // 레벨이 일부만 있으면 나머지 생성
            userLevels = ensureAllLevelsExist(user, userLevels);
        }

        return userLevels;
    }

    // 3. 모든 레벨(1~5) 생성
    private List<UserLevel> createAllLevels(User user) {
        List<UserLevel> levels = new ArrayList<>();

        for (byte levelNumber = 1; levelNumber <= 5; levelNumber++) {
            UserLevel level = UserLevel.builder()
                    .user(user)
                    .levelNumber(levelNumber)
                    .bestScore((byte) 100) // 초기 점수 100
                    .isPassed((byte) 0)    // 초기 통과 상태 false
                    .build();

            UserLevel savedLevel = userLevelRepository.save(level);
            levels.add(savedLevel);
        }

        return levels;
    }

    // 4. 모든 레벨이 존재하는지 확인하고 없는 레벨 생성
    private List<UserLevel> ensureAllLevelsExist(User user, List<UserLevel> existingLevels) {
        // 이미 존재하는 레벨 번호 확인
        Set<Byte> existingLevelNumbers = existingLevels.stream()
                .map(UserLevel::getLevelNumber)
                .collect(Collectors.toSet());

        // 없는 레벨 생성
        for (byte levelNumber = 1; levelNumber <= 5; levelNumber++) {
            if (!existingLevelNumbers.contains(levelNumber)) {
                UserLevel newLevel = UserLevel.builder()
                        .user(user)
                        .levelNumber(levelNumber)
                        .bestScore((byte) 100) // 초기 점수 100
                        .isPassed((byte) 0)    // 초기 통과 상태 false
                        .build();

                UserLevel savedLevel = userLevelRepository.save(newLevel);
                existingLevels.add(savedLevel);
            }
        }

        // 레벨 번호 순으로 정렬
        existingLevels.sort(Comparator.comparing(UserLevel::getLevelNumber));

        return existingLevels;
    }

    // 5. 레벨 엔티티를 DTO로 변환
    private List<LevelDTO> buildLevelDTOs(List<UserLevel> userLevels) {
        List<LevelDTO> result = new ArrayList<>();

        for (UserLevel userLevel : userLevels) {
            byte levelNumber = userLevel.getLevelNumber();
            boolean isPassed = userLevel.getIsPassed() == 1;

            // 접근 가능 여부 결정: 레벨 1은 항상 접근 가능, 다른 레벨은 이전 레벨 통과해야 접근 가능
            boolean isAccessible = false;
            if (levelNumber == 1) {
                isAccessible = true; // 레벨 1은 항상 접근 가능
            } else {
                // 이전 레벨 찾기
                UserLevel previousLevel = userLevels.stream()
                        .filter(level -> level.getLevelNumber() == levelNumber - 1)
                        .findFirst()
                        .orElse(null);

                // 이전 레벨이 존재하고 통과했으면 접근 가능
                if (previousLevel != null && previousLevel.getIsPassed() == 1) {
                    isAccessible = true;
                }
            }

            // DTO 생성
            LevelDTO dto = LevelDTO.builder()
                    .level_id(levelNumber)
                    .level_name("Lv." + levelNumber)
                    .accessible(isAccessible)
                    .best_score(userLevel.getBestScore())
                    .passed(isPassed)
                    .build();

            result.add(dto);
        }

        return result;
    }

    // 6. 레벨 통과 처리 (기존 processLevelPass 메소드)
    @Transactional
    public void processLevelPass(User user, Byte levelId, UserLevel userLevel) {
        // 레벨 통과 처리
        userLevel.pass(); // 통과 상태로 변경
        userLevelRepository.save(userLevel); // 저장

        // 다음 레벨 접근 가능하도록 설정 (여기서는 별도로 작업 필요 없음, 조회 시 결정됨)
    }

    // 7. isLevelAccessible 메소드 수정
    public boolean isLevelAccessible(User user, Byte levelNumber) {
        // 레벨 1은 항상 접근 가능
        if (levelNumber == 1) {
            return true;
        }

        // 이전 레벨 조회
        Optional<UserLevel> previousLevelOpt = userLevelRepository.findByUserAndLevelNumber(user, (byte)(levelNumber - 1));

        // 이전 레벨이 없거나 통과하지 않았으면 접근 불가
        return previousLevelOpt.isPresent() && previousLevelOpt.get().getIsPassed() == 1;
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
                            .bestScore((byte) 100) // 초기 점수는 100으로 설정
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
                LevelDTO dto = convertToLevelDTO(existingLevel);
                allLevels.add(dto);
            } else {
                // 접근 불가능한 레벨 생성
                boolean isAccessible = isLevelAccessible(user, levelNumber);
                allLevels.add(createDefaultLevelDTO(levelNumber, isAccessible));
            }
        }

        return allLevels;
    }

    /**
     * 최고 점수 조정 (레벨 1은 초기 100점, 다른 레벨은 0~10점 범위로 제한)
     */
    private byte adjustBestScore(UserLevel userLevel, byte levelNumber) {
        // 실제 점수가 0~10 사이인 경우 실제 점수 반환
        if (userLevel.getBestScore() >= 0 && userLevel.getBestScore() <= 10) {
            return userLevel.getBestScore();
        }

        // 초기 상태(100점)인 경우 100점 반환
        return 100;
    }


    /**`
     * 기본 레벨 DTO 생성 (접근 불가능한 레벨)
     */
    private LevelDTO createDefaultLevelDTO(byte levelNumber, boolean isAccessible) {
        return LevelDTO.builder()
                .level_id(levelNumber)
                .level_name("Lv." + levelNumber)
                .accessible(isAccessible) // 파라미터로 받은 접근 가능 여부 사용
                .best_score((byte) 100)
                .passed(false)
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
                .bestScore((byte) 100)
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
                .accessible(isAccessible)
                .best_score(adjustBestScore(userLevel, userLevel.getLevelNumber()))
                .passed(isCurrentLevelPassed)
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