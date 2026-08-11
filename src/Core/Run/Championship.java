package Core.Run;

import Core.domain.Match;
import Core.domain.Referee;
import Core.domain.Team;
import Core.domain.TournamentZone;
import Core.loader.TournamentData;
import Core.loader.TournamentLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Championship {
    private final List<TournamentZone> tournamentZones;
    private final List<Match> matches;
    private final List<Team> teams;
    private final List<Referee> referees;

    public Championship(String JSON_PATH) throws IOException {
        TournamentData tournament = TournamentLoader.load(JSON_PATH);
        teams = tournament.getTeams();
        referees = tournament.getReferees();
        tournamentZones = getTournamentZones(tournament.getTeams());
        this.matches = new ArrayList<>();
    }

    public static void main (String[] args) throws IOException {

        Championship championship = new Championship("torneo.json");

        for (TournamentZone zone : championship.getTournamentZones()) {
            System.out.println(zone.getName());
            for (Team team : zone.getTeams()) {
                System.out.println(team.getName());
            }
            System.out.println("-------------------------------------");
        }
    }

    public List<TournamentZone> getTournamentZones(List<Team> teams) {
        List<TournamentZone> tournamentZones = new ArrayList<>();

        if (!teams.isEmpty()) {
            List<Team> teamsByRanking = new ArrayList<>();
            teamsByRanking.addAll(teams);
            teamsByRanking.sort(Comparator.comparing(Team::getRanking));

            int name = 65;
            for(int i = 0; i < teamsByRanking.size()/4 ; i++){
                TournamentZone tZone = new TournamentZone("Zone " + (char)name);
                tZone.addTeam(teamsByRanking.get(i));
                tZone.addTeam(teamsByRanking.get(i+4));
                tZone.addTeam(teamsByRanking.get(i+8));
                tZone.addTeam(teamsByRanking.get(i+12));
                tournamentZones.add(tZone);
                name++;
            }
        }

        return tournamentZones;
    }

    public List<TournamentZone> getTournamentZones() {
        return tournamentZones;
    }
}