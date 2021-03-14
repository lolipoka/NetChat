package ru.geekbrains.netchat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.stream.Stream;

public class MyClient extends JFrame {
    private static final String WRONG_CREDENTIALS = "Неверные логин/пароль";
    private static final String CREDENTIALS_IN_USE = "Учетная запись уже используется";
    private static final int HISTORY_LINES_TO_SHOW = 100;

    private JTextField msgInputField;
    private JTextArea chatArea;
    private JTextField loginField;
    private JPasswordField passField;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String userHome = System.getProperty("user.home");
    private String chatLogName;
    private BufferedWriter writer;

    public MyClient() {
        prepareGUI();
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
            public void windowClosing(WindowEvent event) {
                super.windowClosing(event);
                try {
                    out.writeUTF("/end");
                    closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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
            writer.close();
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
            msgInputField.grabFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread t = new Thread(this::run);
            t.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Не удалось подключиться к серверу");
            e.printStackTrace();
        }
    }

    private void run() {
        String nick;
        try {
            while (true) {
                if (in.available() > 0) {
                    String str = in.readUTF();
                    if (str.startsWith("/authok ") || str.startsWith("/changeNickOK ")) {
                        nick = str.split("\\s")[1];
                        setTitle(String.format("Клиент: %s", nick));
                        setChatLogName(nick);
                        chatArea.setText("");
                        printLog();
                        writer = new BufferedWriter(new FileWriter(chatLogName, true));
                        continue;
                    }
                    if (str.equals(WRONG_CREDENTIALS) || str.equals(CREDENTIALS_IN_USE)) {
                        loginField.grabFocus();
                    }
                    chatArea.append(str + "\n");
                    if (writer != null) {
                        writer.write(str);
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                setTitle("Клиент");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setChatLogName(String nick) {
        chatLogName = String.format("%s%shistory_%s.txt", userHome, File.separator, nick);
    }

    private void printLog() {
        File chatLog = new File(chatLogName);
        if (chatLog.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(chatLog))) {
                ArrayList<String> linesToPrint = getLinesToPrint(reader);

                int startIndex = Math.max(linesToPrint.size() - HISTORY_LINES_TO_SHOW, 0);
                for (int i = startIndex; i < linesToPrint.size(); i++) {
                    chatArea.append(linesToPrint.get(i) + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<String> getLinesToPrint(BufferedReader reader) {
        Stream<String> fileLines = reader.lines();
        ArrayList<String> linesToPrint = new ArrayList<>();
        fileLines.forEach(linesToPrint::add);
        return linesToPrint;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MyClient::new);
    }
}
