package org.yisus.services;

import java.io.*;
import java.util.zip.*;
import java.nio.file.*;

/**
 * Utilidades para comprimir y descomprimir archivos y carpetas
 */
public class CompressionUtils {

    public static byte[] compress(Path sourcePath) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            File fileToZip = sourcePath.toFile();
            zipFile(fileToZip, fileToZip.getName(), zos);
        }
        return baos.toByteArray();
    }

    public static void decompress(byte[] data, Path destDir) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ZipInputStream zis = new ZipInputStream(bais)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = newFile(destDir.toFile(), entry);
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }


    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) return;
        if (fileToZip.isDirectory()) {
            if (!fileName.endsWith("/")) fileName += "/";
            zos.putNextEntry(new ZipEntry(fileName));
            zos.closeEntry();
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + childFile.getName(), zos);
                }
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[4096];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry fuera del directorio destino: " + zipEntry.getName());
        }
        return destFile;
    }
}

