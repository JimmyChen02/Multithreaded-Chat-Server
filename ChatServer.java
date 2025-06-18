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
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>(); // map users to their client handlers
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
        for (ClientHandler client : clients.values()) { // loop through all connected clients
            // Check for null username before calling equals
            String clientUsername = client.getUsername();
            if (clientUsername != null && !clientUsername.equals(senderUsername)) { // skip the sender, send to everyone else
                client.sendMessage(formattedMessage);
            }
        }
        logMessage("BROADCAST from " + senderUsername + ": " + message);
    }

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
        return false;
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
            userList.append("  ~ ").append(username).append("\n");
        }
        return userList.toString().trim();
    }
    

    /*
     * client management methods
     */


    // add new client to server
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

    /**
     * Remove a client from the server
     */
    public static synchronized void removeClient(String username, Socket socket) {
        // Only remove from clients map if username is not null
        if (username != null) {
            clients.remove(username);
            broadcastMessage(username + " left the chat.", SERVER_NAME);
            logMessage("User " + username + " disconnected");
        }
        
        // Always try to remove the socket mapping
        if (socket != null) {
            socketToUsername.remove(socket);
        }
    }

    /*
     * Change a user's username
     */
    public static synchronized boolean changeUsername(String oldUsername, String newUsername, ClientHandler clientHandler) {
        if (clients.containsKey(newUsername)) {
            return false; // New username already taken
        }
        
        clients.remove(oldUsername);
        clients.put(newUsername, clientHandler);
        socketToUsername.put(clientHandler.getSocket(), newUsername);
        
        broadcastMessage( oldUsername + " is now known as " + newUsername, SERVER_NAME);
        logMessage("User " + oldUsername + " changed name to " + newUsername);
        
        return true;
    }

    /*
     * Get current timestamp
     */
    private static String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    /*
     * Log server messages with timestamp
     */
    public static void logMessage(String message) {
        System.out.println("[" + getCurrentTime() + "] " + message);
    }
}

/*
 * ClientHandler class runs own thread for each connected client
 */

class ClientHandler implements Runnable { // runnable has run()
    private Socket socket; 
    private BufferedReader input; //from client
    private PrintWriter output; // to client
    private String username;
    private volatile boolean shouldQuit = false;


    public ClientHandler(Socket socket) {
            this.socket = socket;
        }

    @Override
    public void run() {
        try {
            // set up in/output streams
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));  //socket.getinput... (returns raw bytes from client) ~> InuptStream... (converts bytes to chars) ~> Buffered... (read line by line)
            output = new PrintWriter(socket.getOutputStream(), true); // send text to client, true enables auto-flush
            
            sendMessage("Welcome to " + ChatServer.class.getSimpleName() + "!");
            sendMessage("Please enter your username:");

            // get username from client
            while (true) {
                String inputUsername = input.readLine();
                if (inputUsername == null) {
                    return; //client dc-ed
                }
                inputUsername = inputUsername.trim();
                if (inputUsername.isEmpty()) {
                    sendMessage("Username cannot be empty, please try again:");
                    continue;
                }
                if (ChatServer.addClient(inputUsername, this)) {
                    this.username = inputUsername;
                    sendMessage("Welcome, " + username + "! You're now connected to the chat.");
                    sendMessage("Commands: /list (users), /whisper <user> <msg> (private), /nick <name> (change name), /quit (exit)");
                    sendMessage("Start chatting! Your messages will be broadcasted to everyone!");
                    break;
                } else {
                    sendMessage("Username '" + inputUsername + "' is already taken. Please choose another:");
            }
        }
        // Main message loop
            String message;
            while (!shouldQuit && (message = input.readLine()) != null) {
                handleMessage(message.trim());
            }
    } catch (IOException e) {
            ChatServer.logMessage("Error handling client " + username + ": " + e.getMessage());
    } finally {
            disconnect();
        }
    }


    /*
     * Handle incoming messages and commands
     */
    private void handleMessage(String message) {
        if (message.isEmpty()) {
            return;
        }

        // Handle commands
        if (message.startsWith("/")) {
            handleCommand(message);
        } else {
            // Regular chat message - broadcast to everyone
            ChatServer.broadcastMessage(username + ": " + message, username);
        }
    }

    /*
     * Handle chat commands
     */

    private void handleCommand(String command) {
        String[] parts = command.split(" ", 3);
        String cmd = parts[0].toLowerCase();
        
        switch (cmd) {
            case "/quit":
                sendMessage("Goodbye, " + username + " :(");
                shouldQuit = true;
                break;
            case "/list":
                sendMessage(ChatServer.getUserList());
                break;
            case "/whisper":
                if (parts.length < 3) {
                    sendMessage("Usage: /whisper <username> <message>");
                } else {
                    String targetUser = parts[1];
                    String privateMessage = parts[2];

                    if (!ChatServer.sendPrivateMessage(username, targetUser, privateMessage)) {
                        sendMessage("User '" + targetUser + "' is not found.");
                    }
                }
                break;
            case "/nick":
                if (parts.length < 2) {
                    sendMessage("Usage: /nick <new_username>");
                } else {
                    String newUsername = parts[1];
                    if (ChatServer.changeUsername(username, newUsername, this)) {
                        sendMessage("Your username has been changed to: " + newUsername);
                        this.username = newUsername;
                    } else {
                        sendMessage("Username '" + newUsername + "' is already taken.");
                    }
                }
                break;
            case "/help":
                sendMessage("Available Commands:");
                sendMessage("  /list - Show connected users");
                sendMessage("  /whisper <user> <msg> - Send private message");
                sendMessage("  /nick <name> - Change your username");
                sendMessage("  /quit - Leave the chat");
                sendMessage("  /help - Show this help message");
                break;
                    
            default:
                sendMessage("Unknown command: " + cmd + ". Type /help for available commands.");
        }
    }

    /* Send a message to this client */

    public void sendMessage(String message) {
        if (output != null) {
            output.println(message);
        }
    }

    /* disconnect client */

    private void disconnect() {
       try {
        // Remove client from server maps
        ChatServer.removeClient(username, socket);
        
        // Close streams
        if (input != null) {
            input.close();
        }
        if (output != null) {
            output.close();
        }
        
        // Close socket
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        } catch (IOException e) {
            String userInfo = (username != null) ? username : "unknown user";
            ChatServer.logMessage("Error closing socket for " + userInfo + ": " + e.getMessage());
        }
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public Socket getSocket() {
        return socket;
    }
}