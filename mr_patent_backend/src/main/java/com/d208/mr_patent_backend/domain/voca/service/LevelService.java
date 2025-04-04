package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.domain.voca.dto.LevelDTO;
import com.d208.mr_patent_backend.domain.voca.entity.UserLevel;
import com.d208.mr_patent_backend.domain.voca.repository.UserLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final UserRepository userRepository;
    private final UserLevelRepository userLevelRepository;

    /**
     * 사용자의 모든 레벨 정보 조회
     */
    @Transactional
    public List<LevelDTO> getUserLevels(String userEmail) {
        User user = findUserByEmail(userEmail);
        List<UserLevel> userLevels = userLevelRepository.findByUserOrderByLevelNumberAsc(user);

        // 레벨 1이 없으면 초기 생성
        if (userLevels.isEmpty()) {
            UserLevel level1 = createInitialLevel(user);
            userLevels.add(level1);
        }

        return userLevels.stream()
                .map(this::convertToLevelDTO)
                .collect(Collectors.toList());
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
        return LevelDTO.builder()
                .level_id(userLevel.getLevelNumber())
                .level_name("Lv." + userLevel.getLevelNumber())
                .is_accessible(isLevelAccessible(userLevel.getUser(), userLevel.getLevelNumber()))
                .best_score(userLevel.getBestScore())
                .is_passed(userLevel.getIsPassed() == 1)
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

        // 이전 레벨이 통과되었는지 확인
        return userLevelRepository.existsByUserAndLevelNumberAndIsPassed(
                user, (byte)(levelNumber - 1), (byte) 1);
    }

    /**
     * 레벨 통과 처리 및 다음 레벨 생성
     */
    @Transactional
    public void processLevelPass(User user, Byte levelId, UserLevel userLevel) {
        // 퀴즈를 통과했고, 이전에 통과한 적이 없으면
        if (userLevel.getIsPassed() == 0) {
            userLevel.pass();

            // 다음 레벨 생성 (최대 레벨 5까지)
            if (levelId < 5) {
                createNextLevel(user, levelId);
            }
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