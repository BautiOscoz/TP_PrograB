package Core.incidents;

import Core.domain.Player;

import java.io.Serial;
import java.io.Serializable;

public class PenaltyShootout extends Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Player player;
    private boolean scored;

    public PenaltyShootout(int minute, Player player, boolean scored) {
        super(minute);
        this.player = player;
        this.scored = scored;
    }

    public Player getPlayer() { return player; }
    public boolean isScored() { return scored; }

    @Override
    public String getDescription() {
        return "SHOOTOUT PENALTY - " + player.getName() + " " + player.getLastName()
                + (scored ? " scored" : " missed");
    }
}
