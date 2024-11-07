package org.example;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Scanner;

public class MenuSystem {

    private final BufferedReader in;
    private final PrintWriter out;
    private final Scanner scanner;
    private final Gson gson = new Gson();
    private boolean IsHost = false;

    public MenuSystem(BufferedReader in, PrintWriter out, Scanner scanner) {
        this.in = in;
        this.out = out;
        this.scanner = scanner;
    }

    public void run() {
        while (true) {
            System.out.println("Landing Page:");
            System.out.println("1. Stwórz grę");
            System.out.println("2. Dołącz do gry");
            System.out.print("Wybierz opcję (1 lub 2, 'exit' aby zakończyć): ");
            String choice = scanner.nextLine();

            if ("exit".equalsIgnoreCase(choice)) {
                sendExitRequest();
                break;
            }

            switch (choice) {
                case "1":
                    handleCreateGame();
                    IsHost = true;
                    break;
                case "2":
                    handleJoinGame();
                    IsHost = false;
                    break;
                default:
                    System.out.println("Nieprawidłowy wybór.");
            }

            try {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println("Serwer: " + response);
                    if (response.contains("Przeniesiono do Lobby")) {
                        enterLobby();
                        //break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Błąd podczas odczytu odpowiedzi od serwera: " + e.getMessage());
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

    private void sendExitRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "exit");
        out.println(gson.toJson(request));
    }

    private void enterLobby() {
        System.out.println("Jesteś w Lobby. Oczekuj na innych graczy...");
        if (IsHost) {
            System.out.println("Wpisz 'start' aby rozpocząć grę.");
        }    }
}