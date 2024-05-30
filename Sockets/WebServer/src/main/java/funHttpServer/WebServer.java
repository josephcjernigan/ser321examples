package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.Charset;
import org.json.JSONArray;
import org.json.JSONObject;

class WebServer {
    public static void main(String args[]) {
        try {
            WebServer server = new WebServer(9000);
            server.startServer();
        } catch (IOException e) {
            System.out.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ServerSocket server;

    public WebServer(int port) throws IOException {
        server = new ServerSocket(port);
    }

    public void startServer() {
        System.out.println("Server started on port " + server.getLocalPort() + "...");
        try {
            while (true) {
                Socket sock = server.accept();
                new Thread(new ClientHandler(sock)).start();
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket sock;

        public ClientHandler(Socket socket) {
            this.sock = socket;
        }

        public void run() {
            try {
                InputStream in = sock.getInputStream();
                OutputStream out = sock.getOutputStream();
                byte[] response = createResponse(in);
                out.write(response);
                out.flush();
                sock.close();
            } catch (IOException e) {
                System.out.println("I/O error: " + e.getMessage());
            }
        }
    }

    public byte[] createResponse(InputStream inStream) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"))) {
            String request = null;
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("GET")) {
                    int firstSpace = line.indexOf(" ");
                    int secondSpace = line.indexOf(" ", firstSpace + 1);
                    request = line.substring(firstSpace + 2, secondSpace);
                }
            }

            if (request == null) {
                builder.append("HTTP/1.1 400 Bad Request\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("<html><body><h1>Bad Request: No GET line.</h1></body></html>");
                return builder.toString().getBytes();
            }

            switch (request) {
                case "":
                    return serveRootPage();
                case "json":
                    return serveRandomJsonImage(builder);
                case "multiply":
                    return handleMultiply(request, builder);
                case "github":
                    return handleGitHub(request, builder);
                case "convertCurrency":
                    return handleCurrencyConversion(request, builder);
                case "fetchWeather":
                    return handleWeatherFetch(request, builder);
                default:
                    builder.append("HTTP/1.1 404 Not Found\n");
                    builder.append("Content-Type: text/html; charset=utf-8\n");
                    builder.append("\n");
                    builder.append("<html><body><h1>Resource Not Found</h1></body></html>");
                    return builder.toString().getBytes();
            }
        } catch (IOException e) {
            builder = new StringBuilder();
            builder.append("HTTP/1.1 500 Internal Server Error\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>Internal Server Error: " + e.getMessage() + "</h1></body></html>");
            return builder.toString().getBytes();
        }
    }

    private byte[] serveRootPage() {
        StringBuilder builder = new StringBuilder();
        builder.append("HTTP/1.1 200 OK\n");
        builder.append("Content-Type: text/html; charset=utf-8\n");
        builder.append("\n");
        builder.append("<html><body>");
        builder.append("<h1>Welcome to the Java Web Server</h1>");
        builder.append("<p>Visit /multiply?num1=5&num2=3 to multiply two numbers.</p>");
        builder.append("<p>Visit /github?query=users/amehlhase316/repos to fetch GitHub repos.</p>");
        builder.append("<p>Visit /convertCurrency?from=USD&to=EUR&amount=100 to convert currency.</p>");
        builder.append("<p>Visit /fetchWeather?location=London to fetch weather.</p>");
        builder.append("</body></html>");
        return builder.toString().getBytes();
    }

    private byte[] serveRandomJsonImage(StringBuilder builder) {
        int index = random.nextInt(_images.size());
        String header = (String) _images.keySet().toArray()[index];
        String url = _images.get(header);
        builder.append("HTTP/1.1 200 OK\n");
        builder.append("Content-Type: application/json; charset=utf-8\n");
        builder.append("\n");
        builder.append("{\"header\":\"").append(header).append("\",");
        builder.append("\"image\":\"").append(url).append("\"}");
        return builder.toString().getBytes();
    }

    private byte[] handleMultiply(String request, StringBuilder builder) {
        Map<String, String> query_pairs = splitQuery(request);
        try {
            int num1 = Integer.parseInt(query_pairs.get("num1"));
            int num2 = Integer.parseInt(query_pairs.get("num2"));
            int result = num1 * num2;
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>Result: ").append(result).append("</h1></body></html>");
        } catch (NumberFormatException e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>Error: Invalid input for multiplication.</h1></body></html>");
        }
        return builder.toString().getBytes();
    }

    private byte[] handleGitHub(String request, StringBuilder builder) {
        Map<String, String> query_pairs = splitQuery(request);
        try {
            String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));
            JSONArray repos = new JSONArray(json);
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>GitHub Repositories</h1>");
            for (int i = 0; i < repos.length(); i++) {
                JSONObject repo = repos.getJSONObject(i);
                builder.append("<p>").append(repo.getString("full_name")).append(" - ").append(repo.getInt("id")).append("</p>");
            }
            builder.append("</body></html>");
        } catch (Exception e) {
            builder.append("HTTP/1.1 500 Internal Server Error\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>Error fetching GitHub data.</h1></body></html>");
        }
        return builder.toString().getBytes();
    }

    private byte[] handleCurrencyConversion(String request, StringBuilder builder) {
        Map<String, String> query_pairs = splitQuery(request);
        try {
            String fromCurrency = query_pairs.get("from");
            String toCurrency = query_pairs.get("to");
            double amount = Double.parseDouble(query_pairs.get("amount"));
            double convertedAmount = convertCurrency(fromCurrency, toCurrency, amount);

            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>Converted Amount: ").append(convertedAmount).append("</h1></body></html>");
        } catch (NumberFormatException e) {
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>Error: Invalid input for currency conversion.</h1></body></html>");
        }
        return builder.toString().getBytes();
    }

    private byte[] handleWeatherFetch(String request, StringBuilder builder) {
        Map<String, String> query_pairs = splitQuery(request);
        try {
            String location = query_pairs.get("location");
            String weatherData = fetchWeatherData(location);

            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>Weather in ").append(location).append(": ").append(weatherData).append("</h1></body></html>");
        } catch (Exception e) {
            builder.append("HTTP/1.1 500 Internal Server Error\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>Error fetching weather data.</h1></body></html>");
        }
        return builder.toString().getBytes();
    }

    public static Map<String, String> splitQuery(String query) {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    private double convertCurrency(String from, String to, double amount) {
        // Mock conversion logic
        return amount * 1.1;  // Assuming a mock conversion rate
    }

    private String fetchWeatherData(String location) {
        // Mock API response
        return "Sunny, 24°C";  // Mock weather for example
    }

    public String fetchURL(String aUrl) {
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(aUrl);
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(20 * 1000);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.defaultCharset()))) {
                int ch;
                while ((ch = br.read()) != -1) {
                    sb.append((char) ch);
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception in URL request: " + ex.getMessage());
        }
        return sb.toString();
    }
}

