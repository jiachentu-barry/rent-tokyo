package com.jiachentu.rent_tokyo.dto;

import com.jiachentu.rent_tokyo.entity.PropertyChangeLog;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PropertyPriceHistoryResponse {

    private Long id;
    private Integer oldRent;
    private Integer newRent;
    private Integer changeAmount;
    private LocalDateTime detectedAt;

    public static PropertyPriceHistoryResponse from(PropertyChangeLog changeLog) {
        Integer oldRent = changeLog.getOldRent();
        Integer newRent = changeLog.getNewRent();

        return PropertyPriceHistoryResponse.builder()
                .id(changeLog.getId())
                .oldRent(oldRent)
                .newRent(newRent)
                .changeAmount(oldRent != null && newRent != null ? newRent - oldRent : null)
                .detectedAt(changeLog.getDetectedAt())
                .build();
    }
}
