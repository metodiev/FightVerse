package com.fightverse.catalog.infrastructure;

import com.fightverse.catalog.domain.Discipline;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplineRepository extends JpaRepository<Discipline, UUID> {

  Optional<Discipline> findByCode(String code);

  Optional<Discipline> findByCodeAndActiveTrue(String code);

  boolean existsByCode(String code);

  Page<Discipline> findByActiveTrue(Pageable pageable);
}
