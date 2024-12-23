package streamingsettlement.streaming.common.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import streamingsettlement.streaming.common.exception.CustomGlobalException;
import streamingsettlement.streaming.common.exception.ErrorType;
import streamingsettlement.streaming.common.util.RedisKeyUtil;
import streamingsettlement.streaming.domain.repository.StreamingAdvertisementRepository;
import streamingsettlement.streaming.domain.repository.StreamingRedisRepository;
import streamingsettlement.streaming.domain.repository.StreamingRepository;
import streamingsettlement.streaming.domain.entity.Streaming;
import streamingsettlement.streaming.domain.entity.StreamingAdvertisement;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private final StreamingRepository streamingRepository;
    private final StreamingAdvertisementRepository streamingAdvertisementRepository;
    private final StreamingRedisRepository streamingRedisRepository;

    @Scheduled(fixedRate = 60000)  // 1분마다 실행
    @Transactional
    public void syncViewCountsToDB() {
        Set<String> keys = streamingRedisRepository.getViewCountKeys(RedisKeyUtil.VIEW_COUNT_KEY);

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
}
