package com.jiachentu.rent_tokyo.service;

import com.jiachentu.rent_tokyo.dto.PropertySearchRequest;
import com.jiachentu.rent_tokyo.dto.PropertySearchResponse;
import com.jiachentu.rent_tokyo.entity.Property;
import com.jiachentu.rent_tokyo.repository.PropertyRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PropertySearchService {

    private final PropertyRepository propertyRepository;

    public PropertySearchResponse getById(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        return PropertySearchResponse.from(property);
    }

    public Page<PropertySearchResponse> search(PropertySearchRequest request) {
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 20 : Math.min(request.getSize(), 100);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("rent"), Sort.Order.desc("id")));

        Specification<Property> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(request.getWard())) {
                List<String> wards = Arrays.stream(request.getWard().split(","))
                        .map(String::trim)
                        .filter(this::hasText)
                        .distinct()
                        .toList();

                if (!wards.isEmpty()) {
                    predicates.add(root.get("ward").in(wards));
                }
            }
            if (request.getRentMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rent"), request.getRentMin()));
            }
            if (request.getRentMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rent"), request.getRentMax()));
            }
            if (hasText(request.getLayout())) {
                predicates.add(cb.equal(root.get("layout"), request.getLayout().trim()));
            }
            if (request.getWalkMinutesMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("walkMinutes"), request.getWalkMinutesMax()));
            }
            if (Boolean.TRUE.equals(request.getHasPriceChanges())) {
                predicates.add(cb.isNotEmpty(root.get("changeLogs")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return propertyRepository.findAll(spec, pageable).map(PropertySearchResponse::from);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
