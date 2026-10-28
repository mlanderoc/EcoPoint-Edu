package com.ecopoint.ui;

import com.ecopoint.exceptions.EcoPointException;
import com.ecopoint.model.Container;
import com.ecopoint.model.EcologicalPoint;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Zone;
import com.ecopoint.service.EcologicalPointService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Panel providing full CRUD for Institutions, Zones, and EcologicalPoints,
 * integrated with an interactive campus floor-plan map that calculates
 * relative percentage coordinates (coordX and coordY in 0.00 to 100.00%).
 *
 * @author craulgomez
 * @version 1.0
 */
public class CampusAndPointPanel extends JPanel {

    private final EcologicalPointService pointService;
    private final Runnable onDataChanged;

    private JComboBox<String> institutionCombo;
    private JTextField zoneNameField;
    private JTextField zoneDescField;
    private JComboBox<String> zoneCombo;

    private JTextField pointIdField;
    private JTextField locationField;
    private JComboBox<String> statusCombo;
    private JComboBox<String> initialStatusCombo;
    private JTextField coordXField;
    private JTextField coordYField;
    private JTextField capacityField;
    private JTextField observationsField;

    private JTable pointTable;
    private DefaultTableModel pointTableModel;
    private CampusMapCanvas mapCanvas;

    /**
     * Constructs the CampusAndPointPanel.
     *
     * @param pointService  ecological point business service
     * @param onDataChanged callback invoked when data changes
     */
    public CampusAndPointPanel(EcologicalPointService pointService, Runnable onDataChanged) {
        this.pointService = pointService;
        this.onDataChanged = onDataChanged;
        initComponents();
        refreshAll();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(EcoPointTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel leftColumn = new JPanel(new BorderLayout(8, 8));
        leftColumn.setBackground(EcoPointTheme.BG_MAIN);

        JPanel zoneCard = EcoPointTheme.createCardPanel("1. Institución y CRUD de Zonas");
        zoneCard.setLayout(new BorderLayout(6, 6));

        JPanel zoneGrid = new JPanel(new GridLayout(3, 2, 6, 6));
        zoneGrid.setBackground(EcoPointTheme.CARD_BG);

        institutionCombo = new JComboBox<>();
        institutionCombo.setFont(EcoPointTheme.FONT_BODY);

        zoneNameField = new JTextField();
        zoneNameField.setFont(EcoPointTheme.FONT_BODY);

        zoneDescField = new JTextField();
        zoneDescField.setFont(EcoPointTheme.FONT_BODY);

        zoneGrid.add(EcoPointTheme.createLabel("Institución Educativa:"));
        zoneGrid.add(institutionCombo);
        zoneGrid.add(EcoPointTheme.createLabel("Nombre de Nueva Zona:"));
        zoneGrid.add(zoneNameField);
        zoneGrid.add(EcoPointTheme.createLabel("Descripción de la Zona:"));
        zoneGrid.add(zoneDescField);

        JPanel zoneBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        zoneBtnRow.setBackground(EcoPointTheme.CARD_BG);

        JButton addInstBtn = EcoPointTheme.createButton("+ Institución", EcoPointTheme.FOREST_DARK);
        addInstBtn.addActionListener(e -> showAddInstitutionDialog());

        JButton addZoneBtn = EcoPointTheme.createButton("Crear Zona", EcoPointTheme.ECO_GREEN);
        addZoneBtn.addActionListener(e -> createZone());

        JButton deleteZoneBtn = EcoPointTheme.createButton("Eliminar Zona Seleccionada", EcoPointTheme.DANGER_RED);
        deleteZoneBtn.addActionListener(e -> deleteSelectedZone());

        zoneBtnRow.add(addInstBtn);
        zoneBtnRow.add(addZoneBtn);
        zoneBtnRow.add(deleteZoneBtn);

        zoneCard.add(zoneGrid, BorderLayout.CENTER);
        zoneCard.add(zoneBtnRow, BorderLayout.SOUTH);

        JPanel pointCard = EcoPointTheme.createCardPanel(
                "2. CRUD de Puntos Ecológicos (3 Recipientes: Blanco, Verde, Negro)");
        pointCard.setLayout(new BorderLayout(6, 6));

        JPanel pointGrid = new JPanel(new GridLayout(9, 2, 6, 6));
        pointGrid.setBackground(EcoPointTheme.CARD_BG);

        pointIdField = new JTextField();
        pointIdField.setEditable(false);
        pointIdField.setFont(EcoPointTheme.FONT_MONO);
        pointIdField.setBackground(EcoPointTheme.BG_MAIN);

        zoneCombo = new JComboBox<>();
        zoneCombo.setFont(EcoPointTheme.FONT_BODY);

        locationField = new JTextField();
        locationField.setFont(EcoPointTheme.FONT_BODY);

        statusCombo = new JComboBox<>(new String[]{"ACTIVO", "CRITICO", "MANTENIMIENTO", "INACTIVO"});
        statusCombo.setFont(EcoPointTheme.FONT_BODY);

        initialStatusCombo = new JComboBox<>(new String[]{"BUENO", "REGULAR", "MALO"});
        initialStatusCombo.setFont(EcoPointTheme.FONT_BODY);

        coordXField = new JTextField("50.00");
        coordXField.setFont(EcoPointTheme.FONT_MONO);

        coordYField = new JTextField("50.00");
        coordYField.setFont(EcoPointTheme.FONT_MONO);

        capacityField = new JTextField("55.00");
        capacityField.setFont(EcoPointTheme.FONT_MONO);

        observationsField = new JTextField("Equipado con 3 recipientes normativos Res. 2184");
        observationsField.setFont(EcoPointTheme.FONT_BODY);

        pointGrid.add(EcoPointTheme.createLabel("ID Punto (Automático):"));
        pointGrid.add(pointIdField);
        pointGrid.add(EcoPointTheme.createLabel("Zona del Campus:"));
        pointGrid.add(zoneCombo);
        pointGrid.add(EcoPointTheme.createLabel("Ubicación Específica:"));
        pointGrid.add(locationField);
        pointGrid.add(EcoPointTheme.createLabel("Estado Operativo:"));
        pointGrid.add(statusCombo);
        pointGrid.add(EcoPointTheme.createLabel("Estado Físico Inicial:"));
        pointGrid.add(initialStatusCombo);
        pointGrid.add(EcoPointTheme.createLabel("Coord X (%) [Clic en el Mapa]:"));
        pointGrid.add(coordXField);
        pointGrid.add(EcoPointTheme.createLabel("Coord Y (%) [Clic en el Mapa]:"));
        pointGrid.add(coordYField);
        pointGrid.add(EcoPointTheme.createLabel("Capacidad por Recipiente (L):"));
        pointGrid.add(capacityField);
        pointGrid.add(EcoPointTheme.createLabel("Observaciones:"));
        pointGrid.add(observationsField);

        JPanel pointBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pointBtnRow.setBackground(EcoPointTheme.CARD_BG);

        JButton createPointBtn = EcoPointTheme.createButton("Registrar Punto (3 Colores)", EcoPointTheme.ECO_GREEN);
        createPointBtn.addActionListener(e -> createPoint());

        JButton updatePointBtn = EcoPointTheme.createButton("Actualizar Punto", EcoPointTheme.INFO_BLUE);
        updatePointBtn.addActionListener(e -> updatePoint());

        JButton deletePointBtn = EcoPointTheme.createButton("Eliminar Punto", EcoPointTheme.DANGER_RED);
        deletePointBtn.addActionListener(e -> deletePoint());

        JButton demoErrorBtn = EcoPointTheme.createButton("Simular Error Sin 3 Colores", EcoPointTheme.WARNING_AMBER);
        demoErrorBtn.addActionListener(e -> simulateInvalidContainersError());

        pointBtnRow.add(createPointBtn);
        pointBtnRow.add(updatePointBtn);
        pointBtnRow.add(deletePointBtn);
        pointBtnRow.add(demoErrorBtn);

        pointCard.add(pointGrid, BorderLayout.CENTER);
        pointCard.add(pointBtnRow, BorderLayout.SOUTH);

        leftColumn.add(zoneCard, BorderLayout.NORTH);
        leftColumn.add(pointCard, BorderLayout.CENTER);

        JPanel rightColumn = new JPanel(new BorderLayout(8, 8));
        rightColumn.setBackground(EcoPointTheme.BG_MAIN);

        JPanel mapCard = EcoPointTheme.createCardPanel(
                "Plano Institucional Interactivo — Haz clic para capturar Coord X (%) y Coord Y (%)");
        mapCard.setLayout(new BorderLayout());
        mapCanvas = new CampusMapCanvas();
        mapCanvas.setPreferredSize(new Dimension(480, 250));
        mapCard.add(mapCanvas, BorderLayout.CENTER);

        JPanel tableCard = EcoPointTheme.createCardPanel("Puntos Ecológicos Registrados (data/points.txt)");
        tableCard.setLayout(new BorderLayout());

        pointTableModel = new DefaultTableModel(
                new Object[]{"ID", "Zona", "Ubicación", "Estado", "Coord X (%)", "Coord Y (%)", "Recipientes"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pointTable = new JTable(pointTableModel);
        EcoPointTheme.styleTable(pointTable);
        pointTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pointTable.getSelectedRow() >= 0) {
                int row = pointTable.getSelectedRow();
                int idPoint = (Integer) pointTableModel.getValueAt(row, 0);
                loadPointIntoForm(idPoint);
            }
        });

        tableCard.add(new JScrollPane(pointTable), BorderLayout.CENTER);

        rightColumn.add(mapCard, BorderLayout.NORTH);
        rightColumn.add(tableCard, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftColumn, rightColumn);
        splitPane.setDividerLocation(490);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Refreshes institutions, zones, ecological points table, and the campus map.
     */
    public void refreshAll() {
        institutionCombo.removeAllItems();
        for (Institution inst : pointService.listAllInstitutions()) {
            institutionCombo.addItem(inst.getIdInstitution() + " - " + inst.getName());
        }

        zoneCombo.removeAllItems();
        for (Zone z : pointService.listAllZones()) {
            zoneCombo.addItem(z.getIdZone() + " - " + z.getName());
        }

        pointTableModel.setRowCount(0);
        List<EcologicalPoint> points = pointService.listPointsByZone(null);
        for (EcologicalPoint p : points) {
            String zoneLabel = "Zona #" + p.getIdZone();
            try {
                zoneLabel = pointService.getZoneById(p.getIdZone()).getName();
            } catch (Exception ignored) {
            }
            pointTableModel.addRow(new Object[]{
                    p.getIdPoint(),
                    zoneLabel,
                    p.getLocation(),
                    p.getStatus(),
                    p.getCoordX() + "%",
                    p.getCoordY() + "%",
                    "BLANCO · VERDE · NEGRO"
            });
        }
        mapCanvas.repaint();
    }

    private void showAddInstitutionDialog() {
        JTextField nameInput = new JTextField();
        JTextField addressInput = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Nombre de la Institución Educativa:"));
        panel.add(nameInput);
        panel.add(new JLabel("Dirección Física:"));
        panel.add(addressInput);

        int res = JOptionPane.showConfirmDialog(this, panel,
                "Registrar Nueva Institución", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                pointService.registerInstitution(new Institution(0, nameInput.getText(), addressInput.getText()));
                refreshAll();
                notifyDataChanged();
            } catch (EcoPointException ex) {
                showError(ex);
            }
        }
    }

