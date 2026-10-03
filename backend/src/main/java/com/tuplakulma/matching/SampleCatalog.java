package com.tuplakulma.matching;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hardcoded activities for local exploration, until there's a real catalog behind a database.
 * Picked to track the "exciting vs. mundane" activity examples used in the self-expansion field
 * studies (skiing, dancing, hiking, concerts, learning a new skill/cuisine vs. rewatching a
 * favorite movie), not invented from scratch. {@code physicalIntensity} is set once here, by the
 * catalog curator, so it stays a comparable, fixed scale across activities (running &gt; a walk
 * &gt; watching a movie) instead of being guessed per recommendation.
 */
public final class SampleCatalog {

  private SampleCatalog() {}

  public static List<Activity> activities() {
    return List.of(
        new Activity(
            "easy-run",
            "Easy run together",
            Map.of("running", 1.0),
            60,
            CostTier.FREE,
            Set.of(TimeOfDay.MORNING, TimeOfDay.EVENING),
            0.7,
            Setting.OUTDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "dance-class",
            "Beginner dance class",
            Map.of("dancing", 1.0),
            90,
            CostTier.MEDIUM,
            Set.of(TimeOfDay.EVENING),
            0.6,
            Setting.INDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.GROUP_FRIENDLY),
        new Activity(
            "movie-night",
            "Movie night at home",
            Map.of("movies", 1.0),
            160,
            CostTier.FREE,
            Set.of(TimeOfDay.EVENING),
            0.05,
            Setting.INDOOR,
            Interactivity.PASSIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "cooking-class",
            "Cook a new cuisine together",
            Map.of("cooking", 0.8, "dancing", 0.1),
            90,
            CostTier.MEDIUM,
            Set.of(TimeOfDay.EVENING),
            0.2,
            Setting.INDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "sunset-hike",
            "Sunset hike",
            Map.of("hiking", 1.0),
            90,
            CostTier.FREE,
            Set.of(TimeOfDay.EVENING),
            0.6,
            Setting.OUTDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "rock-climbing-taster",
            "Indoor rock climbing taster session",
            Map.of("heights", 0.8, "hiking", 0.2),
            90,
            CostTier.MEDIUM,
            Set.of(TimeOfDay.EVENING),
            0.85,
            Setting.INDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.GROUP_FRIENDLY),
        new Activity(
            "ski-day-trip",
            "Day trip skiing",
            Map.of("skiing", 1.0),
            240,
            CostTier.HIGH,
            Set.of(TimeOfDay.MORNING),
            0.8,
            Setting.OUTDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "live-concert",
            "Live concert",
            Map.of("concerts", 1.0, "dancing", 0.2),
            180,
            CostTier.HIGH,
            Set.of(TimeOfDay.EVENING, TimeOfDay.NIGHT),
            0.3,
            Setting.INDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.GROUP_FRIENDLY),
        new Activity(
            "museum-visit",
            "Museum visit",
            Map.of("museum", 1.0),
            90,
            CostTier.LOW,
            Set.of(TimeOfDay.MORNING, TimeOfDay.AFTERNOON),
            0.15,
            Setting.INDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "pottery-workshop",
            "Pottery workshop",
            Map.of("newskill", 1.0),
            120,
            CostTier.MEDIUM,
            Set.of(TimeOfDay.AFTERNOON, TimeOfDay.EVENING),
            0.25,
            Setting.INDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.GROUP_FRIENDLY),
        new Activity(
            "photography-walk",
            "City photography walk",
            Map.of("photography", 1.0),
            90,
            CostTier.FREE,
            Set.of(TimeOfDay.MORNING, TimeOfDay.AFTERNOON),
            0.35,
            Setting.OUTDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "park-picnic",
            "Park picnic",
            Map.of("picnic", 1.0),
            90,
            CostTier.LOW,
            Set.of(TimeOfDay.AFTERNOON),
            0.1,
            Setting.OUTDOOR,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "routine-takeout-night",
            "Regular takeout night at home",
            Map.of("routine", 1.0),
            60,
            CostTier.LOW,
            Set.of(TimeOfDay.EVENING, TimeOfDay.NIGHT),
            0.05,
            Setting.INDOOR,
            Interactivity.PASSIVE_TOGETHER,
            SocialContext.PARTNER_ONLY),
        new Activity(
            "volunteering-together",
            "Volunteer together",
            Map.of("volunteering", 1.0),
            180,
            CostTier.FREE,
            Set.of(TimeOfDay.MORNING, TimeOfDay.AFTERNOON),
            0.4,
            Setting.EITHER,
            Interactivity.ACTIVE_TOGETHER,
            SocialContext.GROUP_FRIENDLY));
  }
}
