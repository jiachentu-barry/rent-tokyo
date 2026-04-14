package com.jiachentu.rent_tokyo.repository;

import com.jiachentu.rent_tokyo.entity.PropertyChangeLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyChangeLogRepository extends JpaRepository<PropertyChangeLog, Long> {

    List<PropertyChangeLog> findByPropertyIdOrderByDetectedAtDescIdDesc(Long propertyId);
}
