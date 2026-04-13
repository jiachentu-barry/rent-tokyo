package com.jiachentu.rent_tokyo.dto;

import com.jiachentu.rent_tokyo.entity.Favorite;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoriteResponse {

    private Long favoriteId;
    private Long propertyId;
    private String propertyName;
    private String ward;
    private Integer rent;
    private String layout;
    private Integer walkMinutes;
    private LocalDateTime createdAt;

    public static FavoriteResponse from(Favorite favorite) {
        return FavoriteResponse.builder()
                .favoriteId(favorite.getId())
                .propertyId(favorite.getProperty().getId())
                .propertyName(favorite.getProperty().getName())
                .ward(favorite.getProperty().getWard())
                .rent(favorite.getProperty().getRent())
                .layout(favorite.getProperty().getLayout())
                .walkMinutes(favorite.getProperty().getWalkMinutes())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
