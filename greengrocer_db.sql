-- ============================================================
-- CMPE343 Project 3 - Greengrocer DB (Schema + Realistic Seed)
-- MySQL compatible
-- ============================================================

DROP DATABASE IF EXISTS greengrocer_db;
CREATE DATABASE greengrocer_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE greengrocer_db;

-- ------------------------
-- TABLES
-- ------------------------

CREATE TABLE users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50) NOT NULL UNIQUE,
  password_hash CHAR(64)    NOT NULL, -- SHA2-256 hex
  role          ENUM('customer','carrier','owner') NOT NULL,
  is_active     TINYINT(1)  NOT NULL DEFAULT 1,
  address       VARCHAR(255),
  phone         VARCHAR(30),
  wallet_balance DECIMAL(10,2) DEFAULT 0.0 COMMENT 'User wallet balance for payments',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE products (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  type          ENUM('VEG','FRUIT') NOT NULL,
  price         DECIMAL(10,2) NOT NULL,
  stock_kg      DECIMAL(10,2) NOT NULL,
  threshold_kg  DECIMAL(10,2) NOT NULL,
  image_path    VARCHAR(255) NULL COMMENT 'Path to product image file',
  image_blob    LONGBLOB COMMENT 'Product image stored as Binary Large Object (BLOB)',
  is_active     TINYINT(1) NOT NULL DEFAULT 1,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE coupons (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(30) NOT NULL UNIQUE,
  kind       ENUM('AMOUNT','PERCENT') NOT NULL,
  value      DECIMAL(10,2) NOT NULL,
  min_cart   DECIMAL(10,2) NOT NULL DEFAULT 0,
  is_active  TINYINT(1) NOT NULL DEFAULT 1,
  expires_at DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE orders (
  id                      INT AUTO_INCREMENT PRIMARY KEY,
  customer_id             INT NOT NULL,
  carrier_id              INT NULL,
  status                  ENUM('CREATED','ASSIGNED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'CREATED',
  order_time              DATETIME NOT NULL,
  requested_delivery_time DATETIME NOT NULL,
  delivered_time          DATETIME NULL,

  total_before_tax        DECIMAL(10,2) NOT NULL DEFAULT 0,
  vat                     DECIMAL(10,2) NOT NULL DEFAULT 0,
  total_after_tax         DECIMAL(10,2) NOT NULL DEFAULT 0,

  coupon_id               INT NULL,
  loyalty_discount        DECIMAL(10,2) NOT NULL DEFAULT 0,

  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id),
  CONSTRAINT fk_orders_carrier  FOREIGN KEY (carrier_id)  REFERENCES users(id),
  CONSTRAINT fk_orders_coupon   FOREIGN KEY (coupon_id)   REFERENCES coupons(id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  order_id           INT NOT NULL,
  product_id         INT NOT NULL,
  kg                 DECIMAL(10,2) NOT NULL,
  unit_price_applied DECIMAL(10,2) NOT NULL,
  line_total         DECIMAL(10,2) NOT NULL,

  CONSTRAINT fk_items_order   FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE messages (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  customer_id INT NOT NULL,
  owner_id    INT NOT NULL,
  text_clob   LONGTEXT NOT NULL COMMENT 'Message text stored as Character Large Object (CLOB)',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reply_text  LONGTEXT NULL COMMENT 'Reply text stored as Character Large Object (CLOB)',
  replied_at  TIMESTAMP NULL,
  CONSTRAINT fk_msg_customer FOREIGN KEY (customer_id) REFERENCES users(id),
  CONSTRAINT fk_msg_owner    FOREIGN KEY (owner_id)    REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE ratings (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  order_id    INT NOT NULL UNIQUE,
  carrier_id  INT NOT NULL,
  customer_id INT NOT NULL,
  rating      TINYINT NOT NULL,
  comment     VARCHAR(255),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rat_order    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_rat_carrier  FOREIGN KEY (carrier_id) REFERENCES users(id),
  CONSTRAINT fk_rat_customer FOREIGN KEY (customer_id) REFERENCES users(id),
  CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE invoices (
  order_id      INT PRIMARY KEY,
  pdf_blob      LONGBLOB NOT NULL COMMENT 'PDF invoice stored as Binary Large Object (BLOB)',
  invoice_text  LONGTEXT NULL COMMENT 'Invoice/transaction log text stored as Character Large Object (CLOB)',
  transaction_log LONGTEXT NULL COMMENT 'Transaction log details stored as Character Large Object (CLOB)',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inv_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------
-- SEED (Realistic)
-- ------------------------

INSERT INTO users (id, username, password_hash, role, is_active, address, phone, created_at) VALUES
(1,'ahmet.yilmaz',SHA2('Ahmet1234',256),'customer',1,'Maslak Mah. Büyükdere Cd. No:12 Şişli/İstanbul','+90 532 101 10 01','2025-10-01 10:10:00'),
(2,'ayse.demir',SHA2('Ayse1234',256),'customer',1,'Levent Mah. Karanfil Sk. No:8 Beşiktaş/İstanbul','+90 533 202 20 02','2025-10-02 11:20:00'),
(3,'mehmet.kaya',SHA2('Mehmet1234',256),'customer',1,'Ataşehir Bulvarı No:15 Ataşehir/İstanbul','+90 534 303 30 03','2025-10-03 09:05:00'),
(4,'elif.sahin',SHA2('Elif1234',256),'customer',1,'Bağdat Cd. No:120 Kadıköy/İstanbul','+90 535 404 40 04','2025-10-04 13:40:00'),
(5,'mert.ozkan',SHA2('Mert1234',256),'customer',1,'İnönü Mah. No:5 Maltepe/İstanbul','+90 536 505 50 05','2025-10-05 08:30:00'),
(6,'zeynep.akar',SHA2('Zeynep1234',256),'customer',1,'Göztepe Mah. No:22 Kadıköy/İstanbul','+90 537 606 60 06','2025-10-06 14:15:00'),
(7,'can.kilic',SHA2('Can1234',256),'customer',1,'Yeniköy Mah. No:3 Sarıyer/İstanbul','+90 538 707 70 07','2025-10-07 16:45:00'),
(8,'eda.celik',SHA2('Eda1234',256),'customer',1,'Huzur Mah. No:18 Sarıyer/İstanbul','+90 539 808 80 08','2025-10-08 12:00:00'),
(9,'deniz.arslan',SHA2('Deniz1234',256),'customer',1,'Kozyatağı Mah. No:44 Kadıköy/İstanbul','+90 530 909 90 09','2025-10-09 18:10:00'),
(10,'selin.gunes',SHA2('Selin1234',256),'customer',1,'Avcılar Merkez Mah. No:7 Avcılar/İstanbul','+90 531 111 11 10','2025-10-10 10:55:00'),
(11,'burak.yavuz',SHA2('Burak1234',256),'customer',1,'Bahçelievler Mah. No:9 Bahçelievler/İstanbul','+90 532 222 22 11','2025-10-11 15:25:00'),
(12,'sude.koc',SHA2('Sude1234',256),'customer',1,'Bostancı Mah. No:16 Kadıköy/İstanbul','+90 533 333 33 12','2025-10-12 09:35:00'),
(13,'emre.yildirim',SHA2('Emre1234',256),'customer',1,'Etiler Mah. No:2 Beşiktaş/İstanbul','+90 534 444 44 13','2025-10-13 19:05:00'),
(14,'hazal.aydin',SHA2('Hazal1234',256),'customer',1,'Kartal Merkez Mah. No:11 Kartal/İstanbul','+90 535 555 55 14','2025-10-14 08:05:00'),
(15,'onur.dogan',SHA2('Onur1234',256),'customer',1,'Üsküdar Selimiye Mah. No:6 Üsküdar/İstanbul','+90 536 666 66 15','2025-10-15 11:45:00'),
(16,'irem.kara',SHA2('Irem1234',256),'customer',1,'Sancaktepe Mah. No:14 Sancaktepe/İstanbul','+90 537 777 77 16','2025-10-16 13:10:00'),
(17,'kerem.ulus',SHA2('Kerem1234',256),'customer',1,'Taksim Mah. No:27 Beyoğlu/İstanbul','+90 538 888 88 17','2025-10-17 20:20:00'),
(18,'dilara.boz',SHA2('Dilara1234',256),'customer',1,'Pendik Yenişehir Mah. No:19 Pendik/İstanbul','+90 539 999 99 18','2025-10-18 17:00:00'),
(19,'kargo.umut',SHA2('Umut1234',256),'carrier',1,'Depo 1: İkitelli OSB, Başakşehir/İstanbul','+90 532 121 21 19','2025-10-05 09:00:00'),
(20,'kargo.melike',SHA2('Melike1234',256),'carrier',1,'Depo 2: Dudullu OSB, Ümraniye/İstanbul','+90 533 232 32 20','2025-10-06 09:10:00'),
(21,'kargo.kaan',SHA2('Kaan1234',256),'carrier',1,'Depo 3: Yenibosna, Bahçelievler/İstanbul','+90 534 343 43 21','2025-10-07 09:20:00'),
(22,'kargo.asli',SHA2('Asli1234',256),'carrier',1,'Depo 4: Topkapı, Zeytinburnu/İstanbul','+90 535 454 54 22','2025-10-08 09:30:00'),
(23,'kargo.furkan',SHA2('Furkan1234',256),'carrier',1,'Depo 5: Haramidere, Esenyurt/İstanbul','+90 536 565 65 23','2025-10-09 09:40:00'),
(24,'kargo.naz',SHA2('Naz1234',256),'carrier',1,'Depo 6: Bayrampaşa, İstanbul','+90 537 676 76 24','2025-10-10 09:50:00'),
(25,'owner.murat',SHA2('Murat1234',256),'owner',1,'Merkez: Hal Müdürlüğü Yakını, Kağıthane/İstanbul','+90 538 787 87 25','2025-09-25 08:00:00'),
(26,'cust',SHA2('cust',256),'customer',1,'Default Customer Address','+90 555 000 00 01','2025-09-01 00:00:00'),
(27,'carr',SHA2('carr',256),'carrier',1,'Default Carrier Address','+90 555 000 00 02','2025-09-01 00:00:00'),
(28,'own',SHA2('own',256),'owner',1,'Default Owner Address','+90 555 000 00 03','2025-09-01 00:00:00');

INSERT INTO products (id, name, type, price, stock_kg, threshold_kg, image_blob, is_active) VALUES
(1,'Tomato','VEG',34.90,120.00,25.00,NULL,1),
(2,'Cucumber','VEG',29.90,90.00,20.00,NULL,1),
(3,'Pepper (Charleston)','VEG',44.90,70.00,18.00,NULL,1),
(4,'Eggplant','VEG',39.90,65.00,15.00,NULL,1),
(5,'Potato','VEG',19.90,200.00,40.00,NULL,1),
(6,'Onion','VEG',14.90,180.00,35.00,NULL,1),
(7,'Carrot','VEG',22.90,110.00,22.00,NULL,1),
(8,'Lettuce (Curly)','VEG',24.90,55.00,12.00,NULL,1),
(9,'Spinach','VEG',29.90,45.00,10.00,NULL,1),
(10,'Zucchini','VEG',27.90,60.00,14.00,NULL,1),
(11,'Broccoli','VEG',49.90,35.00,9.00,NULL,1),
(12,'Cauliflower','VEG',46.90,38.00,10.00,NULL,1),
(13,'Garlic','VEG',119.90,18.00,6.00,NULL,1),
(14,'Parsley','VEG',9.90,25.00,8.00,NULL,1),
(15,'Lemon (Per Piece)','VEG',6.50,80.00,18.00,NULL,1),
(16,'Apple (Starking)','FRUIT',34.90,140.00,30.00,NULL,1),
(17,'Banana','FRUIT',59.90,85.00,20.00,NULL,1),
(18,'Orange','FRUIT',29.90,160.00,35.00,NULL,1),
(19,'Mandarin','FRUIT',27.90,120.00,25.00,NULL,1),
(20,'Strawberry','FRUIT',99.90,25.00,8.00,NULL,1),
(21,'Grape (Black)','FRUIT',79.90,35.00,10.00,NULL,1),
(22,'Pear','FRUIT',39.90,90.00,20.00,NULL,1),
(23,'Pomegranate','FRUIT',44.90,70.00,18.00,NULL,1),
(24,'Peach','FRUIT',54.90,45.00,12.00,NULL,1),
(25,'Apricot','FRUIT',69.90,32.00,10.00,NULL,1),
(26,'Pineapple','FRUIT',129.90,14.00,6.00,NULL,1),
(27,'Kiwi','FRUIT',74.90,22.00,8.00,NULL,1),
(28,'Avocado','FRUIT',89.90,18.00,7.00,NULL,1),
(29,'Watermelon','FRUIT',14.90,220.00,50.00,NULL,1),
(30,'Melon','FRUIT',24.90,160.00,40.00,NULL,1);

INSERT INTO coupons (id, code, kind, value, min_cart, is_active, expires_at) VALUES
(1,'HOSGELDIN25','AMOUNT',25.00,150.00,1,'2026-06-30 23:59:59'),
(2,'KIS10','PERCENT',10.00,200.00,1,'2026-03-31 23:59:59'),
(3,'SEPET30','AMOUNT',30.00,250.00,1,'2026-02-28 23:59:59'),
(4,'MEYVE5','PERCENT',5.00,120.00,1,'2026-04-30 23:59:59'),
(5,'SEBZE8','PERCENT',8.00,180.00,1,'2026-05-31 23:59:59'),
(6,'BESIKTAS20','AMOUNT',20.00,200.00,1,'2026-06-30 23:59:59'),
(7,'KADIKOY15','AMOUNT',15.00,160.00,1,'2026-06-30 23:59:59'),
(8,'VIP12','PERCENT',12.00,300.00,1,'2026-12-31 23:59:59'),
(9,'NAR20','AMOUNT',20.00,220.00,1,'2026-01-31 23:59:59'),
(10,'PAZAR7','PERCENT',7.00,140.00,1,'2026-07-31 23:59:59'),
(11,'KARGO0','AMOUNT',10.00,200.00,1,'2026-08-31 23:59:59'),
(12,'ELMA10','AMOUNT',10.00,120.00,1,'2026-09-30 23:59:59'),
(13,'MUTFAK25','AMOUNT',25.00,240.00,1,'2026-10-31 23:59:59'),
(14,'YAZ10','PERCENT',10.00,200.00,1,'2026-08-31 23:59:59'),
(15,'KIS20','PERCENT',20.00,400.00,1,'2026-02-28 23:59:59'),
(16,'MURAT30','AMOUNT',30.00,300.00,1,'2026-12-31 23:59:59'),
(17,'HIZLI5','PERCENT',5.00,100.00,1,'2026-11-30 23:59:59'),
(18,'GUNUNU','AMOUNT',18.00,180.00,1,'2026-12-31 23:59:59'),
(19,'FIRSAT15','PERCENT',15.00,250.00,1,'2026-03-31 23:59:59'),
(20,'SEPET50','AMOUNT',50.00,500.00,1,'2026-12-31 23:59:59'),
(21,'MARKET12','PERCENT',12.00,220.00,1,'2026-06-30 23:59:59'),
(22,'AVOKADO20','AMOUNT',20.00,260.00,1,'2026-05-31 23:59:59'),
(23,'PORTAKAL6','PERCENT',6.00,130.00,1,'2026-04-30 23:59:59'),
(24,'MERVE25','AMOUNT',25.00,230.00,1,'2026-09-30 23:59:59'),
(25,'SONBAHAR9','PERCENT',9.00,170.00,1,'2026-10-31 23:59:59');

INSERT INTO orders
(id, customer_id, carrier_id, status, order_time, requested_delivery_time, delivered_time,
 total_before_tax, vat, total_after_tax, coupon_id, loyalty_discount)
VALUES
(1, 1, NULL,'CREATED','2025-12-18 10:10:00','2025-12-19 10:00:00',NULL,0,0,0,1,0),
(2, 2, NULL,'CREATED','2025-12-18 11:05:00','2025-12-19 11:00:00',NULL,0,0,0,NULL,0),
(3, 3, NULL,'CREATED','2025-12-18 12:20:00','2025-12-19 12:00:00',NULL,0,0,0,2,0),
(4, 4, NULL,'CREATED','2025-12-18 13:10:00','2025-12-19 13:00:00',NULL,0,0,0,NULL,15.00),
(5, 5, NULL,'CREATED','2025-12-18 14:05:00','2025-12-19 14:00:00',NULL,0,0,0,3,0),
(6, 6, NULL,'CREATED','2025-12-18 15:15:00','2025-12-19 15:00:00',NULL,0,0,0,NULL,0),
(7, 7, NULL,'CREATED','2025-12-18 16:20:00','2025-12-19 16:00:00',NULL,0,0,0,4,0),
(8, 8, NULL,'CREATED','2025-12-18 17:00:00','2025-12-19 17:00:00',NULL,0,0,0,NULL,0),
(9, 9, 19,'ASSIGNED','2025-12-17 10:10:00','2025-12-18 10:00:00',NULL,0,0,0,5,0),
(10,10,20,'ASSIGNED','2025-12-17 11:30:00','2025-12-18 12:00:00',NULL,0,0,0,NULL,0),
(11,11,21,'ASSIGNED','2025-12-17 12:45:00','2025-12-18 13:30:00',NULL,0,0,0,6,0),
(12,12,22,'ASSIGNED','2025-12-17 13:10:00','2025-12-18 14:00:00',NULL,0,0,0,NULL,10.00),
(13,13,23,'ASSIGNED','2025-12-17 14:20:00','2025-12-18 15:00:00',NULL,0,0,0,7,0),
(14,14,24,'ASSIGNED','2025-12-17 15:00:00','2025-12-18 16:00:00',NULL,0,0,0,NULL,0),
(15,15,19,'ASSIGNED','2025-12-17 16:30:00','2025-12-18 18:00:00',NULL,0,0,0,8,0),
(16,16,20,'ASSIGNED','2025-12-17 17:10:00','2025-12-18 19:00:00',NULL,0,0,0,NULL,0),
(17,17,21,'ASSIGNED','2025-12-17 18:05:00','2025-12-18 20:00:00',NULL,0,0,0,9,0),
(18,18,22,'ASSIGNED','2025-12-17 19:20:00','2025-12-18 21:00:00',NULL,0,0,0,NULL,0),
(19, 1, 23,'DELIVERED','2025-12-16 10:00:00','2025-12-17 11:00:00','2025-12-17 11:20:00',0,0,0,10,0),
(20, 2, 24,'DELIVERED','2025-12-16 11:15:00','2025-12-17 12:00:00','2025-12-17 12:35:00',0,0,0,NULL,0),
(21, 3, 19,'DELIVERED','2025-12-16 12:30:00','2025-12-17 13:30:00','2025-12-17 13:55:00',0,0,0,11,0),
(22, 4, 20,'DELIVERED','2025-12-16 13:20:00','2025-12-17 14:15:00','2025-12-17 14:40:00',0,0,0,NULL,15.00),
(23, 5, 21,'DELIVERED','2025-12-16 14:10:00','2025-12-17 15:00:00','2025-12-17 15:25:00',0,0,0,12,0),
(24, 6, 22,'DELIVERED','2025-12-16 15:05:00','2025-12-17 16:00:00','2025-12-17 16:30:00',0,0,0,NULL,0),
(25, 7, 23,'DELIVERED','2025-12-16 16:40:00','2025-12-17 17:30:00','2025-12-17 18:05:00',0,0,0,13,0);

INSERT INTO order_items (id, order_id, product_id, kg, unit_price_applied, line_total) VALUES
(1,1,1,1.50,34.90,52.35),(2,1,2,1.00,29.90,29.90),
(3,2,5,3.00,19.90,59.70),(4,2,6,2.00,14.90,29.80),
(5,3,16,2.00,34.90,69.80),(6,3,18,3.00,29.90,89.70),
(7,4,17,1.20,59.90,71.88),(8,4,20,0.75,99.90,74.93),
(9,5,3,1.00,44.90,44.90),(10,5,4,1.00,39.90,39.90),(11,5,7,1.50,22.90,34.35),
(12,6,8,2.00,24.90,49.80),(13,6,14,1.00,9.90,9.90),
(14,7,21,1.00,79.90,79.90),(15,7,22,2.00,39.90,79.80),
(16,8,29,5.00,14.90,74.50),(17,8,30,3.00,24.90,74.70),
(18,9,1,2.00,34.90,69.80),(19,9,15,2.00,6.50,13.00),
(20,10,5,4.00,19.90,79.60),(21,10,7,2.00,22.90,45.80),
(22,11,16,2.50,34.90,87.25),(23,11,23,1.50,44.90,67.35),
(24,12,11,1.00,49.90,49.90),(25,12,12,1.00,46.90,46.90),
(26,13,17,1.00,59.90,59.90),(27,13,28,1.00,89.90,89.90),
(28,14,10,2.00,27.90,55.80),(29,14,9,1.50,29.90,44.85),
(30,15,18,4.00,29.90,119.60),(31,15,19,2.50,27.90,69.75),
(32,16,2,2.00,29.90,59.80),(33,16,3,1.50,44.90,67.35),
(34,17,24,1.20,54.90,65.88),(35,17,25,1.00,69.90,69.90),
(36,18,26,1.00,129.90,129.90),(37,18,27,1.00,74.90,74.90),
(38,19,1,1.00,34.90,34.90),(39,19,2,1.00,29.90,29.90),(40,19,16,1.00,34.90,34.90),
(41,20,5,3.00,19.90,59.70),(42,20,6,2.00,14.90,29.80),
(43,21,20,0.50,99.90,49.95),(44,21,21,1.00,79.90,79.90),
(45,22,17,1.00,59.90,59.90),(46,22,18,2.00,29.90,59.80),
(47,23,11,1.00,49.90,49.90),(48,23,12,1.00,46.90,46.90),(49,23,13,0.30,119.90,35.97),
(50,24,29,4.00,14.90,59.60),(51,24,30,2.00,24.90,49.80),
(52,25,22,2.00,39.90,79.80),(53,25,23,1.50,44.90,67.35),
(54,6,16,1.00,34.90,34.90),
(55,7,15,3.00,6.50,19.50),
(56,9,7,2.00,22.90,45.80),
(57,10,18,2.00,29.90,59.80),
(58,12,14,2.00,9.90,19.80),
(59,14,16,1.00,34.90,34.90),
(60,20,1,1.50,34.90,52.35);

UPDATE orders o
JOIN (
  SELECT order_id, ROUND(SUM(line_total),2) AS subtotal
  FROM order_items
  GROUP BY order_id
) x ON x.order_id=o.id
SET
  o.total_before_tax = x.subtotal,
  o.vat = ROUND(x.subtotal * 0.20, 2),
  o.total_after_tax = ROUND(x.subtotal + (x.subtotal * 0.20) - o.loyalty_discount, 2);

INSERT INTO messages (id, customer_id, owner_id, text_clob, created_at) VALUES
(1,1,25,'Hello, can I get information about the freshness of tomatoes and cucumbers?','2025-12-18 10:30:00'),
(2,2,25,'Can we move the delivery time for my order forward by 1 hour?','2025-12-18 11:40:00'),
(3,3,25,'Are the avocados ripe?','2025-12-18 12:55:00'),
(4,4,25,'When will the strawberry stock be replenished?','2025-12-18 13:35:00'),
(5,5,25,'Why was my coupon not applied? (HOSGELDIN25)','2025-12-18 14:25:00'),
(6,6,25,'How is spinach packaged?','2025-12-18 15:40:00'),
(7,7,25,'Will the bananas arrive without turning black?','2025-12-18 16:50:00'),
(8,8,25,'Can I choose whether the watermelon comes sliced or whole?','2025-12-18 17:10:00'),
(9,9,25,'The potatoes won''t have sprouts, right?','2025-12-17 10:30:00'),
(10,10,25,'I updated my address, will my order come to the new address?','2025-12-17 11:50:00'),
(11,11,25,'Will the oranges be juicy?','2025-12-17 13:10:00'),
(12,12,25,'Is the broccoli fresh and suitable for freezing?','2025-12-17 13:40:00'),
(13,13,25,'Are the grapes seeded or seedless?','2025-12-17 14:40:00'),
(14,14,25,'Can I add products to my order later?','2025-12-17 15:10:00'),
(15,15,25,'Is delivery done with kargo.melike?','2025-12-17 16:50:00'),
(16,16,25,'Can coupons be combined?','2025-12-17 17:20:00'),
(17,17,25,'The garlic price seems a bit high, is that correct?','2025-12-17 18:15:00'),
(18,18,25,'Is there a ripe selection for kiwi?','2025-12-17 19:40:00'),
(19,1,25,'I need the invoice for my previous order.','2025-12-16 10:20:00'),
(20,2,25,'Where can I rate the delivery courier?','2025-12-16 11:35:00'),
(21,3,25,'Are the pomegranates sweet or sour?','2025-12-16 12:40:00'),
(22,4,25,'Is lemon sold per piece or per kg?','2025-12-16 13:30:00'),
(23,5,25,'Do the apricots come soft or hard?','2025-12-16 14:25:00'),
(24,6,25,'Is there a selection for melon?','2025-12-16 15:15:00'),
(25,7,25,'My order appears to be delivered but I didn''t receive it.','2025-12-16 16:55:00');

INSERT INTO ratings (id, order_id, carrier_id, customer_id, rating, comment, created_at) VALUES
(1,1,19,1,4,'Expecting fast delivery.','2025-12-19 12:00:00'),
(2,2,20,2,5,'Packaging was very good.','2025-12-19 12:10:00'),
(3,3,21,3,4,'Products were fresh.','2025-12-19 12:20:00'),
(4,4,22,4,3,'Delivery was a bit delayed.','2025-12-19 12:30:00'),
(5,5,23,5,5,'Perfect!','2025-12-19 12:40:00'),
(6,6,24,6,4,'Generally good.','2025-12-19 12:50:00'),
(7,7,19,7,4,'Courier was polite.','2025-12-19 13:00:00'),
(8,8,20,8,5,'Very satisfied.','2025-12-19 13:10:00'),
(9,9,19,9,4,'Products were intact.','2025-12-18 10:30:00'),
(10,10,20,10,5,'Arrived quickly.','2025-12-18 12:20:00'),
(11,11,21,11,4,'Nice.','2025-12-18 13:40:00'),
(12,12,22,12,5,'Great.','2025-12-18 14:10:00'),
(13,13,23,13,4,'Good.','2025-12-18 15:20:00'),
(14,14,24,14,3,'Average.','2025-12-18 16:10:00'),
(15,15,19,15,5,'Very good.','2025-12-18 18:40:00'),
(16,16,20,16,4,'Satisfied.','2025-12-18 19:20:00'),
(17,17,21,17,5,'Super.','2025-12-18 20:05:00'),
(18,18,22,18,4,'Good.','2025-12-18 21:00:00'),
(19,19,23,1,5,'Delivery on time.','2025-12-17 12:00:00'),
(20,20,24,2,4,'Products are fresh.','2025-12-17 13:00:00'),
(21,21,19,3,5,'Very satisfied.','2025-12-17 14:00:00'),
(22,22,20,4,4,'Good.','2025-12-17 15:00:00'),
(23,23,21,5,5,'Great service.','2025-12-17 16:00:00'),
(24,24,22,6,4,'Good packaging.','2025-12-17 17:00:00'),
(25,25,23,7,2,'Shows as delivered but there is a problem.','2025-12-17 18:10:00');

INSERT INTO invoices (order_id, pdf_blob, created_at) VALUES
(1, CAST('%PDF-1.4\n% Dummy invoice for order 1\n%%EOF' AS BINARY),'2025-12-18 10:10:05'),
(2, CAST('%PDF-1.4\n% Dummy invoice for order 2\n%%EOF' AS BINARY),'2025-12-18 11:05:05'),
(3, CAST('%PDF-1.4\n% Dummy invoice for order 3\n%%EOF' AS BINARY),'2025-12-18 12:20:05'),
(4, CAST('%PDF-1.4\n% Dummy invoice for order 4\n%%EOF' AS BINARY),'2025-12-18 13:10:05'),
(5, CAST('%PDF-1.4\n% Dummy invoice for order 5\n%%EOF' AS BINARY),'2025-12-18 14:05:05'),
(6, CAST('%PDF-1.4\n% Dummy invoice for order 6\n%%EOF' AS BINARY),'2025-12-18 15:15:05'),
(7, CAST('%PDF-1.4\n% Dummy invoice for order 7\n%%EOF' AS BINARY),'2025-12-18 16:20:05'),
(8, CAST('%PDF-1.4\n% Dummy invoice for order 8\n%%EOF' AS BINARY),'2025-12-18 17:00:05'),
(9, CAST('%PDF-1.4\n% Dummy invoice for order 9\n%%EOF' AS BINARY),'2025-12-17 10:10:05'),
(10,CAST('%PDF-1.4\n% Dummy invoice for order 10\n%%EOF' AS BINARY),'2025-12-17 11:30:05'),
(11,CAST('%PDF-1.4\n% Dummy invoice for order 11\n%%EOF' AS BINARY),'2025-12-17 12:45:05'),
(12,CAST('%PDF-1.4\n% Dummy invoice for order 12\n%%EOF' AS BINARY),'2025-12-17 13:10:05'),
(13,CAST('%PDF-1.4\n% Dummy invoice for order 13\n%%EOF' AS BINARY),'2025-12-17 14:20:05'),
(14,CAST('%PDF-1.4\n% Dummy invoice for order 14\n%%EOF' AS BINARY),'2025-12-17 15:00:05'),
(15,CAST('%PDF-1.4\n% Dummy invoice for order 15\n%%EOF' AS BINARY),'2025-12-17 16:30:05'),
(16,CAST('%PDF-1.4\n% Dummy invoice for order 16\n%%EOF' AS BINARY),'2025-12-17 17:10:05'),
(17,CAST('%PDF-1.4\n% Dummy invoice for order 17\n%%EOF' AS BINARY),'2025-12-17 18:05:05'),
(18,CAST('%PDF-1.4\n% Dummy invoice for order 18\n%%EOF' AS BINARY),'2025-12-17 19:20:05'),
(19,CAST('%PDF-1.4\n% Dummy invoice for order 19\n%%EOF' AS BINARY),'2025-12-16 10:00:05'),
(20,CAST('%PDF-1.4\n% Dummy invoice for order 20\n%%EOF' AS BINARY),'2025-12-16 11:15:05'),
(21,CAST('%PDF-1.4\n% Dummy invoice for order 21\n%%EOF' AS BINARY),'2025-12-16 12:30:05'),
(22,CAST('%PDF-1.4\n% Dummy invoice for order 22\n%%EOF' AS BINARY),'2025-12-16 13:20:05'),
(23,CAST('%PDF-1.4\n% Dummy invoice for order 23\n%%EOF' AS BINARY),'2025-12-16 14:10:05'),
(24,CAST('%PDF-1.4\n% Dummy invoice for order 24\n%%EOF' AS BINARY),'2025-12-16 15:05:05'),
(25,CAST('%PDF-1.4\n% Dummy invoice for order 25\n%%EOF' AS BINARY),'2025-12-16 16:40:05');

SELECT 'users' tbl, COUNT(*) cnt FROM users
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'coupons', COUNT(*) FROM coupons
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL SELECT 'messages', COUNT(*) FROM messages
UNION ALL SELECT 'ratings', COUNT(*) FROM ratings
UNION ALL SELECT 'invoices', COUNT(*) FROM invoices;
