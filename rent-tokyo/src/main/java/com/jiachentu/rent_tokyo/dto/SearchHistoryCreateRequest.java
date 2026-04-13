package com.jiachentu.rent_tokyo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchHistoryCreateRequest {

    private String ward;
    private Integer rentMin;
    private Integer rentMax;
    private String layout;
    private Integer walkMinutesMax;
}
