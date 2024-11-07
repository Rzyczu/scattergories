package org.example;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class GameSystem {
    private final BufferedReader in;
    private final PrintWriter out;
    private final Gson gson = new Gson();

    public GameSystem(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    public void startGame() {
        System.out.println("Gra rozpoczęta!");

        Thread gameListener = new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                    String action = jsonResponse.get("action").getAsString();

                    switch (action) {
                        case "game_action": // Placeholder dla przyszłych akcji w grze
                            handleGameAction(jsonResponse);
                            break;
                        case "game_end":
                            System.out.println("Gra zakończona: " + jsonResponse.get("message").getAsString());
                            return; // Zakończenie gry
                        default:
                            System.out.println("Serwer: " + jsonResponse.get("message").getAsString());
                    }
                }
            } catch (Exception e) {
                System.err.println("Błąd podczas odczytu odpowiedzi od serwera w grze: " + e.getMessage());
            }
        });

        gameListener.setDaemon(true);
        gameListener.start();
    }

    private void handleGameAction(JsonObject jsonResponse) {
        // Implementacja akcji gry, np. odbieranie ruchów, wyników itp.
        System.out.println("Akcja gry: " + jsonResponse.get("message").getAsString());
    }
}
