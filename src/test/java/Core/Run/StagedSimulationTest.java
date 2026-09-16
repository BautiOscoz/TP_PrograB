package Core.Run;

import Core.domain.Match;
import Core.enums.TournamentStage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class StagedSimulationTest {
    public static void main(String[] args) throws Exception {
        Championship tournament = new Championship("torneo.json", 2026L);
        check(tournament.getStage() == TournamentStage.GROUP_STAGE, "New tournament stage");
        check(tournament.getMatches().size() == 24, "Only group fixtures at creation");

        for (int date = 1; date <= 3; date++) {
            tournament.simulateNextMatchday();
            check(tournament.getPlayedMatches().size() == date * 8, "One date per call");
            tournament = roundTrip(tournament);
        }
        check(tournament.getStage() == TournamentStage.QUARTER_FINALS, "Quarter-final stage");
        tournament.scheduleQuarterFinals(tournament.getMatches().get(0).getMatchDate().plusDays(9));
        tournament.scheduleQuarterFinals(tournament.getMatches().get(0).getMatchDate().plusDays(9));
        check(tournament.getMatches().size() == 28, "Quarter-finals scheduled only once");
        check(tournament.getPlayedMatches().size() == 24, "Scheduling does not play");

        for (int matchNumber = 1; matchNumber <= 13; matchNumber++) {
            int previouslyPlayed = tournament.getPlayedMatches().size();
            Match played = tournament.simulateNextKnockoutMatch();
            check(played.isPlayed(), "Returned match is played");
            check(tournament.getPlayedMatches().size() == previouslyPlayed + 1,
                    "Exactly one knockout match per call");

            if (matchNumber == 4) {
                check(tournament.getMatches().size() == 32, "Four second legs scheduled");
            } else if (matchNumber == 8) {
                check(tournament.getStage() == TournamentStage.SEMI_FINALS,
                        "Semi-finals follow decided quarters");
                check(tournament.getMatches().size() == 34, "Two semi-final first legs scheduled");
            } else if (matchNumber == 10) {
                check(tournament.getMatches().size() == 36, "Two semi-final second legs scheduled");
            } else if (matchNumber == 12) {
                check(tournament.getStage() == TournamentStage.FINAL, "Final follows decided semis");
                check(tournament.getMatches().size() == 37, "Final scheduled once");
            }
            tournament = roundTrip(tournament);
        }

        check(tournament.getStage() == TournamentStage.FINISHED, "Finished stage persisted");
        check(tournament.getChampion() != null, "Champion persisted");
        check(tournament.getMatches().size() == 37, "37 unique fixtures in a full tournament");
        check(tournament.getPlayedMatches().size() == 37, "All fixtures played");
        boolean refusedReplay = false;
        try {
            tournament.simulateNextKnockoutMatch();
        } catch (IllegalStateException expected) {
            refusedReplay = true;
        }
        check(refusedReplay && tournament.getMatches().size() == 37,
                "Loading a completed tournament cannot replay the final");
        System.out.println("All staged simulation checks passed.");
    }

    private static Championship roundTrip(Championship tournament) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(tournament);
        }
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Championship) input.readObject();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
