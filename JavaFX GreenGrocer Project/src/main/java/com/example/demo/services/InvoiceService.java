package com.example.demo.services;

import com.example.demo.models.Order;
import com.example.demo.models.OrderItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Service class for generating PDF invoices.
 * 
 * @author Group07
 * @version 1.0
 */
public class InvoiceService {
    
    /**
     * Generates a PDF invoice for an order.
     * 
     * @param order the order to generate invoice for
     * @return the PDF as byte array
     * @throws IOException if PDF generation fails
     */
    public byte[] generateInvoicePDF(Order order) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Set up fonts
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            
            float yPosition = 750;
            float margin = 50;
            float lineHeight = 20;
            
            // Header
            contentStream.beginText();
            contentStream.setFont(fontBold, 24);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Group07 GreenGrocer");
            contentStream.endText();
            
            yPosition -= 40;
            
            // Invoice title
            contentStream.beginText();
            contentStream.setFont(fontBold, 18);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("INVOICE");
            contentStream.endText();
            
            yPosition -= 40;
            
            // Order details
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Order ID: " + order.getId());
            contentStream.endText();
            
            yPosition -= lineHeight;
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Order Date: " + order.getOrderTime().format(formatter));
            contentStream.endText();
            
            yPosition -= lineHeight;
            if (order.getRequestedDeliveryTime() != null) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Delivery Date: " + order.getRequestedDeliveryTime().format(formatter));
                contentStream.endText();
                yPosition -= lineHeight;
            }
            
            yPosition -= 20;
            
            // Items header
            contentStream.beginText();
            contentStream.setFont(fontBold, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Items:");
            contentStream.endText();
            
            yPosition -= lineHeight;
            
            // Draw table header line
            contentStream.setLineWidth(1);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(550, yPosition);
            contentStream.stroke();
            yPosition -= lineHeight;
            
            // Items - simplified: assume all items fit on one page
            // For production, implement proper pagination
            for (OrderItem item : order.getItems()) {
                
                String itemLine = String.format("%s - %.2f kg @ %.2f TL = %.2f TL",
                        item.getProduct() != null ? item.getProduct().getName() : "Product " + item.getProductId(),
                        item.getQuantityKg().doubleValue(),
                        item.getUnitPriceApplied().doubleValue(),
                        item.getLineTotal().doubleValue());
                
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(itemLine);
                contentStream.endText();
                
                yPosition -= lineHeight;
            }
            
            yPosition -= 20;
            
            // Totals
            contentStream.setLineWidth(1);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(550, yPosition);
            contentStream.stroke();
            yPosition -= lineHeight;
            
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Subtotal: " + formatCurrency(order.getTotalBeforeTax()));
            contentStream.endText();
            
            yPosition -= lineHeight;
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("VAT (20%): " + formatCurrency(order.getVat()));
            contentStream.endText();
            
            if (order.getLoyaltyDiscount().compareTo(BigDecimal.ZERO) > 0) {
                yPosition -= lineHeight;
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Loyalty Discount: -" + formatCurrency(order.getLoyaltyDiscount()));
                contentStream.endText();
            }
            
            if (order.getCoupon() != null) {
                yPosition -= lineHeight;
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(margin, yPosition);
                BigDecimal couponDiscount = order.getCoupon().calculateDiscount(
                        order.getTotalBeforeTax().add(order.getVat()));
                contentStream.showText("Coupon Discount (" + order.getCoupon().getCode() + "): -" + formatCurrency(couponDiscount));
                contentStream.endText();
            }
            
            yPosition -= lineHeight;
            contentStream.setLineWidth(2);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(550, yPosition);
            contentStream.stroke();
            yPosition -= lineHeight;
            
            contentStream.beginText();
            contentStream.setFont(fontBold, 14);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("TOTAL: " + formatCurrency(order.getTotalAfterTax()));
            contentStream.endText();
        }
        
        // Save to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();
        
        return baos.toByteArray();
    }
    
    /**
     * Formats a BigDecimal as currency string.
     * 
     * @param amount the amount
     * @return formatted currency string
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("%.2f TL", amount.doubleValue());
    }
}

