package org.yisus.cli;


import org.yisus.client.ClientControl;

import java.util.Scanner;
import java.io.File;

public class ClientMain {
    public static void main(String[] args) {
        try {
            String serverHost = "localhost";
            int controlPort = 2121;
            int dataPort = 2020;
            String localRoot = "src/local"; // Carpeta local simulando la máquina cliente
            String serverRoot = "src/server"; // Carpeta raíz del servidor (para referencia)
            ClientControl client = new ClientControl(serverHost, controlPort);

            // Verificar conexión (acepta "NOOP" o "NOOP_OK")
            String ping = client.noop();
            if (!"NOOP_OK".equalsIgnoreCase(ping) && !"NOOP".equalsIgnoreCase(ping)) {
                System.out.println("No se pudo conectar al servidor. Respuesta: " + ping);
                client.close();
                return;
            }
            System.out.println("Conectado al servidor correctamente.");

            // Solicitar y mostrar el directorio raíz del servidor
            String rootDir = client.root();
            System.out.println("Directorio raíz del servidor: " + rootDir);

            // Descargar el contenido del directorio raíz y guardarlo en local
            System.out.println("Preparando para descargar el contenido del directorio raíz a la carpeta local...");
            String remoteRootPath = ""; // o "/" si así lo requiere el servidor
            File localRootDir = new File(localRoot);
            if (!localRootDir.exists()) localRootDir.mkdirs();

            // Mensaje de depuración antes de descargar
            System.out.println("Llamando a client.download(remoteRootPath, localRoot)...");
            String downloadResp = client.download(remoteRootPath, localRoot);
            System.out.println("Respuesta de descarga: " + downloadResp);
            System.out.println("Descarga inicial finalizada.");

            Scanner scanner = new Scanner(System.in);
            System.out.println("Escribe comandos FTP personalizados (escribe 'quit' para salir):");
            String ruta="ftp> ";
            while (true) {
                System.out.print(ruta);
                String input = scanner.nextLine();
                if (input.trim().equalsIgnoreCase("quit")) {
                    System.out.println("QUIT: " + client.quit());
                    break;
                }
                String[] parts = input.trim().split("\\s+");
                String cmd = parts[0].toLowerCase();
                String[] cmdArgs = parts.length > 1 ? java.util.Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                try {
                    switch (cmd) {
                        case "mkdir":
                            System.out.println("MKDIR: " + client.mkdir(cmdArgs[0]));
                            break;
                        case "mkfile":
                            System.out.println("MKFILE: " + client.mkfile(cmdArgs[0]));
                            break;
                        case "delete":
                            System.out.println("DELETE: " + client.delete(cmdArgs[0]));
                            break;
                        case "rename":
                            System.out.println("RENAME: " + client.rename(cmdArgs[0], cmdArgs[1]));
                            break;
                        case "copy":
                            System.out.println("COPY: " + client.copy(cmdArgs[0], cmdArgs[1]));
                            break;
                        case "move":
                            System.out.println("MOVE: " + client.move(cmdArgs[0], cmdArgs[1]));
                            break;
                        case "stat":
                            System.out.println("STAT: " + client.stat(cmdArgs[0]));
                            break;
                        case "size":
                            System.out.println("SIZE: " + client.size(cmdArgs[0]));
                            break;
                        case "dir":
                            System.out.println("DIR: " + client.dir());
                            break;
                        case "cd":
                            String res= client.cd(cmdArgs[0]);
                            if(res.contains("CD_OK")){
                                String clean= res.substring(res.indexOf("/"), res.length()-1);
                                String sust=ruta.substring(ruta.indexOf(">"), ruta.length()-1);
                                if(ruta.contains("/"))
                                    sust=ruta.substring(ruta.indexOf("/"), ruta.length()-1);
                                ruta=ruta.replace(sust,clean+">");
                            }

                            System.out.println("CD: " + res);
                            break;
                        case "pwd":
                            System.out.println("PWD: " + client.pwd());
                            break;
                        case "root":
                            System.out.println("ROOT: " + client.root());
                            break;
                        case "noop":
                            System.out.println("NOOP: " + client.noop());
                            break;
                        case "upl":
                            // upload <ruta_local>
                            if (cmdArgs.length < 1) {
                                System.out.println("Uso: upload <ruta_local>");
                                break;
                            }
                            // Combinar todos los argumentos como la ruta local
                            String localPath = String.join(" ", cmdArgs);
                            System.out.println("UPLOAD: " + client.upload(localPath, "."));
                            break;
                        case "dwl":
                            // download <ruta_remota> <destino_local>
                            if (cmdArgs.length < 2) {
                                System.out.println("Uso: download <ruta_remota> <destino_local>");
                                break;
                            }
                            String remotePath = cmdArgs[0];
                            String destLocal = cmdArgs[1];
                            System.out.println("DOWNLOAD: " + client.download(remotePath, destLocal));
                            break;
                        default:
                            System.out.println("Comando no reconocido o argumentos insuficientes.");
                    }
                } catch (Exception e) {
                    System.out.println("Error ejecutando comando: " + e.getMessage());
                }
            }
            client.close();
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}