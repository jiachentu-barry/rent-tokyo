package com.jiachentu.rent_tokyo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertySearchRequest {

    private String ward;
    private Integer rentMin;
    private Integer rentMax;
    private String layout;
    private Integer walkMinutesMax;
    private Boolean hasPriceChanges;
    private int page = 0;
    private int size = 20;
}
