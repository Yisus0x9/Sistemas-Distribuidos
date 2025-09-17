package org.example.downloaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResponseDownloader {
    public volatile boolean archivoEncontrado = false;
    public volatile int tamañoArchivo = 0;
    public volatile boolean completo = false;
    public String contenidoCompleto = "";
    public final List<String> chunks = Collections.synchronizedList(new ArrayList<>());
}