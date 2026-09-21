package com.tuplakulma.matching;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interactive terminal front-end for {@link RecommendationEngine}, so the formula can be tried out
 * by answering questions instead of editing Java. Run with:
 *
 * <pre>./mvnw exec:java</pre>
 */
public final class RecommendationCli {

  private RecommendationCli() {}

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    List<Activity> catalog = SampleCatalog.activities();
    List<String> tags = distinctTags(catalog);

    System.out.println("=== Tupla Kupla activity recommender (CLI playground) ===");
    Person partnerA = readPerson(scanner, "Partner A", tags);
    Person partnerB = readPerson(scanner, "Partner B", tags);
    CoupleConstraints constraints = readConstraints(scanner);
    Couple couple = new Couple(partnerA, partnerB, constraints);

    RecommendationEngine engine = new RecommendationEngine();
    List<ScoredActivity> ranked = engine.recommend(couple, catalog, ScoringWeights.defaults());

    printResults(catalog, ranked);
  }

  private static Person readPerson(Scanner scanner, String label, List<String> tags) {
    System.out.println();
    System.out.println("--- " + label + " ---");
    Map<String, Double> affinities = new LinkedHashMap<>();
    for (String tag : tags) {
      affinities.put(tag, readDouble(scanner, "Interest in " + tag + " (0.0-1.0): "));
    }
    Set<String> tried =
        readTagSet(scanner, "Already tried, comma-separated from " + tags + " (blank for none): ");
    Set<String> aversions =
        readTagSet(scanner, "Hard no's, comma-separated from " + tags + " (blank for none): ");
    return new Person(label, label, affinities, aversions, tried);
  }

  private static CoupleConstraints readConstraints(Scanner scanner) {
    System.out.println();
    System.out.println("--- Constraints ---");
    int timeBudget =
        readInt(scanner, "Time budget in minutes (blank = no limit): ", Integer.MAX_VALUE);
    CostTier costTier =
        readEnum(
            scanner,
            CostTier.class,
            "Max budget tier (FREE/LOW/MEDIUM/HIGH, blank = no limit): ",
            CostTier.HIGH);
    Set<TimeOfDay> times =
        readEnumSet(
            scanner,
            TimeOfDay.class,
            "Preferred times, comma-separated (MORNING/AFTERNOON/EVENING/NIGHT, blank = any time): ",
            Set.of(TimeOfDay.values()));
    return new CoupleConstraints(timeBudget, costTier, times);
  }

  private static void printResults(List<Activity> catalog, List<ScoredActivity> ranked) {
    System.out.println();
    System.out.println("=== Recommendations ===");
    if (ranked.isEmpty()) {
      System.out.println(
          "Nothing cleared the filters. Try a bigger time budget, higher cost tier, or more"
              + " preferred times.");
      return;
    }
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
              + " activity(ies) infeasible (didn't fit the time budget, cost tier, time of day, or"
              + " an aversion).");
    }
  }

  private static double readDouble(Scanner scanner, String prompt) {
    System.out.print(prompt);
    while (true) {
      String line = scanner.nextLine().trim();
      try {
        return Double.parseDouble(line);
      } catch (NumberFormatException e) {
        System.out.print("Enter a number between 0.0 and 1.0: ");
      }
    }
  }

  private static int readInt(Scanner scanner, String prompt, int defaultValue) {
    System.out.print(prompt);
    while (true) {
      String line = scanner.nextLine().trim();
      if (line.isEmpty()) {
        return defaultValue;
      }
      try {
        return Integer.parseInt(line);
      } catch (NumberFormatException e) {
        System.out.print("Enter a whole number: ");
      }
    }
  }

  private static Set<String> readTagSet(Scanner scanner, String prompt) {
    System.out.print(prompt);
    String line = scanner.nextLine().trim();
    if (line.isEmpty()) {
      return Set.of();
    }
    return Arrays.stream(line.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  private static <E extends Enum<E>> E readEnum(
      Scanner scanner, Class<E> type, String prompt, E defaultValue) {
    System.out.print(prompt);
    while (true) {
      String line = scanner.nextLine().trim();
      if (line.isEmpty()) {
        return defaultValue;
      }
      try {
        return Enum.valueOf(type, line.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        System.out.print(
            "Not one of " + Arrays.toString(type.getEnumConstants()) + ", try again: ");
      }
    }
  }

  private static <E extends Enum<E>> Set<E> readEnumSet(
      Scanner scanner, Class<E> type, String prompt, Set<E> defaultValue) {
    System.out.print(prompt);
    while (true) {
      String line = scanner.nextLine().trim();
      if (line.isEmpty()) {
        return defaultValue;
      }
      try {
        return Arrays.stream(line.split(","))
            .map(String::trim)
            .map(s -> s.toUpperCase(Locale.ROOT))
            .map(s -> Enum.valueOf(type, s))
            .collect(Collectors.toCollection(LinkedHashSet::new));
      } catch (IllegalArgumentException e) {
        System.out.print(
            "Not all valid values from "
                + Arrays.toString(type.getEnumConstants())
                + ", try again: ");
      }
    }
  }

  private static List<String> distinctTags(List<Activity> catalog) {
    return catalog.stream()
        .flatMap(a -> a.tagRelevance().keySet().stream())
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }
}
