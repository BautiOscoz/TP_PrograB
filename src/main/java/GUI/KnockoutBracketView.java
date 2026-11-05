package GUI;

import Core.Run.Championship;
import Core.domain.FinalMatch;
import Core.domain.FirstLegMatch;
import Core.domain.Match;
import Core.domain.SecondLegMatch;
import Core.domain.Team;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


import java.util.List;
import java.util.Objects;


import static Core.report.MatchScoreSummary.*;

/** Renders the knockout bracket and opens match details. */
public final class KnockoutBracketView {
    private Championship championship;
    private final VBox quarterColumn;
    private final VBox quarterConnectorColumn;
    private final VBox semiColumn;
    private final VBox semiConnectorColumn;
    private final VBox finalColumn;
    private final Label winnerBanner;
    private final MatchDetailsView details;

    public KnockoutBracketView(VBox quarterColumn,
            VBox quarterConnectorColumn,
            VBox semiColumn,
            VBox semiConnectorColumn,
            VBox finalColumn,
            Label winnerBanner,
            MatchDetailsView details) {
        this.quarterColumn = quarterColumn;
        this.quarterConnectorColumn = quarterConnectorColumn;
        this.semiColumn = semiColumn;
        this.semiConnectorColumn = semiConnectorColumn;
        this.finalColumn = finalColumn;
        this.winnerBanner = winnerBanner;
        this.details = details;
    }
    public void show(Championship championship) {
        this.championship = championship;
        quarterColumn.getChildren().clear();
        quarterConnectorColumn.getChildren().clear();
        semiColumn.getChildren().clear();
        semiConnectorColumn.getChildren().clear();
        finalColumn.getChildren().clear();
        winnerBanner.setText("WINNER: -");

        List<FirstLegMatch> firstLegs = championship.getMatches().stream()
                .map(Match::asFirstLeg)
                .filter(Objects::nonNull)
                .toList();
        List<SecondLegMatch> secondLegs = championship.getMatches().stream()
                .map(Match::asSecondLeg)
                .filter(Objects::nonNull)
                .toList();

        addSeriesCards(quarterColumn, firstLegs, secondLegs, 0, 4);
        addSeriesCards(semiColumn, firstLegs, secondLegs, 4, 2);
        addConnectors(quarterConnectorColumn, 2);
        addConnectors(semiConnectorColumn, 1);

        championship.getMatches().stream()
                .map(Match::asFinal)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(finalMatch -> {
                    if (finalMatch.isPlayed()
                            && !championship.isPendingPenaltyShootout(finalMatch)) {
                        Team winner = championship.getRecordedFinalWinner(finalMatch);
                        finalColumn.getChildren().add(createMatchCard(List.of(finalMatch), winner));
                        winnerBanner.setText("WINNER: " + winner.getName());
                    } else if (championship.isPendingPenaltyShootout(finalMatch)) {
                        finalColumn.getChildren().add(createPendingPenaltyCard(finalMatch));
                    } else {
                        finalColumn.getChildren().add(createPendingMatchCard(finalMatch));
                    }
                });
    }

    private void addConnectors(VBox column, int count) {
        for (int i = 0; i < count; i++) {
            Label arrow = new Label("›");
            arrow.getStyleClass().add("bracket-arrow");
            arrow.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(arrow, javafx.scene.layout.Priority.ALWAYS);
            column.getChildren().add(arrow);
        }
    }

    private void addSeriesCards(VBox column, List<FirstLegMatch> firstLegs,
                                List<SecondLegMatch> secondLegs, int startIndex, int count) {
        for (int index = startIndex; index < startIndex + count && index < firstLegs.size(); index++) {
            FirstLegMatch first = firstLegs.get(index);
            SecondLegMatch second = index < secondLegs.size() ? secondLegs.get(index) : null;
            if (second != null && first.isPlayed() && second.isPlayed()
                    && !championship.isPendingPenaltyShootout(second)) {
                Team winner = championship.getRecordedSeriesWinner(first, second);
                column.getChildren().add(createMatchCard(List.of(first, second), winner));
            } else {
                column.getChildren().add(createPendingSeriesCard(first, second));
            }
        }
    }

