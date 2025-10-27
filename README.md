# Multithreaded Chat Server in Java

A terminal-based, real-time chat application built using Java Sockets and multithreading. Supports private messaging, command handling, and multiple concurrent users with thread-safe design.

![Java](https://img.shields.io/badge/Language-Java-orange) 

---

## Demo

[Watch the demo video](https://youtu.be/CGI9tOv7lh8)


---

## Features

### Core 

- **Multithreaded server**: each client handled on its own thread
- **Real-time message broadcasting** to all connected clients
- **Command-line interface** for both server and clients
- **Supported commands**:
  - `/quit` – leave the chat
  - `/nick <newName>` – change username
  - `/list` – list connected users
  - `/whisper <user> <msg>` – private message
  - `/help` – show command help


### Mid-Level Features 

- **Username management** with duplicate detection
- **Private messaging** between users
- **Live user listing**
- **Timestamped logging** of all messages and events
- **Error handling** for client disconnects and exceptions

### Advanced Touches

- **Thread-safe `ConcurrentHashMap`** for shared state
- **Synchronized methods** for client registration
- **Graceful disconnection** with socket and map cleanup
- **Rich command system** with descriptive error messages

---

## How to Use

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/multithreaded-chat-server.git
cd multithreaded-chat-server
```

### 2. Compile the Code

```bash
javac ChatServer.java ChatClient.java
```

### 3. Run the Server

```bash
java ChatServer
```

### 4. Run One or More Clients

```bash
java ChatClient
```

**Tip**: Use multiple terminal windows to simulate different users.

---

## Use Over LAN or Internet

### On the Server Machine

1. Run `ChatServer.java`
2. Allow port 12345 through firewall
3. Set up port forwarding on your router to your local IP

### 💻 On the Client Machine

1. Edit in `ChatClient.java`:

```java
private static final String SERVER_HOST = "YOUR_PUBLIC_IP";
```

2. Then compile and run:

```bash
javac ChatClient.java
java ChatClient
```

**For LAN use**: Use your server's local IP address instead of public IP.

---

## Code Architecture

### Thread Architecture Visualization

```
SERVER PROCESS:
Main Thread: ServerSocket.accept() → Creates ClientHandler → Starts new thread
    ├── ClientHandler Thread (Alice)   ┐
    ├── ClientHandler Thread (Bob)     ├─ Each handles one client
    ├── ClientHandler Thread (Charlie) ┘
    └── (More threads as clients connect)

CLIENT PROCESS (Alice):
Main Thread: Handles keyboard input → Sends to server
Background Thread: Receives messages from server → Displays immediately

CLIENT PROCESS (Bob):  
Main Thread: Handles keyboard input → Sends to server
Background Thread: Receives messages from server → Displays immediately
```

### Error Handling Flow

```
When client disconnects unexpectedly:
Client closes terminal window
↓
Socket connection breaks
↓
ClientHandler.run(): input.readLine() returns null
↓
Loop exits, finally block runs
↓
disconnect() method called
↓
ChatServer.removeClient() updates maps
↓
Other clients see "Alice left the chat"
```

### Example Use Case

1. **Alice types a message**:
   ```
   Hello everyone!
   ```

2. **ClientHandler on server reads input**:
   ```java
   ChatServer.broadcastMessage("Alice: Hello everyone!", "Alice")
   ```

3. **Other clients receive**:
   ```
   [10:30:15] Alice: Hello everyone!
   ```

4. **Alice doesn't see her own message back** (prevents echo)


---

## Future Ideas

- **Chat rooms** and channel-based messaging
- **GUI client** (Swing or JavaFX)
- **TLS encryption** using SSLSocket
- **Message history** with file or DB persistence
- **Unit tests** for server logic and protocol

---

## File Overview

| File | Description |
|------|-------------|
| `ChatServer.java` | Multithreaded server that handles clients |
| `ChatClient.java` | Terminal-based client that connects to server |
| `ClientHandler` | Manages one connected user (runs in a thread) |

---


## Author

**Jimmy Chen**  
jc3673@cornell.edu  
[LinkedIn](https://www.linkedin.com/in/jimmychen02/)  
