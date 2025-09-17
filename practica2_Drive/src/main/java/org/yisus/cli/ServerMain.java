package org.yisus.cli;


import org.yisus.server.ServerControl;

public class ServerMain {
    public static void main(String[] args) {
        int controlPort = 2121;
        try {
            System.out.println("=== Quantum Drive Server ===");
            System.out.println("Iniciando servidor en puerto " + controlPort + "...");

            ServerControl server = new ServerControl(controlPort);
            System.out.println("Servidor FTP listo y escuchando en puerto " + controlPort);
            System.out.println("Directorio raíz: C:/Files/Cursos/CursoJava/aplicaciones-redes/Drive/src/server");
            System.out.println("Presiona Ctrl+C para detener el servidor");
            System.out.println("================================");

            server.run();
        } catch (Exception e) {
            System.err.println("Error fatal iniciando el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
