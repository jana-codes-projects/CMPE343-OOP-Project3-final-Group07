package com.cmpe343.service;

import com.cmpe343.dao.ProductDao;

/**
 * Service for populating product images from the resources folder to the database.
 * 
 * @author Group07
 * @version 1.0
 */
public class ImagePopulationService {
    
    /**
     * Populates product images from the resources/images/products folder into the database.
     * This method should be called once on application startup or when images need to be updated.
     * 
     * @return Number of images successfully populated
     */
    public static int populateProductImages() {
        ProductDao productDao = new ProductDao();
        
        // Get the images directory path - try multiple locations
        java.io.File imagesDir = null;
        String[] possiblePaths = {
            "src/main/resources/images/products",
            "src/resources/images/products",
            "images/products",
            "../src/main/resources/images/products",
            "../../src/main/resources/images/products"
        };
        
        for (String path : possiblePaths) {
            java.io.File testDir = new java.io.File(path);
            if (testDir.exists() && testDir.isDirectory()) {
                imagesDir = testDir;
                break;
            }
        }
        
        if (imagesDir == null) {
            System.err.println("Images directory not found. Tried: " + String.join(", ", possiblePaths));
            return 0;
        }
        
        System.out.println("Populating product images from: " + imagesDir.getAbsolutePath());
        int count = productDao.populateProductImagesFromResources(imagesDir.getAbsolutePath());
        System.out.println("Successfully populated " + count + " product images.");
        
        return count;
    }
}
