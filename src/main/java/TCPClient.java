import java.io.*;
import java.net.*;
import java.util.*;

public class TCPClient {
    public static void main(String[] args) {
        List<String> commandHistory = new ArrayList<>(); // Store history locally

        try (Socket socket = new Socket("localhost", 5000);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            // Authentication
            System.out.print("Enter username:password > ");
            String credentials = console.readLine();
            out.println("LOGIN:" + credentials); // Prefix with LOGIN:
            String serverResponse = in.readLine();
            System.out.println("Server: " + serverResponse);

            if (!serverResponse.equals("Authentication successful.")) {
                return;
            }


            // Command loop
            String command;
            while (true) {
                System.out.print("Enter command (or 'exit' to quit) > ");
                command = console.readLine();

                if (command.equalsIgnoreCase("exit")) {
                    break;
                }

                out.println(command);
                commandHistory.add(command); // Add to local history
                serverResponse = in.readLine();
                System.out.println("Server: " + serverResponse);

                if (command.equals("HIST:")) {
                    // Display local history when requested
                    System.out.println("Client-side history:");
                    for (String histCommand : commandHistory) {
                        System.out.println(histCommand);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}