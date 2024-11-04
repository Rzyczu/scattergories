package org.example;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    private final String id;
    private final LocalDateTime start_time;
    private LocalDateTime end_time;
    private final String code;
    private final String host;
    private final List<String> players;

    public Game(String host) {
        this.id = generateUniqueId();
        this.start_time = LocalDateTime.now();
        this.code = generateRandomCode();
        this.host = host;
        this.players = new ArrayList<>();
        this.players.add(host); // Add host to the list of players
    }

    private String generateUniqueId() {
        return "GAME-" + System.currentTimeMillis();
    }

    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            codeBuilder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return codeBuilder.toString();
    }

    // Getters
    public String getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return start_time;
    }

    public LocalDateTime getEndTime() {
        return end_time;
    }

    public String getCode() {
        return code;
    }

    public String getHost() {
        return host;
    }

    public List<String> getPlayers() {
        return players;
    }

    // Set end time when the game finishes
    public void setEndTime(LocalDateTime end_time) {
        this.end_time = end_time;
    }
}