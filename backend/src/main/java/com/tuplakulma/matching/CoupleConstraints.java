package com.tuplakulma.matching;

import java.util.Set;

public record CoupleConstraints(
    int timeBudgetMinutes, CostTier maxCostTier, Set<TimeOfDay> preferredTimes) {}
