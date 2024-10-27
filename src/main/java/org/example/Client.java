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

    public static void main(String[] args) {
        System.out.println("Łączenie z serwerem na " + SERVER_ADDRESS + ":" + PORT);

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Połączono z serwerem.");
            System.out.println("Serwer: " + in.readLine());

            String message;
            while (true) {
                System.out.print("Wpisz wiadomość (lub 'exit' aby zakończyć): ");
                message = scanner.nextLine();
                out.println(message);

                if ("exit".equalsIgnoreCase(message)) {
                    System.out.println("Zamykanie połączenia...");
                    break;
                }

                String response = in.readLine();
                System.out.println("Serwer: " + response);
            }

        } catch (IOException e) {
            System.err.println("Błąd podczas komunikacji z serwerem: " + e.getMessage());
        }
    }
}