package main;

import javax.crypto.SecretKey;
import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends JFrame {
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private SecretKey key;     //SHOULD BE TYPE SECRETKEY
    private JTextArea textArea;
    private JTextField textField;
    private String username;
    private static List<String> userList = new ArrayList<>();

    public Client(String hostname, int port) {
        // Create GUI
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        

        // Text area
        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Text field
        textField = new JTextField();
        add(textField, BorderLayout.SOUTH);
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(textField.getText());
                textField.setText("");
            }
        });

        // Menu to open lobby
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        final JMenu lobby = new JMenu("Lobby");
        menuBar.add(lobby);
        JMenuItem lobbyItem = new JMenuItem("");
        lobby.add(lobbyItem);
        for(String user : userList){
            lobby.add(new JMenuItem(user));
        }



        // Connect to server
        try {
            socket = new Socket(hostname, port);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            do{
                username = JOptionPane.showInputDialog(Client.this, "Enter your username:", "Username", JOptionPane.PLAIN_MESSAGE);
                if (username == null || username.trim().isEmpty() || username.equalsIgnoreCase("Server") || username.equalsIgnoreCase("")){
                    JOptionPane.showMessageDialog(Client.this, "Error! Forbidden Username");
                }
                if(userList.contains(username)){
                    JOptionPane.showMessageDialog(Client.this, "Error! Username already in use");
                }
            } while (username == null || username.trim().isEmpty() || username.equals("Server") || username.equalsIgnoreCase("") || userList.contains(username));
    
            setTitle("Chat "+username);

            sendMessage("#NEWUSER:"+username);

            // Read messages from the server in a new thread
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        String serverMessage;
                        while ((serverMessage = input.readLine()) != null) {        //Keep reading server messages
                            if(serverMessage.contains("#NEWUSER:")){    //If new user, add it to the list and to the lobby tab
                                JMenuItem newUser = new JMenuItem(addUser(serverMessage));
                                lobby.add(newUser);
                            } else if(serverMessage.contains("#KEY")){
                                //key = extractKey(serverMessage);
                            } else if(serverMessage.contains("#NEWLIST")){      //If server says to create new list (client was erased or added)
                                lobby.removeAll();
                                userList = new ArrayList<String>();
                            } else if(serverMessage.contains("#REFRESH:")){
                                addUserToList(serverMessage);
                            } else if(serverMessage.contains("#DONEREFRESHING")){   //Stop refreshing means we can create the new lobby list
                                for(String user : userList){
                                    lobby.add(new JMenuItem(user));
                                }
                            } else {
                                serverMessage = decryptMessage(serverMessage);
                                textArea.append(serverMessage + "\n");      //Regular messages are added from the input to the text area
                            }
                        }
                    } catch (IOException e) {
                        return;
                    }
                }

                
            });
            thread.start();
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {      //On window close, terminate client and close socket with the server.
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private String decryptMessage(String message) {
        try {
            //String decryptedMessage = Encrypt.decrypt(message, key);  We don't have the key so we can't decrypt
            return message;     //Should be decryptedMessage if it worked
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    private String extractKey(String message) {
        int separatorIndex = message.indexOf(':');      //Split the message through the ':'
        if (separatorIndex != -1) {
            String u = message.substring(separatorIndex + 1).trim();    //Contains username
            return u;
        }
        return "";
    }

    private void sendMessage(String message) {      //DECISION: Message is "sender: message"
        if (message != null && !message.trim().isEmpty()) {
            output.println(username + ": " + message);

        }
    }

    private String addUser(String message) {
        int separatorIndex = message.indexOf(':');      //Split the message through the ':'
        if (separatorIndex != -1) {
            String u = message.substring(separatorIndex + 1).trim();    //Contains username
            userList.add(u);
            return u;
        }
        return "";
    }

    private void addUserToList(String message) {
        int separatorIndex = message.indexOf(':');      //Split the message through the ':'
        if (separatorIndex != -1) {
            String u = message.substring(separatorIndex + 1).trim();    //Contains username
            if(!userList.contains(u)){
                userList.add(u);
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 8888);      //Connect to port 8888 from localhost
        client.setVisible(true);    //Set gui visible
    }
}

