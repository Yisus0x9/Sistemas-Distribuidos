package org.example.server;

import org.example.downloaders.RequestDownloader;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FileServer {
    private static final Map<String, String> repositorioArchivos = new HashMap<>();
    private static final AtomicInteger contadorHilos = new AtomicInteger(0);
    private final BlockingQueue<RequestDownloader> colaSolicitudes = new LinkedBlockingQueue<>();
    private volatile boolean corriendo = true;

    static {
        // Inicializar repositorio de archivos simulados
        repositorioArchivos.put("archivo1.txt", generarContenidoArchivo("archivo1.txt", 5000));
        repositorioArchivos.put("archivo2.txt", generarContenidoArchivo("archivo2.txt", 8000));
        repositorioArchivos.put("archivo3.txt", generarContenidoArchivo("archivo3.txt", 3000));
        repositorioArchivos.put("video.mp4", generarContenidoArchivo("video.mp4", 50000));
        repositorioArchivos.put("imagen.jpg", generarContenidoArchivo("imagen.jpg", 2000));
    }

    public FileServer() {
        for (int i = 0; i < 3; i++) {
            HiloTrabajadorServidor trabajador = new HiloTrabajadorServidor(i + 1);
            imprimirEstadoHilo("SERVIDOR", trabajador, "NEW", "Hilo trabajador creado");
            trabajador.start();
            imprimirEstadoHilo("SERVIDOR", trabajador, "RUNNABLE", "Hilo trabajador iniciado");
        }

        System.out.println("🌐 SERVIDOR: Iniciado con 3 hilos trabajadores");
        System.out.println("📁 Archivos disponibles: " + repositorioArchivos.keySet());
        System.out.println("=" .repeat(70));
    }

    public void procesarSolicitud(RequestDownloader solicitud) {
        try {
            colaSolicitudes.put(solicitud); // Agregar a cola de procesamiento
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String generarContenidoArchivo(String nombre, int tamaño) {
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== ARCHIVO: ").append(nombre.toUpperCase()).append(" ===\n");

        String linea = "Esta es una línea de contenido del archivo " + nombre + " ";
        while (contenido.length() < tamaño) {
            contenido.append(linea).append("línea ").append(contenido.length() / 100).append("\n");
        }

        return contenido.toString();
    }

    // Método para imprimir estados de hilos del servidor
    public static void imprimirEstadoHilo(String tipo, Thread hilo, String estadoLogico, String descripcion) {
        System.out.printf("🔵 %s | Hilo: %-25s  | Estado Lógico: %-15s | %s%n",
                tipo, hilo.getName(), estadoLogico, descripcion);
    }

    class HiloTrabajadorServidor extends Thread {
        private final int numeroTrabajador;

        public HiloTrabajadorServidor(int numero) {
            this.numeroTrabajador = numero;
            setName("Servidor-Trabajador-" + numero);
        }

        @Override
        public void run() {
            while (corriendo) {
                try {
                    imprimirEstadoHilo("SERVIDOR", this, "ESPERANDO", "Esperando solicitudes en cola");

                    // WAITING - esperando solicitudes
                    RequestDownloader solicitud = colaSolicitudes.take();
                    imprimirEstadoHilo("SERVIDOR", this, "EJECUCION", "Procesando solicitud de: " + solicitud.nombreArchivo);

                    // Simular búsqueda en "disco"
                    imprimirEstadoHilo("SERVIDOR", this, "DORMIDO", "Buscando archivo en disco...");
                    Thread.sleep(500 + (int)(Math.random() * 1000)); // TIMED_WAITING

                    String contenido = repositorioArchivos.get(solicitud.nombreArchivo);

                    if (contenido != null) {
                        imprimirEstadoHilo("SERVIDOR", this, "EJECUCION", "Archivo encontrado, preparando envío");

                        // Simular transferencia por chunks
                        int tamaño = contenido.length();
                        int chunkSize = 1000;
                        int totalChunks = (tamaño + chunkSize - 1) / chunkSize;

                        solicitud.respuesta.archivoEncontrado = true;
                        solicitud.respuesta.tamañoArchivo = tamaño;
                        solicitud.respuesta.contenidoCompleto = contenido;

                        // Simular envío chunk por chunk
                        for (int i = 0; i < totalChunks; i++) {
                            int inicio = i * chunkSize;
                            int fin = Math.min(inicio + chunkSize, tamaño);
                            String chunk = contenido.substring(inicio, fin);

                            // BLOCKED - operación de E/S simulada
                            synchronized(solicitud.respuesta) {
                                imprimirEstadoHilo("SERVIDOR", this, "BLOQUEADO",
                                        "Enviando chunk " + (i+1) + "/" + totalChunks + " (E/S)");
                                solicitud.respuesta.chunks.add(chunk);
                                solicitud.respuesta.notify(); // Notificar cliente
                            }

                            // Simular latencia de red
                            imprimirEstadoHilo("SERVIDOR", this, "DORMIDO", "Simulando latencia de red");
                            Thread.sleep(200);
                        }

                        // Marcar como completado
                        synchronized(solicitud.respuesta) {
                            solicitud.respuesta.completo = true;
                            solicitud.respuesta.notifyAll();
                        }

                        System.out.println("✅ SERVIDOR: Archivo " + solicitud.nombreArchivo +
                                " enviado completamente (" + tamaño + " bytes)");

                    } else {
                        imprimirEstadoHilo("SERVIDOR", this, "EJECUCION", "Archivo no encontrado");
                        synchronized(solicitud.respuesta) {
                            solicitud.respuesta.archivoEncontrado = false;
                            solicitud.respuesta.completo = true;
                            solicitud.respuesta.notifyAll();
                        }
                        System.out.println("❌ SERVIDOR: Archivo " + solicitud.nombreArchivo + " no existe");
                    }

                } catch (InterruptedException e) {
                    imprimirEstadoHilo("SERVIDOR", this, "INTERRUMPIDO", "Trabajador interrumpido");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            imprimirEstadoHilo("SERVIDOR", this, "TERMINADO", "Hilo trabajador finalizado");
        }
    }
}