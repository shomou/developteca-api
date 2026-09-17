package com.developteca.dto;

public class RatingResponse {

    private double averageRating;
    private Integer myRating;

    public RatingResponse() {}

    public RatingResponse(double averageRating, Integer myRating) {
        this.averageRating = averageRating;
        this.myRating = myRating;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getMyRating() {
        return myRating;
    }

    public void setMyRating(Integer myRating) {
        this.myRating = myRating;
    }
}