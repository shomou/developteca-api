package com.developteca.dto;

public class DashboardStatsResponse {
    
    private long totalArticles;
    private long publishedArticles;
    private long draftArticles;
    private long archivedArticles;
    private long totalViews;
    private long totalComments;
    private double averageRatingOverall;

    public DashboardStatsResponse(){}

    public DashboardStatsResponse(long totalArticles, long publishedArticles,
                                    long draftArticles, long archivedArticles,
                                    long totalViews,    long totalComments,
                                    double averageRatingOverall){
        
         this.totalArticles = totalArticles;
         this.publishedArticles = publishedArticles;
         this.draftArticles = draftArticles;
         this.archivedArticles = archivedArticles;
         this.totalViews = totalViews;
         this.totalComments = totalComments;
         this.averageRatingOverall = averageRatingOverall;

         
    }

    public long getTotalArticles() {
        return totalArticles;
    }

    public void setTotalArticles(long totalArticles) {
        this.totalArticles = totalArticles;
    }

    public long getPublishedArticles() {
        return publishedArticles;
    }

    public void setPublishedArticles(long publishedArticles) {
        this.publishedArticles = publishedArticles;
    }

    public long getDraftArticles() {
        return draftArticles;
    }

    public void setDraftArticles(long draftArticles) {
        this.draftArticles = draftArticles;
    }

    public long getArchivedArticles() {
        return archivedArticles;
    }

    public void setArchivedArticles(long archivedArticles) {
        this.archivedArticles = archivedArticles;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(long totalViews) {
        this.totalViews = totalViews;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }

    public double getAverageRatingOverall() {
        return averageRatingOverall;
    }

    public void setAverageRatingOverall(double averageRatingOverall) {
        this.averageRatingOverall = averageRatingOverall;
    }

}
