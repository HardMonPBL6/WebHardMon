package com.telemetria.web.model;

public record HourlyAggregateDTO(
        String slot,
        double cpuAvg, double cpuMin, double cpuMax,
        double ramAvg, double ramMin, double ramMax,
        double stoAvg, double stoMin, double stoMax,
        double batAvg, double batMin, double batMax,
        double tmpAvg, double tmpMin, double tmpMax,
        double strAvg, double strMin, double strMax,
        long count
) {}
