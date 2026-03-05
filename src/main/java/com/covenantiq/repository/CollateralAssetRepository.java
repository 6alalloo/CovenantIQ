package com.covenantiq.repository;

import com.covenantiq.domain.CollateralAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollateralAssetRepository extends JpaRepository<CollateralAsset, Long> {

    List<CollateralAsset> findByLoanIdOrderByIdDesc(Long loanId);
}
