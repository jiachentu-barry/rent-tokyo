package com.jiachentu.rent_tokyo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteCreateRequest {

    @NotNull
    private Long propertyId;
}
