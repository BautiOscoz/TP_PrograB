package Core.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Temporary calculation of participation; does not modify players or recorded lineups. */
public final class ParticipationTimeline {
    private final Map<Player, Integer> entries = new HashMap<>();
    private final Map<Player, Integer> exits = new HashMap<>();

    public ParticipationTimeline(List<Player> home, List<Player> away) {
        home.forEach(player -> enter(player, 0));
        away.forEach(player -> enter(player, 0));
    }

    public void enter(Player player, int minute) {
        if (player != null && minute >= 0 && minute <= 90) entries.putIfAbsent(player, minute);
    }

    public void leave(Player player, int minute) {
        if (player != null && minute >= 0 && minute <= 90 && entries.containsKey(player)) {
            exits.putIfAbsent(player, minute);
        }
    }

    /** Includes players entering at minute 90, with zero minutes played. */
    public Map<Player, Integer> minutesPlayed() {
        Map<Player, Integer> minutes = new HashMap<>();
        entries.forEach((player, entry) -> minutes.put(player,
                Math.max(0, exits.getOrDefault(player, 90) - entry)));
        return Map.copyOf(minutes);
    }
}
