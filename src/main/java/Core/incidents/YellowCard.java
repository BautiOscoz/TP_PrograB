package Core.incidents;

import Core.domain.Player;

import java.io.Serial;
import java.io.Serializable;

public class YellowCard extends Incident implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Player playerWarned;
    public YellowCard (int minute, Player playerWarned){
        super(minute);
        this.playerWarned = playerWarned;
    }
    public Player getPlayerWarned (){
        return playerWarned;
    }
}
