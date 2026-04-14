package com.jiachentu.rent_tokyo.service;

import com.jiachentu.rent_tokyo.dto.PropertyPriceHistoryResponse;
import com.jiachentu.rent_tokyo.repository.PropertyChangeLogRepository;
import com.jiachentu.rent_tokyo.repository.PropertyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PropertyPriceHistoryService {

    private final PropertyRepository propertyRepository;
    private final PropertyChangeLogRepository propertyChangeLogRepository;

    public List<PropertyPriceHistoryResponse> listByPropertyId(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }

        return propertyChangeLogRepository.findByPropertyIdOrderByDetectedAtDescIdDesc(propertyId)
                .stream()
                .map(PropertyPriceHistoryResponse::from)
                .toList();
    }
}
