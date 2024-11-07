package org.example;

import java.util.List;
import java.util.Map;

public class Round {
    private final int roundNumber; // Numer aktualnej rundy
    private char chosenLetter; // Wylosowana litera dla rundy
    private final List<String> categories = List.of("Państwo", "Miasto", "Zwierzę", "Roślina", "Jedzenie"); // Kategorię gry
    private Map<String, String> answers; // Mapa odpowiedzi gracza: kategoria -> odpowiedź
    private Map<String, Integer> scores; // Mapa punktów graczy w tej rundzie: gracz -> punkty

    public Round(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    // Inicjalizacja rundy: losowanie litery, ustawienie odpowiedzi, itp.
    public void initializeRound() {
        // TODO: Losowanie litery i przypisanie do chosenLetter
    }

    // Wysłanie litery do graczy
    public void sendLetterToPlayers() {
        // TODO: Implementacja wysyłania litery do graczy
    }

    // Przesłanie kategorii do graczy
    public void sendCategoriesToPlayers() {
        // TODO: Implementacja wysyłania kategorii do graczy
    }

    // Zbieranie odpowiedzi od graczy
    public void collectAnswers() {
        // TODO: Implementacja zbierania odpowiedzi od graczy
    }

    // Ustalanie pozostałego czasu (5 sekund) dla innych graczy
    public void startCountdownForRemainingPlayers() {
        // TODO: Implementacja licznika czasu dla pozostałych graczy
    }

    // Wysyłanie odpowiedzi graczy do serwera
    public void sendAnswersToServer() {
        // TODO: Implementacja wysyłania odpowiedzi do serwera
    }

    // Sprawdzanie odpowiedzi i przypisywanie punktów
    public void evaluateAnswers() {
        // TODO: Implementacja oceny odpowiedzi i przypisania punktów
    }

    // Wyświetlanie wyników rundy
    public void displayRoundResults() {
        // TODO: Implementacja wyświetlania wyników rundy
    }

    // Gettery dla potrzebnych pól
    public char getChosenLetter() {
        return chosenLetter;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }
}
