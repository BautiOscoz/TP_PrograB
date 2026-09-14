package Core.incidents;

import Core.domain.Player;

import java.io.Serial;
import java.io.Serializable;

public class YellowCard extends Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Player playerWarned;

    public YellowCard(int minute, Player playerWarned) {
        super(minute);
        this.playerWarned = playerWarned;
    }

    public Player getPlayerWarned() {
        return playerWarned;
    }

    @Override
    public String getDescription() {
        return "YELLOW CARD - " + playerWarned.getName() + " " + playerWarned.getLastName();
    }

    @Override
    public Player getAffectedPlayer() {
        return playerWarned;
    }

    @Override
    public int getFairPlayPoints() {
        return 1;
    }

    @Override
    public boolean isYellowCard() {
        return true;
    }
}
