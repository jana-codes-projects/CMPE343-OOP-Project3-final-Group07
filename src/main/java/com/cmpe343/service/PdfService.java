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

    /**
     * Generates a PDF invoice for a given order, including item details and a QR code for verification.
     * @param order The order object containing transaction details
     * @return A temporary File object pointing to the generated PDF
     * @throws IOException If file creation or writing fails
     */
    public File generateInvoice(Order order) throws IOException {
        // Create a temporary file for the invoice
        File tempFile = File.createTempFile("invoice_" + order.getId() + "_", ".pdf");

        try (PdfWriter writer = new PdfWriter(tempFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // 1. Header Section
            Paragraph title = new Paragraph("GREEN GROCER INVOICE")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            document.add(new Paragraph(" ")); // Spacer

            // 2. Order Metadata
            Paragraph orderInfo = new Paragraph()
                    .add("Order ID: #" + order.getId() + "\n")
                    .add("Date: " + order.getOrderTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n")
                    .add("Status: " + order.getStatus().name());
            document.add(orderInfo);

            document.add(new Paragraph(" "));

            // 3. Items Table
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
                    table.addCell(String.format("%.2f TL", item.getUnitPrice()));
                    table.addCell(String.format("%.2f TL", item.getLineTotal()));
                }

                document.add(table);
            }

            document.add(new Paragraph(" "));

            // 4. Financial Calculations & Coupon Logic
            OrderDao orderDao = new OrderDao();
            double couponDiscount = orderDao.getCouponDiscountForOrder(order.getId());
            Paragraph totals = new Paragraph();

            if (couponDiscount > 0) {
                double originalSubtotal = order.getTotalBeforeTax() + couponDiscount;
                totals.add("Subtotal (Gross): " + String.format("%.2f TL", originalSubtotal) + "\n");
                totals.add("Coupon Discount: -" + String.format("%.2f TL", couponDiscount) + "\n");
            }

            totals.add("Subtotal (Net): " + String.format("%.2f TL", order.getTotalBeforeTax()) + "\n");
            totals.add("VAT (20%): " + String.format("%.2f TL", order.getVat()) + "\n");
            totals.add("GRAND TOTAL: " + String.format("%.2f TL", order.getTotalAfterTax()));
            totals.setBold().setTextAlignment(TextAlignment.RIGHT);
            document.add(totals);

            // 5. QR Code Generation for Verification
            document.add(new Paragraph("\n"));
            String qrData = "OrderID:" + order.getId() + "|Total:" + order.getTotalAfterTax() + "|Verify:GreenGrocerApp";
            BarcodeQRCode qrCode = new BarcodeQRCode(qrData);
            PdfFormXObject qrObject = qrCode.createFormXObject(pdf);
            Image qrImage = new Image(qrObject)
                    .setWidth(80)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            document.add(new Paragraph("Scan to Verify Invoice Validity")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(qrImage);

            // 6. Footer
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Thank you for choosing Green Grocer! This is a computer-generated invoice.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9)
                    .setItalic();
            document.add(footer);
        }

        return tempFile;
    }
}