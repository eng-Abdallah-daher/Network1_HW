import java.io.*;
import java.net.*;
import java.util.*;

public class TCPServer {
    private static final int PORT = 5000;
    private static Map<Socket, List<String>> sessionHistory = new HashMap<>();
    private static Map<String, String> records = new HashMap<>();
    private static Set<String> credentials = new HashSet<>();

    public static void main(String[] args) {
        loadRecords();
        loadCredentials();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("UDP Server is running on port 5000...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            sessionHistory.put(clientSocket, new ArrayList<>());
            String request;
            while ((request = in.readLine()) != null) {
                sessionHistory.get(clientSocket).add(request);
                String response = processRequest(request, clientSocket);
                out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String processRequest(String request, Socket clientSocket) {
        List<String> history = sessionHistory.get(clientSocket);
        if (request.startsWith("LOGIN:")) {
            String[] credentialsInput = request.substring(6).split(":");
            if (credentialsInput.length == 2) {
                String userInput = credentialsInput[0].trim() + ":" + credentialsInput[1].trim();
                if (credentials.contains(userInput)) {
                    history.add(request);
                    return "Authentication successful.";
                } else {
                    return "Authentication failed.";
                }
            } else {
                return "Invalid login format. Use LOGIN:username:password.";
            }
        } else if (request.startsWith("INQ:")) {
            String id = request.substring(4).trim();
            return records.getOrDefault(id, "Record not found.");
        } else if (request.startsWith("ADD:")) {
            return addRecord(request.substring(4).trim());
        } else if (request.startsWith("DEL:")) {
            return deleteRecord(request.substring(4).trim());
        } else if (request.startsWith("UPD:")) {
            return updateRecord(request.substring(4).trim());
        } else if (request.startsWith("HIST:")) {
            return String.join("\n", history);
        }
        return "Invalid command.";
    }

    private static void loadRecords() {
        try (BufferedReader reader = new BufferedReader(new FileReader("database.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    records.put(parts[0], line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadCredentials() {
        try (BufferedReader reader = new BufferedReader(new FileReader("credentials.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                credentials.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String addRecord(String record) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("database.txt", true))) {
            writer.write(record);
            writer.newLine();
            String[] parts = record.split(",", 2);
            if (parts.length == 2) {
                records.put(parts[0], record);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to add record.";
        }
        return "Record added successfully.";
    }

    private static String deleteRecord(String recordId) {
        boolean deleted = false;
        StringBuilder updatedContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("database.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(recordId + ",")) {
                    deleted = true;
                } else {
                    updatedContent.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to delete record.";
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("database.txt"))) {
            writer.write(updatedContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to delete record.";
        }

        if (deleted) {
            records.remove(recordId);
            return "Record deleted successfully.";
        }
        return "Record not found.";
    }

    private static String updateRecord(String record) {
        String[] parts = record.split(":");
        if (parts.length != 2) return "Invalid update format.";

        String studentId = parts[0].trim();
        String newRecord = parts[1].trim();

        File dbFile = new File("database.txt");
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
