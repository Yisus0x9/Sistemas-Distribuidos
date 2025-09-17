package org.yisus.client;

import org.yisus.quantumdrive.config.Commands;
import org.yisus.services.CompressionUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Clase base para el control del cliente (envía comandos al servidor)
 * Versión corregida con todos los comandos implementados
 */
public class ClientControl {
    private Socket controlSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private static int lastPort = 2020;
    private String serverHost;

    public ClientControl(String host, int port) throws IOException {
        this.serverHost = host;
        this.controlSocket = new Socket(host, port);
        this.in = new DataInputStream(controlSocket.getInputStream());
        this.out = new DataOutputStream(controlSocket.getOutputStream());
    }


    public String sendCommand(Commands command, String[] args) throws IOException {
        String argString = args == null ? "" : String.join(",", args);
        out.writeUTF(command.name().toLowerCase());
        out.writeUTF(argString);
        out.flush();
        return in.readUTF();
    }

    public void close() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (controlSocket != null) controlSocket.close();
    }

    public String upload(String localPath, String remoteDir) throws IOException {
        int dataPort = getNextDataPort();

        File localFile = new File(localPath);
        if (!localFile.exists()) {
            throw new IOException("El archivo local no existe: " + localPath);
        }

        byte[] data = CompressionUtils.compress(localFile.toPath());
        String fileName = localFile.getName();
        String remotePath = (remoteDir + "/" + fileName).replace("\\", "/");

        String resp = sendCommand(Commands.UPLOAD, new String[]{remotePath, String.valueOf(dataPort)});
        if (!"UPLOAD_READY".equals(resp)) {
            throw new IOException("Error al iniciar upload: " + resp);
        }

        try (ClientDataTransfer transfer = new ClientDataTransfer(serverHost, dataPort)) {
            transfer.sendBytes(data);
            String finalResp = in.readUTF();
            if (!"UPLOAD_OK".equals(finalResp)) {
                throw new IOException("Error durante el upload: " + finalResp);
            }
            return "UPLOAD_OK";
        }
    }

    public String download(String remotePath, String localDir) throws IOException {
        int dataPort = getNextDataPort();

        remotePath = remotePath.replace('\\', '/');
        new File(localDir).mkdirs();

        String resp = sendCommand(Commands.DOWNLOAD, new String[]{remotePath, String.valueOf(dataPort)});
        if (!"DOWNLOAD_READY".equals(resp)) {
            throw new IOException("Error al iniciar download: " + resp);
        }

        try (ClientDataTransfer transfer = new ClientDataTransfer(serverHost, dataPort)) {
            byte[] compressedData = transfer.receiveBytes();

            // ✅ Descomprimir directamente
            CompressionUtils.decompress(compressedData, new File(localDir).toPath());

            String finalResp = in.readUTF();
            if (!"DOWNLOAD_OK".equals(finalResp)) {
                throw new IOException("Error durante el download: " + finalResp);
            }
            return "DOWNLOAD_OK";
        }
    }

    public String cd(String dir) throws IOException {
        return sendCommand(Commands.CD, new String[]{dir});
    }

    public String pwd() throws IOException {
        return sendCommand(Commands.PWD, new String[]{});
    }

    public String dir() throws IOException {
        return sendCommand(Commands.DIR, new String[]{});
    }

    public String root() throws IOException {
        return sendCommand(Commands.ROOT, new String[]{});
    }

    public String mkdir(String dirName) throws IOException {
        return sendCommand(Commands.MKDIR, new String[]{dirName});
    }

    public String mkfile(String fileName) throws IOException {
        return sendCommand(Commands.MKFILE, new String[]{fileName});
    }

    public String delete(String path) throws IOException {
        return sendCommand(Commands.DELETE, new String[]{path});
    }

    public String rename(String oldName, String newName) throws IOException {
        return sendCommand(Commands.RENAME, new String[]{oldName, newName});
    }

    public String copy(String src, String dest) throws IOException {
        return sendCommand(Commands.COPY, new String[]{src, dest});
    }

    public String move(String src, String dest) throws IOException {
        return sendCommand(Commands.MOVE, new String[]{src, dest});
    }

    public String stat(String path) throws IOException {
        return sendCommand(Commands.STAT, new String[]{path});
    }

    public String size(String path) throws IOException {
        return sendCommand(Commands.SIZE, new String[]{path});
    }

    public String noop() throws IOException {
        return sendCommand(Commands.NOOP, new String[]{});
    }

    public String quit() throws IOException {
        return sendCommand(Commands.QUIT, new String[]{});
    }

    private synchronized int getNextDataPort() throws IOException {
        int attempts = 3;
        int port = lastPort;

        while (attempts > 0) {
            port = (port >= 65535) ? 2020 : port + 1;

            try {
                ServerSocket testSocket = new ServerSocket(port);
                testSocket.setReuseAddress(true);
                testSocket.close();

                Thread.sleep(100);

                lastPort = port;
                return port;
            } catch (IOException e) {
                attempts--;
                if (attempts == 0) {
                    throw new IOException("No se pudo encontrar un puerto disponible después de varios intentos");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupción mientras se esperaba la liberación del puerto");
            }
        }

        throw new IOException("No se pudo obtener un puerto disponible");
    }

    public String uploadToCurrentDir(String localPath) throws IOException {
        return upload(localPath, ".");
    }

    public String downloadToCurrentDir(String remotePath) throws IOException {
        return download(remotePath, ".");
    }

    public boolean isConnected() {
        try {
            noop();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getServerInfo() {
        return "Conectado a: " + serverHost + " desde puerto: " + controlSocket.getLocalPort();
    }
}