package com.rama.mudstock.model.option;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OptionsInternalAnalyseEntity(Long id,
                                           Long stockId,
                                           String ticker,
                                           String contractType,
                                           String status,
                                           LocalDate expirationDate,
                                           BigDecimal strikeFrom,
                                           BigDecimal strikeTo,
                                           BigDecimal interval,
                                           LocalDateTime createdAt,
                                           LocalDateTime updatedAt) {
}
