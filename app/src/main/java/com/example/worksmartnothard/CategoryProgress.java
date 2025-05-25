package com.example.worksmartnothard;

public class CategoryProgress {
    public String category;
    public double target;
    public double achieved;
    public int month;
    public int year;

    public CategoryProgress(String category, double target, double achieved, int month, int year) {
        this.category = category;
        this.target = target;
        this.achieved = achieved;
        this.month = month;
        this.year = year;
    }

    public int getPercentage() {
        if (target == 0) return 0;
        return (int) ((achieved * 100.0) / target);
    }
}
