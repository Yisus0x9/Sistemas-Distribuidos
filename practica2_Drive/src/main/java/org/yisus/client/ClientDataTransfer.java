package org.yisus.client;

import java.io.*;
import java.net.Socket;

/**
 * Maneja la transferencia de datos (archivos/carpetas) del lado del cliente
 */
public class ClientDataTransfer implements AutoCloseable {
    private Socket dataSocket;
    private DataOutputStream out;
    private DataInputStream in;

    public ClientDataTransfer(String host, int port) throws IOException {
        this.dataSocket = new Socket(host, port);
        this.out = new DataOutputStream(dataSocket.getOutputStream());
        this.in = new DataInputStream(dataSocket.getInputStream());
    }


    public void sendBytes(byte[] data) throws IOException {
        System.out.println("[ClientDataTransfer] Iniciando envío de " + data.length + " bytes");
        try {
            out.writeInt(data.length);

            int chunkSize = 4096;
            int offset = 0;
            while (offset < data.length) {
                int remaining = data.length - offset;
                int thisChunk = Math.min(chunkSize, remaining);
                out.write(data, offset, thisChunk);
                offset += thisChunk;
                System.out.println("[ClientDataTransfer] Progreso: " + offset + "/" + data.length + " bytes");
            }
            out.flush();
            System.out.println("[ClientDataTransfer] Datos enviados exitosamente");
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        } catch (IOException e) {
            System.err.println("[ClientDataTransfer] Error al enviar datos: " + e.getMessage());
            throw e;
        }
    }


    public byte[] receiveBytes() throws IOException {
        System.out.println("[ClientDataTransfer] Iniciando recepción de datos");

        // Leer el tamaño total de los datos
        int totalSize = in.readInt();
        byte[] data = new byte[totalSize];

        // Leer los datos en chunks
        int bytesRead = 0;
        int chunkSize = 4096;

        while (bytesRead < totalSize) {
            int remaining = totalSize - bytesRead;
            int thisChunk = Math.min(chunkSize, remaining);
            int count = in.read(data, bytesRead, thisChunk);
            if (count < 0) {
                throw new IOException("Conexión cerrada prematuramente");
            }
            bytesRead += count;
            System.out.println("[ClientDataTransfer] Progreso: " + bytesRead + "/" + totalSize + " bytes");
        }
        System.out.println("[ClientDataTransfer] Datos recibidos exitosamente");
        return data;
    }

    @Override
    public void close() throws IOException {
        if (out != null) out.close();
        if (in != null) in.close();
        if (dataSocket != null) dataSocket.close();
    }

    public Socket getSocket() {
        return dataSocket;
    }
}