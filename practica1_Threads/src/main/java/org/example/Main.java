package org.example;

import org.example.client.FileClient;
import org.example.downloaders.ThreadDownloader;
import org.example.server.FileServer;

import static org.example.client.FileClient.imprimirEstadoHilo;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("🔥INICIANDO MULTIDOWNLOADER");
        System.out.println("=" .repeat(70));

        // Crear servidor
        FileServer servidor = new FileServer();
        FileClient cliente = new FileClient();

        // Crear múltiples hilos de descarga
        ThreadDownloader[] descargas = {
                new ThreadDownloader("archivo1.txt", 1,servidor),
                new ThreadDownloader("archivo2.txt", 2,servidor),
                new ThreadDownloader("video.mp4", 3,servidor),
                new ThreadDownloader("archivo_inexistente.txt", 4,servidor),
                new ThreadDownloader("imagen.jpg", 5,servidor)
        };

        // Mostrar estado NEW
        for (ThreadDownloader descarga : descargas) {
            imprimirEstadoHilo("CLIENTE", descarga, "NEW", "Hilo de descarga creado");
        }

        System.out.println("\n🚀 INICIANDO TODAS LAS DESCARGAS...\n");

        // Iniciar todas las descargas
        for (ThreadDownloader descarga : descargas) {
            descarga.start();
            imprimirEstadoHilo("CLIENTE", descarga, "RUNNABLE", "Hilo iniciado y listo");
            Thread.sleep(100); // Pequeño delay para ver el orden
        }

        // Demostrar control de hilos después de un tiempo
        Thread controlador = new Thread(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("\n🔄 DEMOSTRANDO CONTROL DE HILOS...\n");

                // Pausar descarga 2
                if (descargas[1].isAlive()) {
                    cliente.pausarDescarga(descargas[1]);
                    Thread.sleep(2000);
                    cliente.reanudarDescarga(descargas[1]);
                }

                Thread.sleep(2000);
                // Interrumpir descarga 3 si aún está corriendo
                if (descargas[2].isAlive()) {
                    cliente.interrumpirDescarga(descargas[2]);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        controlador.setName("Controlador-Hilos");
        controlador.start();

        // Esperar a que terminen todas las descargas
        for (ThreadDownloader descarga : descargas) {
            descarga.join();
        }
        controlador.join();

        System.out.println("\n🎉 TODAS LAS DESCARGAS FINALIZADAS");
        System.out.println("=" .repeat(70));
    }

}