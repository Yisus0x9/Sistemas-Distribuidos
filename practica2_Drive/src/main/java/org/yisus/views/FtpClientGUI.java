package org.yisus.views;



import org.yisus.client.ClientControl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class FtpClientGUI extends JFrame {

    private ClientControl client;
    private DefaultListModel<String> fileListModel;
    private JList<String> fileList;
    private JTextArea console;
    private JTextField pathField;
    private List<String> rawDirEntries = new java.util.ArrayList<>();


    public FtpClientGUI() {
        super("🌐 QuantumDrive FTP");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
        connectAndInitialize();
    }

    private void showContextMenu(MouseEvent e, int index) {
        JPopupMenu menu = new JPopupMenu();

        if (index >= 0 && index < rawDirEntries.size()) {
            String raw = rawDirEntries.get(index);
            boolean isDir = raw.startsWith("DIR");
            String name = extractName(raw, isDir ? "DIR" : "FILE");

            JMenuItem renameItem = new JMenuItem("✏️ Renombrar");
            renameItem.addActionListener(ae -> renameEntry(name));

            JMenuItem deleteItem = new JMenuItem("🗑 Eliminar");
            deleteItem.addActionListener(ae -> deleteEntry(name));

            menu.add(renameItem);
            menu.add(deleteItem);

            if (isDir) {
                menu.addSeparator();

                JMenuItem newFile = new JMenuItem("📄 Crear archivo");
                JMenuItem newFolder = new JMenuItem("📁 Crear carpeta");

                newFile.addActionListener(ae -> createNewFile());
                newFolder.addActionListener(ae -> createNewFolder());

                menu.add(newFile);
                menu.add(newFolder);
            }

        } else {
            JMenuItem newFile = new JMenuItem("📄 Crear archivo");
            JMenuItem newFolder = new JMenuItem("📁 Crear carpeta");

            newFile.addActionListener(ae -> createNewFile());
            newFolder.addActionListener(ae -> createNewFolder());

            menu.add(newFile);
            menu.add(newFolder);
        }

        menu.show(fileList, e.getX(), e.getY());
    }


    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Ruta actual
        pathField = new JTextField();
        pathField.setEditable(false);
        mainPanel.add(pathField, BorderLayout.NORTH);

        // Lista de archivos
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setFont(new Font("monospaced", Font.PLAIN, 14));

        fileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = fileList.locationToIndex(e.getPoint());

                if (SwingUtilities.isRightMouseButton(e)) {
                    fileList.setSelectedIndex(index); // selecciona el elemento si da clic derecho sobre él
                    showContextMenu(e, index);
                }

                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (index < 0 || index >= rawDirEntries.size()) return;

                    String raw = rawDirEntries.get(index);
                    if (raw.startsWith("DIR")) {
                        String folderName = extractName(raw, "DIR");
                        try {
                            String res = client.cd(folderName);
                            if (res.contains("CD_OK")) {
                                refreshDir();
                            } else {
                                showError("No se pudo acceder: " + res);
                            }
                        } catch (Exception ex) {
                            showError("Error al acceder: " + ex.getMessage());
                        }
                    }
                }
            }
        });





        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(300, 300));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton uploadButton = new JButton("📤 Subir");
        JButton downloadButton = new JButton("⬇️ Descargar");
        JButton refreshButton = new JButton("🔄 Refrescar");
        JButton backButton = new JButton("⬅️ Atrás");
        JButton quitButton = new JButton("❌ Salir");

        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(backButton);
        buttonPanel.add(quitButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Consola
        console = new JTextArea(6, 40);
        console.setEditable(false);
        console.setFont(new Font("monospaced", Font.PLAIN, 12));
        console.setBorder(BorderFactory.createTitledBorder("Consola"));

        mainPanel.add(new JScrollPane(console), BorderLayout.EAST);

        // Acciones
        uploadButton.addActionListener(e -> uploadFile());
        refreshButton.addActionListener(e -> refreshDir());
        backButton.addActionListener(e -> goBack());
        quitButton.addActionListener(e -> quit());
        downloadButton.addActionListener(e -> downloadSelected());

        setContentPane(mainPanel);
    }

    private void connectAndInitialize() {
        try {
            client = new ClientControl("localhost", 2121);
            if (!client.isConnected()) {
                showError("No se pudo conectar al servidor");
                System.exit(1);
            }
            showMessage("✅ Conectado al servidor correctamente");
            refreshDir();
        } catch (Exception e) {
            showError("❌ Error al conectar: " + e.getMessage());
        }
    }

    private String extractName(String line, String prefix) {
        try {
            String sinPrefix = line.substring(prefix.length()).trim();

            // Si es DIR y contiene un "-", extraer antes del guión
            if (prefix.equals("DIR")) {
                int dashIndex = sinPrefix.indexOf('-');
                if (dashIndex > 0) {
                    return sinPrefix.substring(0, dashIndex).trim();
                } else {
                    return sinPrefix.trim();
                }
            }

            // Para FILE: usa tabulador o espacios
            int idx = sinPrefix.indexOf('\t');
            if (idx == -1) {
                idx = sinPrefix.indexOf("  "); // dos espacios o más
            }

            if (idx > 0) {
                return sinPrefix.substring(0, idx).trim();
            } else {
                return sinPrefix.trim(); // por si no hay metadatos
            }
        } catch (Exception e) {
            return line.trim();
        }
    }



    private void refreshDir() {
        try {
            String pwd = client.pwd();
            pathField.setText(pwd);
            fileListModel.clear();
            rawDirEntries.clear();

            String[] lines = client.dir().split("\n");
            for (String rawLine : lines) {
                if (rawLine.trim().isEmpty()) continue;

                rawDirEntries.add(rawLine.trim());
                String formattedLine = formatDirEntry(rawLine.trim());
                fileListModel.addElement(formattedLine);
            }

            showMessage("📁 Listado de: " + pwd);
        } catch (Exception e) {
            showError("Error al refrescar: " + e.getMessage());
        }
    }


    private String formatDirEntry(String line) {
        try {
            if (line.startsWith("FILE")) {
                String info = line.substring(4).trim(); // remove "FILE"
                int tabIndex = info.indexOf('\t');
                if (tabIndex == -1) tabIndex = info.indexOf("  "); // fallback if no tab

                String name = info.substring(0, tabIndex).trim();
                String rest = info.substring(tabIndex).trim();

                String[] parts = rest.split("\t| {2,}");
                String size = parts.length > 0 ? parts[0] : "";
                String date = parts.length > 1 ? rest.substring(size.length()).trim() : "";

                return String.format("📄 %-25s | %6s B | %s", name, size, date);
            } else if (line.startsWith("DIR")) {
                String info = line.substring(3).trim(); // remove "DIR"

                int dashIndex = info.indexOf('-');
                String name, date;

                if (dashIndex > 0) {
                    name = info.substring(0, dashIndex).trim();
                    date = info.substring(dashIndex + 1).trim();
                } else {
                    name = info.trim();
                    date = "";
                }

                return String.format("📁 %-25s |         | %s", name, date);
            } else {
                return line;
            }
        } catch (Exception e) {
            return line; // fallback
        }
    }



    private void goBack() {
        try {
            String res = client.cd("..");
            if (res.contains("CD_OK")) {
                refreshDir();
            } else {
                showError("Error al retroceder: " + res);
            }
        } catch (Exception e) {
            showError("Error al retroceder: " + e.getMessage());
        }
    }

    private void downloadSelected() {
        int index = fileList.getSelectedIndex();
        if (index < 0 || index >= rawDirEntries.size()) {
            showError("Selecciona un elemento válido para descargar");
            return;
        }

        String raw = rawDirEntries.get(index);
        boolean isDir = raw.startsWith("DIR");
        boolean isFile = raw.startsWith("FILE");

        if (!isDir && !isFile) {
            showError("Elemento desconocido.");
            return;
        }

        String name = extractName(raw, isDir ? "DIR" : "FILE");

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = chooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File localDir = chooser.getSelectedFile();
            try {
                showMessage("⬇️ Descargando: " + name);
                String resp = client.download(name, localDir.getAbsolutePath());
                showMessage("✅ " + resp);
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error al descargar: " + e.getMessage());
            }
        }
    }



    private void uploadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            try {
                showMessage("📤 Subiendo: " + selected.getAbsolutePath());
                String resp = client.upload(selected.getAbsolutePath(), pathField.getText());
                showMessage("✅ " + resp);
                refreshDir();
            } catch (Exception e) {
                showError("Error al subir: " + e.getMessage());
            }
        }
    }

    private void quit() {
        try {
            client.quit();
            client.close();
            System.exit(0);
        } catch (Exception e) {
            showError("Error al cerrar: " + e.getMessage());
        }
    }

    private void renameEntry(String oldName) {
        String newName = JOptionPane.showInputDialog(this, "Nuevo nombre:", oldName);
        if (newName != null && !newName.trim().isEmpty()) {
            try {
                String resp = client.rename(oldName, newName.trim());
                showMessage("✅ " + resp);
                refreshDir();
            } catch (Exception e) {
                showError("Error al renombrar: " + e.getMessage());
            }
        }
    }

    private void deleteEntry(String name) {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar '" + name + "'?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String resp = client.delete(name);
                showMessage("✅ " + resp);
                refreshDir();
            } catch (Exception e) {
                showError("Error al eliminar: " + e.getMessage());
            }
        }
    }

    private void createNewFile() {
        String name = JOptionPane.showInputDialog(this, "Nombre del archivo:");
        if (name != null && !name.trim().isEmpty()) {
            try {
                String resp = client.mkfile(name.trim());
                showMessage("✅ " + resp);
                refreshDir();
            } catch (Exception e) {
                showError("Error al crear archivo: " + e.getMessage());
            }
        }
    }

    private void createNewFolder() {
        String name = JOptionPane.showInputDialog(this, "Nombre de la carpeta:");
        if (name != null && !name.trim().isEmpty()) {
            try {
                String resp = client.mkdir(name.trim());
                showMessage("✅ " + resp);
                refreshDir();
            } catch (Exception e) {
                showError("Error al crear carpeta: " + e.getMessage());
            }
        }
    }

    private void showMessage(String msg) {
        console.append(msg + "\n");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        console.append("❌ " + msg + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FtpClientGUI().setVisible(true));
    }
}
