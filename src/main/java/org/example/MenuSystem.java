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

    // Initializes MenuSystem with server input/output and user input
    public MenuSystem(BufferedReader in, PrintWriter out, Scanner scanner) {
        this.in = in;
        this.out = out;
        this.scanner = scanner;
        startListeningToServer();
    }

    // Main run loop for displaying the menu and processing user choices
    public void run() {
        while (true) {
            if (!inGame) {
                System.out.println("Landing Page:");
                System.out.println("1. Create game");
                System.out.println("2. Join game");
                if (isHost) {
                    System.out.println("'start' - Start the game");
                }
                System.out.print("Choose an option (1 or 2, 'exit' to quit): ");
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
                            System.out.println("Only the host can start the game.");
                        }
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } else {
                try {
                    Thread.sleep(100); // Small delay to prevent excessive loop speed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Starts a separate thread to listen to server responses
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
                            System.out.println("Server: " + jsonResponse.get("message").getAsString());
                            break;
                        case "game_started":
                            System.out.println("Server: " + jsonResponse.get("message").getAsString());
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
                            System.out.println("Server: " + jsonResponse.get("message").getAsString());
                            break;
                        default:
                            System.out.println("Server: Unknown action.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading server response: " + e.getMessage());
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // Handles game creation process
    private void handleCreateGame() {
        System.out.println("Choose game type:");
        System.out.println("1. Open");
        System.out.println("2. Close");
        System.out.print("Your choice: ");
        String gameType = scanner.nextLine().equals("1") ? "open" : "close";

        JsonObject request = new JsonObject();
        request.addProperty("action", "create_game");
        request.addProperty("game_type", gameType);
        out.println(gson.toJson(request));
    }

    // Handles joining an existing game
    private void handleJoinGame() {
        System.out.println("Join a game:");
        System.out.println("1. Random Game");
        System.out.println("2. Enter by Code");
        System.out.print("Choose an option (1 or 2): ");
        String joinChoice = scanner.nextLine();

        if (joinChoice.equals("1")) {
            JsonObject request = new JsonObject();
            request.addProperty("action", "join_game_random");
            out.println(gson.toJson(request));
        } else if (joinChoice.equals("2")) {
            System.out.print("Enter game code: ");
            String gameCode = scanner.nextLine();
            JsonObject request = new JsonObject();
            request.addProperty("action", "join_game_by_code");
            request.addProperty("game_code", gameCode);
            out.println(gson.toJson(request));
        } else {
            System.out.println("Invalid choice.");
        }
    }

    // Sends an exit request to the server
    private void sendExitRequest() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "exit");
        out.println(gson.toJson(request));
    }

    // Enters the lobby and notifies the user
    private void enterLobby() {
        System.out.println("You are in the Lobby. Waiting for other players...");
        if (isHost) {
            System.out.println("You are the host. Type 'start' in the main menu to begin the game.");
        }
    }

    // Updates the lobby display with the current player list
    private void updateLobbyDisplay(JsonObject jsonResponse) {
        System.out.println("Current players in the lobby:");
        List<String> players = gson.fromJson(jsonResponse.get("players"), new TypeToken<List<String>>() {}.getType());
        for (String player : players) {
            System.out.println("- " + player);
        }
        System.out.println("Player count: " + players.size() + "/6");
    }

    // Starts a new game round with a chosen letter and round number
    private void handleNewRound(JsonObject jsonResponse) {
        inGame = true;
        int roundNumber = jsonResponse.get("round_number").getAsInt();
        String letter = jsonResponse.get("letter").getAsString();
        System.out.println("Round " + roundNumber + ". Chosen letter: " + letter);
    }

    // Displays the categories for the current round and collects answers from the user
    private void handleCategories(JsonObject jsonResponse) {
        List<String> categories = gson.fromJson(jsonResponse.get("categories"), new TypeToken<List<String>>() {}.getType());

        System.out.println("Categories for this round:");
        for (String category : categories) {
            System.out.println("- " + category);
        }

        JsonObject answers = new JsonObject();
        System.out.println("Enter your answers for each category:");

        try (Scanner inputScanner = new Scanner(System.in)) {
            for (String category : categories) {
                System.out.print(category + ": ");
                if (inputScanner.hasNextLine()) {
                    String answer = inputScanner.nextLine();
                    answers.addProperty(category, answer);
                } else {
                    System.out.println("Error: No input data.");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading answers: " + e.getMessage());
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "submit_answers");
        request.add("answers", answers);

        out.println(gson.toJson(request));
        System.out.println("Answers have been submitted.");
    }
}
