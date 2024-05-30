package httpServer;

import java.io.*;
import java.net.*;

public class WebServer {

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: WebServer <port>");
            System.exit(1);
        }
        WebServer server = new WebServer(Integer.parseInt(args[0]));
    }

    public WebServer(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                try (Socket sock = server.accept();
                     InputStream in = sock.getInputStream();
                     OutputStream out = sock.getOutputStream()) {

                    System.out.println("Accepted connection from " + sock.getInetAddress());

                    byte[] response = createResponse(in);
                    out.write(response);
                    out.flush();

                    System.out.println("Response sent");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] createResponse(InputStream inStream) {
        byte[] response = null;
        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
            String filename = null;
            boolean done = false;

            while (!done) {
                String line = in.readLine();
                System.out.println("Received: " + line);

                if (line == null || line.equals("")) {
                    done = true;
                } else if (line.startsWith("GET")) {
                    int firstSpace = line.indexOf(" ");
                    int secondSpace = line.indexOf(" ", firstSpace + 1);
                    filename = line.substring(firstSpace + 2, secondSpace);
                }
            }

            System.out.println("Requested file: " + filename);

            if (filename == null) {
                response = "<html>Illegal request: no GET</html>".getBytes();
            } else {
                File file = new File("www/" + filename); // Ensure this is the correct path
                System.out.println("Looking for file: " + file.getAbsolutePath());

                if (!file.exists()) {
                    response = ("<html>File not found: " + filename + "</html>").getBytes();
                } else {
                    response = readFileInBytes(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
        }

        return response;
    }

    public static byte[] readFileInBytes(File f) throws IOException {
        try (FileInputStream file = new FileInputStream(f);
             ByteArrayOutputStream data = new ByteArrayOutputStream(file.available())) {

            byte[] buffer = new byte[512];
            int numRead;

            while ((numRead = file.read(buffer)) > 0) {
                data.write(buffer, 0, numRead);
            }

            return data.toByteArray();
        }
    }
}
