package GUI;

import Core.Run.Championship;
import Core.domain.Player;
import Core.domain.Team;
import Core.report.TournamentReportService;
import Core.enums.Position;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


import java.util.List;

import java.time.LocalDate;
import Core.report.TournamentReportService.PlayerStatsRow;
import Core.report.PlayerAttributeFormatter;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;
import java.util.Map;

import static GUI.RankingTableFactory.*;

/** Owns player filters, statistics and player detail tabs. */
public final class PlayerStatsView {
    private Championship championship;
    private boolean updatingPlayerChoices;
    public record PlayerChoice(Player player, String label) {
        @Override public String toString() { return label; }
    }
    private final VBox rankingPanel;
    private final HBox playerFilterBar;
    private final ComboBox<String> playerPositionFilter;
    private final ComboBox<PlayerChoice> individualPlayerFilter;
    private final RankingPanelView rankingView;
    private final TournamentReportService reportService;

    public PlayerStatsView(VBox rankingPanel,
            HBox playerFilterBar,
            ComboBox<String> playerPositionFilter,
            ComboBox<PlayerChoice> individualPlayerFilter,
            RankingPanelView rankingView,
            TournamentReportService reportService) {
        this.rankingPanel = rankingPanel;
        this.playerFilterBar = playerFilterBar;
        this.playerPositionFilter = playerPositionFilter;
        this.individualPlayerFilter = individualPlayerFilter;
        this.rankingView = rankingView;
        this.reportService = reportService;
        playerPositionFilter.getItems().setAll(
                "All",
                "Goalkeepers",
                "Defenders",
                "Midfielders",
                "Forwards"
        );

        playerPositionFilter.setValue("All");

        individualPlayerFilter.setOnAction(event -> {
            if (!updatingPlayerChoices && championship != null && playerFilterBar.isVisible()) {
                show(championship);
            }
        });

        playerPositionFilter.setOnAction(event -> {
            if (championship != null && playerFilterBar.isVisible()) {
                show(championship);
            }
        });

        playerPositionFilter.getStyleClass().add("player-filter");
        individualPlayerFilter.getStyleClass().add("player-filter");
    }
    public void show(Championship championship) {
        this.championship = championship;
        Position position = switch (playerPositionFilter.getValue()) {
            case "Goalkeepers" -> Position.GOALKEEPER;
            case "Defenders" -> Position.DEFENDER;
            case "Midfielders" -> Position.MIDFIELDER;
            case "Forwards" -> Position.FORWARD;
            default -> null;
        };

        List<PlayerStatsRow> rows =
                reportService.getPlayerStats(championship, position);

        // Actualizar las opciones conservando el jugador seleccionado.
        PlayerChoice previous = individualPlayerFilter.getValue();

        List<PlayerChoice> choices = new java.util.ArrayList<>();
        choices.add(new PlayerChoice(null, "All players"));

        for (PlayerStatsRow row : rows) {
            Player player = row.getPlayer();

            choices.add(new PlayerChoice(
                    player,
                    player.getLastName() + ", " + player.getName()
                            + " - " + row.getTeam().getName()
                            + " (#" + player.getShirtNumber() + ")"
            ));
        }

        updatingPlayerChoices = true;

        try {
            individualPlayerFilter.getItems().setAll(choices);

            individualPlayerFilter.setValue(
                    choices.stream()
                            .filter(choice -> previous != null
                                    && choice.player() == previous.player())
                            .findFirst()
                            .orElse(choices.get(0))
            );
        } finally {
            updatingPlayerChoices = false;
        }

        Player selectedPlayer = individualPlayerFilter.getValue().player();

        List<PlayerStatsRow> filteredRows = rows.stream()
                .filter(row -> selectedPlayer == null
                        || row.getPlayer() == selectedPlayer)
                .toList();

        TableView<PlayerStatsRow> table = rankingView.showRanking(
                "Players Stats",
                filteredRows,

                rankingColumn("Player",
                        row -> row.getPlayer().getName()
                                + " " + row.getPlayer().getLastName()),

                rankingColumn("Team",
                        row -> row.getTeam().getName()),

                rankingColumn("Position",
                        PlayerStatsRow::getPosition),

                rankingColumn("Shirt number",
                        row -> row.getPlayer().getShirtNumber()),

                rankingColumn("Matches",
                        PlayerStatsRow::getMatches),

                rankingColumn("Minutes",
                        PlayerStatsRow::getMinutes),

                rankingColumn("Goals",
                        PlayerStatsRow::getGoals),

                rankingColumn("Goals conceded",
                        row -> row.getPlayer().getPosition() == Position.GOALKEEPER
                                ? row.getGoalsAgainst()
                                : null),

                decimalRankingColumn("Avg conceded / match",
                        row -> row.getPlayer().getPosition() == Position.GOALKEEPER
                                && row.getMatches() > 0
                                ? row.getAverageGoalsAgainst()
                                : null,
                        "%.2f"),

                decimalRankingColumn("Average rating",
                        row -> row.getPlayer().getAverageRating(),
                        "%.2f"),

                rankingColumn("Suspended",
                        row -> row.getPlayer().isSuspended() ? "Yes" : "No")
        );

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(
                "No players found for the selected filters."
        ));

