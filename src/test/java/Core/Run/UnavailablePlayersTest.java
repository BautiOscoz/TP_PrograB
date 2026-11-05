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
        randomFixture();
        newRedCardsRemainSuspended();
        tiedAdministrativeSeries();
        permanentSquadProblem();
        incidentTeams();
        polymorphicMatchTimeline();
        protectedMatchState();
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

    private static void randomFixture() throws Exception {
        Championship c = tournament();
        Match m = c.getMatches().get(0);
        var date = m.getMatchDate();
        Match later = c.getMatches().stream().filter(x -> x.getMatchday() == 2).findFirst().orElseThrow();
        var laterDate = later.getMatchDate();
        suspendKeepers(m.getHomeTeam()); suspendKeepers(m.getAwayTeam());
        c.simulateNextMatchday();
        check(m.isPlayed() && m.isForfeit() && m.getPostponements() == 0, "Both unavailable: random administrative result");
        check(m.getMatchDate().equals(date) && later.getMatchDate().equals(laterDate), "Dates unchanged");
        check(c.getCurrentMatchday() == 2 && c.getPlayedMatches().size() == 8, "Matchday completes immediately");
        check(m.getAdministrativeReason().contains("random draw"), "Random decision explained");
        check(m.getIncidents().isEmpty(), "No fictitious incidents");
        Championship restored = roundTrip(c);
        check(restored.getMatches().get(0).getAdministrativeReason().equals(m.getAdministrativeReason()), "Random decision survives save/load");
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
        Random random = new Random(2026);
        MatchSimulator simulator = new MatchSimulator(random);
        var knockout = new Core.simulation.KnockoutStageService(new java.util.ArrayList<>(),
                new Core.scheduling.TournamentScheduler(random, c.getReferees()),
                new Core.classification.KnockoutResolver(),
                new Core.simulation.PenaltyShootoutSimulator(random, simulator), simulator, random);
        Team winner = knockout.determineSeriesWinner(first, second);
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
        for (boolean homeWins : new boolean[]{true, false}) {
            Match fixture = new FinalMatch(source.getMatchDate(), emptyA, emptyB, source.getReferee());
            new MatchSimulator(new Random() {
                @Override public boolean nextBoolean() { return homeWins; }
            }).simulate(fixture);
            check(fixture.isPlayed() && fixture.isForfeit(), "Empty squads resolved by draw");
            check(fixture.getHomeGoals() == (homeWins ? 3 : 0)
                    && fixture.getAwayGoals() == (homeWins ? 0 : 3), "Both draw outcomes supported");
        }
    }

    private static void polymorphicMatchTimeline() throws Exception {
        Match match = tournament().getMatches().get(0);
        Team home = match.getHomeTeam(), away = match.getAwayTeam();
        FormationType homeFormation = Arrays.stream(FormationType.values())
                .filter(f -> Lineup.canUseFormation(home, f)).findFirst().orElseThrow();
        FormationType awayFormation = Arrays.stream(FormationType.values())
                .filter(f -> Lineup.canUseFormation(away, f)).findFirst().orElseThrow();
        Lineup homeLineup = new Lineup(home, homeFormation);
        Lineup awayLineup = new Lineup(away, awayFormation);
        match.setInitialLineups(homeLineup, awayLineup);
        Player outgoing = homeLineup.getPlayers().get(0);
        Player incoming = home.getPlayers().stream().filter(p -> !homeLineup.getPlayers().contains(p))
                .findFirst().orElseThrow();
        Player opponent = awayLineup.getPlayers().get(0);
        match.addIncident(new Core.incidents.YellowCard(10, outgoing));
        match.addIncident(new Core.incidents.Goal(15, outgoing, opponent, false, false));
        match.addIncident(new Core.incidents.Substitution(30, incoming, outgoing));
        match.addIncident(new Core.incidents.RedCard(40, opponent));
        match.addIncident(new Core.incidents.RedCard(60, incoming));
        check(match.getPlayersOnField(home, 29).equals(homeLineup.getPlayers()), "Goals and yellows leave lineup unchanged");
        check(match.getPlayersOnField(home, 30).contains(incoming)
                && !match.getPlayersOnField(home, 30).contains(outgoing), "Substitution takes effect at its minute");
        check(match.getPlayersOnField(home, 40).size() == 11
                && match.getPlayersOnField(away, 40).size() == 10, "Red only affects its own team");
        check(match.getPlayersOnField(home, 60).size() == 10
                && !match.getPlayersOnField(home, 60).contains(incoming), "Substitute can be sent off");
        check(homeLineup.getPlayers().contains(outgoing) && homeLineup.getPlayers().size() == 11,
                "Timeline queries preserve recorded starting lineup");
        check(match.getPlayersOnField(home, 29).contains(outgoing) && !incoming.isSuspended(),
                "Historical queries have no side effects");
        List<Match> types = List.of(match,
                new FirstLegMatch(match.getMatchDate(), home, away, match.getReferee()),
                new SecondLegMatch(match.getMatchDate(), home, away, match.getReferee(), 0, 0),
                new FinalMatch(match.getMatchDate(), home, away, match.getReferee()));
        List<String> labels = List.of("Group matchday " + match.getMatchday(), "First leg", "Second leg", "Final");
        for (int i = 0; i < types.size(); i++) {
            check(types.get(i).getStageDescription().equals(labels.get(i)), "Polymorphic stage description");
            check(new MatchDetailsFormatter().title(types.get(i)).contains(labels.get(i)), "Report uses stage description");
        }
        Match restored = roundTrip(match);
        check(restored.getPlayersOnField(restored.getHomeTeam(), 60).size() == 10,
                "Polymorphic effects survive save/load");
    }

    private static void protectedMatchState() throws Exception {
        Match match = tournament().getMatches().get(0);
        new MatchSimulator(new Random(22)).simulate(match);
        int homeGoals = match.getHomeGoals();
        expectRejected(() -> match.setHomeGoals(homeGoals + 1));
        expectRejected(() -> match.setPlayed(false));
        expectRejected(() -> match.complete(0, 0));
        expectRejected(() -> match.getHomeLineup().getPlayers().clear());
        expectRejected(() -> match.setInitialLineups(match.getHomeLineup(), match.getAwayLineup()));
        Match pending = tournament().getMatches().get(0);
        expectRejected(() -> pending.complete(3, -1));
        check(!pending.isPlayed() && pending.getHomeGoals() == 0, "Invalid completion changes nothing");
        expectRejected(() -> pending.setHomeGoals(-1));
        Player starter = match.getHomeLineup().getPlayers().get(0);
        Player substitute = match.getAwayLineup().getPlayers().get(0);
        ParticipationTimeline timeline = new ParticipationTimeline(List.of(starter), List.of());
        new Core.incidents.Goal(10, starter, substitute, false, false).applyToParticipation(timeline);
        new Core.incidents.Substitution(90, substitute, starter).applyToParticipation(timeline);
        new Core.incidents.PenaltyShootout(91, substitute, true).applyToParticipation(timeline);
        check(timeline.minutesPlayed().get(starter) == 90
                && timeline.minutesPlayed().containsKey(substitute)
                && timeline.minutesPlayed().get(substitute) == 0, "Minute 90 counts appearance, shootout adds no minutes");
        expectRejected(() -> timeline.minutesPlayed().clear());
    }

    private static void expectRejected(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException expected) { return; }
        throw new AssertionError("Invalid mutation was accepted");
    }

    private static void incidentTeams() throws Exception {
        Match match = tournament().getMatches().get(0);
        Player home = match.getHomeTeam().getPlayers().get(0);
        Player away = match.getAwayTeam().getPlayers().get(0);
        var events = List.of(
                new Core.incidents.Goal(1, home, away, false, false),
                new Core.incidents.Goal(2, home, home, false, true),
                new Core.incidents.YellowCard(3, home),
                new Core.incidents.RedCard(4, home),
                new Core.incidents.Substitution(5, home, home),
                new Core.incidents.PenaltyShootout(91, home, true));
        for (var event : events) {
            check(IncidentFormatter.describe(match, event).startsWith("[" + match.getHomeTeam().getName() + "]"),
                    "Participant team shown for every incident, including own goals");
        }
        check(IncidentFormatter.describe(match, new Core.incidents.YellowCard(6, away))
                .startsWith("[" + match.getAwayTeam().getName() + "]"), "Away team correctly identified");
    }

    private static void knockoutProgression() throws Exception {
        Championship c = tournament();
        c.simulateGroupStage();
        c.scheduleQuarterFinals(c.getMatches().stream().map(Match::getMatchDate)
                .max(java.time.LocalDate::compareTo).orElseThrow().plusDays(7));
        Match first = c.getMatches().stream().filter(m -> m.asFirstLeg() != null).findFirst().orElseThrow();
        suspendKeepers(first.getHomeTeam()); suspendKeepers(first.getAwayTeam());
        check(c.simulateNextKnockoutMatch() == first && first.isForfeit(), "Knockout random result resolved immediately");
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
