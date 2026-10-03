package com.tuplakulma.web;

public record RecommendationRequest(
    PartnerAnswersRequest partnerA, PartnerAnswersRequest partnerB) {}
