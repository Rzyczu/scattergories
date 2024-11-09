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
    private Player host;
    private final List<Player> players;
    private Type gameType;

    // Enum to define game types
    public enum Type {
        OPEN("Open"),
        CLOSE("Close");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Initializes a new game instance with a host player
    public Game(Player host) {
        this.id = generateUniqueId();
        this.startTime = LocalDateTime.now();
        this.code = generateRandomCode();
        this.host = host;
        this.players = new ArrayList<>();
        this.gameType = Type.OPEN; // Default setting to OPEN
    }

    // Generates a unique game ID
    private String generateUniqueId() {
        return "GAME-" + System.currentTimeMillis();
    }

    // Generates a random code for the game
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            codeBuilder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return codeBuilder.toString();
    }

    // Adds a player to the game if it's not full
    public boolean addPlayer(Player player) {
        if (players.size() < MAX_PLAYERS) {
            players.add(player);
            return true;
        }
        return false;
    }

    // Removes a player from the game
    public boolean removePlayer(Player player) {
        return players.remove(player);
    }

    // Assigns a new host if the current host leaves
    public void assignNewHost() {
        if (!players.isEmpty()) {
            host = players.get(0); // Assigns the earliest added player as host
        } else {
            host = null; // No players left
        }
    }

    // Checks if the game has reached the maximum number of players
    public boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }

    // Returns the display name of the game type
    public String getGameTypeDisplayName() {
        return gameType.getDisplayName();
    }

    // Sets the game type
    public void setGameType(Type gameType) {
        this.gameType = gameType;
    }

    // Getter for game ID
    public String getId() {
        return id;
    }

    // Getter for game start time
    public LocalDateTime getStartTime() {
        return startTime;
    }

    // Getter for game end time
    public LocalDateTime getEndTime() {
        return endTime;
    }

    // Getter for game code
    public String getCode() {
        return code;
    }

    // Getter for game host
    public Player getHost() {
        return host;
    }

    // Getter for the list of players
    public List<Player> getPlayers() {
        return players;
    }

    // Getter for game type
    public Type getType() {
        return gameType;
    }

    // Returns a list of player nicknames
    public List<String> getPlayerNicknames() {
        List<String> nicknames = new ArrayList<>();
        for (Player player : players) {
            nicknames.add(player.getNickname());
        }
        return nicknames;
    }

    // Sets the end time of the game
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
