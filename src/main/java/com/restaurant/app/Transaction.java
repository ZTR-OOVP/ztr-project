/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.restaurant.app;

/**
 *
 * @author tersf
 */
public class Transaction {
 String buyerName, sellerName, orderId, invoiceNo, itemName;
    String paymentMethod, paymentStatus, paymentDetail, proofPath, timestamp, date;
    double unitPrice, totalAmount;
    int qty;
    int rating;
    String reviewComment;

    Transaction(String buyerName, String sellerName, String orderId, String invoiceNo, String itemName,
                double unitPrice, int qty, double totalAmount, String paymentMethod, String paymentStatus,
                String paymentDetail, String proofPath) {
        this(buyerName, sellerName, orderId, invoiceNo, itemName, unitPrice, qty, totalAmount, 
             paymentMethod, paymentStatus, paymentDetail, proofPath, 0, "");
    }

    Transaction(String buyerName, String sellerName, String orderId, String invoiceNo, String itemName,
                double unitPrice, int qty, double totalAmount, String paymentMethod, String paymentStatus,
                String paymentDetail, String proofPath, int rating, String reviewComment) {
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.orderId = orderId;
        this.invoiceNo = invoiceNo;
        this.itemName = itemName;
        this.unitPrice = unitPrice;
        this.qty = qty;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paymentDetail = paymentDetail;
        this.proofPath = proofPath;
        this.rating = rating;
        this.reviewComment = reviewComment;
        this.timestamp = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
        this.date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
    }
}
