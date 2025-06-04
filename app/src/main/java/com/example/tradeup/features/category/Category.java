package com.example.tradeup.features.category;

public class Category {
    private String name;
    private String imageUrl;

    public Category() {
        // Bắt buộc với Firebase
    }

    public Category(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}

