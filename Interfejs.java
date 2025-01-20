import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;

public class Interfejs {
    private static String obecnyUser = "";
    private static String celnazwaPliku = "";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("""
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    Wybierz opcję!
                    """);
            if (obecnyUser.equals("")) {
                System.out.println("""
                        REJESTRACJA : Rejestracja
                        LOGOWANIE : Logowanie
                        """
                );
            }
            System.out.println("""
                    POST : Napisz coś na tablicy
                    CZYTAJ-POSTS : Wyświetl ostatnie 10 postów
                    WGRAJ : Wgraj plik na serwer
                    POBIERZ : Pobierz plik
                    WYLOGUJ : Wyloguj
                    WYJDZ : Do Widzenia
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    Twój wybór to:\s"""
            );

            String wybor = scanner.nextLine().toUpperCase();

            if (wybor.equals("WYJDZ")) {
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Koniec~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                break;
            } else if (wybor.equals("WYLOGUJ")) {
                if (obecnyUser.equals("")) {
                    System.out.println("Nie jesteś nawet zalogowany!");
                    continue;
                }
                obecnyUser = "";
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Wylogowano cię~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                continue;
            }
            String typ;
            String dane;
            String login = "";
            String haslo;

            switch (wybor) {
                case "REJESTRACJA" -> {
                    if (obecnyUser.equals("")) {
                        typ = "rejestracja";
                        System.out.print("Login: ");
                        login = scanner.nextLine();
                        System.out.print("Hasło: ");
                        haslo = scanner.nextLine();
                        dane = login + ";" + haslo;
                    } else {
                        System.out.println("Proszę, wybierz poprawną opcję");
                        continue;
                    }
                }
                case "LOGOWANIE" -> {
                    if (obecnyUser.equals("")) {
                        typ = "logowanie";
                        System.out.print("Login: ");
                        login = scanner.nextLine();
                        System.out.print("Hasło: ");
                        haslo = scanner.nextLine();
                        dane = login + ";" + haslo;
                    } else {
                        System.out.println("Proszę, wybierz poprawną opcję");
                        continue;
                    }
                }
                case "POST" -> {
                    if (obecnyUser.equals("")) {
                        System.out.println("Zanim to zrobisz, proszę zaloguj się!");
                        continue;
                    }
                    typ = "post";
                    System.out.print("Treść posta: ");
                    dane = obecnyUser + ";" + scanner.nextLine();
                }
                case "CZYTAJ-POSTS" -> {
                    if (obecnyUser.equals("")) {
                        System.out.println("Zanim to zrobisz, proszę zaloguj się!");
                        continue;
                    }
                    typ = "czytaj-posts";
                    System.out.println("Ostatnie 10 postów:\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                    dane = "";
                }
                case "WGRAJ" -> {
                    if (obecnyUser.equals("")) {
                        System.out.println("Zanim to zrobisz, proszę zaloguj się!");
                        continue;
                    }
                    typ = "wgraj_plik";
                    System.out.print("Ścieżka pliku ");
                    String sciezkaPliku = scanner.nextLine();
                    if (sciezkaPliku.equals("")) {
                        System.out.println("Zła ścieżka.");
                        continue;
                    }
                    System.out.print("Nazwa pliku: ");
                    celnazwaPliku = scanner.nextLine();
                    dane = obecnyUser + ";" + sciezkaPliku;
                }
                case "POBIERZ" -> {
                    if (obecnyUser.equals("")) {
                        System.out.println("Zanim to zrobisz, proszę zaloguj się!");
                        continue;
                    }
                    typ = "pobierz_plik";
                    System.out.print("Nazwa pliku który chcesz pobrać: ");
                    String nazwaPliku = scanner.nextLine();
                    dane = obecnyUser + ";" + nazwaPliku;
                }
                default -> {
                    System.out.println("Proszę, wybierz poprawną opcję");
                    continue;
                }
            }

            String request = typ + ";" + dane;

            String response = sendRequestToApiGateway(request);

            String[] responseParts = response.split(";", 2);
            String responseType = responseParts[0];
            String responseData = responseParts.length > 1 ? responseParts[1] : "";

            if (responseType.equals("200")) {
                if (typ.equals("logowanie") || typ.equals("rejestracja")) {
                    obecnyUser = login;
                } else if (typ.equals("wgraj_plik") | typ.equals("pobierz_plik")) {
                    System.out.println("Przesyłanie pliku powiodło się.");
                }
            }
            if (responseType.equals("299")) {
                String[] posts = responseData.split("\t%\t");
                for (String post : posts) {
                    System.out.println(post);
                }
                continue;
            }
            System.out.println(responseData);
        }
    }

    private static String sendRequestToApiGateway(String request) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Plik konfiguracyjny nie załadował się.");
            return "ERROR";
        }
        int apiGatewayPort = Integer.parseInt(properties.getProperty("api.gateway.port"));
        String apiGatewayIP = properties.getProperty("api.gateway.ip");

        String[] requestPart = request.split(";");
        if (requestPart[0].equals("wgraj_plik")) {
            String sciezkaPliku = requestPart[2];
            try {
                byte[] fileBytes = Files.readAllBytes(Paths.get(sciezkaPliku));
                String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                request = requestPart[0] + ";" + requestPart[1] + ";" + celnazwaPliku + ";" + encodedFile;
            } catch (IOException e) {
                System.err.println("Błąd odczytu pliku." + e.getMessage());
                return "Błąd odczytu pliku";
            }
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                output.println(request);
                return input.readLine();
            } catch (IOException e) {
                System.err.println("Błąd połączenia z ApiGateway." + e.getMessage());
                return "Błąd połączenia z ApiGateway.";
            }
        } else if (requestPart[0].equals("pobierz_plik")) {
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                output.println(request);
                String response = input.readLine();
                String[] responseParts = response.split(";", 2);

                if ("200".equals(responseParts[0])) {
                    String encodedFile = responseParts[1];
                    byte[] fileBytes = Base64.getDecoder().decode(encodedFile);
                    String nazwaPliku = requestPart[2];
                    String celPath = System.getProperty("user.home") + File.separator + "DOWNLOADED_" + nazwaPliku;
                    try {
                        Files.write(Paths.get(celPath), fileBytes);
                        return "200;Pobranie pliku zakończone pomyślnie: " + celPath;
                    } catch (IOException e) {
                        System.err.println("Błąd zapisu pliku" + e.getMessage());
                        return "Błąd zapisu pliku";
                    }
                } else {
                    return response;
                }
            } catch (IOException e) {
                System.err.println("Błąd połączenia z ApiGateway." + e.getMessage());
                return "Błąd połączenia z ApiGateway.";
            }
        } else {
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                output.println(request);
                return input.readLine();
            } catch (IOException e) {
                System.err.println("Błąd połączenia z ApiGateway." + e.getMessage());
                return "503;Błąd połączenia z ApiGateway.";
            }
        }
    }
}