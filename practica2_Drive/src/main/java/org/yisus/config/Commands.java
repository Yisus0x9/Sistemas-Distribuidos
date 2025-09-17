package org.yisus.quantumdrive.config;

/**
 * Enumeración de comandos del protocolo FTP personalizado
 * Cada comando tiene un código único y descripción de su propósito
 */
public enum Commands {
    // Comandos de navegación
    DIR(1, "Listar contenido de directorio"),
    CD(2, "Cambiar directorio"),
    PWD(3, "Mostrar directorio actual"),
    ROOT(0, "Obtener directorio raíz del servidor"),

    // Comandos de transferencia
    UPLOAD(4, "Subir archivo/carpeta al servidor"),
    DOWNLOAD(5, "Descargar archivo/carpeta del servidor"),

    // Comandos de manipulación de archivos
    MKDIR(6, "Crear directorio"),
    MKFILE(7, "Crear archivo"),
    DELETE(8, "Eliminar archivo/carpeta"),
    RENAME(9, "Renombrar archivo/carpeta"),
    COPY(10, "Copiar archivo/carpeta"),
    MOVE(11, "Mover archivo/carpeta"),

    // Comandos de información
    STAT(12, "Obtener información de archivo/carpeta"),
    SIZE(13, "Obtener tamaño de archivo/carpeta"),

    // Comandos de control
    QUIT(99, "Cerrar conexión"),
    NOOP(98, "No operación (keep-alive)");

    private final int codigo;
    private final String descripcion;

    Commands(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Obtiene un comando por su código
     */
    public static Commands fromCodigo(int codigo) {
        for (Commands cmd : Commands.values()) {
            if (cmd.codigo == codigo) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("Código de comando no válido: " + codigo);
    }

    /**
     * Valida si un comando requiere transferencia de datos
     */
    public boolean requiereTransferenciaDatos() {
        return this == UPLOAD || this == DOWNLOAD || this == DIR ||
                this == MKDIR || this == MKFILE || this == DELETE ||
                this == RENAME || this == COPY || this == MOVE;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

