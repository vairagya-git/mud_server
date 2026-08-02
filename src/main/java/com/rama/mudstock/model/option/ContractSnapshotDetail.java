package com.rama.mudstock.model.option;

import java.math.BigDecimal;

public record ContractSnapshotDetail(Long optionContractId,
                                     String contractTicker,
                                     Integer optVolume,
                                     BigDecimal optOpen,
                                     BigDecimal optClose,
                                     BigDecimal optHigh,
                                     BigDecimal optLow,
                                     Long nearOptionSnapshotId,
                                     BigDecimal bid,
                                     BigDecimal ask,
                                     BigDecimal midpoint,
                                     BigDecimal delta,
                                     BigDecimal gamma,
                                     BigDecimal theta,
                                     BigDecimal vega) {
}
