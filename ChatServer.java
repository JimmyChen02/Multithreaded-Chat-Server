import java.io.*; // import output operation streams
import java.net.*;  //network operation streams
import java.time.LocalDateTime; //for date and time
import java.time.format.DateTimeFormatter; //for formatting date and time
import java.util.*; //for collections
import java.util.concurrent.ConcurrentHashMap; // thread-safe version of hash,ap (used for multithreading)

public class ChatServer {
    /*
     * Each client gets their own thread to handle their messages allowing for multiple clients to connect and chat simultaneously (multithreading)
     */
    private static final int PORT = 12345; //port number for the server
    private static final String SERVER_NAME = "ChatServer";
    private static final Map<String, ClientHandler> clients = new ConcurrentHashmap<>(); // map users to their client handlers
    private static final Map<Socket, String> socketToUsername = new ConcurrentHashMap<>(); // map sockets to usernames

    public static void main(String[] args) {
        System.out.println("Starting " + SERVER_NAME + " on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // Creates server that listens on port 12345
            System.out.println("Server is running on port " + PORT + " and waiting for clients..."); 
            while (true) { // server runs indefinitely waiting for clients to connect
                Socket clientSocket = serverSocket.accept(); //pauses until a client connects and when they do it returns Socket for the client

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(clientHandler); // multi-threading
                clientThread.start(); 
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    /*
     * message sending
     */

    public static void broadcastMessage(String message, String senderUsername) {
        String formattedMessage = "[" + getCurrentTime() + "] " + message;
        for (ClientHandler client: clients.values()) { //loop through all connected clients
            if (!client.getUsername().equals(senderUsername)) { //skip the sender, send to everyone else
                client.sendMessage(formattedMessage);
            }
        }
    }
    logMessage("BROADCAST from " + senderUsername + ": " + message)

    /*
     * private messaging 
     */
    public static boolean sendPrivateMessage(String fromUser, String toUser, String message) {
        ClientHandler targetClient = clients.get(toUser); //find recipient

        if (targetClient != null) {
            String privateMsg = "[" + getCurrentTime() + "] " + fromUser + " whispered: " + message;
            targetClient.sendMessage(privateMsg);

            //send confirmation to sender
            ClientHandler senderClient = clients.get(fromUser);
            if (senderClient != null) {
                senderClient.sendMessage("[" + getCurrentTime() + "] Whispered to " + toUser + ": " + message);
            }
            logMessage("PRIVATE from " + fromUser + " to " + toUser + ": " + message);
            return true;
        }
        else {
            senderClient.sendMessage(toUser + " not found");
            return false; //client not found
        }
    }

    /*
     * get a list of all the connected clients
     */
    public static String getUserList() {
        if (clients.isEmpty()) {
            return "No users currently connected";
        }

        StringBuilder userList = new StringBuilder("Connected Users (" + clients.size() + "): \n");
        for (String username: clients.keySet()) {
            userList.append("  ~ ").append(username).append("\n";)
        }
        return userList.toString().trim()
    }

    /*
     * client management methods
     */

    public static synchronized boolean addClient(String username, ClientHandler clientHandler) { //sync to handle multiple user registration at same time, handles one at a time
        if (clients.containsKey(username)) { 
            return false; //username already taken
        }
        clients.put(username, clientHandler);
        socketToUsername.put(clientHandler.getSocket(), username);
        broadcastMessage(username + " joined the chat!", SERVER_NAME);
        logMessage("User " + username + " joined the chat");
        return true;
    }
}



