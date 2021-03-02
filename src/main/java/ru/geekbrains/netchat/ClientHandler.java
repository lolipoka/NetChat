package ru.geekbrains.netchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private static final long MINUTE = 60_000L;
    private static final long AUTH_TIMEOUT = 2 * MINUTE;
    private static final long SEND_TIMEOUT = 3 * MINUTE;
    private final MyServer myServer;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String name;


    public String getName() {
        return name;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "";

            new Thread(this::run).start();
        } catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента");
        }
    }

    private void run() {
        try {
            authentication();
            readMessages();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    public void authentication() throws IOException {
        long startTime = System.currentTimeMillis();
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/auth")) {
                String[] parts = str.split("\\s");
                if (parts.length != 3) {
                    sendMsg("Неверные логин/пароль");
                    continue;
                }
                if (System.currentTimeMillis() - startTime >= AUTH_TIMEOUT) {
                    sendMsg("Превышено время ожидания авторизации. Подключение разорвано.\n");
                    out.writeUTF("/end");
                    closeConnection();
                    return;
                }
                String login = parts[1];
                String password = parts[2];
                String nick = myServer.getAuthService().getNickByLoginPass(login, password);
                if (nick != null) {
                    if (!myServer.isNickBusy(nick)) {
                        sendMsg("/authok " + nick);
                        name = nick;
                        myServer.broadcastMsg(name + " зашел в чат");
                        myServer.subscribe(this);
                        return;
                    } else {
                        sendMsg("Учетная запись уже используется");
                    }
                } else {
                    sendMsg("Неверные логин/пароль");
                }
            }
        }
    }

    public void readMessages() throws IOException {
        long lastSentTime = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - lastSentTime >= SEND_TIMEOUT) {
                sendMsg("Превышен интервал ожидания отправки нового сообщения. Подключение разорвано.\n");
                out.writeUTF("/end");
                closeConnection();
                return;
            }
            String str = in.readUTF();
            if (str.startsWith("/")) {
                if (str.equals("/end")) {
                    break;
                }
                if (str.startsWith("/w ")) {
                    String[] tokens = str.split("\\s");
                    String nick = tokens[1];
                    String msg = str.substring(4 + nick.length());
                    myServer.sendMsgToClient(this, nick, msg);
                    lastSentTime = System.currentTimeMillis();
                }
                continue;
            }
            myServer.broadcastMsg(name + ": " + str);
            lastSentTime = System.currentTimeMillis();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(String.format("%s вышел из чата", name));
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
