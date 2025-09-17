package org.example.client;

import org.example.downloaders.ThreadDownloader;


public class FileClient {
    public FileClient() {
    }
    
    public void pausarDescarga(ThreadDownloader descarga) {
        descarga.pausar();
        imprimirEstadoHilo("CLIENTE", descarga, "PAUSADO", "Descarga pausada por usuario");
    }

    public void reanudarDescarga(ThreadDownloader descarga) {
        descarga.reanudar();
        imprimirEstadoHilo("CLIENTE", descarga, "REANUDADO", "Descarga reanudada por usuario");
    }

    public void interrumpirDescarga(ThreadDownloader descarga) {
        descarga.interrupt();
        imprimirEstadoHilo("CLIENTE", descarga, "INTERRUMPIDO", "Descarga interrumpida por usuario");
    }

    // Método para imprimir estados de hilos del cliente
    public static void imprimirEstadoHilo(String tipo, Thread hilo, String estadoLogico, String descripcion) {
        System.out.printf("🔴 %s | Hilo: %-25s | Estado Lógico: %-15s | %s%n",
                tipo, hilo.getName(), estadoLogico, descripcion);
    }

   
}
