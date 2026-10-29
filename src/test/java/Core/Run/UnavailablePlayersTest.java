package Core.Run;

import Core.domain.*;
import Core.enums.*;
import Core.report.*;
import Core.simulation.MatchSimulator;
import java.io.*;
import java.util.*;

/** Executable regression checks; no database or saved tournament is modified. */
public class UnavailablePlayersTest {
    public static void main(String[] args) throws Exception {
        forfeitsAndReports(true);
        forfeitsAndReports(false);
        lineupIsReadOnly();
        postponedFixture();
        newRedCardsRemainSuspended();
        tiedAdministrativeSeries();
        permanentSquadProblem();
        if (Arrays.asList(args).contains("--database")) knockoutProgression();
        System.out.println("Unavailable player checks passed.");
    }

    private static Championship tournament() throws Exception {
        return new Championship("torneo.json", 2026L);
    }

    private static void suspendKeepers(Team team) {
        team.getPlayers().stream().filter(p -> p.getPosition() == Position.GOALKEEPER)
                .forEach(p -> p.setSuspended(true));
    }

    private static void forfeitsAndReports(boolean home) throws Exception {
        Championship c = tournament();
        Match m = c.getMatches().get(0);
        suspendKeepers(home ? m.getHomeTeam() : m.getAwayTeam());
        new MatchSimulator(new Random(4)).simulate(m);
        check(m.isPlayed() && m.isForfeit(), "Administrative fixture resolved");
        check(m.getHomeGoals() == (home ? 0 : 3) && m.getAwayGoals() == (home ? 3 : 0), "Correct 0-3 result");
        check(m.getHomeLineup() == null && m.getIncidents().isEmpty(), "No fictitious lineup or goals");
        check(m.getHomeTeam().getPlayers().stream().noneMatch(Player::isSuspended)
                && m.getAwayTeam().getPlayers().stream().noneMatch(Player::isSuspended), "Previous bans served");
        Team winner = home ? m.getAwayTeam() : m.getHomeTeam();
        TeamStanding standing = c.getStandings(c.getTournamentZones().get(0)).stream()
                .filter(s -> s.getTeam() == winner).findFirst().orElseThrow();
        check(standing.getPoints() == 3 && standing.getGoalsFor() == 3, "Standings count administrative goals");
        TournamentReportService reports = new TournamentReportService();
        check(reports.getTopScorers(c).isEmpty(), "No scorer credited");
        check(reports.getParticipation(c).stream().allMatch(p -> p.getMatches() == 0 && p.getMinutes() == 0), "No participation");
        check(reports.getRefereeStats(c).stream().allMatch(r -> r.getMatches() == 0), "No referee appearance");
        check(new MatchDetailsFormatter().format(c, m).contains("NOT DISPUTED"), "Reason displayed");
        Match restored = roundTrip(m);
        check(restored.isForfeit() && restored.getAdministrativeReason().equals(m.getAdministrativeReason()), "Forfeit survives save/load");
        boolean refused = false;
        try { new MatchSimulator(new Random()).simulate(m); } catch (IllegalStateException e) { refused = true; }
        check(refused, "Cannot simulate resolved fixture twice");
    }

    private static void lineupIsReadOnly() throws Exception {
        Team team = tournament().getTeams().get(0);
        Player banned = team.getPlayers().stream().filter(p -> p.getPosition() == Position.MIDFIELDER).findFirst().orElseThrow();
        banned.setSuspended(true);
        FormationType formation = Arrays.stream(FormationType.values()).filter(f -> Lineup.canUseFormation(team, f)).findFirst().orElseThrow();
        Lineup lineup = new Lineup(team, formation);
        check(banned.isSuspended() && !lineup.getPlayers().contains(banned), "Lineup construction must not serve bans");
    }

    private static void postponedFixture() throws Exception {
        Championship c = tournament();
        Match m = c.getMatches().get(0);
        var date = m.getMatchDate();
        Match later = c.getMatches().stream().filter(x -> x.getMatchday() == 2).findFirst().orElseThrow();
        var laterDate = later.getMatchDate();
        suspendKeepers(m.getHomeTeam()); suspendKeepers(m.getAwayTeam());
        c.simulateNextMatchday();
        check(!m.isPlayed() && m.getPostponements() == 1 && m.getMatchDate().equals(date.plusDays(1)), "Both unavailable: postpone");
        check(c.getCurrentMatchday() == 1 && c.getPlayedMatches().size() == 7, "Do not advance with pending fixture");
        check(later.getMatchDate().equals(laterDate.plusDays(1)), "Later dates shifted");
        Championship restored = roundTrip(c);
        check(restored.getMatches().get(0).getPostponements() == 1, "Postponement survives save/load");
        restored.simulateNextMatchday();
        check(restored.getMatches().get(0).isPlayed() && !restored.getMatches().get(0).isForfeit(), "Replay with bans served");
        check(restored.getPlayedMatches().size() == 8 && restored.getCurrentMatchday() == 2, "Other fixtures not replayed");
    }

