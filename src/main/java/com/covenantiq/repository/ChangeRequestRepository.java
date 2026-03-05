package com.covenantiq.repository;

import com.covenantiq.domain.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {
}
