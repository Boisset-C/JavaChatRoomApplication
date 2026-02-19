package JavaChatRoomApplication;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author yasin
 */
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

public class ChatServer {

    // we will store a hash map of current active users in chatroom
    // using thread safe concurentHashmap to avoid concurrency issues
    private final Map<String, ChatIncomingClientHandler> activeUsers = new ConcurrentHashMap<>();

    private ServerSocket server = null;

    public ChatServer(int port) {
        try {
            // serve is listening on port 8989, waiting for clients to connect
            server = new ServerSocket(port);

            // allows for the server to reuse the same port number if the server is
            // restarted
            server.setReuseAddress(true);
            System.out.println("Server started on port: " + port);

            // continuously listen for clients to connect
            while (true) {

                // endpoint for the client to connect to the server
                Socket client = server.accept();
                System.out.println("Client accepted" + client.getInetAddress().getHostAddress());

                // create a new thread for each client that connects to the server using helper
                // function
                // we pas this in for context, we tell the clienthandler which server it is
                // working for
                ChatIncomingClientHandler clientHandler = new ChatIncomingClientHandler(client);

                // create and start a new thread for the incoming client
                Thread userThread = new Thread(clientHandler);

                // set a name for the thread
                userThread.setName("Client-" + client.getInetAddress().getHostAddress());

                // attach thread to that current client
                clientHandler.setThread(userThread);

                // print out list of
                System.out.println("Client connected, awaiting username" + activeUsers.toString());

                // activate client
                userThread.start();
            }

        } catch (Exception exception) {
            // if we throw an exception, print stack trace to find where the error happened.
            exception.printStackTrace();
        }

    }

    // loop through all active users and send them client messages
    public void sendToAll(String message) {
        for (ChatIncomingClientHandler user : activeUsers.values()) {
            user.send(message);
        }
    }

    // This is where I will handle incoming clients, I will need to create a new
    // thread for each client that connects to the server and then I will need to
    // read from the input stream of the socket and write to the output stream of
    // the socket and then close the socket and streams when done
    public class ChatIncomingClientHandler implements Runnable {

        private Socket clientSocket = null;
        private Thread thread = null;
        private String username = null;
        private PrintWriter writeToClient = null;

        // construct chatincomingclienthandler class
        public ChatIncomingClientHandler(Socket socket) throws IOException {
            // set socket for appropriate client
            this.clientSocket = socket;
            // send text messages to the connected this client
            this.writeToClient = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        // thread setter
        public void setThread(Thread newThread) {
            this.thread = newThread;
        }

        public void send(String message) {
            if (writeToClient != null) {
                writeToClient.println(message);
            }
        }

        // client loop
        public void run() {

            BufferedReader readFromClient = null;

            try {

                // read incoming client responses from this client
                readFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // prompt client for username first time
                send("ENTER_USERNAME");

                // wait for username reply before accepting other messages
                while (true) {
                    String userInput = readFromClient.readLine();

                    if (userInput == null) {
                        return;
                    }

                    userInput = userInput.trim();
                    // check that '=' exists
                    if (!userInput.contains("=")) {
                        send("Invalid format. Use: username = YourName");
                        send("ENTER_USERNAME");
                        continue;
                    }

                    // split into left and right of '='
                    String[] parts = userInput.split("=", 2);

                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    // check left side is literally "username"
                    if (!key.equalsIgnoreCase("username") || value.isEmpty()) {
                        send("Invalid format. Use: username = YourName");
                        send("ENTER_USERNAME");
                        continue;
                    }

                    // username with valid format has been input
                    this.username = userInput;

                    // map new username to active users, avoid duplicates
                    if (activeUsers.putIfAbsent(username, this) != null) {
                        // reset duplicate
                        this.username = null;
                        send("USERNAME_TAKEN");
                        send("ENTER_USERNAME");
                        continue;
                    }

                    // announce to chatroom that someone has joined
                    ChatServer.this.sendToAll("Server: Welcome " + username);
                    break;
                }

                String traffic;

                while ((traffic = readFromClient.readLine()) != null) {
                    System.out.println("Received from client: " + traffic);
                    ChatServer.this.sendToAll(username + " sent: " + traffic);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {

                    if (username != null) {
                        activeUsers.remove(username);
                        ChatServer.this.sendToAll(username + " left the chat");
                    }

                    if (readFromClient != null)
                        readFromClient.close();
                    if (writeToClient != null)
                        writeToClient.close();
                    if (clientSocket != null)
                        clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public static void main(String args[]) {
        ChatServer serverInstance = new ChatServer(8989);

    }

}
