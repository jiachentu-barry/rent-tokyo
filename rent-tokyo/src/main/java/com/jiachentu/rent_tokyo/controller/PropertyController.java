package com.jiachentu.rent_tokyo.controller;

import com.jiachentu.rent_tokyo.dto.PropertyPriceHistoryResponse;
import com.jiachentu.rent_tokyo.dto.PropertySearchRequest;
import com.jiachentu.rent_tokyo.dto.PropertySearchResponse;
import com.jiachentu.rent_tokyo.service.PropertyPriceHistoryService;
import com.jiachentu.rent_tokyo.service.PropertySearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertySearchService propertySearchService;
    private final PropertyPriceHistoryService propertyPriceHistoryService;

    @GetMapping("/search")
    public Page<PropertySearchResponse> search(@ModelAttribute PropertySearchRequest request) {
        return propertySearchService.search(request);
    }

    @GetMapping("/{propertyId}")
    public PropertySearchResponse detail(@PathVariable Long propertyId) {
        return propertySearchService.getById(propertyId);
    }

    @GetMapping("/{propertyId}/price-history")
    public List<PropertyPriceHistoryResponse> priceHistory(@PathVariable Long propertyId) {
        return propertyPriceHistoryService.listByPropertyId(propertyId);
    }
}