    private static void newRedCardsRemainSuspended() throws Exception {
        Championship c = tournament();
        Match m = c.getMatches().get(0);
        Player oldBan = m.getHomeTeam().getPlayers().stream().filter(p -> p.getPosition() == Position.MIDFIELDER).findFirst().orElseThrow();
        oldBan.setSuspended(true);
        Random forceRed = new Random() {
            @Override public double nextDouble() { return 0; }
            @Override public double nextGaussian() { return 0; }
            @Override public int nextInt(int bound) { return 0; }
        };
        new MatchSimulator(forceRed).simulate(m);
        check(!oldBan.isSuspended(), "Old suspension served");
        check(!m.getHomeLineup().getPlayers().contains(oldBan)
                && m.getIncidents().stream().noneMatch(i -> i.getPlayerIn() == oldBan), "Old ban excludes bench too");
        check(m.getIncidents().stream().filter(i -> i.isRedCard()).count() == 2, "Forced red cards");
        check(m.getIncidents().stream().filter(i -> i.isRedCard()).allMatch(i -> i.getAffectedPlayer().isSuspended()), "New bans survive match end");
    }

    private static void tiedAdministrativeSeries() throws Exception {
        Championship c = tournament();
        Match source = c.getMatches().get(0);
        FirstLegMatch first = new FirstLegMatch(source.getMatchDate(), source.getAwayTeam(), source.getHomeTeam(), source.getReferee());
        first.setHomeGoals(0); first.setAwayGoals(3); first.setPlayed(true);
        SecondLegMatch second = new SecondLegMatch(source.getMatchDate().plusDays(7), source.getHomeTeam(), source.getAwayTeam(), source.getReferee(), 0, 3);
        suspendKeepers(second.getHomeTeam());
        new MatchSimulator(new Random(5)).simulate(second);
        var decide = Championship.class.getDeclaredMethod("determineSeriesWinner", FirstLegMatch.class, SecondLegMatch.class);
        decide.setAccessible(true);
        Team winner = (Team) decide.invoke(c, first, second);
        check(second.isForfeit() && second.isSettledByPenalties(), "Tied series gets separate shootout");
        check(second.getIncidents().stream().allMatch(i -> i.isShootoutPenalty()), "Only shootout incidents");
        check(c.getRecordedSeriesWinner(first, second) == winner, "Winner can be read without resimulation");
        // Even if an administrative result has shootout lineups, no match minutes count.
        source.awardForfeit(true);
        new MatchSimulator(new Random(6)).prepareAdministrativeShootout(source);
        check(new TournamentReportService().getParticipation(c).stream().allMatch(p -> p.getMinutes() == 0 && p.getMatches() == 0), "Shootout lineups do not create appearances");
    }

    private static void permanentSquadProblem() throws Exception {
        Match source = tournament().getMatches().get(0);
        Team emptyA = new Team(101, "Empty A", source.getHomeTeam().getCountry(), 1, source.getHomeTeam().getCoach());
        Team emptyB = new Team(102, "Empty B", source.getAwayTeam().getCountry(), 2, source.getAwayTeam().getCoach());
        Match m = new FinalMatch(source.getMatchDate(), emptyA, emptyB, source.getReferee());
        boolean refused = false;
        try { new MatchSimulator(new Random()).simulate(m); } catch (IllegalStateException e) { refused = true; }
        check(refused && !m.isPlayed() && m.getPostponements() == 0, "Invalid squads must not postpone forever");
    }

    private static void knockoutProgression() throws Exception {
        Championship c = tournament();
        c.simulateGroupStage();
        c.scheduleQuarterFinals(c.getMatches().stream().map(Match::getMatchDate)
                .max(java.time.LocalDate::compareTo).orElseThrow().plusDays(7));
        Match first = c.getMatches().stream().filter(m -> m.asFirstLeg() != null).findFirst().orElseThrow();
        suspendKeepers(first.getHomeTeam()); suspendKeepers(first.getAwayTeam());
        check(c.simulateNextKnockoutMatch() == first && !first.isPlayed(), "Knockout postponement returns pending fixture");
        suspendKeepers(first.getHomeTeam());
        check(c.simulateNextKnockoutMatch().isForfeit(), "First leg administrative loss");
        boolean injectedReturn = false, administrativeShootout = false, finalForfeit = false;
        for (int step = 0; c.getStage() != TournamentStage.FINISHED && step < 60; step++) {
            if (!c.hasPendingPenaltyShootout()) {
                Match next = c.getMatches().stream().filter(m -> !m.isGroupStage() && !m.isPlayed()).findFirst().orElseThrow();
                if (!injectedReturn && next.asSecondLeg() != null && next.getHomeTeam().getId() == first.getAwayTeam().getId()) {
                    next.getHomeTeam().getPlayers().forEach(p -> p.setSuspended(false));
                    next.getAwayTeam().getPlayers().forEach(p -> p.setSuspended(false));
                    suspendKeepers(next.getHomeTeam());
                    injectedReturn = true;
                }
                if (next.asFinal() != null) {
                    next.getHomeTeam().getPlayers().forEach(p -> p.setSuspended(false));
                    next.getAwayTeam().getPlayers().forEach(p -> p.setSuspended(false));
                    suspendKeepers(next.getHomeTeam());
                    finalForfeit = true;
                }
            }
            c.simulateNextKnockoutMatch();
            if (c.hasPendingPenaltyShootout() && c.getPendingPenaltyShootout().isForfeit()) {
                administrativeShootout = true;
                c = roundTrip(c);
                check(c.hasPendingPenaltyShootout(), "Administrative shootout survives save/load");
            }
        }
        check(injectedReturn && administrativeShootout && finalForfeit, "Administrative cases exercised in real progression");
        check(c.getStage() == TournamentStage.FINISHED && c.getChampion() != null
                && c.getPlayedMatches().size() == 37, "Complete tournament after postponement and administrative results");
        for (Match m : c.getPlayedMatches()) new MatchDetailsFormatter().format(c, m);
        System.out.println("Knockout progression with PostgreSQL passed.");
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(value); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) { return (T) in.readObject(); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
