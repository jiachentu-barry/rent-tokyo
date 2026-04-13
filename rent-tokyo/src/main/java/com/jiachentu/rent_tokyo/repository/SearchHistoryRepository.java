package com.jiachentu.rent_tokyo.repository;

import com.jiachentu.rent_tokyo.entity.SearchHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
