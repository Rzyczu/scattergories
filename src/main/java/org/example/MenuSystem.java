package org.example;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Scanner;

public class MenuSystem {

    private final BufferedReader in;
    private final PrintWriter out;
    private final Scanner scanner;

    public MenuSystem(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            // Display Landing Page Menu
            System.out.println("Landing Page:");
            System.out.println("1. New Game");
            System.out.println("2. Join to Game");
            System.out.print("Wybierz opcję (1 lub 2, 'exit' aby zakończyć): ");
            String choice = scanner.nextLine();

            if ("exit".equalsIgnoreCase(choice)) {
                out.println("exit");
                System.out.println("Zamykanie połączenia...");
                break;
            }

            switch (choice) {
                case "1":
                    handleNewGame();
                    break;
                case "2":
                    handleJoinGame();
                    break;
                default:
                    System.out.println("Nieprawidłowy wybór.");
            }

            try {
                // Read and print server response
                String response = in.readLine();
                System.out.println("Serwer: " + response);
            } catch (Exception e) {
                System.err.println("Błąd podczas odczytu odpowiedzi od serwera: " + e.getMessage());
            }
        }
    }

    private void handleNewGame() {
        System.out.println("New Game:");
        System.out.println("1. Open");
        System.out.println("2. Close");
        System.out.print("Wybierz opcję (1 lub 2): ");
        String gameType = scanner.nextLine();

        if (gameType.equals("1")) {
            out.println("New Game: Open");
            System.out.println("You are in the Lobby (Open Game).");
        } else if (gameType.equals("2")) {
            out.println("New Game: Close");
            System.out.println("You are in the Lobby (Close Game).");
        } else {
            System.out.println("Nieprawidłowy wybór.");
        }
    }

    private void handleJoinGame() {
        System.out.println("Join to Game:");
        System.out.println("1. Random Game");
        System.out.println("2. Enter by Code");
        System.out.print("Wybierz opcję (1 lub 2): ");
        String joinType = scanner.nextLine();

        if (joinType.equals("1")) {
            out.println("Join: Random Game");
            System.out.println("You are in the Lobby (Random Game).");
        } else if (joinType.equals("2")) {
            System.out.print("Wpisz kod gry: ");
            String gameCode = scanner.nextLine();
            out.println("Join: Enter by Code - " + gameCode);
            System.out.println("You are in the Lobby (Game Code: " + gameCode + ").");
        } else {
            System.out.println("Nieprawidłowy wybór.");
        }
    }
}
