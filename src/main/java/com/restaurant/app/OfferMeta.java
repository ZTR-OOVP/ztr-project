/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.restaurant.app;

/**
 *
 * @author tersf
 */
public class OfferMeta {
    String sellerName;
    double totalPrice;
    long responseMs;

    OfferMeta(String sellerName, double totalPrice, long responseMs) {
        this.sellerName = sellerName;
        this.totalPrice = totalPrice;
        this.responseMs = responseMs;
    }
}
