package com.covenantiq.repository;

import com.covenantiq.domain.RulesetVersion;
import com.covenantiq.enums.RulesetVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RulesetVersionRepository extends JpaRepository<RulesetVersion, Long> {

    List<RulesetVersion> findByRulesetIdOrderByVersionDesc(Long rulesetId);

    Optional<RulesetVersion> findByRulesetIdAndVersion(Long rulesetId, int version);

    Optional<RulesetVersion> findByRulesetIdAndStatus(Long rulesetId, RulesetVersionStatus status);
}
