package com.ecopoint.ui;

import com.ecopoint.exceptions.EcoPointException;
import com.ecopoint.model.ContainerDetail;
import com.ecopoint.model.EcologicalPoint;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.InspectionDay;
import com.ecopoint.model.InspectionRecord;
import com.ecopoint.model.User;
import com.ecopoint.service.AlertService;
import com.ecopoint.service.EcologicalPointService;
import com.ecopoint.service.InspectionService;
import com.ecopoint.service.UserService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Panel providing management of characterization days (InspectionDay) and field
 * inspections of ecological points (InspectionRecord), evaluating the three
 * regulatory containers and triggering automatic alerts.
 *
 * @author craulgomez
 * @version 1.0
 */
public class InspectionManagementPanel extends JPanel {

    private final InspectionService inspectionService;
    private final EcologicalPointService pointService;
    private final UserService userService;
    private final AlertService alertService;
    private final Runnable onDataChanged;

    private JTextField dayDateField;
    private JTextField dayObsField;
    private JComboBox<String> dayCombo;
    private JComboBox<String> pointCombo;
    private JComboBox<String> userCombo;
    private JComboBox<String> pointStatusCombo;
    private JTextField recordObsField;
    private JTextField evidencePathField;

    private JComboBox<String> whiteFillCombo;
    private JComboBox<String> whiteCondCombo;
    private JCheckBox whiteImproperCheck;

    private JComboBox<String> greenFillCombo;
    private JComboBox<String> greenCondCombo;
    private JCheckBox greenImproperCheck;

    private JComboBox<String> blackFillCombo;
    private JComboBox<String> blackCondCombo;
    private JCheckBox blackImproperCheck;

    private JTable recordTable;
    private DefaultTableModel recordTableModel;

    /**
     * Constructs the InspectionManagementPanel.
     *
     * @param inspectionService inspection business service
     * @param pointService      ecological point service
     * @param userService       user service
     * @param alertService      alert service
     * @param onDataChanged     callback invoked when data changes
     */
    public InspectionManagementPanel(InspectionService inspectionService,
                                     EcologicalPointService pointService,
                                     UserService userService,
                                     AlertService alertService,
                                     Runnable onDataChanged) {
        this.inspectionService = inspectionService;
        this.pointService = pointService;
        this.userService = userService;
        this.alertService = alertService;
        this.onDataChanged = onDataChanged;

        initComponents();
        refreshAll();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(EcoPointTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topSection = new JPanel(new BorderLayout(8, 8));
        topSection.setBackground(EcoPointTheme.BG_MAIN);

        JPanel dayCard = EcoPointTheme.createCardPanel("1. Crear Jornada de Caracterización (YYYY-MM-DD)");
        dayCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));
        dayCard.add(EcoPointTheme.createLabel("Fecha:"));
        dayDateField = new JTextField(LocalDate.now().toString(), 10);
        dayDateField.setFont(EcoPointTheme.FONT_MONO);
        dayCard.add(dayDateField);

        dayCard.add(EcoPointTheme.createLabel("Observaciones:"));
        dayObsField = new JTextField("Jornada institucional de monitoreo ambiental", 26);
        dayObsField.setFont(EcoPointTheme.FONT_BODY);
        dayCard.add(dayObsField);

        JButton addDayBtn = EcoPointTheme.createButton("Crear Jornada", EcoPointTheme.FOREST_DARK);
        addDayBtn.addActionListener(e -> createDay());
        dayCard.add(addDayBtn);

        JPanel formCard = EcoPointTheme.createCardPanel(
                "2. Registrar Inspección de Punto Ecológico y Evaluación de los 3 Recipientes");
        formCard.setLayout(new BorderLayout(8, 8));

        JPanel headerGrid = new JPanel(new GridLayout(2, 6, 8, 6));
        headerGrid.setBackground(EcoPointTheme.CARD_BG);

        dayCombo = new JComboBox<>();
        dayCombo.setFont(EcoPointTheme.FONT_BODY);

        pointCombo = new JComboBox<>();
        pointCombo.setFont(EcoPointTheme.FONT_BODY);

        userCombo = new JComboBox<>();
        userCombo.setFont(EcoPointTheme.FONT_BODY);

        pointStatusCombo = new JComboBox<>(new String[]{"ACTIVO", "CRITICO", "MANTENIMIENTO"});
        pointStatusCombo.setFont(EcoPointTheme.FONT_BODY);

        recordObsField = new JTextField("Revisión rutinaria de campo");
        recordObsField.setFont(EcoPointTheme.FONT_BODY);

        evidencePathField = new JTextField("evidencia_punto.jpg");
        evidencePathField.setFont(EcoPointTheme.FONT_BODY);

