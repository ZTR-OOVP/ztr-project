/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.restaurant.app;

/**
 *
 * @author tersf
 */
public class OrderRecord {
String buyerName, sellerName, timestamp, date;
    int orderNumber;
    String[] items, unitPrices;
    int[] stock, ratings;

    OrderRecord(String buyerName, String sellerName, int orderNumber,
                String[] items, String[] unitPrices, int[] stock, int[] ratings) {
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.orderNumber = orderNumber;
        this.items = items.clone();
        this.unitPrices = unitPrices.clone();
        this.stock = stock.clone();
        this.ratings = ratings.clone();
        this.timestamp = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
        this.date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
    }
}