        // showRanking oculta los filtros: volver a mostrarlos.
        setPlayerFilterVisible(true);

        // Quitar la tabla del panel para colocarla dentro de una pestaña.
        rankingPanel.getChildren().remove(table);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("players-tabs");

        Tab statsTab = new Tab("Players Stats", table);
        statsTab.setClosable(false);

        tabs.getTabs().add(statsTab);

        VBox.setVgrow(tabs, javafx.scene.layout.Priority.ALWAYS);
        rankingPanel.getChildren().add(tabs);

        // Abrir la ficha al hacer clic sobre una fila con un jugador.
        table.setRowFactory(tv -> {
            TableRow<PlayerStatsRow> tableRow = new TableRow<>();

            tableRow.setOnMouseClicked(event -> {
                if (!tableRow.isEmpty()
                        && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 1) {

                    openPlayerTab(tabs, tableRow.getItem());
                }
            });

            return tableRow;
        });
    }

    private void openPlayerTab(TabPane tabs, PlayerStatsRow row) {
        Player player = row.getPlayer();

        Tab existingTab = tabs.getTabs().stream()
                .filter(tab -> tab.getUserData() == player)
                .findFirst()
                .orElse(null);

        if (existingTab != null) {
            tabs.getSelectionModel().select(existingTab);
        } else {
            Label title = new Label(
                    player.getName() + " " + player.getLastName()
                            + " - " + row.getTeam().getName()
            );
            title.getStyleClass().add("section-title");
            title.setWrapText(true);

            Label playerId = new Label(
                    String.format("PLAYER ID  •  #%04d", player.getId())
            );
            playerId.getStyleClass().add("player-id-badge");

            Label document = new Label(
                    "Document: " + player.getDocumentType()
                            + " " + player.getDocumentNumber()
            );
            document.getStyleClass().add("player-info-text");
            document.setWrapText(true);

            Label birthDate = new Label(
                    "Birth date: " + player.getBirthDate()
                            + "  |  Age: "
                            + java.time.Period.between(
                            player.getBirthDate(),
                            LocalDate.now()
                    ).getYears()
            );
            birthDate.getStyleClass().add("player-info-text");
            birthDate.setWrapText(true);

            VBox personalData = new VBox(8, playerId, document, birthDate);
            personalData.getStyleClass().add("player-info-card");

            VBox characteristics = createAttributesTable(
                    "Characteristics",
                    player.getCharacteristics()
            );

            VBox statistics = createAttributesTable(
                    "Statistics",
                    player.getStatistics()
            );


            HBox tables = new HBox(14, characteristics, statistics);

            characteristics.setMinWidth(0);
            statistics.setMinWidth(0);

            characteristics.setPrefWidth(0);
            statistics.setPrefWidth(0);

            characteristics.setMaxWidth(Double.MAX_VALUE);
            statistics.setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(characteristics, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(statistics, javafx.scene.layout.Priority.ALWAYS);

            VBox content = new VBox(12, title, personalData, tables);
            content.setStyle("-fx-padding: 12;");
            VBox.setVgrow(tables, javafx.scene.layout.Priority.ALWAYS);

            Tab playerTab = new Tab(
                    player.getName() + " " + player.getLastName(),
                    content
            );

            playerTab.setClosable(true);
            playerTab.setUserData(player);

            tabs.getTabs().add(playerTab);
            tabs.getSelectionModel().select(playerTab);
        }
    }

    private VBox createAttributesTable(
            String title,
            Map<String, Integer> attributes) {

        Label heading = new Label(title);
        heading.getStyleClass().add("ranking-summary");
        heading.setWrapText(true);

        TableView<Map.Entry<String, Integer>> table = new TableView<>();

        TableColumn<Map.Entry<String, Integer>, String> attributeColumn =
                rankingColumn(
                        "Attribute",
                        entry -> PlayerAttributeFormatter.label(entry.getKey())
                );

        TableColumn<Map.Entry<String, Integer>, Integer> valueColumn =
                rankingColumn("Value", entry -> entry.getValue());

        attributeColumn.setMinWidth(150);
        attributeColumn.setPrefWidth(240);

        valueColumn.setMinWidth(65);
        valueColumn.setPrefWidth(90);

        table.setSelectionModel(null);
        table.setFocusTraversable(false);
        table.getStyleClass().add("attributes-table");

        table.getColumns().add(attributeColumn);
        table.getColumns().add(valueColumn);

        if (attributes != null) {
            table.getItems().setAll(attributes.entrySet());
        }

        attributeColumn.setSortType(TableColumn.SortType.ASCENDING);
        table.getSortOrder().add(attributeColumn);
        table.sort();

        attributeColumn.setSortable(false);
        valueColumn.setSortable(false);

        attributeColumn.setReorderable(false);
        valueColumn.setReorderable(false);

        attributeColumn.setResizable(false);
        valueColumn.setResizable(false);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("ranking-table");
        table.setPlaceholder(new Label("No data available."));

        VBox container = new VBox(8, heading, table);
        container.setMinWidth(0);
        container.setMinHeight(0);

        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        return container;
    }

    private void setPlayerFilterVisible(boolean visible) {
        playerFilterBar.setVisible(visible);
        playerFilterBar.setManaged(visible);
    }
}
