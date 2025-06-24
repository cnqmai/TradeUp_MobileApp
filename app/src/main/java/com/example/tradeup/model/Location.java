package com.example.tradeup.model;

public class Location {
    private double lat;
    private double lng;
    private String manual_address;

    public Location() {
        // Required for Firebase
    }

    public Location(double lat, double lng, String manual_address) {
        this.lat = lat;
        this.lng = lng;
        this.manual_address = manual_address;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
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