        headerGrid.add(EcoPointTheme.createLabel("Jornada:"));
        headerGrid.add(dayCombo);
        headerGrid.add(EcoPointTheme.createLabel("Punto Ecológico:"));
        headerGrid.add(pointCombo);
        headerGrid.add(EcoPointTheme.createLabel("Inspector:"));
        headerGrid.add(userCombo);

        headerGrid.add(EcoPointTheme.createLabel("Estado del Punto:"));
        headerGrid.add(pointStatusCombo);
        headerGrid.add(EcoPointTheme.createLabel("Observación:"));
        headerGrid.add(recordObsField);
        headerGrid.add(EcoPointTheme.createLabel("Evidencia (.jpg/.png):"));
        headerGrid.add(evidencePathField);

        String[] fillLevels = {"VACIO", "BAJO", "MEDIO", "ALTO", "DESBORDADO"};
        String[] conditions = {"BUENO", "REGULAR", "MALO", "AUSENTE"};

        JPanel binsGrid = new JPanel(new GridLayout(1, 3, 10, 6));
        binsGrid.setBackground(EcoPointTheme.CARD_BG);

        whiteFillCombo = new JComboBox<>(fillLevels);
        whiteFillCombo.setSelectedItem("MEDIO");
        whiteCondCombo = new JComboBox<>(conditions);
        whiteImproperCheck = new JCheckBox("¿Mala Separación?");
        whiteImproperCheck.setOpaque(false);
        binsGrid.add(createBinEvaluationBox("Recipiente 1: BLANCO (Aprovechables)",
                whiteFillCombo, whiteCondCombo, whiteImproperCheck));

        greenFillCombo = new JComboBox<>(fillLevels);
        greenFillCombo.setSelectedItem("DESBORDADO");
        greenCondCombo = new JComboBox<>(conditions);
        greenImproperCheck = new JCheckBox("¿Mala Separación?", true);
        greenImproperCheck.setOpaque(false);
        binsGrid.add(createBinEvaluationBox("Recipiente 2: VERDE (Orgánicos)",
                greenFillCombo, greenCondCombo, greenImproperCheck));

        blackFillCombo = new JComboBox<>(fillLevels);
        blackFillCombo.setSelectedItem("BAJO");
        blackCondCombo = new JComboBox<>(conditions);
        blackImproperCheck = new JCheckBox("¿Mala Separación?");
        blackImproperCheck.setOpaque(false);
        binsGrid.add(createBinEvaluationBox("Recipiente 3: NEGRO (No Aprovechables)",
                blackFillCombo, blackCondCombo, blackImproperCheck));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        btnRow.setBackground(EcoPointTheme.CARD_BG);

        JButton registerBtn = EcoPointTheme.createButton(
                "Registrar Inspección y Evaluar Alertas Automáticas", EcoPointTheme.ECO_GREEN);
        registerBtn.addActionListener(e -> registerInspection());

        JButton deleteBtn = EcoPointTheme.createButton(
                "Eliminar Inspección Seleccionada", EcoPointTheme.DANGER_RED);
        deleteBtn.addActionListener(e -> deleteSelectedInspection());

        btnRow.add(registerBtn);
        btnRow.add(deleteBtn);

        formCard.add(headerGrid, BorderLayout.NORTH);
        formCard.add(binsGrid, BorderLayout.CENTER);
        formCard.add(btnRow, BorderLayout.SOUTH);

        topSection.add(dayCard, BorderLayout.NORTH);
        topSection.add(formCard, BorderLayout.CENTER);

        JPanel tableCard = EcoPointTheme.createCardPanel(
                "Historial de Inspecciones de Campo (data/inspection_records.txt)");
        tableCard.setLayout(new BorderLayout());

