package com.tuplakulma.matching;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Not a real test — a manual scratchpad for eyeballing how the scoring formula feels. Edit the
 * people/activities below to your own real interests and re-run:
 *
 * <pre>./mvnw test -Dtest=RecommendationPlayground</pre>
 *
 * Deliberately named without a "Test" suffix so it's excluded from the default `./mvnw verify` run
 * picked up by CI.
 */
class RecommendationPlayground {

  @Test
  void printRankedRecommendations() {
    Person partnerA =
        new Person(
            "a",
            "Partner A",
            Map.of(
                "running", 0.9,
                "dancing", 0.15,
                "cooking", 0.6,
                "hiking", 0.7,
                "movies", 0.5),
            Set.of(),
            Set.of("running", "movies")); // things Partner A has already done

    Person partnerB =
        new Person(
            "b",
            "Partner B",
            Map.of(
                "running", 0.8,
                "dancing", 0.3,
                "cooking", 0.8,
                "hiking", 0.4,
                "movies", 0.6),
            Set.of("heights"),
            Set.of("running", "movies", "cooking")); // things Partner B has already done

    Couple couple =
        new Couple(
            partnerA,
            partnerB,
            new CoupleConstraints(
                120, CostTier.MEDIUM, Set.of(TimeOfDay.EVENING, TimeOfDay.MORNING)));

    List<Activity> catalog = SampleCatalog.activities();

    RecommendationEngine engine = new RecommendationEngine();
    List<ScoredActivity> ranked = engine.recommend(couple, catalog, ScoringWeights.defaults());

    System.out.printf(
        "%-28s %10s %10s %10s %10s %10s%n",
        "activity", "confidence", "interest", "novelty", "interactiv", "social");
    for (ScoredActivity scored : ranked) {
      ScoreBreakdown b = scored.breakdown();
      System.out.printf(
          "%-28s %9.0f%% %10.2f %10.2f %10.2f %10.2f%n",
          scored.activity().name(),
          b.confidence() * 100,
          b.interestComponent(),
          b.noveltyComponent(),
          b.interactivityComponent(),
          b.socialComponent());
    }

    long excluded = catalog.size() - ranked.size();
    if (excluded > 0) {
      System.out.println();
      System.out.println(
          excluded
              + " activity(ies) infeasible (didn't fit the time budget, cost tier, time of day, or an"
              + " aversion).");
    }
  }
}
