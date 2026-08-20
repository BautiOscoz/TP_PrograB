package Core.incidents;

import Core.domain.Player;

public class YellowCard extends Incident{
    private Player playerWarned;
    public YellowCard (int minute, Player playerWarned){
        super(minute);
        this.playerWarned = playerWarned;
    }
    public Player getPlayerWarned (){
        return playerWarned;
    }
}
