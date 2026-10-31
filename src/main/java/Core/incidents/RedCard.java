package Core.incidents;

import Core.domain.Player;
import Core.domain.Team;
import Core.domain.ParticipationTimeline;
import java.util.List;

import java.io.Serial;
import java.io.Serializable;

public class RedCard extends Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Player penalizedPlayer;

    public RedCard(int minute, Player penalizedPlayer) {
        super(minute);
        this.penalizedPlayer = penalizedPlayer;
    }

    public Player getPenalizedPlayer() { return penalizedPlayer; }

    @Override
    public void applyToParticipation(ParticipationTimeline timeline) {
        timeline.leave(penalizedPlayer, getMinute());
    }

    @Override
    public void applyToLineup(Team team, List<Player> playersOnField) {
        if (team.getPlayers().contains(penalizedPlayer)) {
            playersOnField.remove(penalizedPlayer);
        }
    }

    @Override
    public String getDescription() {
        return "RED CARD - " + penalizedPlayer.getName() + " " + penalizedPlayer.getLastName();
    }

    @Override
    public Player getAffectedPlayer() {
        return penalizedPlayer;
    }

    @Override
    public int getFairPlayPoints() {
        return 3;
    }

    @Override
    public boolean isRedCard() {
        return true;
    }
}
