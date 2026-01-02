package com.cmpe343.util;

import com.cmpe343.dao.ProductDao;
import com.cmpe343.model.Product;
import java.util.List;

public class ListProducts {
    public static void main(String[] args) {
        ProductDao dao = new ProductDao();
        List<Product> products = dao.findAll();
        System.out.println("--- PRODUCTS ---");
        for (Product p : products) {
            System.out.println("ID: " + p.getId() + ", Name: " + p.getName() + ", Type: " + p.getType());
        }
        System.out.println("----------------");
    }
}