        recordTableModel = new DefaultTableModel(new Object[]{
                "ID Registro", "Jornada", "Punto Ecológico", "Inspector",
                "Fecha Captura", "Estado Punto", "Incidencias Críticas", "Observaciones"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recordTable = new JTable(recordTableModel);
        EcoPointTheme.styleTable(recordTable);
        tableCard.add(new JScrollPane(recordTable), BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    private JPanel createBinEvaluationBox(String title,
                                          JComboBox<String> fillCombo,
                                          JComboBox<String> condCombo,
                                          JCheckBox improperCheck) {
        JPanel box = EcoPointTheme.createCardPanel(title);
        box.setLayout(new GridLayout(3, 2, 4, 4));
        box.add(EcoPointTheme.createLabel("Nivel de Llenado:"));
        box.add(fillCombo);
        box.add(EcoPointTheme.createLabel("Estado Físico:"));
        box.add(condCombo);
        box.add(EcoPointTheme.createLabel("Contaminación:"));
        box.add(improperCheck);
        return box;
    }

    /**
     * Reloads days, points, users, and the inspection records table.
     */
    public void refreshAll() {
        dayCombo.removeAllItems();
        for (InspectionDay d : inspectionService.listInspectionDays()) {
            dayCombo.addItem(d.getIdDay() + " - " + d.getDate() + " (" + d.getObservations() + ")");
        }

        pointCombo.removeAllItems();
        for (EcologicalPoint p : pointService.listPointsByZone(null)) {
            pointCombo.addItem(p.getIdPoint() + " - " + p.getLocation());
        }

        userCombo.removeAllItems();
        for (User u : userService.listAllUsers()) {
            userCombo.addItem(u.getIdUser() + " - " + u.getFullName() + " [" + u.getRole() + "]");
        }

        recordTableModel.setRowCount(0);
        List<InspectionRecord> records = inspectionService.listAllInspectionRecords();
        for (InspectionRecord r : records) {
            recordTableModel.addRow(new Object[]{
                    r.getIdRecord(),
                    "Jornada #" + r.getIdDay(),
                    "Punto #" + r.getIdPoint(),
                    "Usuario #" + r.getIdUser(),
                    r.getCaptureDate(),
                    r.getPointStatus(),
                    r.hasCriticalIncidents() ? "SÍ (Generó Alerta)" : "NO",
                    r.getObservations()
            });
        }
    }

    private void createDay() {
        try {
            LocalDate date = LocalDate.parse(dayDateField.getText().trim());
            InspectionDay created = inspectionService.createInspectionDay(
                    new InspectionDay(0, date, dayObsField.getText()));
            refreshAll();
            notifyDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Jornada #" + created.getIdDay() + " registrada para la fecha " + created.getDate() + ".",
                    "Jornada Creada",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "La fecha debe tener el formato YYYY-MM-DD (ejemplo: " + LocalDate.now() + ").",
                    "Formato de Fecha Inválido",
                    JOptionPane.ERROR_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void registerInspection() {
        try {
            int idDay = parseId(dayCombo);
            int idPoint = parseId(pointCombo);
            int idUser = parseId(userCombo);

            int alertsBefore = alertService.listAllAlerts().size();

            InspectionRecord record = new InspectionRecord(
                    0,
                    idDay,
                    idPoint,
                    idUser,
                    LocalDate.now(),
                    (String) pointStatusCombo.getSelectedItem(),
                    recordObsField.getText()
            );

            String wCond = (String) whiteCondCombo.getSelectedItem();
            record.addContainerDetail(new ContainerDetail(
                    0, 0, idPoint, 1,
                    (String) whiteFillCombo.getSelectedItem(),
                    wCond, wCond, whiteImproperCheck.isSelected(),
                    "Evaluación recipiente Blanco"));

            String gCond = (String) greenCondCombo.getSelectedItem();
            record.addContainerDetail(new ContainerDetail(
                    0, 0, idPoint, 2,
                    (String) greenFillCombo.getSelectedItem(),
                    gCond, gCond, greenImproperCheck.isSelected(),
                    "Evaluación recipiente Verde"));

            String bCond = (String) blackCondCombo.getSelectedItem();
            record.addContainerDetail(new ContainerDetail(
                    0, 0, idPoint, 3,
                    (String) blackFillCombo.getSelectedItem(),
                    bCond, bCond, blackImproperCheck.isSelected(),
                    "Evaluación recipiente Negro"));

            if (!evidencePathField.getText().trim().isEmpty()) {
                record.addEvidence(new Evidence(
                        0, 0,
                        evidencePathField.getText().trim(),
                        "Evidencia fotográfica de inspección",
                        LocalDateTime.now()));
            }

            InspectionRecord saved = inspectionService.registerPointInspection(record);
            int alertsGenerated = alertService.listAllAlerts().size() - alertsBefore;

            refreshAll();
            notifyDataChanged();

            String msg = "Inspección #" + saved.getIdRecord() + " registrada exitosamente.";
            if (alertsGenerated > 0) {
                msg += "\n¡ATENCIÓN! Se dispararon automáticamente " + alertsGenerated
                        + " alerta(s) operativa(s) por DESBORDADO o MALA SEPARACIÓN.";
            }
            JOptionPane.showMessageDialog(this, msg, "Inspección Registrada", JOptionPane.INFORMATION_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void deleteSelectedInspection() {
        int row = recordTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero un registro de inspección de la tabla.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idRecord = (Integer) recordTableModel.getValueAt(row, 0);
            inspectionService.deleteInspectionRecord(idRecord);
            refreshAll();
            notifyDataChanged();
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private int parseId(JComboBox<String> combo) {
        String item = (String) combo.getSelectedItem();
        if (item == null || !item.contains(" - ")) {
            return 0;
        }
        return Integer.parseInt(item.split(" - ")[0].trim());
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
}
