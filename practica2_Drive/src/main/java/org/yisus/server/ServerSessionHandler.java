package org.yisus.server;

import org.yisus.services.CompressionUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.Date;

public class ServerSessionHandler implements Runnable {
    private Socket clientSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private String currentDirectory;
    private static final String ROOT_DIR = "src/server";
    private boolean running = true;

    public ServerSessionHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new DataInputStream(clientSocket.getInputStream());
        this.out = new DataOutputStream(clientSocket.getOutputStream());
        this.currentDirectory = new File(ROOT_DIR).getAbsolutePath();

        // Crear directorio raíz si no existe
        File rootFile = new File(ROOT_DIR);
        if (!rootFile.exists()) {
            rootFile.mkdirs();
        }

        System.out.println("[SERVER] Cliente conectado desde " + clientSocket.getInetAddress());
    }

    @Override
    public void run() {
        try {
            while (running) {
                String command = in.readUTF();
                String args = in.readUTF();
                String[] argsArray = args.isEmpty() ? new String[0] : args.split(",");
                handleCommand(command, argsArray);
            }
        } catch (EOFException e) {
            System.out.println("[SERVER] Cliente desconectado normalmente");
        } catch (Exception e) {
            System.out.println("[SERVER] Error en sesión del cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeSession();
        }
    }

    private void handleCommand(String command, String[] args) throws IOException {
        System.out.println("[SERVER] Recibido comando: " + command + " con argumentos: " + String.join(",", args));

        try {
            switch (command) {
                case "root": // ROOT - Obtener directorio raíz
                    String rootPath = new File(ROOT_DIR).getAbsolutePath();
                    System.out.println("[SERVER] Obteniendo directorio raíz: " + rootPath);
                    out.writeUTF(rootPath);
                    break;

                case "noop": // NOOP - No operation
                    System.out.println("[SERVER] Comando NOOP ejecutado");
                    out.writeUTF("NOOP_OK");
                    break;

                case "quit": // QUIT - Cerrar conexión
                    System.out.println("[SERVER] Cliente solicitó desconexión");
                    running = false;
                    out.writeUTF("QUIT_OK");
                    break;

                case "pwd": // PWD - Print working directory
                    String relativeDir = getRelativePath(currentDirectory);
                    System.out.println("[SERVER] Directorio actual: " + relativeDir);
                    out.writeUTF(relativeDir);
                    break;

                case "cd": // CD - Change directory
                    if (args.length < 1) {
                        out.writeUTF("ERROR: Directorio no especificado");
                        break;
                    }
                    handleChangeDirectory(args[0]);
                    break;

                case "dir": // DIR - Listar contenido del directorio
                    handleListDirectory();
                    break;

                case "mkdir": // MKDIR - Crear directorio
                    if (args.length < 1) {
                        out.writeUTF("ERROR: Nombre de directorio no especificado");
                        break;
                    }
                    handleMakeDirectory(args[0]);
                    break;

                case "mkfile": // MKFILE - Crear archivo vacío
                    if (args.length < 1) {
                        out.writeUTF("ERROR: Nombre de archivo no especificado");
                        break;
                    }
                    handleMakeFile(args[0]);
                    break;

                case "delete": // DELETE - Eliminar archivo o directorio
                    if (args.length < 1) {
                        out.writeUTF("ERROR: Ruta no especificada");
                        break;
                    }
                    handleDelete(args[0]);
                    break;

                case "rename": // RENAME - Renombrar archivo o directorio
                    if (args.length < 2) {
                        out.writeUTF("ERROR: Nombres insuficientes para renombrar");
                        break;
                    }
                    handleRename(args[0], args[1]);
                    break;

                case "copy": // COPY - Copiar archivo o directorio
                    if (args.length < 2) {
                        out.writeUTF("ERROR: Rutas insuficientes para copiar");
                        break;
                    }
                    handleCopy(args[0], args[1]);
                    break;

                case "mv": // MOVE - Mover archivo o directorio
                    if (args.length < 2) {
                        out.writeUTF("ERROR: Rutas insuficientes para mover");
                        break;
                    }
                    handleMove(args[0], args[1]);
                    break;

                case "stat": // STAT - Obtener información del archivo
                    if (args.length < 1) {
                        out.writeUTF("ERROR: Ruta no especificada");
                        break;
                    }
                    handleStat(args[0]);
                    break;

                case "size": // SIZE - Obtener tamaño del archivo
                    if (args.length < 1) {
                        out.writeUTF("ERROR: Ruta no especificada");
                        break;
                    }
                    handleSize(args[0]);
                    break;

                case "download": // DOWNLOAD
                    if (args.length < 2) {
                        out.writeUTF("ERROR: Argumentos insuficientes para DOWNLOAD");
                        break;
                    }
                    System.out.println(args[0]);
                    System.out.println(args[1]);
                    handleDownload(args[0], Integer.parseInt(args[1]));
                    break;

                case "upload": // UPLOAD
                    if (args.length < 2) {
                        out.writeUTF("ERROR: Argumentos insuficientes para UPLOAD");
                        break;
                    }
                    handleUpload(args[0], Integer.parseInt(args[1]));
                    break;

                default:
                    out.writeUTF("ERROR: Comando no reconocido: " + command);
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Error ejecutando comando " + command + ": " + e.getMessage());
            e.printStackTrace();
            out.writeUTF("ERROR: " + e.getMessage());
        }
        out.flush();
    }

    private void handleChangeDirectory(String dirName) throws IOException {
        File newDir;

        if (dirName.equals("..")) {
            // Subir un nivel
            File current = new File(currentDirectory);
            File parent = current.getParentFile();
            File root = new File(ROOT_DIR).getAbsoluteFile();

            if (parent != null && (parent.equals(root) || parent.getAbsolutePath().startsWith(root.getAbsolutePath()))) {
                newDir = parent;
            } else {
                newDir = root; // No salir del directorio raíz
            }
        } else if (dirName.equals("/") || dirName.equals("~")) {
            // Ir al directorio raíz
            newDir = new File(ROOT_DIR);
        } else {
            // Directorio relativo o absoluto
            if (dirName.startsWith("/")) {
                newDir = new File(ROOT_DIR, dirName.substring(1));
            } else {
                newDir = new File(currentDirectory, dirName);
            }
        }

        if (!newDir.exists() || !newDir.isDirectory()) {
            out.writeUTF("ERROR: El directorio no existe");
            return;
        }

        // Verificar que no salga del directorio raíz
        File rootFile = new File(ROOT_DIR).getAbsoluteFile();
        if (!newDir.getAbsolutePath().startsWith(rootFile.getAbsolutePath())) {
            out.writeUTF("ERROR: Acceso denegado fuera del directorio raíz");
            return;
        }

        currentDirectory = newDir.getAbsolutePath();
        String relativePath = getRelativePath(currentDirectory);
        System.out.println("[SERVER] Directorio cambiado a: " + relativePath);
        out.writeUTF("CD_OK: " + relativePath);
    }

    private void handleListDirectory() throws IOException {
        File dir = new File(currentDirectory);
        File[] files = dir.listFiles();

        if (files == null) {
            out.writeUTF("ERROR: No se puede leer el directorio");
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("DIR_LISTING:\n");

        for (File file : files) {
            String type = file.isDirectory() ? "DIR" : "FILE";
            String size = file.isDirectory() ? "-" : String.valueOf(file.length());
            String modified = new Date(file.lastModified()).toString();

            result.append(String.format("%s\t%s\t%s\t%s\n",
                    type, file.getName(), size, modified));
        }

        System.out.println("[SERVER] Listando directorio: " + files.length + " elementos");
        out.writeUTF(result.toString());
    }

    private void handleMakeDirectory(String dirName) throws IOException {
        File newDir = new File(currentDirectory, dirName);

        if (newDir.exists()) {
            out.writeUTF("ERROR: El directorio ya existe");
            return;
        }

        if (newDir.mkdirs()) {
            System.out.println("[SERVER] Directorio creado: " + dirName);
            out.writeUTF("MKDIR_OK: " + dirName);
        } else {
            out.writeUTF("ERROR: No se pudo crear el directorio");
        }
    }

    private void handleMakeFile(String fileName) throws IOException {
        File newFile = new File(currentDirectory, fileName);

        if (newFile.exists()) {
            out.writeUTF("ERROR: El archivo ya existe");
            return;
        }

        if (newFile.createNewFile()) {
            System.out.println("[SERVER] Archivo creado: " + fileName);
            out.writeUTF("MKFILE_OK: " + fileName);
        } else {
            out.writeUTF("ERROR: No se pudo crear el archivo");
        }
    }

    private void handleDelete(String path) throws IOException {
        File file = new File(currentDirectory, path);

        if (!file.exists()) {
            out.writeUTF("ERROR: El archivo o directorio no existe");
            return;
        }

        if (deleteRecursively(file)) {
            System.out.println("[SERVER] Eliminado: " + path);
            out.writeUTF("DELETE_OK: " + path);
        } else {
            out.writeUTF("ERROR: No se pudo eliminar");
        }
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    private void handleRename(String oldName, String newName) throws IOException {
        File oldFile = new File(currentDirectory, oldName);
        File newFile = new File(currentDirectory, newName);

        if (!oldFile.exists()) {
            out.writeUTF("ERROR: El archivo origen no existe");
            return;
        }

        if (newFile.exists()) {
            out.writeUTF("ERROR: El archivo destino ya existe");
            return;
        }

        if (oldFile.renameTo(newFile)) {
            System.out.println("[SERVER] Renombrado: " + oldName + " -> " + newName);
            out.writeUTF("RENAME_OK: " + oldName + " -> " + newName);
        } else {
            out.writeUTF("ERROR: No se pudo renombrar");
        }
    }

    private void handleCopy(String src, String dest) throws IOException {
        File srcFile = new File(currentDirectory, src);
        File destFile = new File(currentDirectory, dest);

        if (!srcFile.exists()) {
            out.writeUTF("ERROR: El archivo origen no existe");
            return;
        }

        try {
            if (srcFile.isDirectory()) {
                copyDirectory(srcFile.toPath(), destFile.toPath());
            } else {
                Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("[SERVER] Copiado: " + src + " -> " + dest);
            out.writeUTF("COPY_OK: " + src + " -> " + dest);
        } catch (Exception e) {
            out.writeUTF("ERROR: " + e.getMessage());
        }
    }

    private void copyDirectory(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(source -> {
            try {
                Path destination = dest.resolve(src.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void handleMove(String src, String dest) throws IOException {
        File srcFile = new File(currentDirectory, src);
        File destFile = new File(currentDirectory, dest);

        if (!srcFile.exists()) {
            out.writeUTF("ERROR: El archivo origen no existe");
            return;
        }

        try {
            Files.move(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[SERVER] Movido: " + src + " -> " + dest);
            out.writeUTF("MOVE_OK: " + src + " -> " + dest);
        } catch (Exception e) {
            out.writeUTF("ERROR: " + e.getMessage());
        }
    }

    private void handleStat(String path) throws IOException {
        File file = new File(currentDirectory, path);

        if (!file.exists()) {
            out.writeUTF("ERROR: El archivo no existe");
            return;
        }

        StringBuilder info = new StringBuilder();
        info.append("STAT_INFO:\n");
        info.append("Nombre: ").append(file.getName()).append("\n");
        info.append("Tipo: ").append(file.isDirectory() ? "Directorio" : "Archivo").append("\n");
        info.append("Tamaño: ").append(file.length()).append(" bytes\n");
        info.append("Modificado: ").append(new Date(file.lastModified())).append("\n");
        info.append("Lectura: ").append(file.canRead() ? "Sí" : "No").append("\n");
        info.append("Escritura: ").append(file.canWrite() ? "Sí" : "No").append("\n");
        info.append("Ejecución: ").append(file.canExecute() ? "Sí" : "No").append("\n");

        System.out.println("[SERVER] Información de: " + path);
        out.writeUTF(info.toString());
    }

    private void handleSize(String path) throws IOException {
        File file = new File(currentDirectory, path);

        if (!file.exists()) {
            out.writeUTF("ERROR: El archivo no existe");
            return;
        }

        long size = calculateSize(file);
        System.out.println("[SERVER] Tamaño de " + path + ": " + size + " bytes");
        out.writeUTF("SIZE: " + size + " bytes");
    }

    private long calculateSize(File file) {
        if (file.isFile()) {
            return file.length();
        } else if (file.isDirectory()) {
            long size = 0;
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    size += calculateSize(child);
                }
            }
            return size;
        }
        return 0;
    }

    private void handleDownload(String remotePath, int dataPort) throws IOException {
        File fileToSend = new File(currentDirectory, remotePath);
        System.out.println("[SERVER] Archivo solicitado: " + fileToSend.getAbsolutePath());

        if (!fileToSend.exists()) {
            out.writeUTF("ERROR: El archivo no existe");
            return;
        }

        System.out.println("[SERVER] Iniciando DOWNLOAD en puerto " + dataPort);
        out.writeUTF("DOWNLOAD_READY");
        out.flush();

        try (ServerDataTransfer transfer = new ServerDataTransfer(dataPort)) {
            byte[] compressed = CompressionUtils.compress(fileToSend.toPath());
            transfer.sendBytes(compressed);
            out.writeUTF("DOWNLOAD_OK");
        } catch (Exception e) {
            System.err.println("[SERVER] Error en DOWNLOAD: " + e.getMessage());
            out.writeUTF("ERROR: " + e.getMessage());
        }
    }

    private void handleUpload(String uploadPath, int dataPort) throws IOException {
        System.out.println("[SERVER] Ruta de subida recibida: " + uploadPath);

        uploadPath = uploadPath.replace('\\', '/');
        String fileName = new File(uploadPath).getName();

        File targetDir = new File(currentDirectory);

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            String error = "No se pudo crear el directorio destino";
            System.err.println("[SERVER] " + error);
            out.writeUTF("ERROR: " + error);
            return;
        }

        System.out.println("[SERVER] Ruta destino: " + targetDir.getAbsolutePath());

        System.out.println("[SERVER] Iniciando UPLOAD en puerto " + dataPort);
        out.writeUTF("UPLOAD_READY");

        try (ServerDataTransfer transfer = new ServerDataTransfer(dataPort)) {
            byte[] data = transfer.receiveBytes();

            // Descomprime directamente en el directorio actual
            CompressionUtils.decompress(data, targetDir.toPath());

            out.writeUTF("UPLOAD_OK");
        } catch (Exception e) {
            String error = "Error en UPLOAD: " + e.getMessage();
            System.err.println("[SERVER] " + error);
            out.writeUTF("ERROR: " + error);
        }
    }


    private String getRelativePath(String absolutePath) {
        try {
            File rootFile = new File(ROOT_DIR).getAbsoluteFile();
            File currentFile = new File(absolutePath).getAbsoluteFile();

            String rootPath = rootFile.getAbsolutePath();
            String currentPath = currentFile.getAbsolutePath();

            if (currentPath.equals(rootPath)) {
                return "/";
            } else if (currentPath.startsWith(rootPath)) {
                String relative = currentPath.substring(rootPath.length());
                return relative.replace('\\', '/');
            } else {
                return currentPath;
            }
        } catch (Exception e) {
            return absolutePath;
        }
    }

    private void closeSession() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}