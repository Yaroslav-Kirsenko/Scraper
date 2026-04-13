package org.example.scraper.selectors;

public class BrowserScripts {
    public static final String SCROLL_TO_BOTTOM = "window.scrollTo(0, document.body.scrollHeight);";
    public static final String SCROLL_INTO_VIEW  = "arguments[0].scrollIntoView({block: 'center'});";
    public static final String CLICK             = "arguments[0].click();";
}
