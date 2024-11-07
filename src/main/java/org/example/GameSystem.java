package org.example;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameSystem {

    private static final int TOTAL_ROUNDS = 5; // Liczba rund w grze
    private final List<Round> rounds = new ArrayList<>(); // Lista przechowująca rundy gry
    private ExecutorService executor; // Executor dla wątków rundy
    private final BufferedReader in; // Obiekt do odbierania danych od serwera
    private final PrintWriter out; // Obiekt do wysyłania danych do serwera

    public GameSystem(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
        executor = Executors.newFixedThreadPool(TOTAL_ROUNDS);
    }

    // Główna metoda uruchamiająca grę i obsługująca rundy
    public void startGame() {
        System.out.println("Gra rozpoczęta! Rozgrywka składa się z " + TOTAL_ROUNDS + " rund.");

        for (int i = 1; i <= TOTAL_ROUNDS; i++) {
            final int roundNumber = i;
            Round round = new Round(roundNumber);
            rounds.add(round);

            // Uruchamiamy każdą rundę w osobnym wątku
            executor.execute(() -> playRound(round));
        }

        // Po zakończeniu gry zamykamy Executor
        executor.shutdown();
        System.out.println("Gra zakończona!");
        displayFinalScores();
    }

    // Schemat rundy
    private void playRound(Round round) {
        System.out.println("Rozpoczynamy rundę " + round.getRoundNumber());

        round.initializeRound(); // Inicjalizacja rundy, np. losowanie litery

        round.sendLetterToPlayers(); // Przesłanie wylosowanej litery do graczy
        round.sendCategoriesToPlayers(); // Przesłanie kategorii do graczy

        round.collectAnswers(); // Zbieranie odpowiedzi od graczy

        round.startCountdownForRemainingPlayers(); // Uruchomienie licznika czasu dla pozostałych graczy

        round.sendAnswersToServer(); // Wysłanie odpowiedzi do serwera

        round.evaluateAnswers(); // Ocena odpowiedzi i przyznanie punktów

        round.displayRoundResults(); // Wyświetlenie wyników dla danej rundy

        System.out.println("Runda " + round.getRoundNumber() + " zakończona.");
    }

    // Wyświetlenie końcowej tabeli wyników po zakończeniu gry
    private void displayFinalScores() {
        System.out.println("Końcowa tabela wyników:");

        // TODO: Implementacja sumowania i wyświetlania wyników z wszystkich rund
    }
}