    private void createZone() {
        try {
            int idInst = parseSelectedId(institutionCombo);
            Zone created = pointService.registerZone(new Zone(
                    0, idInst, zoneNameField.getText(), zoneDescField.getText()));
            zoneNameField.setText("");
            zoneDescField.setText("");
            refreshAll();
            notifyDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Zona '" + created.getName() + "' creada con éxito.",
                    "Zona Registrada",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void deleteSelectedZone() {
        try {
            int idZone = parseSelectedId(zoneCombo);
            pointService.deleteZone(idZone);
            refreshAll();
            notifyDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Zona eliminada correctamente.",
                    "Zona Eliminada",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void createPoint() {
        try {
            EcologicalPoint point = buildPointFromForm(0);
            EcologicalPoint saved = pointService.registerEcologicalPoint(point);
            refreshAll();
            notifyDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Punto Ecológico #" + saved.getIdPoint() + " registrado con sus 3 recipientes normativos.",
                    "Punto Registrado",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Las coordenadas X, Y y la capacidad deben ser valores numéricos decimales válidos.",
                    "Formato Numérico Inválido",
                    JOptionPane.ERROR_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void updatePoint() {
        if (pointIdField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero un punto ecológico de la tabla para actualizar.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idPoint = Integer.parseInt(pointIdField.getText().trim());
            EcologicalPoint point = buildPointFromForm(idPoint);
            pointService.updateEcologicalPoint(point);
            refreshAll();
            notifyDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Punto Ecológico #" + idPoint + " actualizado correctamente.",
                    "Punto Actualizado",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Las coordenadas X, Y y la capacidad deben ser números válidos.",
                    "Formato Numérico Inválido",
                    JOptionPane.ERROR_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void deletePoint() {
        if (pointIdField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero un punto ecológico de la tabla para eliminar.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idPoint = Integer.parseInt(pointIdField.getText().trim());
            pointService.deleteEcologicalPoint(idPoint);
            pointIdField.setText("");
            refreshAll();
            notifyDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Punto Ecológico #" + idPoint + " eliminado correctamente.",
                    "Punto Eliminado",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void simulateInvalidContainersError() {
        try {
            int idZone = parseSelectedId(zoneCombo);
            EcologicalPoint invalidPoint = new EcologicalPoint(
                    0, idZone, "Punto de Prueba Incompleto", "ACTIVO", "BUENO",
                    new BigDecimal("40.00"), new BigDecimal("40.00"), "Prueba sin los 3 colores");
            invalidPoint.addContainer(new Container(0, 1, "Aprovechables", "BLANCO", new BigDecimal("55.00")));
            invalidPoint.addContainer(new Container(0, 2, "Orgánicos", "VERDE", new BigDecimal("55.00")));
            pointService.registerEcologicalPoint(invalidPoint);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private EcologicalPoint buildPointFromForm(int idPoint) {
        int idZone = parseSelectedId(zoneCombo);
        BigDecimal coordX = new BigDecimal(coordXField.getText().trim());
        BigDecimal coordY = new BigDecimal(coordYField.getText().trim());
        BigDecimal cap = new BigDecimal(capacityField.getText().trim());

        EcologicalPoint point = new EcologicalPoint(
                idPoint,
                idZone,
                locationField.getText(),
                (String) statusCombo.getSelectedItem(),
                (String) initialStatusCombo.getSelectedItem(),
                coordX,
                coordY,
                observationsField.getText()
        );
        point.addContainer(new Container(idPoint, 1, "Aprovechables (Plástico, Papel, Vidrio)", Container.COLOR_WHITE, cap));
        point.addContainer(new Container(idPoint, 2, "Orgánicos Aprovechables", Container.COLOR_GREEN, cap));
        point.addContainer(new Container(idPoint, 3, "No Aprovechables", Container.COLOR_BLACK, cap));
        return point;
    }

    private void loadPointIntoForm(int idPoint) {
        try {
            EcologicalPoint p = pointService.getEcologicalPointById(idPoint);
            pointIdField.setText(String.valueOf(p.getIdPoint()));
            locationField.setText(p.getLocation());
            statusCombo.setSelectedItem(p.getStatus());
            initialStatusCombo.setSelectedItem(p.getInitialStatus());
            coordXField.setText(p.getCoordX().toPlainString());
            coordYField.setText(p.getCoordY().toPlainString());
            observationsField.setText(p.getObservations());
            if (p.getContainers() != null && !p.getContainers().isEmpty()) {
                capacityField.setText(p.getContainers().get(0).getCapacity().toPlainString());
            }
            for (int i = 0; i < zoneCombo.getItemCount(); i++) {
                if (zoneCombo.getItemAt(i).startsWith(p.getIdZone() + " -")) {
                    zoneCombo.setSelectedIndex(i);
                    break;
                }
            }
            mapCanvas.repaint();
        } catch (Exception ignored) {
        }
    }

    private int parseSelectedId(JComboBox<String> combo) {
        String selected = (String) combo.getSelectedItem();
        if (selected == null || !selected.contains(" - ")) {
            return 0;
        }
        return Integer.parseInt(selected.split(" - ")[0].trim());
    }

    private void notifyDataChanged() {
        if (onDataChanged != null) {
            onDataChanged.run();
        }
    }

    private void showError(EcoPointException ex) {
        JOptionPane.showMessageDialog(this,
                ex.getMessage(),
                "Validación / Regla de Negocio (" + ex.getErrorCode() + ")",
                JOptionPane.ERROR_MESSAGE);
    }

    private class CampusMapCanvas extends JPanel {

        CampusMapCanvas() {
            setBackground(new Color(230, 242, 234));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int w = Math.max(1, getWidth());
                    int h = Math.max(1, getHeight());
                    BigDecimal pctX = BigDecimal.valueOf((e.getX() * 100.0) / w)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal pctY = BigDecimal.valueOf((e.getY() * 100.0) / h)
                            .setScale(2, RoundingMode.HALF_UP);
                    coordXField.setText(pctX.toPlainString());
                    coordYField.setText(pctY.toPlainString());
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(205, 226, 212));
            for (int x = 0; x < w; x += w / 10) {
                g2.drawLine(x, 0, x, h);
            }
            for (int y = 0; y < h; y += h / 10) {
                g2.drawLine(0, y, w, y);
            }

            drawCampusBuilding(g2, (int) (w * 0.08), (int) (h * 0.10), (int) (w * 0.34), (int) (h * 0.28),
                    "BLOQUE AULAS NORTE");
            drawCampusBuilding(g2, (int) (w * 0.54), (int) (h * 0.10), (int) (w * 0.38), (int) (h * 0.28),
                    "CAFETERÍA CENTRAL");
            drawCampusBuilding(g2, (int) (w * 0.25), (int) (h * 0.46), (int) (w * 0.50), (int) (h * 0.22),
                    "PATIO PRINCIPAL / ZONA VERDE");
            drawCampusBuilding(g2, (int) (w * 0.10), (int) (h * 0.74), (int) (w * 0.35), (int) (h * 0.20),
                    "BIBLIOTECA");
            drawCampusBuilding(g2, (int) (w * 0.55), (int) (h * 0.74), (int) (w * 0.35), (int) (h * 0.20),
                    "BLOQUE ADMINISTRATIVO");

            List<EcologicalPoint> points = pointService.listPointsByZone(null);
            for (EcologicalPoint p : points) {
                int px = (int) ((p.getCoordX().doubleValue() / 100.0) * w);
                int py = (int) ((p.getCoordY().doubleValue() / 100.0) * h);

                Color pinColor = EcoPointTheme.ECO_GREEN;
                if ("CRITICO".equalsIgnoreCase(p.getStatus())) {
                    pinColor = EcoPointTheme.DANGER_RED;
                } else if ("MANTENIMIENTO".equalsIgnoreCase(p.getStatus())) {
                    pinColor = EcoPointTheme.WARNING_AMBER;
                }

                g2.setColor(Color.WHITE);
                g2.fillOval(px - 10, py - 10, 20, 20);
                g2.setColor(pinColor);
                g2.fillOval(px - 8, py - 8, 16, 16);

                g2.setColor(EcoPointTheme.FOREST_DARK);
                g2.setFont(EcoPointTheme.FONT_SMALL);
                g2.drawString("P#" + p.getIdPoint(), px + 10, py + 4);
            }

            try {
                double selX = Double.parseDouble(coordXField.getText().trim());
                double selY = Double.parseDouble(coordYField.getText().trim());
                int cx = (int) ((selX / 100.0) * w);
                int cy = (int) ((selY / 100.0) * h);
                g2.setColor(EcoPointTheme.INFO_BLUE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(cx - 12, cy - 12, 24, 24);
                g2.drawLine(cx - 16, cy, cx + 16, cy);
                g2.drawLine(cx, cy - 16, cx, cy + 16);
            } catch (Exception ignored) {
            }
        }

        private void drawCampusBuilding(Graphics2D g2, int x, int y, int bw, int bh, String label) {
            g2.setColor(new Color(255, 255, 255, 210));
            g2.fillRoundRect(x, y, bw, bh, 12, 12);
            g2.setColor(EcoPointTheme.FOREST_DARK);
            g2.drawRoundRect(x, y, bw, bh, 12, 12);
            g2.setFont(EcoPointTheme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (bw - fm.stringWidth(label)) / 2;
            int ty = y + (bh + fm.getAscent()) / 2 - 2;
            g2.drawString(label, tx, ty);
        }
    }
}
