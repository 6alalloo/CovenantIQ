package com.covenantiq.repository;

import com.covenantiq.domain.CollateralValuation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollateralValuationRepository extends JpaRepository<CollateralValuation, Long> {

    List<CollateralValuation> findByCollateralAssetIdOrderByValuationDateDesc(Long collateralAssetId);
}
