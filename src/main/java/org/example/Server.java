package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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

    public static void main(String[] args) {
        System.out.println("Serwer uruchomiony, nasłuchuje na porcie: " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowe połączenie: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                allHandlers.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Błąd podczas uruchamiania serwera: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private Player player;
        private Game currentGame;
        private PrintWriter out;

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

                sendJsonMessage("prompt_nickname", "Proszę podać swój pseudonim:");
                String nickname = in.readLine();
                String ip = clientSocket.getInetAddress().toString();
                player = new Player(nickname, ip);

                sendJsonMessage("welcome", "Witaj, " + player.getNickname() + "!");

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
                        case "exit":
                            sendJsonMessage("disconnect", "Zamykanie połączenia...");
                            return;
                        default:
                            sendJsonMessage("error", "Nieznane działanie: " + action);
                    }
                }

                System.out.println("Zamykam połączenie z klientem: " + player.getNickname());
            } catch (IOException e) {
                System.err.println("Błąd w komunikacji z klientem: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Błąd podczas zamykania połączenia: " + e.getMessage());
                }
            }
        }

        private void handleCreateGame(JsonObject jsonInput) {
            String gameType = jsonInput.get("game_type").getAsString();
            currentGame = new Game(player);

            if (currentGame.addPlayer(player)) {
                activeGames.put(currentGame.getCode(), currentGame);
                currentGame.setGameType("open".equalsIgnoreCase(gameType) ? Game.Type.OPEN : Game.Type.CLOSE);

                sendJsonMessage("game_created", "Stworzono nową grę (" + gameType + ") z kodem: " + currentGame.getCode());
                sendJsonMessage("lobby", "Przeniesiono do Lobby. Czekaj na innych graczy...");

                System.out.println("Gra utworzona przez " + player.getNickname() + " z kodem: " + currentGame.getCode());
                broadcastPlayerList();
            } else {
                sendJsonMessage("error", "Nie można utworzyć gry.");
            }
        }

        private void handleJoinRandomGame() {
            for (Game game : activeGames.values()) {
                if (game.getType() == Game.Type.OPEN && !game.isFull()) {
                    game.addPlayer(player);
                    currentGame = game;
                    sendJsonMessage("joined_game", "Dołączono do losowej gry: " + game.getCode());
                    sendJsonMessage("lobby", "Przeniesiono do Lobby. Oczekiwanie na rozpoczęcie...");

                    System.out.println(player.getNickname() + " dołączył do gry: " + game.getCode());
                    broadcastPlayerList();
                    return;
                }
            }
            sendJsonMessage("error", "Brak dostępnych gier typu open. Spróbuj później.");
        }

        private void handleJoinGameByCode(JsonObject jsonInput) {
            String gameCode = jsonInput.get("game_code").getAsString();
            currentGame = activeGames.get(gameCode);

            if (currentGame != null) {
                if (currentGame.addPlayer(player)) {
                    sendJsonMessage("joined_game", "Dołączono do gry: " + gameCode);
                    sendJsonMessage("lobby", "Przeniesiono do Lobby. Oczekiwanie na rozpoczęcie...");
                    System.out.println(player.getNickname() + " dołączył do gry: " + gameCode);
                    broadcastPlayerList();
                } else {
                    sendJsonMessage("error", "Gra jest pełna.");
                }
            } else {
                sendJsonMessage("error", "Gra o kodzie " + gameCode + " nie istnieje.");
            }
        }

        private void broadcastPlayerList() {
            JsonObject message = new JsonObject();
            message.addProperty("action", "update_lobby");
            message.add("players", gson.toJsonTree(currentGame.getPlayerNicknames()));

            for (ClientHandler handler : getAllHandlersInGame(currentGame)) {
                handler.out.println(message.toString());
            }
        }

        private void sendJsonMessage(String action, String message) {
            JsonObject jsonMessage = new JsonObject();
            jsonMessage.addProperty("action", action);
            jsonMessage.addProperty("message", message);
            out.println(jsonMessage.toString());
        }

        private List<ClientHandler> getAllHandlersInGame(Game game) {
            List<ClientHandler> handlers = new ArrayList<>();
            for (ClientHandler handler : allHandlers) {
                if (handler.currentGame == game) {
                    handlers.add(handler);
                }
            }
            return handlers;
        }

        private void handleStartGame() {
            if (currentGame != null && currentGame.getHost().equals(player)) {
                if (currentGame.getPlayers().size() >= 2) {
                    broadcastGameStart();
                    System.out.println("Gra została rozpoczęta przez " + player.getNickname());
                    startNewRound(1);
                } else {
                    sendJsonMessage("error", "Gra wymaga co najmniej 2 graczy, aby rozpocząć.");
                }
            } else {
                sendJsonMessage("error", "Tylko host może rozpocząć grę.");
            }
        }

        private void broadcastGameStart() {
            JsonObject message = new JsonObject();
            message.addProperty("action", "game_started");
            message.addProperty("message", "Gra została rozpoczęta!");

            for (ClientHandler handler : getAllHandlersInGame(currentGame)) {
                handler.out.println(message.toString());
            }
        }

        private void startNewRound(int roundNumber) {
            // Losowanie litery
            char letter = (char) ('A' + new Random().nextInt(26));

            // Tworzenie JSON-a z informacją o nowej rundzie
            JsonObject message = new JsonObject();
            message.addProperty("action", "new_round");
            message.addProperty("round_number", roundNumber);
            message.addProperty("letter", String.valueOf(letter));

            // Wysyłanie do wszystkich klientów w aktualnej grze
            for (ClientHandler handler : getAllHandlersInGame(currentGame)) {
                handler.out.println(message.toString());
            }
            System.out.println("Rozpoczęto rundę " + roundNumber + " z literą: " + letter);
        }
    }
}
