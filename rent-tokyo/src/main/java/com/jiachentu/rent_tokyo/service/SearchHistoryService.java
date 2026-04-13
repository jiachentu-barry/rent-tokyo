package com.jiachentu.rent_tokyo.service;

import com.jiachentu.rent_tokyo.dto.SearchHistoryCreateRequest;
import com.jiachentu.rent_tokyo.dto.SearchHistoryResponse;
import com.jiachentu.rent_tokyo.entity.SearchHistory;
import com.jiachentu.rent_tokyo.entity.User;
import com.jiachentu.rent_tokyo.repository.SearchHistoryRepository;
import com.jiachentu.rent_tokyo.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    public SearchHistoryResponse create(Long userId, SearchHistoryCreateRequest request) {
        User user = findUser(userId);

        SearchHistory searchHistory = SearchHistory.builder()
                .user(user)
                .ward(trimToNull(request.getWard()))
                .rentMin(request.getRentMin())
                .rentMax(request.getRentMax())
                .layout(trimToNull(request.getLayout()))
                .walkMinutesMax(request.getWalkMinutesMax())
                .build();

        SearchHistory saved = searchHistoryRepository.save(searchHistory);
        return SearchHistoryResponse.from(saved);
    }

    public List<SearchHistoryResponse> list(Long userId) {
        findUser(userId);
        return searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SearchHistoryResponse::from)
                .toList();
    }

    public void delete(Long userId, Long historyId) {
        findUser(userId);
        SearchHistory searchHistory = searchHistoryRepository.findById(historyId)
                .filter(item -> item.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Search history not found"));

        searchHistoryRepository.delete(searchHistory);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
