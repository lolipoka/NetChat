package ru.geekbrains.netchat;

public interface AuthService {
    void start();
    String getNickByLoginPass(String login, String pass);
    void stop();
}
