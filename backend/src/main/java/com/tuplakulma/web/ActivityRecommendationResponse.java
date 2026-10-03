package com.tuplakulma.web;

public record ActivityRecommendationResponse(
    String id,
    String name,
    double confidence,
    double interestComponent,
    double noveltyComponent,
    double interactivityComponent,
    double socialComponent) {}
