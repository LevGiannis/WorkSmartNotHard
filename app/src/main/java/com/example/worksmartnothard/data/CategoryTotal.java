package com.example.worksmartnothard.data;

/**
 * Projection type for aggregation queries (e.g., SUM per category).
 */
public class CategoryTotal {
    public String category;
    public double total;

    public CategoryTotal(String category, double total) {
        this.category = category;
        this.total = total;
    }
}
