package gcp.cloudblog_mailing.crawling.enums;

import org.apache.logging.log4j.core.tools.picocli.CommandLine;

public enum Category {
    AI("AI & Machine Learning"),
    DA("Data Analytics"),
    APPMODERNIZATION("Application Modernization"),
    INFRASTRUCTURE("Infrastructure"),
    NETWORK("Network"),
    SECURITY("Security"),
    DATABASE("Database"),
    CLOUD("Cloud");

    private String categoryName;

    private Category(String categoryName){
        this.categoryName = categoryName;
    }

    public String getCategoryName(){
        return this.categoryName;
    }
}
