package org.enthusia.rep.rep;

import java.util.EnumMap;
import java.util.Map;

public record RepAdvancementEvidence(
        boolean positiveReceived,
        int maxOverall,
        int minOverall,
        boolean recoveredFromSevere,
        Map<RepCategory, Integer> maxPositiveCategoryScores
) {
    public static final int SEVERE_NEGATIVE_THRESHOLD = -12;
    public static final RepAdvancementEvidence EMPTY =
            new RepAdvancementEvidence(false, 0, 0, false, Map.of());

    public RepAdvancementEvidence {
        maxPositiveCategoryScores = maxPositiveCategoryScores == null
                ? Map.of() : Map.copyOf(maxPositiveCategoryScores);
    }

    public RepAdvancementEvidence observePositiveReceived() {
        if (positiveReceived) return this;
        return new RepAdvancementEvidence(true, maxOverall, minOverall,
                recoveredFromSevere, maxPositiveCategoryScores);
    }

    public RepAdvancementEvidence observeScore(int oldScore, int newScore) {
        int nextMax = Math.max(maxOverall, Math.max(oldScore, newScore));
        int nextMin = Math.min(minOverall, Math.min(oldScore, newScore));
        boolean recovered = recoveredFromSevere
                || ((minOverall <= SEVERE_NEGATIVE_THRESHOLD
                || oldScore <= SEVERE_NEGATIVE_THRESHOLD)
                && newScore >= 0);
        return new RepAdvancementEvidence(
                positiveReceived, nextMax, nextMin, recovered, maxPositiveCategoryScores);
    }

    public RepAdvancementEvidence observeCategoryScores(Map<RepCategory, Integer> scores) {
        if (scores == null || scores.isEmpty()) return this;
        EnumMap<RepCategory, Integer> next = new EnumMap<>(RepCategory.class);
        next.putAll(maxPositiveCategoryScores);
        scores.forEach((category, value) -> {
            if (category != null && category.migratedCategory().isPositive() && value != null && value > 0) {
                RepCategory normalized = category.migratedCategory();
                next.merge(normalized, value, Math::max);
            }
        });
        return new RepAdvancementEvidence(
                positiveReceived, maxOverall, minOverall, recoveredFromSevere, next);
    }

    public int maxPositiveCategoryScore(RepCategory category) {
        if (category == null) return 0;
        return maxPositiveCategoryScores.getOrDefault(category.migratedCategory(), 0);
    }
}
