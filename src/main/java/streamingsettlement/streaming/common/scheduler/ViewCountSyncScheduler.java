package streamingsettlement.streaming.common.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import streamingsettlement.streaming.common.exception.CustomGlobalException;
import streamingsettlement.streaming.common.exception.ErrorType;
import streamingsettlement.streaming.domain.StreamingRedisRepository;
import streamingsettlement.streaming.domain.StreamingRepository;
import streamingsettlement.streaming.domain.entity.Streaming;
import streamingsettlement.streaming.domain.entity.StreamingAdvertisement;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {
    private static final String VIEW_COUNT_KEY = "streaming:%d:views";
    private static final String AD_VIEW_KEY = "streaming:%d:ad:%d:views";

    private final StreamingRepository streamingRepository;
    private final StreamingRedisRepository streamingRedisRepository;

    @Scheduled(fixedRate = 60000)  // 1분마다 실행
    @Transactional
    public void syncViewCountsToDB() {
        Set<String> keys = streamingRedisRepository.getViewCountKeys(VIEW_COUNT_KEY);

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            Long streamingId = extractStreamingId(key);
            Long viewCount = streamingRedisRepository.getStreamingView(key);

            if (streamingId != null && viewCount != null) {
                Streaming streaming = streamingRepository.findStreamingById(streamingId)
                        .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_STREAMING));

                streaming.updateViewCount(viewCount);

                streamingRedisRepository.deleteKey(key);
            }
        }
    }

    private Long extractStreamingId(String key) {
        try {
            // key format: "streaming:123:views"
            return Long.parseLong(key.split(":")[1]);
        } catch (Exception e) {
            return null;
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncAdViewCountsToDB() {
        Set<String> keys = streamingRedisRepository.getAdViewKeys(AD_VIEW_KEY);

        for (String key : keys) {
            // key 형식: "streaming:1:ad:420:views"
            String[] parts = key.split(":");
            Long streamingId = Long.parseLong(parts[1]);
            Integer position = Integer.parseInt(parts[3]);
            Long viewCount = streamingRedisRepository.getAdView(key);

            if (viewCount > 0) {
                // DB 업데이트
                StreamingAdvertisement streamingAdvertisement = streamingRepository
                        .findByStreamingIdAndPosition(streamingId, position)
                        .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));

                streamingAdvertisement.updateAdViews(viewCount);

                // Redis 카운터 초기화
                streamingRedisRepository.deleteKey(key);
            }
        }
    }
}
