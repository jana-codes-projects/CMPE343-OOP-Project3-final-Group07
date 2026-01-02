package com.cmpe343.service;

import com.cmpe343.dao.OrderDao;
import com.cmpe343.model.Order;
import com.cmpe343.model.CartItem;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class PdfService {
    
    /**
     * Generates a QR code image as a byte array.
     * 
     * @param data The data to encode in the QR code
     * @param width The width of the QR code image
     * @param height The height of the QR code image
     * @return The QR code image as a byte array
     */
    private byte[] generateQRCodeImage(String data, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }
    
    /**
     * Generates an invoice PDF file.
     * 
     * @param order The order to generate invoice for
     * @return The generated PDF file
     * @throws IOException if PDF generation fails
     */
    public File generateInvoice(Order order) throws IOException {
        File tempFile = File.createTempFile("invoice_" + order.getId() + "_", ".pdf");
        byte[] pdfBytes = generateInvoiceAsBytes(order);
        java.nio.file.Files.write(tempFile.toPath(), pdfBytes);
        return tempFile;
    }
    
    /**
     * Generates an invoice PDF as a byte array (for database storage).
     * 
     * @param order The order to generate invoice for
     * @return The generated PDF as byte array
     * @throws IOException if PDF generation fails
     */
    public byte[] generateInvoiceAsBytes(Order order) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            
            // Set margins
            document.setMargins(50, 50, 50, 50);
            
            // Header with colored background
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                    .useAllAvailableWidth()
                    .setBackgroundColor(ColorConstants.GREEN)
                    .setBorder(Border.NO_BORDER);
            
            Paragraph title = new Paragraph("INVOICE")
                    .setFontSize(32)
                    .setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            
            Paragraph subtitle = new Paragraph("Group07 GreenGrocer")
                    .setFontSize(14)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(0);
            
            Cell headerCell = new Cell().add(title).add(subtitle).setBorder(Border.NO_BORDER).setPadding(20);
            headerTable.addCell(headerCell);
            document.add(headerTable);
            
            document.add(new Paragraph(" ")); // Spacing
            
            // Order Information Section
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            // Left side - Order details
            Paragraph orderInfoTitle = new Paragraph("Order Information")
                    .setFontSize(14)
                    .setBold()
                    .setMarginBottom(8);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            Paragraph orderInfo = new Paragraph()
                    .add("Order ID: #" + order.getId() + "\n")
                    .add("Date: " + order.getOrderTime().format(formatter) + "\n")
                    .add("Status: " + order.getStatus().name())
                    .setFontSize(11);
            
            Cell leftCell = new Cell().add(orderInfoTitle).add(orderInfo).setBorder(Border.NO_BORDER);
            infoTable.addCell(leftCell);
            
            // Right side - QR Code
            try {
                String qrData = "Order #" + order.getId() + " | Total: " + 
                               String.format("%.2f", order.getTotalAfterTax()) + " ₺ | " +
                               order.getOrderTime().format(formatter);
                byte[] qrImageBytes = generateQRCodeImage(qrData, 150, 150);
                ImageData qrImageData = ImageDataFactory.create(qrImageBytes);
                Image qrCode = new Image(qrImageData)
                        .setWidth(100)
                        .setAutoScale(true);
                
                Paragraph qrLabel = new Paragraph("Scan for Order Details")
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(5);
                
                Cell rightCell = new Cell()
                        .add(qrLabel)
                        .add(qrCode)
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.CENTER);
                infoTable.addCell(rightCell);
            } catch (Exception e) {
                // If QR code generation fails, just leave the cell empty
                Cell rightCell = new Cell().setBorder(Border.NO_BORDER);
                infoTable.addCell(rightCell);
            }
            
            document.add(infoTable);
            document.add(new Paragraph(" ")); // Spacing
            
            // Items Table with better styling
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1.5f, 1.5f, 1.5f}))
                        .useAllAvailableWidth()
                        .setMarginBottom(20);
                
                // Header row with styling
                Cell headerCell1 = new Cell().add(new Paragraph("Product").setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setPadding(10);
                Cell headerCell2 = new Cell().add(new Paragraph("Quantity (kg)").setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setPadding(10);
                Cell headerCell3 = new Cell().add(new Paragraph("Unit Price").setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setPadding(10);
                Cell headerCell4 = new Cell().add(new Paragraph("Total").setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setPadding(10);
                
                itemsTable.addHeaderCell(headerCell1);
                itemsTable.addHeaderCell(headerCell2);
                itemsTable.addHeaderCell(headerCell3);
                itemsTable.addHeaderCell(headerCell4);
                
                // Item rows
                for (CartItem item : order.getItems()) {
                    itemsTable.addCell(new Cell().add(new Paragraph(item.getProduct().getName())).setPadding(8));
                    itemsTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", item.getQuantityKg()))).setPadding(8));
                    itemsTable.addCell(new Cell().add(new Paragraph(String.format("%.2f ₺", item.getUnitPrice()))).setPadding(8));
                    itemsTable.addCell(new Cell().add(new Paragraph(String.format("%.2f ₺", item.getLineTotal()))).setPadding(8));
                }
                
                document.add(itemsTable);
            }
            
            document.add(new Paragraph(" ")); // Spacing
            
            // Totals Section with better styling
            OrderDao orderDao = new OrderDao();
            double couponDiscount = orderDao.getCouponDiscountForOrder(order.getId());
            
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            // Left column - labels
            Cell labelCell1 = new Cell().add(new Paragraph("Subtotal:")).setBorder(Border.NO_BORDER).setPadding(5);
            Cell labelCell2 = new Cell();
            Cell labelCell3 = new Cell();
            Cell labelCell4 = new Cell().add(new Paragraph("VAT (20%):")).setBorder(Border.NO_BORDER).setPadding(5);
            Cell labelCell5 = new Cell();
            
            double originalSubtotal = order.getTotalBeforeTax();
            if (couponDiscount > 0) {
                originalSubtotal = order.getTotalBeforeTax() + couponDiscount;
                labelCell1 = new Cell().add(new Paragraph("Subtotal (before discount):")).setBorder(Border.NO_BORDER).setPadding(5);
                labelCell2 = new Cell().add(new Paragraph("Coupon Discount:")).setBorder(Border.NO_BORDER).setPadding(5);
                labelCell3 = new Cell().add(new Paragraph("Subtotal (after discount):")).setBorder(Border.NO_BORDER).setPadding(5);
            }
            
            Cell labelCell6 = new Cell().add(new Paragraph("TOTAL:").setBold().setFontSize(14))
                    .setBorder(Border.NO_BORDER)
                    .setBorderTop(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, 1))
                    .setPaddingTop(10)
                    .setPaddingBottom(5)
                    .setPaddingLeft(5);
            
            // Right column - values
            Cell valueCell1 = new Cell().add(new Paragraph(String.format("%.2f ₺", originalSubtotal)))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5);
            Cell valueCell2 = new Cell();
            Cell valueCell3 = new Cell();
            Cell valueCell4 = new Cell().add(new Paragraph(String.format("%.2f ₺", order.getVat())))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5);
            Cell valueCell5 = new Cell();
            
            if (couponDiscount > 0) {
                valueCell2 = new Cell().add(new Paragraph("-" + String.format("%.2f ₺", couponDiscount)))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(5);
                valueCell3 = new Cell().add(new Paragraph(String.format("%.2f ₺", order.getTotalBeforeTax())))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(5);
            }
            
            Cell valueCell6 = new Cell().add(new Paragraph(String.format("%.2f ₺", order.getTotalAfterTax())).setBold().setFontSize(14))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setBorderTop(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.BLACK, 1))
                    .setPaddingTop(10)
                    .setPaddingBottom(5)
                    .setPaddingRight(5);
            
            totalsTable.addCell(labelCell1);
            totalsTable.addCell(valueCell1);
            if (couponDiscount > 0) {
                totalsTable.addCell(labelCell2);
                totalsTable.addCell(valueCell2);
                totalsTable.addCell(labelCell3);
                totalsTable.addCell(valueCell3);
            }
            totalsTable.addCell(labelCell4);
            totalsTable.addCell(valueCell4);
            totalsTable.addCell(labelCell6);
            totalsTable.addCell(valueCell6);
            
            document.add(totalsTable);
            
            // Footer
            document.add(new Paragraph(" ")); // Spacing
            Paragraph footer = new Paragraph("Thank you for your order!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic()
                    .setFontSize(12)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginTop(30);
            document.add(footer);
        }
        
        return baos.toByteArray();
    }
}
