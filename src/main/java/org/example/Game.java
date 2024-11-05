package org.example;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_PLAYERS = 6;

    private final String id;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final String code;
    private final String host;
    private final List<Player> players;

    public Game(String host) {
        this.id = generateUniqueId();
        this.startTime = LocalDateTime.now();
        this.code = generateRandomCode();
        this.host = host;
        this.players = new ArrayList<>();
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

    public boolean addPlayer(Player player) {
        if (players.size() < MAX_PLAYERS) {
            players.add(player);
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getCode() {
        return code;
    }

    public String getHost() {
        return host;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
