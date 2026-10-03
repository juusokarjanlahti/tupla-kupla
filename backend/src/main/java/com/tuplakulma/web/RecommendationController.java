package com.tuplakulma.web;

import com.tuplakulma.matching.CostTier;
import com.tuplakulma.matching.Couple;
import com.tuplakulma.matching.CoupleConstraints;
import com.tuplakulma.matching.Person;
import com.tuplakulma.matching.RecommendationEngine;
import com.tuplakulma.matching.SampleCatalog;
import com.tuplakulma.matching.ScoreBreakdown;
import com.tuplakulma.matching.ScoredActivity;
import com.tuplakulma.matching.ScoringWeights;
import com.tuplakulma.matching.TimeOfDay;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The frontend only collects interest ratings for now, so constraints are fixed wide open here
 * (unlimited time/budget, any time of day) rather than exposed as UI fields yet — the same "blank =
 * no restriction" defaults the CLI uses when those prompts are skipped.
 */
@RestController
@RequestMapping("/api")
public class RecommendationController {

  private final RecommendationEngine engine;

  public RecommendationController(RecommendationEngine engine) {
    this.engine = engine;
  }

  @GetMapping("/activities/tags")
  public List<String> activityTags() {
    return SampleCatalog.activities().stream()
        .flatMap(activity -> activity.tagRelevance().keySet().stream())
        .distinct()
        .sorted()
        .toList();
  }

  @PostMapping("/recommendations")
  public List<ActivityRecommendationResponse> recommendations(
      @RequestBody RecommendationRequest request) {
    Couple couple =
        new Couple(toPerson(request.partnerA()), toPerson(request.partnerB()), openConstraints());
    List<ScoredActivity> ranked =
        engine.recommend(couple, SampleCatalog.activities(), ScoringWeights.defaults());
    return ranked.stream().map(RecommendationController::toResponse).toList();
  }

  private static Person toPerson(PartnerAnswersRequest answers) {
    return new Person(
        answers.name(), answers.name(), answers.interestAffinities(), Set.of(), Set.of());
  }

  private static CoupleConstraints openConstraints() {
    return new CoupleConstraints(Integer.MAX_VALUE, CostTier.HIGH, Set.of(TimeOfDay.values()));
  }

  private static ActivityRecommendationResponse toResponse(ScoredActivity scored) {
    ScoreBreakdown breakdown = scored.breakdown();
    return new ActivityRecommendationResponse(
        scored.activity().id(),
        scored.activity().name(),
        breakdown.confidence(),
        breakdown.interestComponent(),
        breakdown.noveltyComponent(),
        breakdown.interactivityComponent(),
        breakdown.socialComponent());
  }
}
