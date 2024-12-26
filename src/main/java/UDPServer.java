import java.io.*;
import java.net.*;
import java.util.*;

public class UDPServer {
    private static final String DB_FILE = "database.txt";
    private static final String CREDENTIALS_FILE = "credentials.txt";
    private static final Map<String, List<String>> sessionHistory = new HashMap<>();

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(6000)) {
            System.out.println("UDP Server is running on port 6000...");
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(requestPacket);
                String request = new String(requestPacket.getData(), 0, requestPacket.getLength());

                InetAddress clientAddress = requestPacket.getAddress();
                int clientPort = requestPacket.getPort();
                String clientKey = clientAddress.toString() + ":" + clientPort;

                sessionHistory.putIfAbsent(clientKey, new ArrayList<>());
                sessionHistory.get(clientKey).add(request);

                String response;
                if (request.startsWith("LOGIN:")) {
                    response = authenticate(request.substring(6).trim()) ? "Authentication successful." : "Authentication failed.";
                } else if (request.startsWith("HIST:")) {
                    response = String.join("\n", sessionHistory.get(clientKey));
                } else {
                    response = processRequest(request);
                }

                byte[] responseBytes = response.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddress, clientPort);
                socket.send(responsePacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean authenticate(String credentials) {
        String[] parts = credentials.split(":");
        if (parts.length != 2) return false;
        String username = parts[0].trim();
        String password = parts[1].trim();

        try (BufferedReader reader = new BufferedReader(new FileReader(CREDENTIALS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(username + ":" + password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String processRequest(String request) {
        if (request.startsWith("INQ:")) {
            return inquire(request.substring(4).trim());
        } else if (request.startsWith("ADD:")) {
            return addRecord(request.substring(4).trim());
        } else if (request.startsWith("DEL:")) {
            return deleteRecord(request.substring(4).trim());
        } else if (request.startsWith("UPD:")) {
            return updateRecord(request.substring(4).trim());
        }
        return "Invalid command.";
    }

    private static String inquire(String studentId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(DB_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(studentId)) {
                    return line;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Student's record is not found.";
    }

    private static String addRecord(String record) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DB_FILE, true))) {
            writer.write(record);
            writer.newLine();
            return "Record added successfully.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to add record.";
        }
    }

    private static String deleteRecord(String studentId) {
        File dbFile = new File(DB_FILE);
        File tempFile = new File("temp.txt");

        boolean found = false;
        try (
                BufferedReader reader = new BufferedReader(new FileReader(dbFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(studentId)) {
                    found = true;
                    continue;
                }
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to delete record.";
        }

        if (found) {
            dbFile.delete();
            tempFile.renameTo(dbFile);
            return "Record deleted successfully.";
        } else {
            tempFile.delete();
            return "Student ID is not found.";
        }
    }

    private static String updateRecord(String record) {
        String[] parts = record.split(":");
        if (parts.length != 2) return "Invalid update format.";

        String studentId = parts[0].trim();
        String newRecord = parts[1].trim();

        File dbFile = new File(DB_FILE);
        File tempFile = new File("temp.txt");

        boolean found = false;
        try (
                BufferedReader reader = new BufferedReader(new FileReader(dbFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(studentId)) {
                    writer.write(newRecord);
                    writer.newLine();
                    found = true;
                } else {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to update record.";
        }

        if (found) {
            dbFile.delete();
            tempFile.renameTo(dbFile);
            return "Record updated successfully.";
        } else {
            tempFile.delete();
            return "Student ID is not found.";
        }
    }
}
