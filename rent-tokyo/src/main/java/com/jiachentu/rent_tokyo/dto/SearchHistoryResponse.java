package com.jiachentu.rent_tokyo.dto;

import com.jiachentu.rent_tokyo.entity.SearchHistory;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchHistoryResponse {

    private Long id;
    private String ward;
    private Integer rentMin;
    private Integer rentMax;
    private String layout;
    private Integer walkMinutesMax;
    private LocalDateTime createdAt;

    public static SearchHistoryResponse from(SearchHistory searchHistory) {
        return SearchHistoryResponse.builder()
                .id(searchHistory.getId())
                .ward(searchHistory.getWard())
                .rentMin(searchHistory.getRentMin())
                .rentMax(searchHistory.getRentMax())
                .layout(searchHistory.getLayout())
                .walkMinutesMax(searchHistory.getWalkMinutesMax())
                .createdAt(searchHistory.getCreatedAt())
                .build();
    }
}
