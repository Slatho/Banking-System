package com.bankingsystem.domain;

public enum Branch {
    FRANCISTOWN("Francistown", "FRT"),
    PALAPYE("Palapye", "PAL"),
    GABORONE("Gaborone", "GAB");

    private final String displayName;
    private final String code;

    Branch(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String displayName() {
        return displayName;
    }

    public String code() {
        return code;
    }

    public static Branch fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Select a valid branch.");
        }
        for (Branch branch : values()) {
            if (branch.displayName.equalsIgnoreCase(value.trim())
                    || branch.code.equalsIgnoreCase(value.trim())
                    || branch.name().equalsIgnoreCase(value.trim())) {
                return branch;
            }
        }
        throw new IllegalArgumentException("Branch must be Francistown, Palapye or Gaborone.");
    }

    @Override
    public String toString() {
        return displayName + " (" + code + ")";
    }
}
