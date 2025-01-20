package Login;

import Posty.PolaczenieBaza;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login implements Runnable {
    private final Socket clientSocket;

    public Login(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = input.readLine();
            String[] userData = request.split(";");
            if (userData.length != 3) {
                output.println("Nieprawidłowe dane uwierzytelniające.");
                return;
            }

            String username = userData[1];
            String password = userData[2];
            try (Connection connection = BaseConnection.getConnection()) {
                PreparedStatement checkUserStatement = connection.prepareStatement("SELECT * FROM users WHERE username = ?");
                checkUserStatement.setString(1, username);
                ResultSet resultSet = checkUserStatement.executeQuery();

                if (resultSet.next()) {

                    if (resultSet.getString("password").equals(password)) {
                        output.println("200;Zalogowano");
                    } else {
                        output.println("Błędne hasło.");
                    }
                } else {
                    output.println("Użytkownik nie istnieje DB.");
                }
            } catch (SQLException e) {
                System.err.println("Błąd.");
            }
        } catch (IOException e) {
            System.err.println("Błąd." + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Błąd socketa");
            }
        }
    }
}