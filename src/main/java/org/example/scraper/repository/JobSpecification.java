package org.example.scraper.repository;


import jakarta.persistence.criteria.*;
import org.example.scraper.dto.JobFilterRequest;
import org.example.scraper.entity.Job;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class JobSpecification implements Specification<Job> {

    private final JobFilterRequest filter;

    public JobSpecification(JobFilterRequest filter) {
        this.filter = filter;
    }

    @Override
    public Predicate toPredicate(Root<Job> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.isTrue(root.get("active")));

        if (hasText(filter.getTitle())) {
            predicates.add(cb.like(
                    cb.lower(root.get("title")),
                    "%" + filter.getTitle().toLowerCase() + "%"
            ));
        }

        if (hasText(filter.getCompanyName())) {
            predicates.add(cb.like(
                    cb.lower(root.get("companyName")),
                    "%" + filter.getCompanyName().toLowerCase() + "%"
            ));
        }

        if (hasText(filter.getLocation())) {
            predicates.add(cb.like(
                    cb.lower(root.get("location")),
                    "%" + filter.getLocation().toLowerCase() + "%"
            ));
        }

        if (hasText(filter.getJobFunction())) {
            predicates.add(cb.equal(
                    cb.lower(root.get("jobFunction")),
                    filter.getJobFunction().toLowerCase()
            ));
        }

        if (hasText(filter.getIndustry())) {
            predicates.add(cb.equal(
                    cb.lower(root.get("industry")),
                    filter.getIndustry().toLowerCase()
            ));
        }

        if (hasText(filter.getEmploymentType())) {
            predicates.add(cb.equal(
                    cb.lower(root.get("employmentType")),
                    filter.getEmploymentType().toLowerCase()
            ));
        }

        if (filter.getRemote() != null) {
            predicates.add(cb.equal(root.get("remote"), filter.getRemote()));
        }

        if (hasText(filter.getTag())) {
            Join<Job, String> tagsJoin = root.join("tags", JoinType.INNER);
            predicates.add(cb.like(
                    cb.lower(tagsJoin),
                    "%" + filter.getTag().toLowerCase() + "%"
            ));
            query.distinct(true);
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}