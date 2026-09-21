package com.tuplakulma.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationEngineTest {

  private final RecommendationEngine engine = new RecommendationEngine();

  private static final Person ALEX =
      new Person(
          "alex",
          "Alex",
          Map.of("running", 0.9, "dancing", 0.1, "movies", 0.1),
          Set.of(),
          Set.of("running", "movies"));

  private static final Person SAM =
      new Person(
          "sam",
          "Sam",
          Map.of("running", 0.85, "dancing", 0.15, "movies", 0.1),
          Set.of(),
          Set.of("running", "movies"));

  private static final Couple COUPLE =
      new Couple(
          ALEX,
          SAM,
          new CoupleConstraints(90, CostTier.MEDIUM, Set.of(TimeOfDay.MORNING, TimeOfDay.EVENING)));

  private static final Activity EASY_RUN =
      new Activity(
          "easy-run",
          "Easy run together",
          Map.of("running", 1.0),
          45,
          CostTier.FREE,
          Set.of(TimeOfDay.MORNING, TimeOfDay.EVENING),
          0.7,
          Setting.OUTDOOR,
          Interactivity.ACTIVE_TOGETHER,
          SocialContext.PARTNER_ONLY);

  private static final Activity DANCE_CLASS =
      new Activity(
          "dance-class",
          "Beginner dance class",
          Map.of("dancing", 1.0),
          60,
          CostTier.MEDIUM,
          Set.of(TimeOfDay.EVENING),
          0.6,
          Setting.INDOOR,
          Interactivity.ACTIVE_TOGETHER,
          SocialContext.GROUP_FRIENDLY);

  private static final Activity MOVIE_NIGHT =
      new Activity(
          "movie-night",
          "Movie night at home",
          Map.of("movies", 1.0),
          120,
          CostTier.FREE,
          Set.of(TimeOfDay.EVENING),
          0.05,
          Setting.INDOOR,
          Interactivity.PASSIVE_TOGETHER,
          SocialContext.PARTNER_ONLY);

  private static final Activity RUNNING_RETREAT_WEEKEND =
      new Activity(
          "running-retreat",
          "Weekend running retreat",
          Map.of("running", 1.0),
          600,
          CostTier.MEDIUM,
          Set.of(TimeOfDay.MORNING),
          0.7,
          Setting.OUTDOOR,
          Interactivity.ACTIVE_TOGETHER,
          SocialContext.PARTNER_ONLY);

  @Test
  void highSharedInterestFamiliarActivityStillScoresWell() {
    ScoreBreakdown breakdown = engine.score(COUPLE, EASY_RUN, ScoringWeights.defaults());

    assertEquals(0.874, breakdown.interestComponent(), 0.01);
    assertEquals(0.0, breakdown.noveltyComponent(), 0.001);
    assertEquals(0.70, breakdown.confidence(), 0.01);
  }

  @Test
  void lowInterestNovelActivityStillScoresRespectably() {
    ScoreBreakdown breakdown = engine.score(COUPLE, DANCE_CLASS, ScoringWeights.defaults());

    assertEquals(0.12, breakdown.interestComponent(), 0.01);
    assertEquals(1.0, breakdown.noveltyComponent(), 0.001);
    assertEquals(0.588, breakdown.confidence(), 0.01);
  }

  @Test
  void activityTriedByBothPartnersLosesNoveltyCredit() {
    Person alexTriedDancing =
        new Person(
            ALEX.id(), ALEX.name(), ALEX.interestAffinities(), ALEX.aversions(), Set.of("dancing"));
    Person samTriedDancing =
        new Person(
            SAM.id(), SAM.name(), SAM.interestAffinities(), SAM.aversions(), Set.of("dancing"));
    Couple coupleWhoTriedDancing =
        new Couple(alexTriedDancing, samTriedDancing, COUPLE.constraints());

    ScoreBreakdown neverTried = engine.score(COUPLE, DANCE_CLASS, ScoringWeights.defaults());
    ScoreBreakdown alreadyTried =
        engine.score(coupleWhoTriedDancing, DANCE_CLASS, ScoringWeights.defaults());

    assertTrue(alreadyTried.noveltyComponent() < neverTried.noveltyComponent());
    assertTrue(alreadyTried.confidence() < neverTried.confidence());
  }

  @Test
  void feasibleActivitiesAreAllRankedRegardlessOfHowWeakTheMatchIs() {
    // MOVIE_NIGHT runs 120 minutes, so give this couple enough time budget for both options.
    Couple coupleWithMoreTime =
        new Couple(
            ALEX,
            SAM,
            new CoupleConstraints(
                120, CostTier.MEDIUM, Set.of(TimeOfDay.MORNING, TimeOfDay.EVENING)));

    List<ScoredActivity> results =
        engine.recommend(
            coupleWithMoreTime, List.of(MOVIE_NIGHT, DANCE_CLASS), ScoringWeights.defaults());

    // Nothing gets silently dropped for scoring low — the couple sees every feasible option.
    assertEquals(2, results.size());
    // Movie night is familiar to both (low novelty) and passive (low interactivity), so it
    // should rank behind the novel, active dance class despite similarly low stated interest.
    assertEquals("dance-class", results.get(0).activity().id());
    assertEquals("movie-night", results.get(1).activity().id());
  }

  @Test
  void activityExceedingTimeBudgetIsFilteredOut() {
    List<ScoredActivity> results =
        engine.recommend(COUPLE, List.of(RUNNING_RETREAT_WEEKEND), ScoringWeights.defaults());

    assertTrue(results.isEmpty());
  }

  @Test
  void activityWithAversionTagIsAlwaysFilteredOut() {
    Person samWithAversion =
        new Person("sam", "Sam", SAM.interestAffinities(), Set.of("running"), SAM.triedTags());
    Couple couple = new Couple(ALEX, samWithAversion, COUPLE.constraints());

    List<ScoredActivity> results =
        engine.recommend(couple, List.of(EASY_RUN), ScoringWeights.defaults());

    assertTrue(results.isEmpty());
  }
}
