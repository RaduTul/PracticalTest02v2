package ro.pub.cs.systems.eim.practicaltest02v2;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.net.*;

public class PracticalTest02v2MainActivity extends AppCompatActivity {

    private EditText serverPortEt, op1Et, op2Et;
    private TextView resultTv;
    private ServerThread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02v2_main);

        serverPortEt = findViewById(R.id.server_port);
        op1Et = findViewById(R.id.operator1);
        op2Et = findViewById(R.id.operator2);
        resultTv = findViewById(R.id.result_text);

        findViewById(R.id.start_server).setOnClickListener(v -> {
            String port = serverPortEt.getText().toString();
            if (port.isEmpty()) return;
            serverThread = new ServerThread(Integer.parseInt(port));
            serverThread.start();
        });

        findViewById(R.id.add_button).setOnClickListener(v -> startClient("add"));
        findViewById(R.id.mul_button).setOnClickListener(v -> startClient("mul"));
    }

    private void startClient(String operation) {
        String port = serverPortEt.getText().toString();
        String op1 = op1Et.getText().toString();
        String op2 = op2Et.getText().toString();
        if (port.isEmpty() || op1.isEmpty() || op2.isEmpty()) return;

        new ClientThread("127.0.0.1", Integer.parseInt(port), operation, op1, op2, resultTv).start();
    }

    // --- SERVER LOGIC ---
    class ServerThread extends Thread {
        private int port;
        private ServerSocket serverSocket;

        public ServerThread(int port) { this.port = port; }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                while (!isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    new CommunicationThread(socket).start();
                }
            } catch (IOException e) { Log.e("SERVER", e.getMessage()); }
        }
    }

    class CommunicationThread extends Thread {
        private Socket socket;
        public CommunicationThread(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

                String request = reader.readLine();
                if (request != null) {
                    String[] parts = request.split(",");
                    String type = parts[0];
                    long val1 = Long.parseLong(parts[1]);
                    long val2 = Long.parseLong(parts[2]);
                    long result = 0;

                    if (type.equals("add")) {
                        Thread.sleep(2000);
                        result = val1 + val2;
                    } else {
                        Thread.sleep(2000);
                        result = val1 * val2;
                    }

                    if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
                        writer.println("overflow");
                    } else {
                        writer.println(String.valueOf(result));
                    }
                }
                socket.close();
            } catch (Exception e) { Log.e("COMM", e.getMessage()); }
        }
    }

    class ClientThread extends Thread {
        private String host, op, val1, val2;
        private int port;
        private TextView tv;

        public ClientThread(String h, int p, String op, String v1, String v2, TextView tv) {
            this.host = h; this.port = p; this.op = op; this.val1 = v1; this.val2 = v2; this.tv = tv;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket(host, port);
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                writer.println(op + "," + val1 + "," + val2);
                String response = reader.readLine();
                tv.post(() -> tv.setText("Result: " + response));

            } catch (IOException e) { Log.e("CLIENT", e.getMessage()); }
        }
    }
}