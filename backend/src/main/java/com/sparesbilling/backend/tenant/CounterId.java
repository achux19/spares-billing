package com.sparesbilling.backend.tenant;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/** Key of the bill / credit note / quotation counters: one counter per shop per financial year. */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CounterId implements Serializable {
    private Long shopId;
    private String financialYear;

    public CounterId(Long shopId, String financialYear) {
        this.shopId = shopId;
        this.financialYear = financialYear;
    }
}
