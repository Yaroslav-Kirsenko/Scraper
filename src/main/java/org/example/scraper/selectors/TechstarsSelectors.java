package org.example.scraper.selectors;


public class TechstarsSelectors {
    public static final String JOB_CARD      = "div[data-testid='job-list-item']";
    public static final String JOB_TITLE     = "div[itemprop='title']";
    public static final String JOB_LINK      = "a[data-testid='job-title-link']";
    public static final String COMPANY_LINK  = "a[data-testid='link']";
    public static final String LOAD_MORE_BTN = "button[data-testid='load-more']";
    public static final String TAG           = "div[data-testid='tag'] div";
    public static final String META_DESC     = "meta[itemprop='description']";
    public static final String LOCATION_DIV  = "div.sc-aXZVg.gRUtHQ";
    public static final String META_LOCATION = "meta[itemprop='addressLocality']";
}
