package Core.incidents;

import Core.domain.Player;

public class Substitution extends Incident {
    private Player playerIn;
    private Player playerOut;

    public Substitution(int minute, Player playerIn, Player playerOut) {
        super(minute);
        this.playerIn = playerIn;
        this.playerOut = playerOut;
    }

    public Player getPlayerIn() { return playerIn; }
    public Player getPlayerOut() { return playerOut; }
}
