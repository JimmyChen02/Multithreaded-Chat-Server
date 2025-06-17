import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean isConnected = false;

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
    public void start() {
        try {
            //connect to server
            System.out.println("Connecting to chat server at " + SERVER_HOST + ":" + SERVER_PORT + "...");
            socket = new Socket(SERVER_HOST, SERVER_PORT); // connects to server's IP and port

            //set up IO (same as server side)
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            isConnected = true;
            System.out.println("Successfully connected to server!");

            // start msg reciever thread
            Thread recieverThread = new Thread(new MessageReceiver());
            recieverThread.setDaemon(true); // Dies when main thread (program) dies
            recieverThread.start();

            // Handle user input
            handleUserInput();
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            System.err.println("Make sure the server is running on " + SERVER_HOST + ":" + SERVER_PORT);
        } finally {
            disconnect();
        }
    }

    /* handle user inputs from console */
    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);

        try {
            while(isConnected && scanner.hasNextLine()) {
                String userInput = scanner.nextLine();

                if(userInput.trim().isEmpty()) {
                    continue;
                }

                //send msg to server
                output.println(userInput);

                //check if user wants to quit
                if(userInput.trim().equalsIgnoreCase("/quit")) {
                    break;
                }
            }
        } catch (Exception e) {
            if(isConnected) {
                System.err.println("Error reading user input: " + e.getMessage());
            }
        }
    }

    /* disconnect from server */

    private void disconnect() {
        isConnected = false;

        try {
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    /* 
     * MessageReceiver ~> Handles incoming messages from server in a separate thread
     */
    private class MessageReceiver implements Runnable {
        @Override 
        public void run() {
            try {
                String message;
                while (isConnected && (message = input.readLine()) != null) {
                    System.out.println(message); //displays msg from server
                }
            } catch (IOException e) {
                if (isConnected) {
                    System.err.println("Connection to server lost: " + e.getMessage());
                    isConnected = false;
                }
            }
        }
    }
}