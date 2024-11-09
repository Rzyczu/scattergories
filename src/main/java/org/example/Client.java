package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12121;

    public static void main(String[] args) {
        System.out.println("Connecting to server at " + SERVER_ADDRESS + ":" + PORT);

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server.");
            System.out.println("Server: " + in.readLine());

            // Requesting nickname from user
            String nickname = scanner.nextLine();
            out.println(nickname);

            // Print server's welcome message
            String welcomeMessage = in.readLine();
            System.out.println("Server: " + welcomeMessage);

            // Initialize and start the menu system
            MenuSystem menuSystem = new MenuSystem(in, out, scanner);
            menuSystem.run();

        } catch (IOException e) {
            System.err.println("Error during server communication: " + e.getMessage());
        }
    }
}
