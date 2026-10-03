package com.tuplakulma.web;

import java.util.Map;

public record PartnerAnswersRequest(String name, Map<String, Double> interestAffinities) {}
