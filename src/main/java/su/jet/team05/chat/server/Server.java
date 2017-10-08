package su.jet.team05.chat.server;



import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Server {
    private static final int PORT = 60000;


    private static Set<Client> clients = new HashSet<>();

    // key userName if it not Anonimus , value it's socket
    private static HashMap<String, Client > userNames = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket client = server.accept();
                Client currentClient = new Client(client);
                clients.add(currentClient);
                new Thread(() -> clientLoop(currentClient)).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void clientLoop(Client client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getSocket().getInputStream()))) {

            String inputStringMessage;
            do {
                inputStringMessage = in.readLine();
                parseAndSend(client, inputStringMessage);
                //sendToAll(inputStringMessage);
            } while (inputStringMessage != null);


        } catch (SocketException t) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseAndSend(Client client, String inputStringMessage) throws IOException {
        if (inputStringMessage.length() < 2 && inputStringMessage.length() > 0) {
            if (inputStringMessage.charAt(0) == '1') {
                System.out.print("Клиент " + client.getSocket() + " будет удален");
                clients.remove(client);
                if (userNames.containsKey(client.getUsername())) {
                    userNames.remove(client.getUsername());
                }
                // нужно как-то осободить ник клиента
                //userNames.remove(client);
            } else if (inputStringMessage.charAt(0) == '3') {
                // здесь будет выведена история
                Socket currentSoket = client.getSocket();
                PrintWriter pw2 = new PrintWriter(currentSoket.getOutputStream(), true);
                Saver.getHistory(pw2);
            }
        }
            else if (inputStringMessage.length() > 1) {
                char code = inputStringMessage.charAt(0);
                // если пришло обычное сообщение
                if (code == '0') {

                    String messageToSend = inputStringMessage.substring(1);
                    Message currentMessage = new Message(client.getUsername(), messageToSend);
                    Saver.saveMessage(currentMessage);
                    sendToAll(currentMessage.toString());
                } else if (code == '2') {//если настроить username
                    String userNick = inputStringMessage.substring(1);
                    // if this user name is stored
                    if (userNames.containsKey(userNick)) {
                        // if it's not the same socket, send to this client that username is invalid
                        if (!(userNames.get(userNick) == client)) {
                            PrintWriter pw = new PrintWriter(client.getSocket().getOutputStream(), true);
                            pw.println("Имя пользователя " + userNick + " уже занято, введите другое имя");
                        }
                    } else {
                        client.setUsername(userNick);
                        userNames.put(userNick, client);

                    }
                } else {
                    // невалидный код
                }
            }
        }


    private static void sendToAll(String currentMessage) throws IOException {
        HashSet<Client> clientsToDelete = new HashSet<>();
        for (Client current : clients) {
            Socket currentSocket = current.getSocket();
            if (!currentSocket.isClosed()) {
                PrintWriter pw = new PrintWriter(currentSocket.getOutputStream(), true);
                pw.println(currentMessage);
            } else {
                System.out.println("Сообщение " + currentMessage + " не было отправлено " + current + " . Этот клиент не в сети ");
                clientsToDelete.add(current);
            }
        }
        for (Client toDelete : clientsToDelete) {
            if (clients.contains(toDelete)) {
                clients.remove(toDelete);
                if(userNames.containsKey(toDelete.getUsername())){
                    userNames.remove(toDelete.getUsername());
                }

            }
        }
    }
}