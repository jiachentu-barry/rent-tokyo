package com.jiachentu.rent_tokyo.service;

import com.jiachentu.rent_tokyo.dto.FavoriteCreateRequest;
import com.jiachentu.rent_tokyo.dto.FavoriteResponse;
import com.jiachentu.rent_tokyo.entity.Favorite;
import com.jiachentu.rent_tokyo.entity.Property;
import com.jiachentu.rent_tokyo.entity.User;
import com.jiachentu.rent_tokyo.repository.FavoriteRepository;
import com.jiachentu.rent_tokyo.repository.PropertyRepository;
import com.jiachentu.rent_tokyo.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public FavoriteResponse create(Long userId, FavoriteCreateRequest request) {
        User user = findUser(userId);
        Property property = findProperty(request.getPropertyId());

        if (favoriteRepository.existsByUserIdAndPropertyId(userId, request.getPropertyId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Property is already favorited");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .property(property)
                .build();

        Favorite saved = favoriteRepository.save(favorite);
        return FavoriteResponse.from(saved);
    }

    public List<FavoriteResponse> list(Long userId) {
        findUser(userId);
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    public void delete(Long userId, Long propertyId) {
        findUser(userId);
        Favorite favorite = favoriteRepository.findByUserIdAndPropertyId(userId, propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Favorite not found"));
        favoriteRepository.delete(favorite);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Property findProperty(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
    }
}
