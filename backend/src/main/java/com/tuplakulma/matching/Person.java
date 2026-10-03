package com.tuplakulma.matching;

import java.util.Map;
import java.util.Set;

/**
 * @param interestAffinities tag -&gt; affinity in [0.0, 1.0], from the questionnaire
 * @param aversions tags that must never be recommended, regardless of novelty score
 * @param triedTags tags for things this person has already done — an unlisted tag is assumed
 *     untried, so novelty is only lost for what the questionnaire actually confirmed
 */
public record Person(
    String id,
    String name,
    Map<String, Double> interestAffinities,
    Set<String> aversions,
    Set<String> triedTags) {}
