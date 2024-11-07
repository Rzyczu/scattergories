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
    private static final Map<String, Game> activeGames = new HashMap<>();
    private static final List<Game> openGames = new ArrayList<>();
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
                out.println("Proszę podać swój pseudonim:");
                String nickname = in.readLine();
                String ip = clientSocket.getInetAddress().toString();
                player = new Player(nickname, ip);

                // Logowanie dla dodawania gracza
                System.out.println("Dodano gracza: " + player.getNickname());

                out.println("Witaj, " + player.getNickname() + "!");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    JsonObject jsonInput = gson.fromJson(inputLine, JsonObject.class);
                    String action = jsonInput.get("action").getAsString();

                    System.out.println("Otrzymano akcję od klienta: " + action);

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
                    openGames.add(currentGame);
                }

                System.out.println("Gra stworzona przez " + player.getNickname());
                out.println("Stworzono nową grę (" + gameType + ") z kodem: " + currentGame.getCode());
                out.println("Przeniesiono do Lobby. Czekaj na innych graczy...");
            } else {
                out.println("Nie można utworzyć gry.");
            }
        }

        private void handleJoinRandomGame(PrintWriter out) {
            for (Game game : openGames) {
                if (!game.isFull()) {
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
