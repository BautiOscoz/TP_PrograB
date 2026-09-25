package Infrastructure.database;

import Core.domain.City;
import Core.domain.Stadium;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StadiumRepository {

    public List<Stadium> findAll(
            List<City> cities
    ) throws SQLException {

        String sql = """
                SELECT
                    estadio.id,
                    estadio.nombre AS estadio,
                    ciudad.id AS ciudad_id
                FROM estadio
                JOIN ciudad
                    ON estadio.ciudad = ciudad.id
                JOIN pais
                    ON ciudad.id_pais = pais.id
                ORDER BY
                    pais.nombre,
                    ciudad.nombre,
                    estadio.nombre
                """;

        List<Stadium> stadiums =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.connect();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            while (resultSet.next()) {

                long stadiumId =
                        resultSet.getLong("id");

                String stadiumName =
                        resultSet.getString("estadio");

                long cityId =
                        resultSet.getLong("ciudad_id");

                City city =
                        findCity(cities, cityId);

                Stadium stadium =
                        new Stadium(
                                stadiumId,
                                stadiumName,
                                city
                        );

                stadiums.add(stadium);
            }
        }

        return stadiums;
    }

    private City findCity(
            List<City> cities,
            long cityId
    ) {
        return cities.stream()
                .filter(city ->
                        city.getId() == cityId
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "City from stadium "
                                        + "was not loaded: "
                                        + cityId
                        )
                );
    }
    public void insert(Stadium stadium) throws SQLException {
        if (stadium == null) {
            throw new IllegalArgumentException("El estadio es obligatorio.");
        }

        validate(stadium.getId(), stadium.getName(), stadium.getCity());

        String sql = """
            INSERT INTO estadio (id, nombre, ciudad)
            VALUES (?, ?, ?)
            """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, stadium.getId());
            statement.setString(2, stadium.getName().trim());
            statement.setLong(3, stadium.getCity().getId());

            statement.executeUpdate();
        }
    }

    public boolean update(long id, String name, City city)
            throws SQLException {

        validate(id, name, city);

        String sql = """
            UPDATE estadio
            SET nombre = ?, ciudad = ?
            WHERE id = ?
            """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name.trim());
            statement.setLong(2, city.getId());
            statement.setLong(3, id);

            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteById(long id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del estadio debe ser positivo."
            );
        }

        String sql = "DELETE FROM estadio WHERE id = ?";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            return statement.executeUpdate() == 1;
        }
    }

    private void validate(long id, String name, City city) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del estadio debe ser positivo."
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del estadio es obligatorio."
            );
        }

        if (city == null || city.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Seleccioná una ciudad existente."
            );
        }
    }
}