package Core.incidents;

import Core.domain.Player;

import java.io.Serial;
import java.io.Serializable;

public class Substitution extends Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Player playerIn;
    private Player playerOut;

    public Substitution(int minute, Player playerIn, Player playerOut) {
        super(minute);
        this.playerIn = playerIn;
        this.playerOut = playerOut;
    }

    public Player getPlayerIn() { return playerIn; }
    public Player getPlayerOut() { return playerOut; }

    @Override
    public String getDescription() {
        return "SUBSTITUTION - IN: " + playerIn.getName() + " " + playerIn.getLastName()
                + ", OUT: " + playerOut.getName() + " " + playerOut.getLastName();
    }

}
