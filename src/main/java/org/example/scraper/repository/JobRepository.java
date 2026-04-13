package org.example.scraper.repository;


import org.example.scraper.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>,
        JpaSpecificationExecutor<Job> {

    Optional<Job> findByExternalId(String externalId);

    @Query("SELECT j.externalId FROM Job j WHERE j.active = true")
    List<String> findAllActiveExternalIds();

    @Modifying
    @Query("UPDATE Job j SET j.active = false WHERE j.externalId IN :ids")
    int deactivateByExternalIds(@Param("ids") List<String> ids);

    Page<Job> findAllByActiveTrue(Pageable pageable);

    Page<Job> findByCompanyNameIgnoreCaseAndActiveTrue(String companyName, Pageable pageable);

    boolean existsByExternalId(String externalId);
}
