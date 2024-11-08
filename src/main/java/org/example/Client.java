package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12121  ;

    public static void main(String[] args) {
        System.out.println("Łączenie z serwerem na " + SERVER_ADDRESS + ":" + PORT);

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Połączono z serwerem.");
            System.out.println("Serwer: " + in.readLine());

            // Request user for nickname
            String nickname = scanner.nextLine();
            out.println(nickname);

            // Read and print the server's welcome message
            String welcomeMessage = in.readLine();
            System.out.println("Serwer: " + welcomeMessage);

            MenuSystem menuSystem = new MenuSystem(in, out, scanner);
            menuSystem.run();

        } catch (IOException e) {
            System.err.println("Błąd podczas komunikacji z serwerem: " + e.getMessage());
        }
    }
}