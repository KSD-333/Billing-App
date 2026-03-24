package com.example.billingapp.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.example.billingapp.models.Bill;
import com.example.billingapp.models.CartItem;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfUtils {

    public static File createPdf(Context context, Bill bill) {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size approx
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Load settings
        SharedPreferences prefs = context.getSharedPreferences("StorePrefs", Context.MODE_PRIVATE);
        String storeName = prefs.getString("store_name", "My Store");
        String storeMobile = prefs.getString("store_mobile", "1234567890");
        String storeGst = prefs.getString("store_gst", "GSTIN4754545448544855");
        String storeAddress = prefs.getString("store_address", "A/p Burli Dist:Sangli");

        int x = 40, y = 50;

        // Store Details
        paint.setColor(Color.BLACK);
        paint.setTextSize(24);
        paint.setFakeBoldText(true);
        canvas.drawText(storeName, x, y, paint);
        y += 30;

        paint.setTextSize(14);
        paint.setFakeBoldText(false);
        if (!storeAddress.isEmpty()) {
            for (String line : storeAddress.split("\n")) {
                canvas.drawText(line, x, y, paint);
                y += 20;
            }
        }

        String contactInfo = "Mobile: " + storeMobile + (storeGst.isEmpty() ? "" : " | GST: " + storeGst);
        canvas.drawText(contactInfo, x, y, paint);
        y += 30;

        // Line Separator
        paint.setStrokeWidth(1);
        canvas.drawLine(x, y, 555, y, paint);
        y += 30;

        // Customer Details Section
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("Customer Details:", x, y, paint);

        // Date aligned to right on same header line
        paint.setFakeBoldText(false);
        String dateStr = "Date: "
                + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(bill.getTimestamp()));
        canvas.drawText(dateStr, 350, y, paint);
        y += 25;

        // Name
        canvas.drawText("Name: " + bill.getCustomerName(), x, y, paint);
        y += 20;

        // Mobile
        canvas.drawText("Mobile: " + bill.getCustomerNumber(), x, y, paint);
        y += 30; // Extra spacing before table header

        // Table Header
        paint.setFakeBoldText(true);
        canvas.drawText("Item Name", x, y, paint);
        canvas.drawText("Qty", 300, y, paint);
        canvas.drawText("Price", 380, y, paint);
        canvas.drawText("Total", 480, y, paint);
        y += 10;
        canvas.drawLine(x, y, 555, y, paint);
        y += 25;

        paint.setFakeBoldText(false);

        // Items
        if (bill.getItems() != null) {
            for (CartItem item : bill.getItems()) {
                canvas.drawText(item.getName(), x, y, paint);
                canvas.drawText(item.getQuantity() + " " + item.getUnitType(), 300, y, paint);
                canvas.drawText(String.format("₹%.2f", item.getPricePerUnit()), 380, y, paint);
                canvas.drawText(String.format("₹%.2f", item.getTotalPrice()), 480, y, paint);
                y += 20;
            }
        }

        y += 10;
        canvas.drawLine(x, y, 555, y, paint);
        y += 30;

        // Totals
        int valueX = 480;
        int labelX = 380;

        canvas.drawText("Subtotal:", labelX, y, paint);
        canvas.drawText(String.format("₹%.2f", bill.getTotalAmount()), valueX, y, paint);
        y += 20;

        canvas.drawText("Discount:", labelX, y, paint);
        canvas.drawText(String.format("- ₹%.2f", bill.getDiscount()), valueX, y, paint);
        y += 20;

        paint.setFakeBoldText(true);
        paint.setTextSize(16);
        canvas.drawText("Final Payable:", labelX - 20, y, paint);
        canvas.drawText(String.format("₹%.2f", bill.getFinalAmount()), valueX, y, paint);

        // Status
        y += 40;
        paint.setTextSize(14);
        paint.setColor(bill.getStatus().equalsIgnoreCase("Pending") ? Color.RED : Color.parseColor("#006400")); // Dark
                                                                                                                // Green
        canvas.drawText("Status: " + bill.getStatus(), x, y, paint);

        doc.finishPage(page);

        File file = new File(context.getExternalCacheDir(),
                "Bill_" + bill.getCustomerName().replaceAll("\\s+", "_") + "_" + System.currentTimeMillis() + ".pdf");
        try {
            doc.writeTo(new FileOutputStream(file));
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            doc.close();
        }
    }

    public static void sharePdfToWhatsApp(Context context, File file, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(context, "Customer phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedNumber = phoneNumber.replace("+", "").replace(" ", "").trim();
        if (formattedNumber.length() == 10) {
            formattedNumber = "91" + formattedNumber;
        }

        Uri uri = FileProvider.getUriForFile(context, "com.example.billingapp.fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("jid", formattedNumber + "@s.whatsapp.net");
        intent.setPackage("com.whatsapp");

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "WhatsApp not installed or error", Toast.LENGTH_SHORT).show();
        }
    }
}
