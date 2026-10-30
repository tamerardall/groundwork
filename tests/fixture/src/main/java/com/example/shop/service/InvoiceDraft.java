package com.example.shop.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public record InvoiceDraft(Long orderId, DecimalFormat amountFormat) {

    public static InvoiceDraft forOrder(Long orderId) {
        DecimalFormat format = new DecimalFormat("#,##0.00 EUR", DecimalFormatSymbols.getInstance(Locale.GERMANY));
        return new InvoiceDraft(orderId, format);
    }

    public String render(BigDecimal total) {
        return "Invoice #" + orderId + ": " + amountFormat.format(total);
    }
}
