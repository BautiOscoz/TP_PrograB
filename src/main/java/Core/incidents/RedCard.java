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
}