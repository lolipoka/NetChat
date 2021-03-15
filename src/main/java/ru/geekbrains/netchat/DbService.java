package ru.geekbrains.netchat;

import java.sql.*;

public final class DbService {
    private static final String DB_NAME = "users.db";
    private static DbService instance;

    private DbService() {
    }

    public static synchronized DbService getInstance() {
        if (instance == null) {
            instance = new DbService();
        }
        return instance;
    }

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

    private void createUsersTable() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS users (\n"
                + "	id integer PRIMARY KEY AUTOINCREMENT NOT NULL,\n"
                + "	login text NOT NULL,\n"
                + "	pass text NOT NULL,\n"
                + "	nick text NOT NULL\n"
                + ");";

        try (Connection conn = instance.connect();
             Statement statement = conn.createStatement()) {
            statement.execute(createTableQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fillUsersTable() {
        /* Для исключения повторного создания тех же записей.
         * Проверка ника вместо UNIQUE и INSERT OR IGNORE только потому,
         * что при использовании проверки не увеличивается сохраняемое в БД
         * значение счетчика автоинкремента.
         * Можно было, конечно, вместо создания поля id назначить первичным ключом таблицы
         * поле nick, тогда такая проверка не потребовалась бы. */
        String insertQuery = "INSERT INTO users (login, pass, nick)\n"
                + "SELECT ?, ?, ?\n"
                + "WHERE NOT EXISTS (SELECT 1 FROM users WHERE nick = ?);";

        try (Connection conn = instance.connect();
             PreparedStatement statement = conn.prepareStatement(insertQuery)) {
            for (int i = 1; i <= 3; i++) {
                statement.setString(1, "login" + i);
                statement.setString(2, "pass" + i);
                statement.setString(3, "nick" + i);
                statement.setString(4, "nick" + i);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createDatabase() {
        connect();
        createUsersTable();
        fillUsersTable();
    }

    public String getNickByLoginPass(String login, String pass) {
        String nick;
        String selectQuery = "SELECT nick FROM users WHERE login = ? AND pass = ?;";

        try (Connection conn = instance.connect();
             PreparedStatement statement = conn.prepareStatement(selectQuery)) {
            statement.setString(1, login);
            statement.setString(2, pass);
            nick = statement.executeQuery().getString("nick");
        } catch (SQLException e) {
            e.printStackTrace();
            nick = null;
        }
        return nick;
    }

    public boolean changeNick(String oldNick, String newNick) {
        String updateQuery = "UPDATE users SET nick = ? WHERE nick = ?;";

        try (Connection conn = instance.connect();
             PreparedStatement statement = conn.prepareStatement(updateQuery)) {
            statement.setString(1, newNick);
            statement.setString(2, oldNick);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
