package org.enthusia.rep.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.enthusia.rep.analytics.ReputationChangeAction;
import org.enthusia.rep.analytics.ReputationChangeOutcome;
import org.enthusia.rep.analytics.ReputationChangeSource;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Monotonic evidence only: never pays rewards or changes reputation. */
final class AdvancementEvidenceWriter {
    private AdvancementEvidenceWriter() { }

    static void write(YamlConfiguration output, ConfigurationSection previous, PluginDataSnapshot snapshot) {
        ConfigurationSection evidence = output.createSection("advancementEvidence");
        if (previous != null) {
            previous.getValues(true).forEach((key, value) -> {
                if (!(value instanceof ConfigurationSection)) evidence.set(key, value);
            });
        }
        // Replay only successful authoritative transitions, in their recorded order.
        Map<UUID, Integer> historicalMinimum = new HashMap<>();
        snapshot.reputationChanges().stream()
                .filter(change -> change.outcome() == ReputationChangeOutcome.SUCCEEDED)
                .sorted(Comparator.comparingLong(change -> change.timestamp()))
                .forEach(change -> {
                    ConfigurationSection player = player(evidence, change.targetId());
                    int minimum = Math.min(historicalMinimum.getOrDefault(change.targetId(), 0), change.oldTotal());
                    if (minimum <= -12 && change.newTotal() >= 0) player.set("recoveredFromSevere", true);
                    historicalMinimum.put(change.targetId(), Math.min(minimum, change.newTotal()));
                    extremes(player, change.oldTotal());
                    extremes(player, change.newTotal());
                    if (change.source() == ReputationChangeSource.PLAYER_ACTION
                            && (change.action() == ReputationChangeAction.ADD
                                || change.action() == ReputationChangeAction.UPDATE)
                            && change.amount() > 0 && change.category() != null
                            && change.category().isPositive()) {
                        player.set("positiveReceived", true);
                    }
                });
        snapshot.scores().forEach((id, score) -> total(player(evidence, id), score));
        Map<UUID, Map<String, Integer>> categories = new HashMap<>();
        snapshot.commendations().forEach(commendation -> {
            if (!commendation.isPositive()) return;
            player(evidence, commendation.getTarget()).set("positiveReceived", true);
            categories.computeIfAbsent(commendation.getTarget(), ignored -> new HashMap<>())
                    .merge(commendation.getCategory().name(), commendation.getScoreValue(), Math::addExact);
        });
        categories.forEach((id, totals) -> {
            ConfigurationSection player = player(evidence, id);
            totals.forEach((category, count) -> {
                String key = "categoryMax." + category;
                player.set(key, Math.max(player.getInt(key), count));
            });
        });
    }

    private static ConfigurationSection player(ConfigurationSection evidence, UUID id) {
        String key = id.toString();
        ConfigurationSection player = evidence.getConfigurationSection(key);
        if (player == null) {
            player = evidence.createSection(key);
            player.set("positiveReceived", false);
            player.set("maxOverall", 0);
            player.set("minOverall", 0);
            player.set("recoveredFromSevere", false);
        }
        return player;
    }

    private static void total(ConfigurationSection player, int score) {
        if (player.getInt("minOverall") <= -12 && score >= 0) {
            player.set("recoveredFromSevere", true);
        }
        extremes(player, score);
    }

    private static void extremes(ConfigurationSection player, int score) {
        player.set("maxOverall", Math.max(player.getInt("maxOverall"), score));
        player.set("minOverall", Math.min(player.getInt("minOverall"), score));
    }
}
