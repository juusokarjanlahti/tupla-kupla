package com.tuplakulma.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Filters a catalog down to what's actually feasible for a couple, then scores every survivor —
 * every feasible activity gets ranked with a confidence number, however low, rather than the engine
 * silently dropping weak matches. The couple decides which of the ranked options to do; the score
 * reflects what the research says drives relationship satisfaction (novelty, interactivity, mutual
 * enjoyment), not just interest matching.
 *
 * <p>This is a single-shot recommender: it picks the best first activity from a questionnaire
 * snapshot. It has no notion of what was suggested before, so novelty is derived entirely from each
 * partner's self-reported {@link Person#triedTags()}, not from app usage history.
 */
@Service
public class RecommendationEngine {

  private static final double NEUTRAL_AFFINITY = 0.5;

  private static final Map<Interactivity, Double> INTERACTIVITY_SCORE =
      Map.of(
          Interactivity.ACTIVE_TOGETHER, 1.0,
          Interactivity.PASSIVE_TOGETHER, 0.5,
          Interactivity.PARALLEL, 0.2);

  private static final Map<SocialContext, Double> SOCIAL_SCORE =
      Map.of(
          SocialContext.PARTNER_ONLY, 1.0,
          SocialContext.GROUP_FRIENDLY, 0.6);

  public List<ScoredActivity> recommend(
      Couple couple, List<Activity> catalog, ScoringWeights weights) {
    List<ScoredActivity> results = new ArrayList<>();
    for (Activity activity : catalog) {
      if (!passesHardConstraints(couple, activity)) {
        continue;
      }
      results.add(new ScoredActivity(activity, score(couple, activity, weights)));
    }
    results.sort(
        Comparator.comparingDouble((ScoredActivity r) -> r.breakdown().confidence()).reversed());
    return results;
  }

  public ScoreBreakdown score(Couple couple, Activity activity, ScoringWeights weights) {
    double interest = coupleInterest(couple, activity);
    double novelty = noveltyComponent(couple, activity);
    double interactivity = INTERACTIVITY_SCORE.get(activity.interactivity());
    double social = SOCIAL_SCORE.get(activity.socialContext());
    double confidence =
        weights.interest() * interest
            + weights.novelty() * novelty
            + weights.interactivity() * interactivity
            + weights.social() * social;
    return new ScoreBreakdown(interest, novelty, interactivity, social, confidence);
  }

  private boolean passesHardConstraints(Couple couple, Activity activity) {
    CoupleConstraints constraints = couple.constraints();
    if (activity.durationMinutes() > constraints.timeBudgetMinutes()) {
      return false;
    }
    if (activity.costTier().ordinal() > constraints.maxCostTier().ordinal()) {
      return false;
    }
    if (Collections.disjoint(activity.availableTimes(), constraints.preferredTimes())) {
      return false;
    }
    return !hasAversion(couple.partnerA(), activity) && !hasAversion(couple.partnerB(), activity);
  }

  private boolean hasAversion(Person person, Activity activity) {
    return !Collections.disjoint(person.aversions(), activity.tagRelevance().keySet());
  }

  // Harmonic mean, not average: an activity one partner loves and the other dislikes should
  // score low, because mutual enjoyment predicts relationship quality better than shared time.
  private double coupleInterest(Couple couple, Activity activity) {
    double a = interestScore(couple.partnerA(), activity);
    double b = interestScore(couple.partnerB(), activity);
    return (a + b) == 0 ? 0.0 : (2 * a * b) / (a + b);
  }

  private double interestScore(Person person, Activity activity) {
    double weightedSum = 0.0;
    double weightTotal = 0.0;
    for (Map.Entry<String, Double> tag : activity.tagRelevance().entrySet()) {
      double relevance = tag.getValue();
      double affinity = person.interestAffinities().getOrDefault(tag.getKey(), NEUTRAL_AFFINITY);
      weightedSum += relevance * affinity;
      weightTotal += relevance;
    }
    return weightTotal == 0 ? NEUTRAL_AFFINITY : weightedSum / weightTotal;
  }

  // Novelty isn't a property of the activity — it's relative to what this specific couple has
  // already done. An unlisted tag defaults to "not tried", so novelty is only lost for what the
  // questionnaire actually confirmed, never assumed.
  private double noveltyComponent(Couple couple, Activity activity) {
    double familiarityA = familiarityScore(couple.partnerA(), activity);
    double familiarityB = familiarityScore(couple.partnerB(), activity);
    return 1.0 - (familiarityA + familiarityB) / 2.0;
  }

  private double familiarityScore(Person person, Activity activity) {
    double weightedSum = 0.0;
    double weightTotal = 0.0;
    for (Map.Entry<String, Double> tag : activity.tagRelevance().entrySet()) {
      double relevance = tag.getValue();
      double tried = person.triedTags().contains(tag.getKey()) ? 1.0 : 0.0;
      weightedSum += relevance * tried;
      weightTotal += relevance;
    }
    return weightTotal == 0 ? 0.0 : weightedSum / weightTotal;
  }
}
