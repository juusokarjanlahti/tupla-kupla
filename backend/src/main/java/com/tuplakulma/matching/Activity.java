package com.tuplakulma.matching;

import java.util.Map;
import java.util.Set;

/**
 * @param tagRelevance tag -&gt; how central that interest is to the activity, in [0.0, 1.0]
 * @param physicalIntensity fixed exertion level in [0.0, 1.0], set by whoever curates the catalog
 *     so activities stay comparable (running &gt; a walk &gt; watching a movie), not guessed per
 *     couple
 */
public record Activity(
    String id,
    String name,
    Map<String, Double> tagRelevance,
    int durationMinutes,
    CostTier costTier,
    Set<TimeOfDay> availableTimes,
    double physicalIntensity,
    Setting setting,
    Interactivity interactivity,
    SocialContext socialContext) {}
