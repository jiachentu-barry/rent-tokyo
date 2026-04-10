package com.jiachentu.rent_tokyo.dto;

import com.jiachentu.rent_tokyo.entity.Property;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PropertySearchResponse {

    private Long id;
    private String name;
    private String address;
    private String ward;
    private String nearestStation;
    private Integer walkMinutes;
    private String layout;
    private Float areaSqm;
    private Integer builtYear;
    private Integer rent;
    private Integer managementFee;
    private Integer deposit;
    private Integer keyMoney;
    private String sourceUrl;

    public static PropertySearchResponse from(Property property) {
        return PropertySearchResponse.builder()
                .id(property.getId())
                .name(property.getName())
                .address(property.getAddress())
                .ward(property.getWard())
                .nearestStation(property.getNearestStation())
                .walkMinutes(property.getWalkMinutes())
                .layout(property.getLayout())
                .areaSqm(property.getAreaSqm())
                .builtYear(property.getBuiltYear())
                .rent(property.getRent())
                .managementFee(property.getManagementFee())
                .deposit(property.getDeposit())
                .keyMoney(property.getKeyMoney())
                .sourceUrl(property.getSourceUrl())
                .build();
    }
}
