package com.tuplakulma.matching;

public record ScoringWeights(double interest, double novelty, double interactivity, double social) {

  public static ScoringWeights defaults() {
    return new ScoringWeights(0.40, 0.25, 0.20, 0.15);
  }
}