    private VBox createPendingSeriesCard(FirstLegMatch first, SecondLegMatch second) {
        VBox card = new VBox(3);
        card.getStyleClass().add("match-card");
        Label teams = new Label(first.getHomeTeam().getName()
                + " vs " + first.getAwayTeam().getName());
        teams.setWrapText(true);
        card.getChildren().add(teams);
        if (first.isPlayed()) {
            card.getChildren().add(new Label("First leg: " + first.getHomeGoals()
                    + " - " + first.getAwayGoals() + (first.isForfeit() ? " (administrative)" : "")));
        } else {
            card.getChildren().add(new Label("First leg pending"));
        }
        if (second == null || !second.isPlayed()) {
            card.getChildren().add(new Label("Second leg pending"));
        } else if (championship.isPendingPenaltyShootout(second)) {
            card.getChildren().add(new Label("Penalty shootout pending"));
        }
        if (first.isPlayed()) {
            card.getChildren().add(new Label("Click to view incidents"));
            card.setOnMouseClicked(event -> details.show(championship,
                    second != null && second.isPlayed()
                            ? List.of(first, second)
                            : List.of(first)
            ));
        }
        return card;
    }

    private VBox createPendingPenaltyCard(Match match) {
        VBox card = new VBox(3);
        card.getStyleClass().add("match-card");
        card.getChildren().add(new Label(match.getHomeTeam().getName()
                + " " + match.getHomeGoals() + " - " + match.getAwayGoals()
                + " " + match.getAwayTeam().getName()));
        card.getChildren().add(new Label("Penalty shootout pending"));
        card.getChildren().add(new Label("Click to view incidents"));
        card.setOnMouseClicked(event -> details.show(championship, List.of(match)));
        return card;
    }

    private VBox createPendingMatchCard(FinalMatch match) {
        VBox card = new VBox(3);
        card.getStyleClass().add("match-card");
        card.getChildren().add(new Label(match.getHomeTeam().getName()
                + " vs " + match.getAwayTeam().getName()));
        card.getChildren().add(new Label("Final pending"));
        return card;
    }

    private VBox createMatchCard(List<Match> matches, Team winner) {
        Match displayMatch = matches.size() == 1 ? matches.get(0) : matches.get(matches.size() - 1);

        VBox card = new VBox(3);
        card.getStyleClass().add("match-card");
        card.setFillWidth(true);

        addTeamScoreRow(card, displayMatch.getHomeTeam(), displayMatch.getHomeGoals(), winner);
        addTeamScoreRow(card, displayMatch.getAwayTeam(), displayMatch.getAwayGoals(), winner);

        if (isSettledByPenalties(displayMatch)) {
            int[] penalties = getPenaltyScore(displayMatch);
            Label penaltyLabel = new Label("Penalties " + penalties[0] + " - " + penalties[1]);
            penaltyLabel.getStyleClass().add("penalty-score");
            penaltyLabel.setMaxWidth(Double.MAX_VALUE);
            penaltyLabel.setAlignment(javafx.geometry.Pos.CENTER);
            card.getChildren().add(penaltyLabel);
        }

        if (matches.size() == 2) {
            int[] aggregate = getAggregateScore(matches.get(0), matches.get(1));
            Label aggregateLabel = new Label("Aggregate " + aggregate[0] + " - " + aggregate[1]);
            aggregateLabel.getStyleClass().add("aggregate-score");
            aggregateLabel.setMaxWidth(Double.MAX_VALUE);
            aggregateLabel.setAlignment(javafx.geometry.Pos.CENTER);
            card.getChildren().add(aggregateLabel);
        }

        Label winnerLabel = new Label("WINNER: " + winner.getName());
        winnerLabel.getStyleClass().add("match-winner");
        winnerLabel.setMaxWidth(Double.MAX_VALUE);
        winnerLabel.setAlignment(javafx.geometry.Pos.CENTER);
        card.getChildren().add(winnerLabel);
        card.getChildren().add(new Label(displayMatch.isForfeit() ? "Administrative result - details" : "Click to view incidents"));
        card.setOnMouseClicked(event -> details.show(championship, matches));
        return card;
    }

    private void addTeamScoreRow(VBox card, Team team, int goals, Team winner) {
        HBox row = new HBox(6);
        row.getStyleClass().add("team-score-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label teamLabel = new Label(team.getName());
        teamLabel.getStyleClass().add("match-team");
        teamLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(teamLabel, javafx.scene.layout.Priority.ALWAYS);

        Label scoreLabel = new Label(String.valueOf(goals));
        scoreLabel.getStyleClass().add("match-score");
        scoreLabel.setMinWidth(24);
        scoreLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        if (winner.equals(team)) {
            teamLabel.getStyleClass().add("match-team-winner");
            scoreLabel.getStyleClass().add("match-score-winner");
        }

        row.getChildren().addAll(teamLabel, scoreLabel);
        card.getChildren().add(row);
    }
}
