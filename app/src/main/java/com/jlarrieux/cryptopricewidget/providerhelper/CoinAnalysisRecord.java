package com.jlarrieux.cryptopricewidget.providerhelper;

import com.jlarrieux.cryptopricewidget.record.CryptoPriceRecord;
import com.jlarrieux.cryptopricewidget.record.PercentDifferencesRecord;

public record CoinAnalysisRecord(CryptoPriceRecord price, PercentDifferencesRecord analysis) {
}
