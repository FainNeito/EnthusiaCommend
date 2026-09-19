package org.enthusia.rep.rep;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepAdvancementEvidenceTest {
    @Test
    void tracksDurableOverallExtremesAndRedemption() {
        var evidence = RepAdvancementEvidence.EMPTY
            .observeScore(0, -12)
            .observeScore(-12, -25)
            .observeScore(-25, -5)
            .observeScore(-5, 0);

        assertEquals(0, evidence.maxOverall());
        assertEquals(-25, evidence.minOverall());
        assertTrue(evidence.recoveredFromSevere());
    }

    @Test
    void tracksPositiveReceiptAndCategoryHighWaterMarks() {
        var evidence = RepAdvancementEvidence.EMPTY
            .observePositiveReceived()
            .observeCategoryScores(Map.of(
                RepCategory.WAS_KIND, 3,
                RepCategory.GAVE_ITEMS, 5,
                RepCategory.GRIEFED, -6))
            .observeCategoryScores(Map.of(
                RepCategory.WAS_KIND, 5,
                RepCategory.GAVE_ITEMS, 2));

        assertTrue(evidence.positiveReceived());
        assertEquals(5, evidence.maxPositiveCategoryScore(RepCategory.WAS_KIND));
        assertEquals(5, evidence.maxPositiveCategoryScore(RepCategory.GAVE_ITEMS));
        assertEquals(0, evidence.maxPositiveCategoryScore(RepCategory.GRIEFED));
    }
}
