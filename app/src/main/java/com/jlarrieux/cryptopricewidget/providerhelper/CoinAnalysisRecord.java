package com.jlarrieux.cryptopricewidget.providerhelper;

import com.jlarrieux.cryptopricewidget.record.CryptoPriceRecord;
import com.jlarrieux.cryptopricewidget.record.PercentDifferenceRecord;
import com.jlarrieux.cryptopricewidget.record.PercentDifferencesDailyRecord;

public record CoinAnalysisRecord(CryptoPriceRecord price, PercentDifferenceRecord analysis) {
}
