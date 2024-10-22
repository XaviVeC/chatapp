
package main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import exceptions.EmptyUserException;
import messages.Encrypt;

public class Server {
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final List<String> clientNames = new ArrayList<>();
    private static boolean adminClient = false;
    private static final SecretKey key = Encrypt.generateKey();

    public static void main(String[] args) {
        int port = 8888; // Port number where the server listens

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);    //Create client with socket connected to server port

                synchronized (clients) {
                    if (!adminClient) {     //If there is no admin, we add one
                        adminClient = true;
                        clientHandler.setAdmin();
                        System.out.println("Admin connected");
                    }
                    clients.add(clientHandler);
                }
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + port);
            System.out.println(e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {     //Multithreaded approach to allow multiple clients to join
        private Socket socket;
        private boolean admin = false;
        private PrintWriter output;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void setAdmin() {
            this.admin = true;
        }

        public boolean getAdmin() {
            return this.admin;
        }

        public String getUsername() {
            return this.username;
        }

        

        public void run() {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                this.output = new PrintWriter(socket.getOutputStream(), true);
                String clientMessage;
                String msg;
                
                while ((clientMessage = input.readLine()) != null) {        //Keep reading client messages
                    this.username=getData(clientMessage, true);
                    clientNames.add(username);
                    msg = getData(clientMessage, false);
                    if (clientMessage.contains("#NEWUSER:")){       //If we receive NEWUSER command, we broadcast it to the other clients
                        broadcastMessage("#NEWLIST");
                        broadcastMessage("Server: "+username + " has joined the chat.");
                        for (ClientHandler client : clients){
                            broadcastMessage("#REFRESH:"+client.getUsername());
                        }
                        broadcastMessage("#DONEREFRESHING");
                    } else if (admin && clientMessage.contains("#GAMESTART")) {     //If the admin calls the game to start
                        broadcastMessage("Game Started!");
                    } else if (msg.startsWith("@")){        //If Mentioning somebody with @, this means PRIVATE MESSAGING
                        handlePrivateMessage(msg, username);
                    } else {
                        broadcastMessage(clientMessage);    //If normal message, broadcast it to public chat
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception caught when trying to interact with a client.");
                System.out.println(e.getMessage());
            } catch (EmptyUserException e) {
                System.out.println(e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Couldn't close the socket");
                }
                System.out.println("Connection with client " +username+" closed");

                synchronized (clients) {
                    clients.remove(this);
                    clientNames.remove(this.username);
                    if (this.getAdmin()) {
                        adminClient = false;
                        System.out.println("Admin disconnected");
                    }
                    broadcastMessage("#NEWLIST");
                    for (ClientHandler client : clients){
                        broadcastMessage("#REFRESH:"+client.getUsername());
                    }
                    broadcastMessage("#DONEREFRESHING");
                    broadcastMessage(username + " has left the chat.");
                }
        }
    }

    private void handlePrivateMessage(String message, String sender) {          //Private messaging
        // Assuming the message format is "sender: @receiver actualMessage"
        String[] parts = message.split(" ", 2);     //Split the message in receiver and actual message

        // Extracting sender, receiver, and message
        String receiver = parts[0].replace("@", "");
        if(!clientNames.contains(receiver)){ 
            broadcastMessage(sender+": "+message);  //If @ is used without a connected client, treat it as a regular message and return
        } else {    //Else it is a private message
            String msg = parts[1];
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client.getUsername().equals(receiver)) {        //Print only in sender and receiver chat
                        client.output.println(this.username + " (private): " + msg);
                    }
                    if (client.getUsername().equals(sender)) {
                        client.output.println(this.username + " (to @"+receiver+"): " + msg);
                    }
                }
            }
        }
    }
    private void broadcastMessage(String message) {     //For each client, print on their output stream (encrypted)
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.output.println(message);
            }
        }
    }
    
    //Warning because it is never used locally
    private void broadcastEncryptedMessage(String message) {     //For each client, print on their output stream (encrypted)
        try {
            String encryptedMsg = Encrypt.encrypt(message, key);
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.output.println(encryptedMsg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }


    private String getData(String message, boolean user) throws EmptyUserException {    //Obtain message or sender depending on the user boolean. True means get sender, false means get message
        String[] parts = message.split(" ", 2);
        String sender = parts[0].replace(":", "");
        String msg = parts[1];
        if(parts[0] == null || parts[1] == null) { throw new EmptyUserException(); }
        if(user){ return sender; } else { return msg; }
    }
}
}