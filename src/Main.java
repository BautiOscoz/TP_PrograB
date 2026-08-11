import Core.domain.Team;
import Core.loader.TournamentData;
import Core.loader.TournamentLoader;

public class Main {

    public static void main(String[] args) {
        try {
            String jsonPath = args.length > 0 ? args[0] : "torneo.json";
            TournamentData tournament = TournamentLoader.load(jsonPath);

            System.out.println("Teams loaded: " + tournament.getTeams().size());
            System.out.println("Referees loaded: " + tournament.getReferees().size());
            System.out.println();

            for (Team team : tournament.getTeams()) {
                System.out.println(team.getName()
                        + " (" + team.getCountry() + ")"
                        + " - ranking: " + team.getRanking()
                        + " - players: " + team.getPlayers().size()
                        + " - coach: " + team.getCoach().getLastName()
                        + ", " + team.getCoach().getName());
            }
        } catch (Exception exception) {
            System.err.println("Could not load tournament data: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
