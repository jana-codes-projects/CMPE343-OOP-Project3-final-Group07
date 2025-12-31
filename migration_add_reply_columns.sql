-- Migration script to add reply functionality to messages table
-- Run this if you already have a database without reply columns

USE greengrocer_db;

ALTER TABLE messages 
ADD COLUMN reply_text LONGTEXT NULL AFTER created_at,
ADD COLUMN replied_at TIMESTAMP NULL AFTER reply_text;

