package Core.incidents;

import Core.domain.Player;

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
