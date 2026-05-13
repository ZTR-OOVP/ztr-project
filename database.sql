-- Create Database
CREATE DATABASE IF NOT EXISTS rtz_restaurant;
USE rtz_restaurant;

-- Table: sellers
CREATE TABLE IF NOT EXISTS sellers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sellerId VARCHAR(50),
    phone VARCHAR(20),
    shortVideoUrl VARCHAR(255)
);

-- Table: orders
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    buyer VARCHAR(255),
    seller VARCHAR(255),
    orderNumber INT,
    date DATE,
    time TIME
);

-- Table: transactions
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    buyer VARCHAR(255),
    seller VARCHAR(255),
    orderId VARCHAR(50),
    invoiceNo VARCHAR(50),
    itemName VARCHAR(255),
    unitPrice DOUBLE,
    qty INT,
    totalAmount DOUBLE,
    paymentMethod VARCHAR(50),
    paymentStatus VARCHAR(20),
    paymentDetail TEXT,
    proofPath VARCHAR(255),
    rating INT DEFAULT 0,
    review_comment TEXT,
    date DATE,
    time TIME
);

-- Insert Default Sellers (Matching code logic)
INSERT INTO sellers (name, sellerId, phone, shortVideoUrl) VALUES 
('Seller 1', 'S-1000', '6289626937487', 'https://www.youtube.com/results?search_query=food'),
('Seller 2', 'S-1001', '6281313330251', 'https://www.youtube.com/results?search_query=food'),
('Seller 3', 'S-1002', '6281555853532', 'https://www.youtube.com/results?search_query=food'),
('Seller 4', 'S-1003', '62895365530096', 'https://www.youtube.com/results?search_query=food'),
('Seller 5', 'S-1004', '6281399893170', 'https://www.youtube.com/results?search_query=food');
