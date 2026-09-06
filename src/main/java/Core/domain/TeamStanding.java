package Core.domain;

public class TeamStanding {
    private final Team team;
    private int points;
    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;

    public TeamStanding(Team team) {
        this.team = team;
    }

    public void registerMatch(int scoredGoals, int concededGoals) {
        played++;
        goalsFor += scoredGoals;
        goalsAgainst += concededGoals;

        if (scoredGoals > concededGoals) {
            won++;
            points += 3;
        } else if (scoredGoals == concededGoals) {
            drawn++;
            points++;
        } else {
            lost++;
        }
    }

    public Team getTeam() { return team; }
    public int getPoints() { return points; }
    public int getPlayed() { return played; }
    public int getWon() { return won; }
    public int getDrawn() { return drawn; }
    public int getLost() { return lost; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public int getGoalDifference() { return goalsFor - goalsAgainst; }
}
