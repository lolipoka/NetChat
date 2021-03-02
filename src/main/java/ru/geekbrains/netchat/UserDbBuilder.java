package ru.geekbrains.netchat;

import java.sql.*;

public class UserDbBuilder {

    private static final String DB_NAME = "users.db";

    private Connection connect() {

        String url = "jdbc:sqlite:src/main/resources/" + DB_NAME;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public void createUsersTable() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS users (\n"
                    + "	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,\n"
                    + "	login text NOT NULL,\n"
                    + "	pass text NOT NULL,\n"
                    + "	nick text NOT NULL\n"
                    + ");";

        try (Connection conn = this.connect();
             Statement statement = conn.createStatement()) {
            statement.execute(createTableQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fillUsersTable() {
        String insertQuery = "INSERT INTO users (login, pass, nick) VALUES (?, ?, ?)";

        try (Connection conn = this.connect();
             PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            for (int i = 1; i <= 3; i++) {
                statement.setString(1, "login" + i);
                statement.setString(2, "pass" + i);
                statement.setString(3, "nick" + i);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        UserDbBuilder builder = new UserDbBuilder();
        builder.connect();
        builder.createUsersTable();
        builder.fillUsersTable();
    }
}
