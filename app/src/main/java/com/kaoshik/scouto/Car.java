package com.kaoshik.scouto;

import java.io.Serializable;
import java.util.UUID;

public class Car implements Serializable {
    private String make;
    private String id;
    private String model;
    private String imageUrl;
    private String thumbnailUrl;

    public Car(String make, String model, String imageUrl) {
        this.make = make;
        this.model = model;
        this.imageUrl = imageUrl;
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String toString() {
        return make + " " + model + " " + imageUrl;
    }
}



