package Controller;

import Core.Run.Championship;
import Core.domain.City;
import Core.domain.Country;
import Core.domain.Stadium;
import Core.persistance.ChampionshipRepository;
import Infrastructure.database.CityRepository;
import Infrastructure.database.StadiumRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class VenuesController {
    @FXML private AnchorPane rootPane;
    @FXML private TableView<City> cityTable;
    @FXML private TableColumn<City, Number> cityIdColumn;
    @FXML private TableColumn<City, String> cityNameColumn;
    @FXML private TableColumn<City, String> cityCountryColumn;
    @FXML private TextField cityNameField;
    @FXML private ComboBox<Country> countryCombo;
    @FXML private TableView<Stadium> stadiumTable;
    @FXML private TableColumn<Stadium, Number> stadiumIdColumn;
    @FXML private TableColumn<Stadium, String> stadiumNameColumn;
    @FXML private TableColumn<Stadium, String> stadiumCityColumn;
    @FXML private TextField stadiumNameField;
    @FXML private ComboBox<City> stadiumCityCombo;
    @FXML private Label cityCountLabel;
    @FXML private Label stadiumCountLabel;
    @FXML private Button updateCityButton;
    @FXML private Button deleteCityButton;
    @FXML private Button updateStadiumButton;
    @FXML private Button deleteStadiumButton;

    private final CityRepository cityRepository = new CityRepository();
    private final StadiumRepository stadiumRepository = new StadiumRepository();
    private final ChampionshipRepository championshipRepository =
            new ChampionshipRepository();
    private Championship championship;
    private String tournamentName;

    @FXML
    private void initialize() {
        cityIdColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        cityNameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));
        cityCountryColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getCountry().getName()));
        stadiumIdColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        stadiumNameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));
        stadiumCityColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getCity().getName()));

        cityTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    selectCity(selected);
                    updateSelectionControls();
                }
        );
        stadiumTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    selectStadium(selected);
                    updateSelectionControls();
                }
        );
        updateSelectionControls();
    }

    public void setTournament(Championship championship, String tournamentName) {
        this.championship = championship;
        this.tournamentName = tournamentName;
        countryCombo.getItems().setAll(championship.getCountries());
        refreshDataSafely();
    }

    @FXML
    private void handleAddCity() {
        try {
            cityRepository.create(cityNameField.getText(), countryCombo.getValue());
            refreshData();
            clearCityForm();
        } catch (SQLException | RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleUpdateCity() {
        City selected = cityTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a city to update.");
            return;
        }
        try {
            City updated = new City(
                    selected.getId(),
                    cityNameField.getText(),
                    countryCombo.getValue()
            );
            if (!cityRepository.update(updated)) {
                throw new IllegalStateException("The city no longer exists.");
            }
            refreshData();
            clearCityForm();
        } catch (SQLException | RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleDeleteCity() {
        City selected = cityTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a city to delete.");
            return;
        }
        if (!confirm("Delete city " + selected.getName() + "?")) return;

        try {
            if (!cityRepository.delete(selected.getId())) {
                throw new IllegalStateException("The city no longer exists.");
            }
            refreshData();
            clearCityForm();
        } catch (SQLException | RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleAddStadium() {
        try {
            stadiumRepository.create(stadiumNameField.getText(), stadiumCityCombo.getValue());
            refreshData();
            clearStadiumForm();
        } catch (SQLException | RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleUpdateStadium() {
        Stadium selected = stadiumTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a stadium to update.");
            return;
        }
        try {
            Stadium updated = new Stadium(
                    selected.getId(),
                    stadiumNameField.getText(),
                    stadiumCityCombo.getValue()
            );
            if (!stadiumRepository.update(updated)) {
                throw new IllegalStateException("The stadium no longer exists.");
            }
            refreshData();
            clearStadiumForm();
        } catch (SQLException | RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleDeleteStadium() {
        Stadium selected = stadiumTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a stadium to delete.");
            return;
        }
        boolean assignedToMatch = championship.getMatches().stream()
                .map(match -> match.getStadium())
                .filter(java.util.Objects::nonNull)
                .anyMatch(stadium -> stadium.getId() == selected.getId());
        if (assignedToMatch) {
            showError("The stadium cannot be deleted because it is assigned to a match.");
            return;
        }
        if (!confirm("Delete stadium " + selected.getName() + "?")) return;

        try {
            if (!stadiumRepository.delete(selected.getId())) {
                throw new IllegalStateException("The stadium no longer exists.");
            }
            refreshData();
            clearStadiumForm();
        } catch (SQLException | RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        championshipRepository.save(championship);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tournament.fxml"));
            Parent view = loader.load();
            TournamentController controller = loader.getController();
            controller.setTournament(championship, tournamentName);
            rootPane.getChildren().setAll(view);
        } catch (IOException | IllegalStateException exception) {
            showError("The tournament screen could not be opened.");
        }
    }

    private void refreshDataSafely() {
        try {
            refreshData();
        } catch (SQLException | RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    private void refreshData() throws SQLException {
        List<City> cities = cityRepository.findAll(championship.getCountries());
        List<Stadium> stadiums = stadiumRepository.findAll(cities);
        championship.loadVenues(cities, stadiums);
        cityTable.getItems().setAll(cities);
        stadiumTable.getItems().setAll(stadiums);
        stadiumCityCombo.getItems().setAll(cities);
        cityCountLabel.setText(cities.size() + " cities registered");
        stadiumCountLabel.setText(stadiums.size() + " stadiums available");
        updateSelectionControls();
        championshipRepository.save(championship);
    }

    private void selectCity(City city) {
        if (city == null) return;
        cityNameField.setText(city.getName());
        countryCombo.setValue(city.getCountry());
    }

    private void selectStadium(Stadium stadium) {
        if (stadium == null) return;
        stadiumNameField.setText(stadium.getName());
        stadiumCityCombo.getItems().stream()
                .filter(city -> city.getId() == stadium.getCity().getId())
                .findFirst()
                .ifPresent(stadiumCityCombo::setValue);
    }

    private void clearCityForm() {
        cityTable.getSelectionModel().clearSelection();
        cityNameField.clear();
        countryCombo.setValue(null);
    }

    private void clearStadiumForm() {
        stadiumTable.getSelectionModel().clearSelection();
        stadiumNameField.clear();
        stadiumCityCombo.setValue(null);
    }

    private void updateSelectionControls() {
        boolean citySelected = cityTable.getSelectionModel().getSelectedItem() != null;
        updateCityButton.setDisable(!citySelected);
        deleteCityButton.setDisable(!citySelected);

        boolean stadiumSelected = stadiumTable.getSelectionModel().getSelectedItem() != null;
        updateStadiumButton.setDisable(!stadiumSelected);
        deleteStadiumButton.setDisable(!stadiumSelected);
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("PostgreSQL");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("PostgreSQL");
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "Database operation failed." : message);
        alert.showAndWait();
    }
}
