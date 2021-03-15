package ru.geekbrains.netchat;

public class BaseAuthService implements AuthService {
    private final DbService dbService;

    @Override
    public void start() {
        System.out.println("Сервис аутентификации запущен");
    }

    @Override
    public void stop() {
        System.out.println("Сервис аутентификации остановлен");
    }


    public BaseAuthService() {
        dbService = DbService.getInstance();

        /* Если база пользователей не существует, создаст её.
         * В случае существующей базы просто подключится к ней.
         * Если таблица пользователей создана, то заново не создаётся.
         * Если пользователи по умолчанию созданы, то заново не создаются. */
        dbService.createDatabase();
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        return dbService.getNickByLoginPass(login, pass);
    }
}
