package org.example;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Scanner;
import java.io.IOException;

public class MenuSystem {

    private final BufferedReader in;
    private final PrintWriter out;
    private final Scanner scanner;
    private final Gson gson = new Gson();
    private volatile boolean isHost;
    private volatile boolean running = true;

    public MenuSystem(BufferedReader in, PrintWriter out, Scanner scanner) {
        this.in = in;
        this.out = out;
        this.scanner = scanner;
        this.isHost = false;
    }

    public void start() {
        // Uruchom wątek nasłuchujący
        Thread serverListenerThread = new Thread(this::listenToServer);
        serverListenerThread.start();

        // Uruchom główną pętlę programu
        run();

        // Zatrzymaj wątek nasłuchujący
        running = false;
        try {
            serverListenerThread.join();
        } catch (InterruptedException e) {
            System.err.println("Błąd podczas zamykania wątku nasłuchującego: " + e.getMessage());
        }
    }

    private void listenToServer() {
        try {
            String response;
            while (running && (response = in.readLine()) != null) {
                System.out.println("Odebrano od serwera: " + response); // Log odebranych wiadomości

                // Obsługa wiadomości od serwera
                if (response.equals("Jesteś hostem")) {
                    isHost = true;
                    System.out.println("Teraz jesteś hostem.");
                    System.out.println("Wpisz 'start' aby rozpocząć grę.");
                } else if (response.startsWith("Nowy host to: ")) {
                    String newHostNickname = response.substring("Nowy host to: ".length());
                    System.out.println("Nowy host to: " + newHostNickname);
                } else if (response.contains("Opuszczono lobby")) {
                    // Obsługa opuszczenia lobby
                    isHost = false;
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Błąd podczas odczytu danych z serwera: " + e.getMessage());
            }
        }
    }


    public void run() {
        while (running) {
            System.out.println("Landing Page:");
            System.out.println("1. Stwórz grę");
            System.out.println("2. Dołącz do gry");
            System.out.print("Wybierz opcję (1 lub 2, 'exit' aby zakończyć): ");
            String choice = scanner.nextLine();

            if ("exit".equalsIgnoreCase(choice)) {
                sendExitRequest();
                running = false;
                break;
            }

            switch (choice) {
                case "1":
                    handleCreateGame();
                    isHost = true;
                    // Przejdź do lobby po stworzeniu gry
                    enterLobby();
                    break;
                case "2":
                    handleJoinGame();
                    isHost = false;
                    // Przejdź do lobby po dołączeniu do gry
                    enterLobby();
                    break;
                default:
                    System.out.println("Nieprawidłowy wybór.");
            }
        }
    }

    private void enterLobby() {
        System.out.println("Jesteś w Lobby. Oczekuj na innych graczy...");

        if (isHost) {
            System.out.println("Wpisz 'start' aby rozpocząć grę.");
        }

        System.out.println("Wpisz 'return' aby wrócić do strony startowej.");

        while (running) {
            String input = scanner.nextLine();
            if ("return".equalsIgnoreCase(input)) {
                sendLeaveLobbyRequest();
                isHost = false;
                break;
            } else if ("start".equalsIgnoreCase(input) && isHost) {
                sendStartGameRequest();
                break;
            } else {
                System.out.println("Nieznane polecenie.");
            }
        }
    }

    private void handleCreateGame() {
        System.out.println("Wybierz typ gry:");
        System.out.println("1. Open");
        System.out.println("2. Close");
        System.out.print("Twój wybór: ");
        String gameType = scanner.nextLine().equals("1") ? "open" : "close";

        JsonObject request = new JsonObject();
        request.addProperty("action", "create_game");
        request.addProperty("game_type", gameType);
        out.println(gson.toJson(request));
    }

    private void handleJoinGame() {
        System.out.println("Dołącz do gry:");
        System.out.println("1. Random Game");
        System.out.println("2. Enter by Code");
        System.out.print("Wybierz opcję (1 lub 2): ");
        String joinChoice = scanner.nextLine();

        if (joinChoice.equals("1")) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "join_game_random");
            out.println(gson.toJson(request));
        } else if (joinChoice.equals("2")) {
            System.out.print("Podaj kod gry: ");
            String gameCode = scanner.nextLine();
            JsonObject request = new JsonObject();
            request.addProperty("action", "join_game_by_code");
            request.addProperty("game_code", gameCode);
            out.println(gson.toJson(request));
        } else {
            System.out.println("Nieprawidłowy wybór.");
        }
    }

    private void sendLeaveLobbyRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "leave_lobby");
        out.println(gson.toJson(request));
    }

    private void sendExitRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "exit");
        out.println(gson.toJson(request));
    }

    private void sendStartGameRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "start_game");
        out.println(gson.toJson(request));
    }
}
