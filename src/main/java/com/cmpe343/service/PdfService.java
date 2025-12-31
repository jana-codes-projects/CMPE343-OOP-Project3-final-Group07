package com.cmpe343.service;

import com.cmpe343.dao.OrderDao;
import com.cmpe343.model.Order;
import com.cmpe343.model.CartItem;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class PdfService {
    
    public File generateInvoice(Order order) throws IOException {
        File tempFile = File.createTempFile("invoice_" + order.getId() + "_", ".pdf");
        
        try (PdfWriter writer = new PdfWriter(tempFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            // Header
            Paragraph title = new Paragraph("INVOICE")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            
            document.add(new Paragraph(" ")); // Spacing
            
            // Order Info
            Paragraph orderInfo = new Paragraph()
                    .add("Order ID: #" + order.getId() + "\n")
                    .add("Date: " + order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n")
                    .add("Status: " + order.getStatus().name());
            document.add(orderInfo);
            
            document.add(new Paragraph(" ")); // Spacing
            
            // Items Table
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1, 1}))
                        .useAllAvailableWidth();
                
                table.addHeaderCell("Product");
                table.addHeaderCell("Quantity (kg)");
                table.addHeaderCell("Unit Price");
                table.addHeaderCell("Total");
                
                for (CartItem item : order.getItems()) {
                    table.addCell(item.getProduct().getName());
                    table.addCell(String.format("%.2f", item.getQuantityKg()));
                    table.addCell(String.format("%.2f ₺", item.getUnitPrice()));
                    table.addCell(String.format("%.2f ₺", item.getLineTotal()));
                }
                
                document.add(table);
            }
            
            document.add(new Paragraph(" ")); // Spacing
            
            // Totals
            OrderDao orderDao = new OrderDao();
            double couponDiscount = orderDao.getCouponDiscountForOrder(order.getId());
            
            Paragraph totals = new Paragraph();
            
            // Calculate original subtotal if coupon was applied
            double originalSubtotal = order.getTotalBeforeTax();
            if (couponDiscount > 0) {
                originalSubtotal = order.getTotalBeforeTax() + couponDiscount;
                totals.add("Subtotal (before discount): " + String.format("%.2f ₺", originalSubtotal) + "\n");
                totals.add("Coupon Discount: -" + String.format("%.2f ₺", couponDiscount) + "\n");
                totals.add("Subtotal (after discount): " + String.format("%.2f ₺", order.getTotalBeforeTax()) + "\n");
            } else {
                totals.add("Subtotal: " + String.format("%.2f ₺", order.getTotalBeforeTax()) + "\n");
            }
            
            totals.add("VAT (20%): " + String.format("%.2f ₺", order.getVat()) + "\n");
            totals.add("Total: " + String.format("%.2f ₺", order.getTotalAfterTax()));
            totals.setBold();
            document.add(totals);
            
            // Footer
            document.add(new Paragraph(" ")); // Spacing
            Paragraph footer = new Paragraph("Thank you for your order!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic();
            document.add(footer);
        }
        
        return tempFile;
    }
}
