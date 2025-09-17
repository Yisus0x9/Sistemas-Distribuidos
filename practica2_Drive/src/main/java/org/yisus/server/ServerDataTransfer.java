package org.yisus.server;

import org.yisus.services.CompressionUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerDataTransfer implements AutoCloseable {
    private ServerSocket serverSocket;
    private Socket dataSocket;
    private DataOutputStream out;
    private DataInputStream in;

    public ServerDataTransfer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("[ServerDataTransfer] Esperando conexión de datos en puerto " + port);
        this.dataSocket = serverSocket.accept();
        this.out = new DataOutputStream(dataSocket.getOutputStream());
        this.in = new DataInputStream(dataSocket.getInputStream());
    }

    public void sendFile(File file) throws IOException {
        byte[] compressedData = CompressionUtils.compress(file.toPath());
        out.writeInt(compressedData.length);
        out.write(compressedData);
        out.flush();
    }
    public void sendBytes(byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }


    public byte[] receiveBytes() throws IOException {
        System.out.println("[ServerDataTransfer] Esperando datos...");
        int length = in.readInt();
        System.out.println("[ServerDataTransfer] Leyendo " + length + " bytes");

        byte[] data = new byte[length];
        int bytesRead = 0;
        int totalRead = 0;

        while (totalRead < length) {
            bytesRead = in.read(data, totalRead, length - totalRead);
            if (bytesRead == -1) {
                throw new IOException("Conexión cerrada antes de recibir todos los datos");
            }
            totalRead += bytesRead;
            System.out.println("[ServerDataTransfer] Progreso: " + totalRead + "/" + length + " bytes");
        }

        System.out.println("[ServerDataTransfer] Datos recibidos completamente");
        return data;
    }

    public void receiveFile(File file) throws IOException {
        System.out.println("[ServerDataTransfer] Esperando datos para archivo: " + file.getName());
        byte[] data = receiveBytes();

        System.out.println("[ServerDataTransfer] Descomprimiendo datos en: " + file.getAbsolutePath());
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        CompressionUtils.decompress(data, file.toPath());
    }

    @Override
    public void close() throws IOException {
        try {
            if (in != null) {
                try { in.close(); } catch (IOException e) { /* ignore */ }
                in = null;
            }
            if (out != null) {
                try { out.close(); } catch (IOException e) { /* ignore */ }
                out = null;
            }
            if (dataSocket != null) {
                try {
                    dataSocket.setSoLinger(true, 0); // Cierre inmediato
                    dataSocket.close();
                } catch (IOException e) { /* ignore */ }
                dataSocket = null;
            }
            if (serverSocket != null) {
                try {
                    serverSocket.setReuseAddress(true);
                    serverSocket.close();
                } catch (IOException e) { /* ignore */ }
                serverSocket = null;
            }
        } finally {
            // Esperar un momento para asegurar que el puerto se libere
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
