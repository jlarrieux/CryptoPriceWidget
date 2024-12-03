package com.jlarrieux.cryptopricewidget;

public final class CryptoPriceWidgetUtils {
    private CryptoPriceWidgetUtils() {
    }

    public static String formatPrice(double price) {
        if (price >= 1.0) {
            return String.format("$%,.2f", price);
        } else if (price >= 0.01) {
            return String.format("$%.3f", price);
        } else if (price >=0.0001){
            return String.format("$%.4f", price);
        } else {
            return String.format("$%.6f", price);
        }
    }
}
