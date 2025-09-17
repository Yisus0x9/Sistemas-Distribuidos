package org.example.downloaders;

public class RequestDownloader {
    public final String nombreArchivo;
    public final ResponseDownloader respuesta;

    public RequestDownloader(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
        this.respuesta = new ResponseDownloader();
    }
}
