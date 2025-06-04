package com.example.tradeup.features.product;

public class Product {
    private String name;
    private String imageUrl;
    private String price;
    private float rating;
    private int views;  // lượt xem
    private int likes;  // lượt thích

    public Product() {
        // Firebase cần constructor mặc định
    }

    public Product(String name, String imageUrl, String price, float rating, int views, int likes) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.price = price;
        this.rating = rating;
        this.views = views;
        this.likes = likes;
    }

    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public String getPrice() { return price; }
    public float getRating() { return rating; }
    public int getViews() { return views; }
    public int getLikes() { return likes; }

    // Tính popularity trên fly khi cần
    public int getPopularity() {
        return views + likes;
    }
}
