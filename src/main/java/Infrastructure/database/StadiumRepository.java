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

    public Stadium create(String name, City city) throws SQLException {
        validateStadium(name, city);
        String sql = """
                INSERT INTO estadio (nombre, ciudad)
                VALUES (?, ?)
                RETURNING id
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            statement.setLong(2, city.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("The stadium could not be created.");
                }
                return new Stadium(resultSet.getLong("id"), name.trim(), city);
            }
        }
    }

    public boolean update(Stadium stadium) throws SQLException {
        if (stadium == null) {
            throw new IllegalArgumentException("A stadium is required.");
        }
        validateId(stadium.getId());
        validateStadium(stadium.getName(), stadium.getCity());
        String sql = """
                UPDATE estadio
                SET nombre = ?, ciudad = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, stadium.getName().trim());
            statement.setLong(2, stadium.getCity().getId());
            statement.setLong(3, stadium.getId());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long stadiumId) throws SQLException {
        validateId(stadiumId);
        String sql = "DELETE FROM estadio WHERE id = ?";
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, stadiumId);
            return statement.executeUpdate() == 1;
        }
    }

    private void validateStadium(String name, City city) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The stadium name is required.");
        }
        if (city == null || city.getId() <= 0) {
            throw new IllegalArgumentException("A city is required.");
        }
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("The stadium ID must be positive.");
        }
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
}
