package ru.geekbrains.netchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private static final long MINUTE = 60_000L;
    private static final long AUTH_TIMEOUT = 2 * MINUTE;
    private static final long SEND_TIMEOUT = 3 * MINUTE;
    private static final String WRONG_CREDENTIALS = "Неверные логин/пароль";
    private static final String CREDENTIALS_IN_USE = "Учетная запись уже используется";
    private final MyServer myServer;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final DbService dbService;
    private String name;
    private long lastSentTime;


    public String getName() {
        return name;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.dbService = DbService.getInstance();
            this.name = "";

            this.myServer.getThreadPool().execute(this::run);

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
                    sendMsg(WRONG_CREDENTIALS);
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
                        sendMsg(CREDENTIALS_IN_USE);
                    }
                } else {
                    sendMsg(WRONG_CREDENTIALS);
                }
            }
        }
    }

    public void readMessages() throws IOException {
        lastSentTime = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - lastSentTime >= SEND_TIMEOUT) {
                sendMsg("Превышен интервал ожидания отправки нового сообщения. Подключение разорвано.\n");
                out.writeUTF("/end");
                closeConnection();
                return;
            }
            String incomingMessage = in.readUTF();
            if (incomingMessage.startsWith("/")) {
                if (incomingMessage.equals("/end")) {
                    break;
                }
                if (incomingMessage.startsWith("/w ")) {
                    sendPrivateMessage(incomingMessage);
                }
                if (incomingMessage.startsWith("/changeNick ")) {
                    changeNick(incomingMessage);
                }
                continue;
            }
            myServer.broadcastMsg(String.format("%s: %s", name, incomingMessage));
            lastSentTime = System.currentTimeMillis();
        }
    }

    private void sendPrivateMessage(String incomingMessage) {
        String[] tokens = incomingMessage.split("\\s");
        String nick = tokens[1];
        String msg = incomingMessage.substring(4 + nick.length());
        myServer.sendMsgToClient(this, nick, msg);
        lastSentTime = System.currentTimeMillis();
    }

    private void changeNick(String incomingMessage) {
        String[] tokens = incomingMessage.split("\\s");
        String newName;
        try {
            newName = tokens[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            sendMsg("Не указан новый ник");
            return;
        }
        if (newName == null || newName.trim().isEmpty()) {
            sendMsg("Не указан новый ник");
            return;
        }
        if (!myServer.isNickBusy(newName)) {
            boolean success = dbService.changeNick(name, newName);
            if (success) {
                sendMsg(String.format("/changeNickOK %s", newName));
                myServer.broadcastMsg(String.format("%s поменял ник на %s", name, newName));
                name = newName;
            } else {
                sendMsg(String.format("Не удалось поменять ник с %s на %s", name, newName));
            }
        } else {
            sendMsg(CREDENTIALS_IN_USE);
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
