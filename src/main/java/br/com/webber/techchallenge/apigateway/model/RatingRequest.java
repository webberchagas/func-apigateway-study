package br.com.webber.techchallenge.apigateway.model;

public class RatingRequest {
    private Integer rating;
    private String description;
    private String email;

    public RatingRequest() {
    }

    public RatingRequest(Integer rating, String description, String email) {
        this.rating = rating;
        this.description = description;
        this.email = email;
    }

    public Integer getRating() {
        return rating;
    }

    public String getDescription() {
        return description;
    }

    public String getEmail() {
        return email;
    }

}
