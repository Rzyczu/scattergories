package org.example;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    private static final int PORT = 12121;
    private static final Map<String, Game> activeGames = Collections.synchronizedMap(new HashMap<>());
    private static final Gson gson = new Gson();
    private static final List<ClientHandler> allHandlers = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> categoriesList = List.of("Country", "City", "Animal", "Plant", "Food");

    // Entry point for starting the server
    public static void main(String[] args) {
        System.out.println("Server started, listening on port: " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                allHandlers.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    // ClientHandler class to manage individual client connections and communications
    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private Player player;
        private Game currentGame;
        private PrintWriter out;

        // Initialize ClientHandler with the client's socket
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                this.out = out;

                sendJsonMessage("prompt_nickname", "Please enter your nickname:");
                String nickname = in.readLine();
                String ip = clientSocket.getInetAddress().toString();
                player = new Player(nickname, ip);

                sendJsonMessage("welcome", "Welcome, " + player.getNickname() + "!");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    JsonObject jsonInput = gson.fromJson(inputLine, JsonObject.class);
                    String action = jsonInput.get("action").getAsString();

                    switch (action) {
                        case "create_game":
                            handleCreateGame(jsonInput);
                            break;
                        case "join_game_random":
                            handleJoinRandomGame();
                            break;
                        case "join_game_by_code":
                            handleJoinGameByCode(jsonInput);
                            break;
                        case "start_game":
                            handleStartGame();
                            break;
                        case "submit_answers":
                            handleSubmitAnswers(jsonInput);
                            break;
                        case "exit":
                            sendJsonMessage("disconnect", "Closing connection...");
                            return;
                        default:
                            sendJsonMessage("error", "Unknown action: " + action);
                    }
                }

                System.out.println("Closing connection with client: " + player.getNickname());
            } catch (IOException e) {
                System.err.println("Communication error with client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }

        // Handles creation of a new game and setting up lobby
        private void handleCreateGame(JsonObject jsonInput) {
            String gameType = jsonInput.get("game_type").getAsString();
            currentGame = new Game(player);

            if (currentGame.addPlayer(player)) {
                activeGames.put(currentGame.getCode(), currentGame);
                currentGame.setGameType("open".equalsIgnoreCase(gameType) ? Game.Type.OPEN : Game.Type.CLOSE);

                sendJsonMessage("game_created", "Created a new game (" + gameType + ") with code: " + currentGame.getCode());
                sendJsonMessage("lobby", "Moved to Lobby. Waiting for other players...");

                System.out.println("Game created by " + player.getNickname() + " with code: " + currentGame.getCode());
                broadcastPlayerList();
            } else {
                sendJsonMessage("error", "Unable to create game.");
            }
        }

        // Handles joining a random open game
        private void handleJoinRandomGame() {
            for (Game game : activeGames.values()) {
                if (game.getType() == Game.Type.OPEN && !game.isFull()) {
                    game.addPlayer(player);
                    currentGame = game;
                    sendJsonMessage("joined_game", "Joined a random game: " + game.getCode());
                    sendJsonMessage("lobby", "Moved to Lobby. Waiting to start...");

                    System.out.println(player.getNickname() + " joined the game: " + game.getCode());
                    broadcastPlayerList();
                    return;
                }
            }
            sendJsonMessage("error", "No open games available. Try again later.");
        }

        // Handles joining a game by a specific code
        private void handleJoinGameByCode(JsonObject jsonInput) {
            String gameCode = jsonInput.get("game_code").getAsString();
            currentGame = activeGames.get(gameCode);

            if (currentGame != null) {
                if (currentGame.addPlayer(player)) {
                    sendJsonMessage("joined_game", "Joined the game: " + gameCode);
                    sendJsonMessage("lobby", "Moved to Lobby. Waiting to start...");
                    System.out.println(player.getNickname() + " joined the game: " + gameCode);
                    broadcastPlayerList();
                } else {
                    sendJsonMessage("error", "Game is full.");
                }
            } else {
                sendJsonMessage("error", "Game with code " + gameCode + " does not exist.");
            }
        }

        // Broadcasts the current player list to everyone in the game
        private void broadcastPlayerList() {
            JsonObject message = new JsonObject();
            message.addProperty("action", "update_lobby");
            message.add("players", gson.toJsonTree(currentGame.getPlayerNicknames()));

            for (ClientHandler handler : getAllHandlersInGame(currentGame)) {
                handler.out.println(message.toString());
            }
        }

        // Sends a JSON message to the client
        private void sendJsonMessage(String action, String message) {
            JsonObject jsonMessage = new JsonObject();
            jsonMessage.addProperty("action", action);
            jsonMessage.addProperty("message", message);
            out.println(jsonMessage.toString());
        }

        // Gets all client handlers for the current game
        private List<ClientHandler> getAllHandlersInGame(Game game) {
            List<ClientHandler> handlers = new ArrayList<>();
            for (ClientHandler handler : allHandlers) {
                if (handler.currentGame == game) {
                    handlers.add(handler);
                }
            }
            return handlers;
        }

        // Handles starting a game if there are enough players
        private void handleStartGame() {
            if (currentGame != null && currentGame.getHost().equals(player)) {
                if (currentGame.getPlayers().size() >= 2) {
                    broadcastGameStart();
                    System.out.println("Game started by " + player.getNickname());
                    startNewRound();
                } else {
                    sendJsonMessage("error", "Game requires at least 2 players to start.");
                }
            } else {
                sendJsonMessage("error", "Only the host can start the game.");
            }
        }

        // Broadcasts the game start message to all players in the game
        private void broadcastGameStart() {
            JsonObject message = new JsonObject();
            message.addProperty("action", "game_started");
            message.addProperty("message", "The game has started!");

            for (ClientHandler handler : getAllHandlersInGame(currentGame)) {
                handler.out.println(message.toString());
            }
        }

        // Starts a new game round and sends the chosen letter to all players
        private void startNewRound() {
            if (currentGame.startRound()) {
                char letter = currentGame.getCurrentLetter();
                int roundNumber = currentGame.getCurrentRound();

                JsonObject message = new JsonObject();
                message.addProperty("action", "new_round");
                message.addProperty("round_number", roundNumber);
                message.addProperty("letter", String.valueOf(letter));

                for (ClientHandler handler : getAllHandlersInGame(currentGame)){
                    handler.out.println(message.toString());
                }

                sendCategoriesToPlayers();
            }
        }

        // Sends the categories for the current round to all players
        private void sendCategoriesToPlayers() {
            JsonObject message = new JsonObject();
            message.addProperty("action", "categories");
            message.add("categories", gson.toJsonTree(categoriesList));

            for (ClientHandler handler : getAllHandlersInGame(currentGame)) {
                handler.out.println(message.toString());
            }
            System.out.println("Categories have been sent to players.");
        }

        // Handles receiving answers from players and logging them
        private void handleSubmitAnswers(JsonObject jsonInput) {
            JsonObject answers = jsonInput.getAsJsonObject("answers");
            currentGame.addPlayerAnswer(player, answers);
            System.out.println("Received answers from player " + player.getNickname() + ": " + answers.toString());

            if (isRoundComplete()) {
                processRoundScores();

                if (currentGame.isGameOver()) {
                    sendFinalScores();
                } else {
                    startNewRound(); // Rozpocznij następną rundę
                }
            }
        }

        private void sendFinalScores() {
            JsonObject finalScoresMessage = new JsonObject();
            finalScoresMessage.addProperty("action", "game_over");
            JsonObject finalScoresJson = new JsonObject();

            for (Map.Entry<Player, Integer> entry : currentGame.getScores().entrySet()) {
                finalScoresJson.addProperty(entry.getKey().getNickname(), entry.getValue());
            }
            finalScoresMessage.add("scores", finalScoresJson);

            for (ClientHandler handler : getAllHandlersInGame(currentGame)) {
                handler.out.println(finalScoresMessage.toString());
            }
            System.out.println("Game over. Final scores sent to all players.");
        }

        // Funkcja sprawdza, czy wszyscy gracze udzielili odpowiedzi
        private boolean isRoundComplete() {
            return currentGame.getPlayerAnswers().size() == currentGame.getPlayers().size();
        }

        // Funkcja przetwarza wyniki rundy i aktualizuje wyniki graczy
        private void processRoundScores() {
            char startingLetter = currentGame.getCurrentLetter(); // 'letter' to wylosowana litera
            Map<Player, Integer> roundScores = currentGame.calculateRoundScores(startingLetter);

            sendRoundScores(roundScores, currentGame.getScores());  // Przesyłanie wyników
        }

        // Funkcja przesyła wyniki rundy do graczy
        private void sendRoundScores(Map<Player, Integer> roundScores, Map<Player, Integer> totalScores) {
            JsonObject resultsMessage = new JsonObject();
            resultsMessage.addProperty("action", "results");

            JsonObject scoresJson = new JsonObject();
            for (Map.Entry<Player, Integer> entry : totalScores.entrySet()) {
                String nickname = entry.getKey().getNickname();
                int totalScore = entry.getValue();
                scoresJson.addProperty(nickname, totalScore);
            }
            resultsMessage.add("scores", scoresJson);

            for (ClientHandler handler : getAllHandlersInGame(currentGame)) {
                handler.out.println(resultsMessage.toString());
            }
        }

        // Funkcja kończy rundę i czyści odpowiedzi graczy
        private void endRound() {
            currentGame.endRound();
            System.out.println("Round ended and scores have been sent to all players.");
        }
    }
}
