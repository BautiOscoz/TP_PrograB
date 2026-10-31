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
        useTestVenues(tournament);
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

            if (tournament.hasPendingPenaltyShootout()) {
                tournament = roundTrip(tournament);
                int count = tournament.getPlayedMatches().size();
                Match pending = tournament.getPendingPenaltyShootout();
                check(tournament.simulateNextKnockoutMatch() == pending, "Second step resolves the pending shootout");
                check(tournament.getPlayedMatches().size() == count, "Shootout does not add a match");
            }
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
        check(tournament.getPlayedMatches().stream().filter(m -> !m.isGroupStage())
                .map(m -> m.getStadium().getId()).distinct().count() == 13, "Every knockout fixture uses a different stadium");
        for (Match match : tournament.getMatches()) {
            check(Championship.isRefereeEligible(match.getReferee(), match.getHomeTeam(), match.getAwayTeam()),
                    "Scheduled referee respects nationality rule");
            int incidents = match.getIncidents().size();
            new Core.report.MatchDetailsFormatter().format(tournament, match);
            check(match.getIncidents().size() == incidents, "Reading reports does not simulate penalties");
        }
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
            Championship restored = (Championship) input.readObject();
            useTestVenues(restored);
            return restored;
        }
    }

    private static void useTestVenues(Championship tournament) {
        var city = new Core.domain.City(1, "Test City", tournament.getCountries().get(0));
        java.util.List<Core.domain.Stadium> stadiums = new java.util.ArrayList<>();
        for (int i = 1; i <= 13; i++) stadiums.add(new Core.domain.Stadium(i, "Stadium " + i, city));
        tournament.setVenueLoader(countries -> new Core.loader.VenueLoader.Venues(java.util.List.of(city), stadiums));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
