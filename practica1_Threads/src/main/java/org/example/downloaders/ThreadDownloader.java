package org.example.downloaders;

import org.example.server.FileServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.example.client.FileClient.imprimirEstadoHilo;

public class ThreadDownloader extends  Thread{
    private final String nombreArchivo;
    private final int numeroDescarga;
    private volatile boolean pausado = false;
    private final Object lockPausa = new Object();
    private final FileServer server;

    public ThreadDownloader(String nombreArchivo, int numeroDescarga,FileServer server) {
        this.nombreArchivo = nombreArchivo;
        this.numeroDescarga = numeroDescarga;
        setName("Descarga-" + numeroDescarga + "-" + nombreArchivo);
        this.server =server;
    }

    @Override
    public void run() {
        try {
            imprimirEstadoHilo("CLIENTE", this, "EJECUCION", "Iniciando descarga de " + nombreArchivo);

            // Crear solicitud al server
            RequestDownloader solicitud = new RequestDownloader(nombreArchivo);

            imprimirEstadoHilo("CLIENTE", this, "EJECUCION", "Enviando solicitud al server");
            server.procesarSolicitud(solicitud);

            // Esperar respuesta inicial del server
            synchronized(solicitud.respuesta) {
                imprimirEstadoHilo("CLIENTE", this, "ESPERANDO", "Esperando respuesta del server");

                while (!solicitud.respuesta.archivoEncontrado && !solicitud.respuesta.completo) {
                    solicitud.respuesta.wait(); // WAITING
                }
            }

            if (!solicitud.respuesta.archivoEncontrado) {
                imprimirEstadoHilo("CLIENTE", this, "ERROR", "Archivo no encontrado en server");
                System.out.println("❌ CLIENTE: Descarga " + numeroDescarga + " FALLÓ - Archivo no encontrado: " + nombreArchivo);
                return;
            }

            imprimirEstadoHilo("CLIENTE", this, "EJECUCION",
                    "Archivo encontrado, tamaño: " + solicitud.respuesta.tamañoArchivo + " bytes");

            // Crear archivo local simulado
            StringBuilder archivoLocal = new StringBuilder();
            int bytesRecibidos = 0;
            int porcentajeAnterior = -1;

            // Recibir chunks del server
            while (!solicitud.respuesta.completo) {
                verificarPausa(); // Puede ir a WAITING

                synchronized(solicitud.respuesta) {
                    if (solicitud.respuesta.chunks.isEmpty() && !solicitud.respuesta.completo) {
                        imprimirEstadoHilo("CLIENTE", this, "ESPERANDO", "Esperando más datos del server");
                        solicitud.respuesta.wait(1000); // WAITING con timeout
                        continue;
                    }

                    if (!solicitud.respuesta.chunks.isEmpty()) {
                        String chunk = solicitud.respuesta.chunks.remove(0);

                        // BLOCKED - escribiendo archivo simulado
                        imprimirEstadoHilo("CLIENTE", this, "BLOQUEADO", "Escribiendo datos al archivo (E/S)");
                        archivoLocal.append(chunk);
                        bytesRecibidos += chunk.length();
                    }
                }

                // Calcular progreso
                int porcentaje = (bytesRecibidos * 100) / solicitud.respuesta.tamañoArchivo;
                if (porcentaje != porcentajeAnterior && porcentaje % 25 == 0) {
                    imprimirEstadoHilo("CLIENTE", this, "EJECUCION",
                            "Progreso: " + porcentaje + "% (" + bytesRecibidos + "/" +
                                    solicitud.respuesta.tamañoArchivo + " bytes)");
                    porcentajeAnterior = porcentaje;
                }

                // Simular procesamiento de datos
                imprimirEstadoHilo("CLIENTE", this, "DORMIDO", "Procesando chunk recibido");
                Thread.sleep(150); // TIMED_WAITING
            }

            // Guardar archivo simulado
            synchronized(this) {
                imprimirEstadoHilo("CLIENTE", this, "BLOQUEADO", "Finalizando escritura de archivo");
                File archivo = new File("descargado_" + nombreArchivo);
                try (FileWriter writer = new FileWriter(archivo)) {
                    writer.write(archivoLocal.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println("✅ CLIENTE: Descarga " + numeroDescarga + " COMPLETADA - " +
                    nombreArchivo + " (" + bytesRecibidos + " bytes)");

        } catch (InterruptedException e) {
            imprimirEstadoHilo("CLIENTE", this, "INTERRUMPIDO", "Descarga cancelada por el usuario");
            System.out.println("🛑 CLIENTE: Descarga " + numeroDescarga + " INTERRUMPIDA - " + nombreArchivo);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            imprimirEstadoHilo("CLIENTE", this, "ERROR", "Error de E/S: " + e.getMessage());
            System.out.println("💥 CLIENTE: Error en descarga " + numeroDescarga + " - " + e.getMessage());
        } finally {
            imprimirEstadoHilo("CLIENTE", this, "TERMINADO", "Hilo de descarga finalizado");
        }
    }

    private void verificarPausa() throws InterruptedException {
        synchronized(lockPausa) {
            while (pausado) {
                imprimirEstadoHilo("CLIENTE", this, "ESPERANDO", "Descarga pausada - esperando reanudación");
                lockPausa.wait(); // Estado WAITING
            }
        }
    }

    public void pausar() {
        synchronized(lockPausa) {
            pausado = true;
        }
    }

    public void reanudar() {
        synchronized(lockPausa) {
            pausado = false;
            lockPausa.notifyAll();
        }
    }
}