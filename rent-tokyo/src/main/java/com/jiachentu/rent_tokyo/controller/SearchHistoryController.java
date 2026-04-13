package com.jiachentu.rent_tokyo.controller;

import com.jiachentu.rent_tokyo.dto.SearchHistoryCreateRequest;
import com.jiachentu.rent_tokyo.dto.SearchHistoryResponse;
import com.jiachentu.rent_tokyo.service.SearchHistoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search-histories")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchHistoryResponse create(@RequestParam Long userId, @RequestBody SearchHistoryCreateRequest request) {
        return searchHistoryService.create(userId, request);
    }

    @GetMapping
    public List<SearchHistoryResponse> list(@RequestParam Long userId) {
        return searchHistoryService.list(userId);
    }

    @DeleteMapping("/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam Long userId, @PathVariable Long historyId) {
        searchHistoryService.delete(userId, historyId);
    }
}
