package org.example;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.List;

public class MenuSystem {

    private final BufferedReader in;
    private final PrintWriter out;
    private final Scanner scanner;
    private final Gson gson = new Gson();
    private boolean isHost = false;
    private boolean inGame = false;
    public MenuSystem(BufferedReader in, PrintWriter out, Scanner scanner) {
        this.in = in;
        this.out = out;
        this.scanner = scanner;
        startListeningToServer(); //
    }

    public void run() {

        while (true) {
            if (!inGame) {
                System.out.println("Landing Page:");
                System.out.println("1. Stwórz grę");
                System.out.println("2. Dołącz do gry");
                if (isHost) {
                    System.out.println("'start' - Rozpocznij grę");
                }
                System.out.print("Wybierz opcję (1 lub 2, 'exit' aby zakończyć): ");
                String choice = scanner.nextLine();

                if ("exit".equalsIgnoreCase(choice)) {
                    sendExitRequest();
                    break;
                }

                switch (choice) {
                    case "1":
                        handleCreateGame();
                        break;
                    case "2":
                        handleJoinGame();
                        break;
                    case "start":
                        if (isHost) {
                            JsonObject request = new JsonObject();
                            request.addProperty("action", "start_game");
                            out.println(gson.toJson(request));
                        } else {
                            System.out.println("Tylko host może rozpocząć grę.");
                        }
                        break;
                    default:
                        System.out.println("Nieprawidłowy wybór.");
                }
            } else {
                try {
                    Thread.sleep(100); // Uspokojenie pętli, aby nie przechodziła do Landing Page
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void startListeningToServer() {
        Thread listenerThread = new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    String action = jsonResponse.get("action").getAsString();

                    switch (action) {
                        case "update_lobby":
                            updateLobbyDisplay(jsonResponse);
                            break;
                        case "lobby":
                            enterLobby();
                            break;
                        case "game_created":
                            isHost = true;
                            System.out.println("Serwer: " + jsonResponse.get("message").getAsString());
                            break;
                        case "game_started":
                            System.out.println("Serwer: " + jsonResponse.get("message").getAsString());
                            break;
                        case "new_round":
                            handleNewRound(jsonResponse);
                            break;
                        case "categories":
                            handleCategories(jsonResponse);
                            break;
                        case "joined_game":
                        case "prompt_nickname":
                        case "welcome":
                        case "error":
                        case "disconnect":
                            System.out.println("Serwer: " + jsonResponse.get("message").getAsString());
                            break;
                        default:
                            System.out.println("Serwer: Nieznana akcja.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Błąd podczas odczytu odpowiedzi od serwera: " + e.getMessage());
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
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
        if (isHost) {
            System.out.println("Jesteś gospodarzem. Wpisz 'start' w menu głównym, aby rozpocząć grę.");
        }
    }

    private void updateLobbyDisplay(JsonObject jsonResponse) {
        System.out.println("Aktualna lista graczy w lobby:");
        List<String> players = gson.fromJson(jsonResponse.get("players"), new TypeToken<List<String>>() {}.getType());
        for (String player : players) {
            System.out.println("- " + player);
        }
        System.out.println("Liczba graczy: " + players.size() + "/6");
    }

    private void handleNewRound(JsonObject jsonResponse) {
        inGame = true;
        int roundNumber = jsonResponse.get("round_number").getAsInt();
        String letter = jsonResponse.get("letter").getAsString();
        System.out.println("Runda " + roundNumber + ". Wylosowana litera: " + letter);
    }

    private void handleCategories(JsonObject jsonResponse) {
        List<String> categories = gson.fromJson(jsonResponse.get("categories"), new TypeToken<List<String>>() {}.getType());

        System.out.println("Kategorie dla tej rundy:");
        for (String category : categories) {
            System.out.println("- " + category);
        }

        JsonObject answers = new JsonObject();
        System.out.println("Wprowadź odpowiedzi dla każdej kategorii:");

        try (Scanner inputScanner = new Scanner(System.in)) {  // Nowy `Scanner` dla `System.in`
            for (String category : categories) {
                System.out.print(category + ": ");
                if (inputScanner.hasNextLine()) {
                    String answer = inputScanner.nextLine();
                    answers.addProperty(category, answer);
                } else {
                    System.out.println("Błąd: brak danych wejściowych.");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas odczytu odpowiedzi: " + e.getMessage());
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "submit_answers");
        request.add("answers", answers);

        out.println(gson.toJson(request));
        System.out.println("Odpowiedzi zostały przesłane.");
    }
}