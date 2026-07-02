package com.barterplatform.domain.catalog.enums;

public enum CategorySchemaFieldType {
    TEXT,
    NUMBER,
    BOOLEAN,
    SINGLE_SELECT,
    MULTI_SELECT,
    DATE;

    public boolean supportsOptions() {
        return this == SINGLE_SELECT || this == MULTI_SELECT;
    }
}

