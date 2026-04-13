package com.jiachentu.rent_tokyo.repository;

import com.jiachentu.rent_tokyo.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);

    Optional<Favorite> findByUserIdAndPropertyId(Long userId, Long propertyId);

    @Query("select f from Favorite f join fetch f.property where f.user.id = :userId order by f.createdAt desc")
    List<Favorite> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    void deleteByUserIdAndPropertyId(Long userId, Long propertyId);
}
