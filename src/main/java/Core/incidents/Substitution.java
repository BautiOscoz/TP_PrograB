package Core.incidents;

import Core.domain.Player;
import Core.domain.Team;
import Core.domain.ParticipationTimeline;
import java.util.List;

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

    @Override
    public Player getPlayerIn() { return playerIn; }

    @Override
    public Player getParticipant() { return playerIn; }

    @Override
    public Player getPlayerOut() { return playerOut; }

    @Override
    public void applyToParticipation(ParticipationTimeline timeline) {
        timeline.enter(playerIn, getMinute());
        timeline.leave(playerOut, getMinute());
    }

    @Override
    public void applyToLineup(Team team, List<Player> playersOnField) {
        if (playerIn != null && playerOut != null && team.getPlayers().contains(playerIn)) {
            playersOnField.remove(playerOut);
            if (!playersOnField.contains(playerIn)) {
                playersOnField.add(playerIn);
            }
        }
    }

    @Override
    public String getDescription() {
        return "SUBSTITUTION - IN: " + playerIn.getName() + " " + playerIn.getLastName()
                + ", OUT: " + playerOut.getName() + " " + playerOut.getLastName();
    }

}
