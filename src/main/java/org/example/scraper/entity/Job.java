package org.example.scraper.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_external_id", columnList = "external_id", unique = true),
        @Index(name = "idx_jobs_company",     columnList = "company_name"),
        @Index(name = "idx_jobs_location",    columnList = "location")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 512)
    private String externalId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "location")
    private String location;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "job_url", length = 1024)
    private String jobUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "job_tags",
            joinColumns = @JoinColumn(name = "job_id")
    )
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "job_function")
    private String jobFunction;

    @Column(name = "industry")
    private String industry;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "remote")
    private Boolean remote;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}