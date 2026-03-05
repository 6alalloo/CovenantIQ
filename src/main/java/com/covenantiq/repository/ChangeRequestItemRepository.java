package com.covenantiq.repository;

import com.covenantiq.domain.ChangeRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestItemRepository extends JpaRepository<ChangeRequestItem, Long> {

    List<ChangeRequestItem> findByChangeRequestId(Long changeRequestId);
}
