package com.jiachentu.rent_tokyo.controller;

import com.jiachentu.rent_tokyo.dto.PropertySearchRequest;
import com.jiachentu.rent_tokyo.dto.PropertySearchResponse;
import com.jiachentu.rent_tokyo.service.PropertySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertySearchService propertySearchService;

    @GetMapping("/search")
    public Page<PropertySearchResponse> search(@ModelAttribute PropertySearchRequest request) {
        return propertySearchService.search(request);
    }
}
