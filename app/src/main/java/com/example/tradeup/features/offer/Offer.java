package com.example.tradeup.features.offer;

import java.util.Date;

public class Offer {
    private String offerId;
    private String listingId;
    private String buyerId;
    private String sellerId;
    private double offerPrice;
    private String status; // pending, accepted, rejected, counter
    private Date createdAt;
    private Date updatedAt;

    public Offer() {}

    public Offer(String offerId, String listingId, String buyerId, String sellerId,
                 double offerPrice, String status, Date createdAt, Date updatedAt) {
        this.offerId = offerId;
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.offerPrice = offerPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getOfferId() { return offerId; }
    public String getListingId() { return listingId; }
    public String getBuyerId() { return buyerId; }
    public String getSellerId() { return sellerId; }
    public double getOfferPrice() { return offerPrice; }
    public String getStatus() { return status; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }

    public void setOfferId(String offerId) { this.offerId = offerId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setOfferPrice(double offerPrice) { this.offerPrice = offerPrice; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
