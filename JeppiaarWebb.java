import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class JeppiaarWebb {
    static Map<InetAddress, String> sessionMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8080);
        System.out.println("Jeppiaar College Portal running at http://localhost:8080");

        while (true) {
            Socket client = server.accept();
            new Thread(() -> handle(client)).start();
        }
    }

    private static void handle(Socket client) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream()
        ) {
            String line = in.readLine();
            if (line == null) return;

            String[] request = line.split(" ");
            String method = request[0];
            String path = request[1];

            while (!(line = in.readLine()).isEmpty()) {}

            InetAddress clientIP = client.getInetAddress();
            String currentRole = sessionMap.getOrDefault(clientIP, "none");

            if (path.startsWith("/student")) {
                sendHtml(out, loginForm("student"));
                return;
            }

            if (path.startsWith("/professor")) {
                sendHtml(out, loginForm("professor"));
                return;
            }

            if (path.startsWith("/login")) {
                Map<String, String> params = parseQuery(path);
                String role = params.get("role");
                String user = params.get("username");
                String pass = params.get("password");

                boolean validStudent = role.equals("student") && user.equals("student") && pass.equals("stud123");
                boolean validProfessor = role.equals("professor") && user.equals("professor") && pass.equals("prof123");

                boolean otherLoggedIn = sessionMap.containsValue(role.equals("student") ? "professor" : "student");

                if (validStudent && !otherLoggedIn) {
                    sessionMap.put(clientIP, "student");
                    sendHtml(out, studentInfo());
                } else if (validProfessor && !otherLoggedIn) {
                    sessionMap.put(clientIP, "professor");
                    sendHtml(out, professorInfo());
                } else {
                    sendHtml(out,
                        "<h2>Login Failed</h2>" +
                        "<p>Invalid credentials or other portal already logged in</p>" +
                        "<form action='/'><button class='btn'>Return to Home</button></form>"
                    );
                }
                return;
            }

            if (path.startsWith("/logout")) {
                sessionMap.remove(clientIP);
                String html = "HTTP/1.1 302 Found\r\nLocation: /\r\n\r\n";
                out.write(html.getBytes());
                return;
            }

            if (path.startsWith("/forgot")) {
                sessionMap.remove(clientIP);
                sendHtml(out, "<h2>Password Reset</h2><p>Please contact IT support to reset your password.</p>");
                return;
            }

            String status = currentRole.equals("student") ? "Student logged in" :
                            currentRole.equals("professor") ? "Professor logged in" : "Not logged in";

            String html = "<h2>Jeppiaar Engineering College</h2>" +
                    "<img src='https://akm-img-a-in.tosshub.com/sites/resources/campus/prod/img/logo/2023/10/ylogo214116250897.jpeg' class='logo'><br>" +
                    "<form action='/student'><button class='btn'>Student Login</button></form>" +
                    "<form action='/professor'><button class='btn'>Professor Login</button></form>" +
                    "<form action='/logout'><button class='btn'>Logout</button></form>" +
                    "<form action='/forgot'><button class='btn'>Forgot Password</button></form>" +
                    "<p>Status: " + status + "</p>";

            sendHtml(out, html);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String loginForm(String role) {
        return "<h2>" + role.substring(0, 1).toUpperCase() + role.substring(1) + " Login</h2>" +
                "<form method='get' action='/login'>" +
                "<input type='hidden' name='role' value='" + role + "'>" +
                "<input type='text' name='username' placeholder='Username'><br><br>" +
                "<input type='password' name='password' placeholder='Password'><br><br>" +
                "<button type='submit' class='btn'>Login</button></form>" +
                "<p><a href='/forgot'>Forgot Password?</a></p>";
    }

    private static String studentInfo() {
        return "<h2>Welcome Student!</h2>" +
               "<img src='https://akm-img-a-in.tosshub.com/sites/resources/campus/prod/img/logo/2023/10/ylogo214116250897.jpeg' class='logo'><br>" +
               "<p>Name: Naveen Kumar P</p>" +
               "<p>Department: Information Technology</p>" +
               "<p>Year: 2nd Year</p>" +
               "<p>Roll No: 24JEIT139</p>" +
               "<p>Status: Logged in as Student</p>" +
               "<form action='/logout'><button class='btn'>Logout</button></form>";
    }

    private static String professorInfo() {
        return "<h2>Welcome Professor!</h2>" +
               "<img src='https://akm-img-a-in.tosshub.com/sites/resources/campus/prod/img/logo/2023/10/ylogo214116250897.jpeg' class='logo'><br>" +
               "<p>Name: Dr. Vinoth Kumar</p>" +
               "<p>Department: Information Technology</p>" +
               "<p>Designation: Professor</p>" +
               "<p>Employee ID: 06739</p>" +
               "<p>Status: Logged in as Professor</p>" +
               "<form action='/logout'><button class='btn'>Logout</button></form>";
    }

    private static Map<String, String> parseQuery(String path) {
        Map<String, String> map = new HashMap<>();
        if (!path.contains("?")) return map;
        String query = path.split("\\?", 2)[1];
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                map.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static void sendHtml(OutputStream out, String body) throws IOException {
        String html = "<!DOCTYPE html><html><head><title>Jeppiaar College Portal</title>" +
            "<style>" +
            "body { margin: 0; font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; transition: background 0.5s, color 0.5s; }" +
            "body.day { background: url('https://jeppiaarcollege.org/jeppiaar/wp-content/uploads/2018/02/about_slide1.jpg') no-repeat center center fixed; background-size: cover; color: #fff; }" +
            "body.night { background: url('https://iconstem.dashingknights.com/assets/img/venue-gallery/college.jpg') no-repeat center center fixed; background-size: cover; color: #eee; }" +
            ".overlay { background: rgba(0,0,0,0.6); padding: 30px; border-radius: 10px; text-align: center; }" +
            ".btn { padding: 10px 20px; margin: 10px; background: #009999; color: white; border: none; cursor: pointer; }" +
            ".toggle { position: fixed; top: 10px; right: 10px; background: transparent; border: none; cursor: pointer; width: 40px; height: 40px; background-image: url('https://akm-img-a-in.tosshub.com/sites/resources/campus/prod/img/logo/2023/10/ylogo214116250897.jpeg'); background-size: cover; }" +
            ".logo { width: 80px; margin-top: 20px; }" +
            "</style>" +
            "<script>" +
            "function toggleTheme() {" +
            "  document.body.classList.toggle('night');" +
            "  document.body.classList.toggle('day');" +
            "}" +
            "</script></head>" +
            "<body class='day'>" +
            "<button class='toggle' onclick='toggleTheme()'></button>" +
            "<div class='overlay'>" + body + "</div>" +
            "</body></html>";

        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: " + response.length + "\r\n\r\n").getBytes());
        out.write(response);
    }
}