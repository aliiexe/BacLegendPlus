package com.emsi.baclegend.service;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServiceReseau {

    // Server-side
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Client-side
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    // Common
    private volatile boolean isConnected = false;
    private volatile boolean isServerRunning = false;
    private Thread serverThread;
    private Thread clientThread; // Thread that reads messages as a Client
    private MessageCallback messageCallback;

    private String myPseudo;
    // opponentPseudo is less relevant in multi-mode, but kept for compatibility or
    // last-sender
    private String opponentPseudo;

    public interface MessageCallback {
        void onMessageReceived(String message); // Server: receives from any client. Client: receives from server.

        void onConnectionEstablished();

        void onConnectionFailed(String error);

        // New callback for disconnection
        default void onClientDisconnected(String pseudo) {
        }
    }

    public void setMessageCallback(MessageCallback callback) {
        this.messageCallback = callback;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    public void setMyPseudo(String p) {
        this.myPseudo = p;
    }

    public String getMyPseudo() {
        return myPseudo;
    }

    public String getOpponentPseudo() {
        return opponentPseudo;
    }

    public int getLocalPort() {
        return (serverSocket != null) ? serverSocket.getLocalPort() : -1;
    }

    public int getClientCount() {
        return clients.size();
    }

    // --- HOST / SERVER LOGIC ---

    public void demarrerServeur(int port) throws IOException {
        if (isServerRunning) {
            // Already running
            return;
        }
        fermerConnexion();

        // Synchronous Bind: Ensures port is valid immediately
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        isServerRunning = true;
        isConnected = true; // Host assumes connected state

        System.out.println("Serveur démarré sur le port " + serverSocket.getLocalPort());

        serverThread = new Thread(() -> {
            try {
                while (isServerRunning && !serverSocket.isClosed()) {
                    try {
                        Socket s = serverSocket.accept();
                        System.out.println("Nouveau client connecté: " + s.getInetAddress());
                        ClientHandler handler = new ClientHandler(s);
                        clients.add(handler);
                        handler.start();
                        if (messageCallback != null) {
                            messageCallback.onConnectionEstablished();
                        }
                    } catch (IOException e) {
                        if (isServerRunning)
                            System.err.println("Erreur accept: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                // Outer loop error
            } finally {
                isServerRunning = false;
                isConnected = false;
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void broadcast(String msg) {
        // En tant que Server, envoyer à tous les clients
        for (ClientHandler c : clients) {
            c.sendMessage(msg);
        }
    }

    public void broadcastExclude(ClientHandler exclude, String msg) {
        for (ClientHandler c : clients) {
            if (c != exclude)
                c.sendMessage(msg);
        }
    }

    // --- CLIENT LOGIC ---

    public void connecterAuServeur(String ip, int port) {
        fermerConnexion();
        clientThread = new Thread(() -> {
            try {
                clientSocket = new Socket(ip, port);
                System.out.println("Connecté au serveur " + ip);
                setupClientStreams();
                isConnected = true;
                if (messageCallback != null)
                    messageCallback.onConnectionEstablished();
                lireMessagesClient();
            } catch (IOException e) {
                isConnected = false;
                notifyError("Connexion échouée: " + e.getMessage());
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();
    }

    private void setupClientStreams() throws IOException {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    private void lireMessagesClient() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleIncomingMessage(line);
            }
        } catch (IOException e) {
            isConnected = false;
        }
    }

    // --- COMMON ---

    public void envoyerMessage(String msg) {
        if (isServerRunning) {
            // Si je suis Serveur, envoyerMessage = Broadcast (par défaut)
            // Ou alors le contrôleur appelle explicitement broadcast().
            // Pour compatibilité avec le code existant (GameController.envoyerMessage),
            // assumons que l'hôte envoie à TOUS les clients.
            broadcast(msg);
        } else {
            // Client
            if (out != null)
                out.println(msg);
        }
    }

    private void handleIncomingMessage(String msg) {
        if (msg.startsWith("NAME:")) {
            opponentPseudo = msg.substring(5).trim();
        }
        if (messageCallback != null) {
            messageCallback.onMessageReceived(msg);
        }
    }

    private void notifyError(String err) {
        if (messageCallback != null)
            messageCallback.onConnectionFailed(err);
    }

    public void fermerConnexion() {
        try {
            isConnected = false;
            isServerRunning = false;

            // Fermer Serveur
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler c : clients)
                c.close();
            clients.clear();

            // Fermer Client
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (clientSocket != null && !clientSocket.isClosed())
                clientSocket.close();

        } catch (Exception ignored) {
        }
    }

    // --- INNER CLASS POUR GERER CHAQUE CLIENT (SERVER SIDE) ---
    private class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String pseudo = "Unknown"; // Pseudo storage

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Update pseudo if we see NAME:
                    if (line.startsWith("NAME:")) {
                        this.pseudo = line.substring(5).trim();
                    }
                    // Quand le serveur reçoit un message d'un client
                    // On le passe au Controller
                    handleIncomingMessage(line);
                }
            } catch (IOException e) {
                // Client disconnected
            } finally {
                close();
                clients.remove(this);
                // Notify disconnection
                if (messageCallback != null) {
                    final String disconnectedPseudo = this.pseudo;
                    javafx.application.Platform
                            .runLater(() -> messageCallback.onClientDisconnected(disconnectedPseudo));
                }
            }
        }

        public void sendMessage(String msg) {
            if (writer != null)
                writer.println(msg);
        }

        public void close() {
            try {
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
