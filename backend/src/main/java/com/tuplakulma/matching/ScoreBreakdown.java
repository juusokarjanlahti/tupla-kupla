package com.tuplakulma.matching;

public record ScoreBreakdown(
    double interestComponent,
    double noveltyComponent,
    double interactivityComponent,
    double socialComponent,
    double confidence) {}
