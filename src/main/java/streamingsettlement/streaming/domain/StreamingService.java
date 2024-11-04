package streamingsettlement.streaming.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import streamingsettlement.streaming.common.exception.CustomGlobalException;
import streamingsettlement.streaming.common.exception.ErrorType;
import streamingsettlement.streaming.domain.dto.StreamingDto;
import streamingsettlement.streaming.domain.dto.StreamingResponse;
import streamingsettlement.streaming.domain.entity.PlayHistory;
import streamingsettlement.streaming.domain.entity.Streaming;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StreamingService {

    private static final int AD_INTERVAL = 420;
    private static final String VIEW_COUNT_KEY = "streaming:%d:views";
    private static final String AD_VIEW_KEY = "streaming:%d:ad:%d:views";

    private final StreamingRepository streamingRepository;
    private final StreamingRedisRepository streamingRedisRepository;

    /**
     * ip주소,스트리밍 id로 시청 히스토리에서 찾고 없으면 최초 재생으로 조회수 증가
     * 새로운 시청 기록 생성할때 그 전 마지막 재생 시점으로 생성 어차피 폴링으로 업데이트 시켜줄거기 때문
     */
    @Transactional
    public StreamingResponse.Watch watch(Long streamingId, StreamingDto.Watch dto) {
        Streaming streaming = streamingRepository.findStreamingById(streamingId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_STREAMING));

        Optional<PlayHistory> optionalPlayHistory = streamingRepository.findLatestPlayHistory(dto.getSourceIp(), streamingId);
        if (optionalPlayHistory.isEmpty()) {
            String redisKey = String.format(VIEW_COUNT_KEY, streamingId);
            streamingRedisRepository.incrementStreamingView(redisKey);
        }

        PlayHistory playHistory = PlayHistory.create(dto.getUserId(), streamingId, optionalPlayHistory, dto.getSourceIp());
        streamingRepository.save(playHistory);

        return StreamingResponse.Watch.builder()
                .playHistoryId(playHistory.getId())
                .lastPlayTime(playHistory.getLastPlayTime())
                .build();
    }

    @Transactional
    public void saveAdViewsToRedis(StreamingDto.UpdatePlayTime dto) {
        PlayHistory playHistory = streamingRepository.findPlayHistoryById(dto.getPlayHistoryId())
                .orElseThrow(() -> new RuntimeException("시청 기록이 없습니다."));
        List<Integer> newAdPositions = playHistory.calculateNewAdPositions(dto.getLastPlayTime(), AD_INTERVAL);

        for (Integer position : newAdPositions) {
            String redisKey = String.format(AD_VIEW_KEY, playHistory.getStreamingId(), position);
            streamingRedisRepository.incrementAdView(redisKey);
        }
    }
}