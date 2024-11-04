package org.example;

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

                    if ("exit".equalsIgnoreCase(inputLine)) {
                        out.println("Zamykanie połączenia...");
                        break;
                    } else if (inputLine.startsWith("New Game: Close")) {
                        // Create a new closed game
                        Game newGame = new Game(player.getNickname());
                        activeGames.put(newGame.getId(), newGame);

                        // Send the generated code to the client
                        out.println("Kod gry: " + newGame.getCode());
                        System.out.println("Stworzono nową grę z kodem: " + newGame.getCode() + " przez " + player.getNickname());
                    } else {
                        out.println("Serwer otrzymał: " + inputLine);
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
    }
}
