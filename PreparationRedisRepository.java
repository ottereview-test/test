package com.ssafy.ottereview.preparation.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.ottereview.preparation.dto.PreparationResult;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
@Slf4j
public class PreparationRedisRepository {
    
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * PR 준비 정보를 Redis에 저장
     */
    public void savePrepareInfo(Long repoId, PreparationResult prepareInfo) {
        try {
            String mainKey = generateMainKey(repoId, prepareInfo.getSource(), prepareInfo.getTarget());
            
            String jsonData = objectMapper.writeValueAsString(prepareInfo);
            
            redisTemplate.opsForValue()
                    .set(mainKey, jsonData, CACHE_TTL);
            
            log.info("PR 준비 정보 저장 완료: {}", mainKey);
            
        } catch (Exception e) {
            log.error("PR 준비 정보 저장 실패", e);
            throw new RuntimeException("Redis 저장 실패", e);
        }
    }
    
    /**
     * PR 준비 정보 조회
     */
    public PreparationResult getPrepareInfo(Long repoId,
            String source, String target) {
        try {
            String key = generateMainKey(repoId, source, target);
            String jsonData = redisTemplate.opsForValue()
                    .get(key);
            
            return objectMapper.readValue(jsonData, PreparationResult.class);
            
        } catch (Exception e) {
            log.error("PR 준비 정보 조회 실패", e);
            return null;
        }
    }
    
    /**
     * PR 준비 정보 업데이트(리뷰어 추가)
     */
    public void updatePrepareInfo(Long repoId, PreparationResult updatedPrepareInfo) {
        try {
            String mainKey = generateMainKey(repoId, updatedPrepareInfo.getSource(), updatedPrepareInfo.getTarget());
            
            // JSON으로 변환하여 저장
            String updatedJsonData = objectMapper.writeValueAsString(updatedPrepareInfo);
            redisTemplate.opsForValue()
                    .set(mainKey, updatedJsonData, CACHE_TTL);
            
            log.info("PR 준비 정보 업데이트 완료: {}", mainKey);
            
        } catch (Exception e) {
            log.error("PR 준비 정보 업데이트 실패", e);
            throw new RuntimeException("Redis 업데이트 실패", e);
        }
    }
    
    /**
     * PR 준비 정보 삭제
     */
    public void deletePrepareInfo(Long repoId, String source, String target) {
        try {
            String mainKey = generateMainKey(repoId, source, target);
            
            // 메인 데이터 삭제
            redisTemplate.delete(mainKey);
            
            log.info("PR 준비 정보 삭제 완료: {}", mainKey);
        } catch (Exception e) {
            log.error("PR 준비 정보 삭제 실패", e);
        }
    }
    
    // ========== 키 생성 메서드들 ==========
    
    /**
     * 메인 키 생성: pr:prepare:{userId}:{repoId}:{source}:{target}
     */
    private String generateMainKey(Long repoId, String source, String target) {
        return String.format("pr:prepare:%d:%s:%s",
                repoId, sanitizeBranchName(source), sanitizeBranchName(target));
    }
    
    /**
     * 브랜치명 정제 (Redis 키에 사용할 수 없는 문자 처리)
     */
    private String sanitizeBranchName(String branchName) {
        if (branchName == null) {
            return "null";
        }
        // 특수문자를 안전한 문자로 변경
        return branchName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
