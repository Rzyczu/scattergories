package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private static final int PORT = 12345;
    private static final Map<String, Game> activeGames = new HashMap<>();
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println("Serwer uruchomiony, nasłuchuje na porcie: " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowe połączenie: " + clientSocket.getInetAddress());

                // Uruchomienie nowego wątku do obsługi klienta
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Błąd podczas uruchamiania serwera: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private Player player;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                // Request and set nickname
                out.println("Proszę podać swój pseudonim:");
                String nickname = in.readLine();
                String ip = clientSocket.getInetAddress().toString();
                player = new Player(nickname, ip);

                // Welcome the player
                out.println("Witaj, " + player.getNickname() + "!");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Otrzymano od " + player.getNickname() + ": " + inputLine);

                    // Parse input using Gson
                    JsonObject jsonInput = gson.fromJson(inputLine, JsonObject.class);
                    String action = jsonInput.get("action").getAsString();

                    switch (action) {
                        case "create_game":
                            handleCreateGame(jsonInput, out);
                            break;
                        case "join_game":
                            handleJoinGame(jsonInput, out);
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
            Game newGame = new Game(player.getNickname());
            activeGames.put(newGame.getId(), newGame);

            out.println("Stworzono nową grę (" + gameType + ") z kodem: " + newGame.getCode());
            System.out.println("Gra utworzona przez " + player.getNickname() + " z kodem: " + newGame.getCode());
        }

        private void handleJoinGame(JsonObject jsonInput, PrintWriter out) {
            String gameCode = jsonInput.get("game_code").getAsString();
            Game game = activeGames.get(gameCode);

            if (game != null) {
                game.getPlayers().add(player.getNickname());
                out.println("Dołączono do gry: " + gameCode);
                System.out.println(player.getNickname() + " dołączył do gry: " + gameCode);
            } else {
                out.println("Gra o kodzie " + gameCode + " nie istnieje.");
            }
        }
    }
}
