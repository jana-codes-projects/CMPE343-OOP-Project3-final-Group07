package com.cmpe343.service;

import com.cmpe343.dao.OrderDao;
import com.cmpe343.model.Order;
import com.cmpe343.model.CartItem;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
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
                        Paragraph title = new Paragraph("GREEN GROCER - INVOICE")
                                        .setFontSize(24)
                                        .setBold()
                                        .setTextAlignment(TextAlignment.CENTER);
                        document.add(title);

                        document.add(new Paragraph(" ")); // Spacing

                        // Order Info
                        Paragraph orderInfo = new Paragraph()
                                        .add("Order ID: #" + order.getId() + "\n")
                                        .add("Date: " + order.getOrderTime()
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n")
                                        .add("Status: " + order.getStatus().name());
                        document.add(orderInfo);

                        document.add(new Paragraph(" ")); // Spacing

                        // Items Table
                        if (order.getItems() != null && !order.getItems().isEmpty()) {
                                Table table = new Table(UnitValue.createPercentArray(new float[] { 3, 1, 1, 1 }))
                                                .useAllAvailableWidth();

                                table.addHeaderCell("Product");
                                table.addHeaderCell("Qty (kg)");
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
                        double subtotal = order.getTotalBeforeTax();
                        if (couponDiscount > 0) {
                                totals.add("Subtotal (brut): " + String.format("%.2f ₺", subtotal + couponDiscount)
                                                + "\n");
                                totals.add("Coupon Discount: -" + String.format("%.2f ₺", couponDiscount) + "\n");
                        }
                        totals.add("Subtotal: " + String.format("%.2f ₺", subtotal) + "\n");
                        totals.add("VAT (20%): " + String.format("%.2f ₺", order.getVat()) + "\n");
                        totals.add("Final Total: " + String.format("%.2f ₺", order.getTotalAfterTax()));
                        totals.setBold();
                        document.add(totals);

                        // QR Code for validation
                        document.add(new Paragraph("\n"));
                        String qrData = "VERIFY:ORDER:" + order.getId() + "|TOTAL:" + order.getTotalAfterTax()
                                        + "|GGROCER";
                        BarcodeQRCode qrCode = new BarcodeQRCode(qrData);
                        PdfFormXObject qrObject = qrCode.createFormXObject(pdf);
                        Image qrImage = new Image(qrObject)
                                        .setWidth(80)
                                        .setHorizontalAlignment(HorizontalAlignment.CENTER);

                        document.add(new Paragraph("Scan to verify validity")
                                        .setFontSize(8)
                                        .setTextAlignment(TextAlignment.CENTER));
                        document.add(qrImage);

                        // Footer
                        document.add(new Paragraph(" ")); // Spacing
                        Paragraph footer = new Paragraph(
                                        "Thank you for choosing Green Grocer!\nThis is a computer-generated invoice.")
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setFontSize(9)
                                        .setItalic();
                        document.add(footer);
                }

                return tempFile;
        }

        public String generateInvoiceText(Order order) {
                StringBuilder sb = new StringBuilder();
                sb.append("GREEN GROCER - INVOICE\n");
                sb.append("======================\n");
                sb.append("Order ID: #").append(order.getId()).append("\n");
                sb.append("Date: ").append(order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                                .append("\n");
                sb.append("Status: ").append(order.getStatus().name()).append("\n\n");

                sb.append(String.format("%-20s %-10s %-10s %-10s\n", "Product", "Qty", "Price", "Total"));
                sb.append("----------------------------------------------------------\n");

                for (CartItem item : order.getItems()) {
                        sb.append(String.format("%-20s %-10.2f %-10.2f %-10.2f\n",
                                        item.getProduct().getName(),
                                        item.getQuantityKg(),
                                        item.getUnitPrice(),
                                        item.getLineTotal()));
                }

                sb.append("----------------------------------------------------------\n");
                sb.append(String.format("Subtotal: %.2f ₺\n", order.getTotalBeforeTax()));
                sb.append(String.format("VAT (20%%): %.2f ₺\n", order.getVat()));
                sb.append(String.format("Final Total: %.2f ₺\n", order.getTotalAfterTax()));
                sb.append("\nThank you for choosing Green Grocer!");

                return sb.toString();
        }
}
