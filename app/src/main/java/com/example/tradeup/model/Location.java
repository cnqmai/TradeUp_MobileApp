package com.example.tradeup.model;

public class Location {
    private Double lat;
    private Double lng;
    private String manual_address;

    public Location() {
        // Required for Firebase
    }

    public Location(Double lat, Double lng, String manual_address) {
        this.lat = lat;
        this.lng = lng;
        this.manual_address = manual_address;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getManual_address() {
        return manual_address;
    }

    public void setManual_address(String manual_address) {
        this.manual_address = manual_address;
    }
}