package com.sparesbilling.backend.feature;

/** Modules the Super Admin can switch on or off for each shop. The names are stored in the database. */
public enum Feature {
    PURCHASES("Purchases and suppliers"),
    QUOTATIONS("Quotations"),
    CREDIT_NOTES("Returns (credit notes)"),
    EWAY("E-way bill helper"),
    REPORTS("Reports");

    private final String label;

    Feature(String label) { this.label = label; }

    public String label() { return label; }
}
