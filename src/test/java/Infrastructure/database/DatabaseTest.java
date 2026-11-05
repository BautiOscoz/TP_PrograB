package Infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseTest {

    public static void main(String[] args) {

        try (
                Connection connection =
                        DatabaseConnection.connect()
        ) {
            System.out.println(
                    "Connection established successfully."
            );

            System.out.println(
                    "Database: "
                            + connection.getCatalog()
            );

            System.out.println(
                    "Connection closed: "
                            + connection.isClosed()
            );

        } catch (SQLException exception) {
            System.err.println(
                    "Database connection failed: "
                            + exception.getMessage()
            );

        } catch (IllegalStateException exception) {
            System.err.println(
                    "Configuration error: "
                            + exception.getMessage()
            );
        }
    }
}