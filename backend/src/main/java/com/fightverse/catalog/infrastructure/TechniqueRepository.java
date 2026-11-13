package com.fightverse.catalog.infrastructure;

import com.fightverse.catalog.domain.Technique;
import com.fightverse.catalog.domain.TechniqueDifficulty;
import com.fightverse.catalog.domain.TechniqueStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechniqueRepository extends JpaRepository<Technique, UUID> {

  Optional<Technique> findByIdAndStatus(UUID id, TechniqueStatus status);

  boolean existsByDisciplineIdAndCode(UUID disciplineId, String code);

  Page<Technique> findByStatus(TechniqueStatus status, Pageable pageable);

  Page<Technique> findByStatusAndDifficulty(
      TechniqueStatus status, TechniqueDifficulty difficulty, Pageable pageable);

  Page<Technique> findByStatusAndDisciplineCode(
      TechniqueStatus status, String disciplineCode, Pageable pageable);

  Page<Technique> findByStatusAndDisciplineCodeAndDifficulty(
      TechniqueStatus status,
      String disciplineCode,
      TechniqueDifficulty difficulty,
      Pageable pageable);
}
