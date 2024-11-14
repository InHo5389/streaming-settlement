package streamingsettlement.adjustment.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long streamingId;
    private BigDecimal streamingAmount;
    private BigDecimal advertisementAmount;
    private BigDecimal totalAmount;
    private Long streamingViews;
    private Long advertisementViews;
    private int playTime;
    private BigDecimal advertisementUnitPrice;
    private BigDecimal streamingUnitPrice;
    private LocalDateTime createdAt;
}