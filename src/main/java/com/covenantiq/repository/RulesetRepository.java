package com.covenantiq.repository;

import com.covenantiq.domain.Ruleset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RulesetRepository extends JpaRepository<Ruleset, Long> {

    Optional<Ruleset> findByKey(String key);
}
