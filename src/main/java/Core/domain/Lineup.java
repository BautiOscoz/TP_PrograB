package Core.domain;

import Core.enums.FormationType;
import Core.enums.Position;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Lineup implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Team team;
    private final List<Player> players;
    private final FormationType formation;

    public Lineup(Team team,FormationType formation) {
        this.team = team;
        this.players = selectBestLineup();
        this.formation = formation;
    }
    public List<Player> selectBestLineup() {
        List<Player> available = team.getPlayers().stream()
                .filter(player -> !player.serveSuspensionIfNeeded())
                .sorted(Comparator.comparingDouble(Player::getAverageRating).reversed())
                .toList();

        List<Player> lineupList = new ArrayList<>();
        addByPosition(available, lineupList, Position.GOALKEEPER, 1);
        addByPosition(available, lineupList, Position.DEFENDER, formation.getDefenders());
        addByPosition(available, lineupList, Position.MIDFIELDER, formation.getMidfielders());
        addByPosition(available, lineupList, Position.FORWARD, formation.getForwards());

        if (lineupList.size() != 11) {
            throw new IllegalStateException(team.getName() + " does not have enough available players for a " + formation.name() + " lineup.");
        }
        return lineupList;
    }
    private void addByPosition(List<Player> available, List<Player> lineup, Position position, int amount) {
        available.stream()
                .filter(player -> player.getPosition() == position)
                .limit(amount)
                .forEach(lineup::add);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getRandomPlayer(Random random) {
        return players.get(random.nextInt(players.size()));
    }

    public Player getGoalkeeper() {
        return players.stream()
                .filter(p -> p.getPosition() == Position.GOALKEEPER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Lineup has no goalkeeper."));
    }

    public List<Player> getOutfieldPlayers() {
        return players.stream()
                .filter(p -> p.getPosition() != Position.GOALKEEPER)
                .toList();
    }
}
