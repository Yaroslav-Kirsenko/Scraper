package org.example.scraper.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Job filtering options")
public class JobFilterRequest {

    @Schema(description = "Search by job title", example = "Backend Engineer")
    private String title;

    @Schema(description = "Search by company name", example = "Google")
    private String companyName;

    @Schema(description = "Filter by function", example = "Remote")
    private String location;

    @Schema(description = "Фільтр за функцією", example = "Engineering")
    private String jobFunction;

    @Schema(description = "Filter by industry", example = "Software")
    private String industry;

    @Schema(description = "Type of employment", example = "Full-time")
    private String employmentType;

    @Schema(description = "Only remote jobs", example = "true")
    private Boolean remote;

    @Schema(description = "Filter by tag", example = "Machine Learning")
    private String tag;
}
