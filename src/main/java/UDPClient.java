import java.io.*;
import java.net.*;

public class UDPClient {
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 6000;

            System.out.print("Enter username:password > ");
            String credentials = console.readLine();

            byte[] credentialsBytes = ("LOGIN:" + credentials).getBytes();
            DatagramPacket loginPacket = new DatagramPacket(credentialsBytes, credentialsBytes.length, serverAddress, serverPort);
            socket.send(loginPacket);

            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);
            String serverResponse = new String(responsePacket.getData(), 0, responsePacket.getLength());
            System.out.println("Server: " + serverResponse);

            if (!serverResponse.equals("Authentication successful.")) {
                return;
            }

            String command;
            while (true) {
                System.out.print("Enter command (or 'exit' to quit) > ");
                command = console.readLine();

                if (command.equalsIgnoreCase("exit")) {
                    break;
                }

                byte[] commandBytes = command.getBytes();
                DatagramPacket commandPacket = new DatagramPacket(commandBytes, commandBytes.length, serverAddress, serverPort);
                socket.send(commandPacket);

                responsePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(responsePacket);
                serverResponse = new String(responsePacket.getData(), 0, responsePacket.getLength());
                System.out.println("Server: " + serverResponse);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}