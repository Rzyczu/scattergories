package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;

    public static void main(String[] args) {
        System.out.println("Łączenie z serwerem na " + SERVER_ADDRESS + ":" + PORT);

        try {
            // Initialize connection
            socket = new Socket(SERVER_ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Połączono z serwerem.");
            System.out.println("Serwer: " + in.readLine());

            // Create and run the menu system
            MenuSystem menuSystem = new MenuSystem(in, out);
            menuSystem.run();

        } catch (IOException e) {
            System.err.println("Błąd podczas komunikacji z serwerem: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Błąd podczas zamykania połączenia: " + e.getMessage());
            }
        }
    }
}