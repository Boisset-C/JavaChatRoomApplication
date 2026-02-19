/* Socket programming client code */

//Pseudo code for client
//1. Pre-requisite: Server should be running before client starts
//2. Create a socket to connect to the server using the server's IP address and port number: in this case, localhost and 7777.
//3. Get the input and output streams of the socket
//4. Read from the input stream and write to the output stream
//5. Close the socket and streams when done

/*
1) Parse username = ... and reject invalid format
2) On successful username: broadcast Server: Welcome <username>
3) Implement "Bye": broadcast Server: Goodbye <username> then disconnect
4)Implement "AllUsers": reply to requester with current usernames
5) Align client UI prompts so the user actually types username = ...

*/

import java.net.*;
import java.util.Scanner;
import java.io.*;

public class ChatClient {

    // Socket will will be used to connect to the server, takes in the server's IP
    // address and port number as parameters
    private Socket socket = null;
    // Writing to the server using the output stream of the socket
    PrintWriter writeToServer = null;
    // Reading from the server using the input stream of the socket
    BufferedReader readFromServer = null;

    private Boolean awaitingUsername = false;

    public ChatClient(String address, int port) {
        try {

            // create a socket to connect to the server
            socket = new Socket(address, port);
            System.out.println("Client is connected to server at" + address + ", port: " + port);

            // read and write to and from server
            writeToServer = new PrintWriter(socket.getOutputStream(), true);
            readFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // read keyboard inputs
            Scanner scanner = new Scanner(System.in);

            // client loop, listens to server continuously
            Thread clientLoop = new Thread(() -> {
                try {
                    String message = null;
                    while ((message = readFromServer.readLine()) != null) {

                        if ("ENTER_USERNAME".equals(message)) {
                            System.out.print("Enter username: ");
                            awaitingUsername = (true);
                            continue;
                        }

                        if ("USERNAME_TAKEN".equals(message)) {
                            awaitingUsername = (true);
                            System.out.println("That username is taken. Try again.");
                            System.out.println("Enter username: ");
                            continue;
                        }
                        // Normal chat/broadcast output
                        System.out.println(message);
                    }

                } catch (IOException exception) {
                    System.out.println("Disconnected from server.");
                }
            });

            clientLoop.start();

            System.out.println("Type messages. Type 'exit' to quit.");

            // Send user messages to searver
            while (true) {
                String message = scanner.nextLine();
                if ("exit".equalsIgnoreCase(message)) {
                    socket.close();
                    break;
                }
                // If server is asking for username, treat this line as the username
                if (awaitingUsername) {
                    writeToServer.println(message);
                    awaitingUsername = false;
                } else {
                    writeToServer.println(message);
                }
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    };

    public static void main(String args[]) {

        String address = "localhost";
        int port = 8989;

        ChatClient client = new ChatClient(address, port);

    }

}
