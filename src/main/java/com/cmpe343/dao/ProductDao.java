package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Product;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();

        String sql = """
            SELECT id, name, type, price, stock_kg, threshold_kg
            FROM products
            ORDER BY name
        """;

        try (Connection c = Db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getDouble("price"),
                        rs.getDouble("stock_kg"),
                        rs.getDouble("threshold_kg")
                ));
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException("Product listesi çekilemedi", e);
        }
    }
}
