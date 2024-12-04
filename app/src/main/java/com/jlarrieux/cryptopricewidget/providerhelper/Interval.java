package com.jlarrieux.cryptopricewidget.providerhelper;

public enum Interval {
    Daily("daily"),
    Hourly("hourly");

    private final String value;

    Interval(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
