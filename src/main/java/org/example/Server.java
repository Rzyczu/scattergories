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

    private static final int PORT = 12345;
    private static final Map<String, Game> activeGames = Collections.synchronizedMap(new HashMap<>());
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println("Serwer uruchomiony, nasłuchuje na porcie: " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowe połączenie: " + clientSocket.getInetAddress());

                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Błąd podczas uruchamiania serwera: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private Player player;
        private Game currentGame;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                out.println("Proszę podać swój pseudonim:");
                String nickname = in.readLine();
                String ip = clientSocket.getInetAddress().toString();
                player = new Player(nickname, ip);

                out.println("Witaj, " + player.getNickname() + "!");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    JsonObject jsonInput = gson.fromJson(inputLine, JsonObject.class);
                    String action = jsonInput.get("action").getAsString();

                    switch (action) {
                        case "create_game":
                            handleCreateGame(jsonInput, out);
                            break;
                        case "join_game_random":
                            handleJoinRandomGame(out);
                            break;
                        case "join_game_by_code":
                            handleJoinGameByCode(jsonInput, out);
                            break;
                        case "exit":
                            out.println("Zamykanie połączenia...");
                            return;
                        default:
                            out.println("Nieznane działanie: " + action);
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

        private void handleCreateGame(JsonObject jsonInput, PrintWriter out) {
            String gameType = jsonInput.get("game_type").getAsString();
            currentGame = new Game(player);

            if (currentGame.addPlayer(player)) {
                activeGames.put(currentGame.getCode(), currentGame);
                if ("open".equalsIgnoreCase(gameType)) {
                    currentGame.setGameType(Game.Type.OPEN);
                } else if ("close".equalsIgnoreCase(gameType)) {
                    currentGame.setGameType(Game.Type.CLOSE);
                }
                out.println("Stworzono nową grę (" + gameType + ") z kodem: " + currentGame.getCode());
                out.println("Przeniesiono do Lobby. Czekaj na innych graczy...");
                System.out.println("Gra utworzona przez " + player.getNickname() + " z kodem: " + currentGame.getCode());
            } else {
                out.println("Nie można utworzyć gry.");
            }
        }

        private void handleJoinRandomGame(PrintWriter out) {
            for (Game game : activeGames.values()) {
                if (game.getType() == Game.Type.OPEN && !game.isFull()) {
                    game.addPlayer(player);
                    currentGame = game;
                    out.println("Dołączono do losowej gry: " + game.getCode());
                    out.println("Przeniesiono do Lobby. Oczekiwanie na rozpoczęcie...");
                    System.out.println(player.getNickname() + " dołączył do gry: " + game.getCode());
                    return;
                }
            }
            out.println("Brak dostępnych gier typu open. Spróbuj później.");
        }

        private void handleJoinGameByCode(JsonObject jsonInput, PrintWriter out) {
            String gameCode = jsonInput.get("game_code").getAsString();
            currentGame = activeGames.get(gameCode);

            if (currentGame != null) {
                if (currentGame.addPlayer(player)) {
                    out.println("Dołączono do gry: " + gameCode);
                    out.println("Przeniesiono do Lobby. Oczekiwanie na rozpoczęcie...");
                    System.out.println(player.getNickname() + " dołączył do gry: " + gameCode);
                } else {
                    out.println("Gra jest pełna.");
                }
            } else {
                out.println("Gra o kodzie " + gameCode + " nie istnieje.");
            }
        }
    }
}