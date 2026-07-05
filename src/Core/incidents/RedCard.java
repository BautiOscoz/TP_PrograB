package Core.incidents;

import Core.domain.Player;

public class RedCard extends Incident {
    private Player penalizedPlayer;

    public RedCard(int minute, Player penalizedPlayer) {
        super(minute);
        this.penalizedPlayer = penalizedPlayer;
    }

    public Player getPenalizedPlayer() { return penalizedPlayer; }
}