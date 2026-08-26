package com.example.chemistry.data;

public record Element(
        String id,
        String symbol,
        String name,
        int atomicNumber,
        ElementCategory category,
        ElementState state,
        boolean stable,
        int color) {

    public String translationKey() {
        return "mchemistry.element." + id;
    }
}
