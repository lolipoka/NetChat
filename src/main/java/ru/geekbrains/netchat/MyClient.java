package ru.geekbrains.netchat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MyClient extends JFrame {

//    private static final long MINUTE = 60_000L;
//    private static final long AUTH_TIMEOUT = 2 * MINUTE;
//    private static final long SEND_TIMEOUT = 3 * MINUTE;
    private JTextField msgInputField;
    private JTextArea chatArea;
    private JTextField loginField;
    private JPasswordField passField;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isAuthorized;
    //    private long startTime;
//    private long lastSentTime;

    public MyClient() {
        prepareGUI();
//        lastSentTime = System.currentTimeMillis();
    }

    public void prepareGUI() {
        setWindowParameters();
        addAuthPanel();
        addChatArea();
        addInputPanel();
        addWindowClosingAction();
        setVisible(true);
    }

    private void setWindowParameters() {
        setBounds(600, 300, 500, 500);
        setTitle("Клиент");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void addAuthPanel() {
        JPanel authPanel = new JPanel(new BorderLayout());

        loginField = new JTextField("login1", 15);
        loginField.addActionListener(e -> onAuthClick());

        passField = new JPasswordField(15);
        passField.addActionListener(e -> onAuthClick());

        JButton auth = new JButton("Авторизоваться");
        auth.addActionListener(e -> onAuthClick());

        authPanel.add(loginField, BorderLayout.LINE_START);
        authPanel.add(passField, BorderLayout.CENTER);
        authPanel.add(auth, BorderLayout.LINE_END);

        add(authPanel, BorderLayout.NORTH);
    }

    private void addChatArea() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
    }

    private void addInputPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton btnSendMsg = new JButton("Отправить");
        bottomPanel.add(btnSendMsg, BorderLayout.EAST);

        msgInputField = new JTextField();
        add(bottomPanel, BorderLayout.SOUTH);

        bottomPanel.add(msgInputField, BorderLayout.CENTER);

        btnSendMsg.addActionListener(e -> sendMessage());
        msgInputField.addActionListener(e -> sendMessage());
    }

    private void addWindowClosingAction() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    out.writeUTF("/end");
                    closeConnection();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });
    }

    private void setAuthorized(boolean authStatus) {
        isAuthorized = authStatus;
    }

    public void closeConnection() {
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

    public void sendMessage() {
        if (!msgInputField.getText().trim().isEmpty()) {
            try {
                out.writeUTF(msgInputField.getText());
                msgInputField.setText("");
                msgInputField.grabFocus();
//                lastSentTime = System.currentTimeMillis();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка отправки сообщения");
            }
        }
    }

    public void onAuthClick() {
        if (socket == null || socket.isClosed()) {
            start();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + String.valueOf(passField.getPassword()));
            loginField.setText("");
            passField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            setAuthorized(false);
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(this::run);
//            startTime = System.currentTimeMillis();
            t.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Не удалось подключиться к серверу");
            e.printStackTrace();
        }
    }

    private void run() {
        String myNick;
        try {
            while (true) {
                if (in.available() > 0) {
                    String str = in.readUTF();
                    if (str.startsWith("/authok ")) {
                        setAuthorized(true);
                        myNick = str.split("\\s")[1];
                        setTitle(getTitle() + ": " + myNick);
                        continue;
                    }
                    chatArea.append(str + "\n");
                }
//                if (!isAuthorized && System.currentTimeMillis() - startTime >= AUTH_TIMEOUT) {
//                    chatArea.append("Превышено время ожидания авторизации. Подключение разорвано.\n");
//                    out.writeUTF("/end");
//                    closeConnection();
//                    return;
//                }
//                if (System.currentTimeMillis() - lastSentTime >= SEND_TIMEOUT) {
//                    chatArea.append("Превышен интервал ожидания отправки нового сообщения. Подключение разорвано.\n");
//                    out.writeUTF("/end");
//                    closeConnection();
//                    return;
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                setAuthorized(false);
                socket.close();
                myNick = "";
                setTitle("Клиент");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MyClient::new);
    }
}
