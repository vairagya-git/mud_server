package com.rama.mudstock.model.option;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

public record OptionTrackingSnapshotRow(Long unixTime,
                                        Timestamp localTime,
                                        BigDecimal stockOpen,
                                        BigDecimal stockClose,
                                        Map<Long, ContractSnapshotDetail> contractDetails) {
}
