package org.yisus.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Clase base para el control del servidor (recibe comandos del cliente)
 */
public class ServerControl implements Runnable {
    private ServerSocket serverSocket;
    private boolean running = true;

    public ServerControl(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                // Aquí se debe manejar la sesión del cliente en un hilo separado
                new Thread(new ServerSessionHandler(clientSocket)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() throws IOException {
        running = false;
        serverSocket.close();
    }
}

