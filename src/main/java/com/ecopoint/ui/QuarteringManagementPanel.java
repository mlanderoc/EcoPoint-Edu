package com.ecopoint.ui;

import com.ecopoint.exceptions.EcoPointException;
import com.ecopoint.model.InspectionRecord;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.service.InspectionService;
import com.ecopoint.service.QuarteringService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.List;

/**
 * Panel for executing and managing the Quartering Method (Metodo del Cuarteo).
 * Performs real-time composition percentage calculations and enforces physical
 * weight coherence rules through QuarteringValidator.
 *
 * @author craulgomez
 * @version 1.0
 */
public class QuarteringManagementPanel extends JPanel {

    private final QuarteringService quarteringService;
    private final InspectionService inspectionService;
    private final Runnable onDataChanged;

    private JComboBox<String> recordCombo;
    private JTextField initialWeightField;
    private JTextField sampleWeightField;
    private JTextField observationsField;

    private JTextField whiteWeightField;
    private JTextField whiteQtyField;
    private JLabel whitePctLabel;

    private JTextField greenWeightField;
    private JTextField greenQtyField;
    private JLabel greenPctLabel;

    private JTextField blackWeightField;
    private JTextField blackQtyField;
    private JLabel blackPctLabel;

    private JLabel summaryCalcLabel;
    private JTable quarteringTable;
    private DefaultTableModel tableModel;

    /**
     * Constructs the QuarteringManagementPanel.
     *
     * @param quarteringService quartering business service
     * @param inspectionService inspection service
     * @param onDataChanged     callback invoked when data changes
     */
    public QuarteringManagementPanel(QuarteringService quarteringService,
                                     InspectionService inspectionService,
                                     Runnable onDataChanged) {
        this.quarteringService = quarteringService;
        this.inspectionService = inspectionService;
        this.onDataChanged = onDataChanged;

        initComponents();
        refreshAll();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(EcoPointTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formCard = EcoPointTheme.createCardPanel(
                "Registro del Método del Cuarteo y Cálculo Automático de Porcentajes (%)");
        formCard.setLayout(new BorderLayout(8, 8));

        JPanel topGrid = new JPanel(new GridLayout(2, 4, 8, 6));
        topGrid.setBackground(EcoPointTheme.CARD_BG);

        recordCombo = new JComboBox<>();
        recordCombo.setFont(EcoPointTheme.FONT_BODY);

        initialWeightField = new JTextField("24.00");
        initialWeightField.setFont(EcoPointTheme.FONT_MONO);

        sampleWeightField = new JTextField("12.00");
        sampleWeightField.setFont(EcoPointTheme.FONT_MONO);

        observationsField = new JTextField("Homogeneización y cuarteo en zona de acopio");
        observationsField.setFont(EcoPointTheme.FONT_BODY);

        topGrid.add(EcoPointTheme.createLabel("Inspección Origen:"));
        topGrid.add(recordCombo);
        topGrid.add(EcoPointTheme.createLabel("Peso Inicial Recolectado (kg):"));
        topGrid.add(initialWeightField);
        topGrid.add(EcoPointTheme.createLabel("Peso de Muestra Cuarteo (kg):"));
        topGrid.add(sampleWeightField);
        topGrid.add(EcoPointTheme.createLabel("Observaciones Técnicas:"));
        topGrid.add(observationsField);

        JPanel categoriesGrid = new JPanel(new GridLayout(4, 4, 8, 6));
        categoriesGrid.setBackground(EcoPointTheme.CARD_BG);
        categoriesGrid.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        categoriesGrid.add(EcoPointTheme.createLabel("Categoría Normativa (Res. 2184)"));
        categoriesGrid.add(EcoPointTheme.createLabel("Peso Clasificado (kg)"));
        categoriesGrid.add(EcoPointTheme.createLabel("Cantidad (Unidades)"));
        categoriesGrid.add(EcoPointTheme.createLabel("Porcentaje Calculado (%)"));

        whiteWeightField = new JTextField("4.20");
        whiteWeightField.setFont(EcoPointTheme.FONT_MONO);
        whiteQtyField = new JTextField("35");
        whiteQtyField.setFont(EcoPointTheme.FONT_MONO);
        whitePctLabel = new JLabel("35.00 %");
        whitePctLabel.setFont(EcoPointTheme.FONT_MONO);
        whitePctLabel.setForeground(EcoPointTheme.INFO_BLUE);

        categoriesGrid.add(EcoPointTheme.createLabel("1. BLANCO — Aprovechables (Plástico/Papel)"));
        categoriesGrid.add(whiteWeightField);
        categoriesGrid.add(whiteQtyField);
        categoriesGrid.add(whitePctLabel);

        greenWeightField = new JTextField("5.40");
        greenWeightField.setFont(EcoPointTheme.FONT_MONO);
        greenQtyField = new JTextField("28");
        greenQtyField.setFont(EcoPointTheme.FONT_MONO);
        greenPctLabel = new JLabel("45.00 %");
        greenPctLabel.setFont(EcoPointTheme.FONT_MONO);
        greenPctLabel.setForeground(EcoPointTheme.ECO_GREEN);

        categoriesGrid.add(EcoPointTheme.createLabel("2. VERDE — Orgánicos Aprovechables"));
        categoriesGrid.add(greenWeightField);
        categoriesGrid.add(greenQtyField);
        categoriesGrid.add(greenPctLabel);

        blackWeightField = new JTextField("2.10");
        blackWeightField.setFont(EcoPointTheme.FONT_MONO);
        blackQtyField = new JTextField("19");
        blackQtyField.setFont(EcoPointTheme.FONT_MONO);
        blackPctLabel = new JLabel("17.50 %");
        blackPctLabel.setFont(EcoPointTheme.FONT_MONO);
        blackPctLabel.setForeground(EcoPointTheme.TEXT_PRIMARY);

        categoriesGrid.add(EcoPointTheme.createLabel("3. NEGRO — No Aprovechables"));
        categoriesGrid.add(blackWeightField);
        categoriesGrid.add(blackQtyField);
        categoriesGrid.add(blackPctLabel);

        JPanel bottomBar = new JPanel(new BorderLayout(8, 6));
        bottomBar.setBackground(EcoPointTheme.CARD_BG);

        summaryCalcLabel = new JLabel(
                "Suma Clasificada: 11.70 kg | Muestra: 12.00 kg | Merma Física: 0.30 kg");
        summaryCalcLabel.setFont(EcoPointTheme.FONT_MONO);
        summaryCalcLabel.setForeground(EcoPointTheme.FOREST_DARK);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(EcoPointTheme.CARD_BG);

        JButton previewBtn = EcoPointTheme.createButton("Calcular Porcentajes (%)", EcoPointTheme.INFO_BLUE);
        previewBtn.addActionListener(e -> previewCalculations());

        JButton saveBtn = EcoPointTheme.createButton("Registrar Cuarteo", EcoPointTheme.ECO_GREEN);
        saveBtn.addActionListener(e -> registerQuartering());

        JButton testOverflowBtn = EcoPointTheme.createButton(
                "Simular Error: Suma > Muestra", EcoPointTheme.WARNING_AMBER);
        testOverflowBtn.addActionListener(e -> {
            whiteWeightField.setText("8.00");
            greenWeightField.setText("6.00");
            registerQuartering();
        });

        JButton deleteBtn = EcoPointTheme.createButton("Eliminar Seleccionado", EcoPointTheme.DANGER_RED);
        deleteBtn.addActionListener(e -> deleteSelectedQuartering());

        btnRow.add(previewBtn);
        btnRow.add(saveBtn);
        btnRow.add(testOverflowBtn);
        btnRow.add(deleteBtn);

        bottomBar.add(summaryCalcLabel, BorderLayout.NORTH);
        bottomBar.add(btnRow, BorderLayout.SOUTH);

        formCard.add(topGrid, BorderLayout.NORTH);
        formCard.add(categoriesGrid, BorderLayout.CENTER);
        formCard.add(bottomBar, BorderLayout.SOUTH);

        JPanel tableCard = EcoPointTheme.createCardPanel(
                "Procedimientos de Cuarteo Registrados (data/quarterings.txt)");
        tableCard.setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new Object[]{
                "ID Cuarteo", "ID Inspección", "Peso Inicial (kg)", "Peso Muestra (kg)",
                "Suma Clasificada (kg)", "Merma (kg)", "% Blanco", "% Verde", "% Negro"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        quarteringTable = new JTable(tableModel);
        EcoPointTheme.styleTable(quarteringTable);
        tableCard.add(new JScrollPane(quarteringTable), BorderLayout.CENTER);

        add(formCard, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    /**
     * Reloads available inspection records and the quarterings table.
     */
    public void refreshAll() {
        recordCombo.removeAllItems();
        for (InspectionRecord r : inspectionService.listAllInspectionRecords()) {
            recordCombo.addItem(r.getIdRecord() + " - Punto #" + r.getIdPoint()
                    + " (" + r.getCaptureDate() + ")");
        }

        tableModel.setRowCount(0);
        List<Quartering> list = quarteringService.listAllQuarterings();
        for (Quartering q : list) {
            String pctWhite = "0.00%";
            String pctGreen = "0.00%";
            String pctBlack = "0.00%";
            for (QuarteringResult res : q.getResults()) {
                if (res.getIdWaste() == 1) {
                    pctWhite = res.getPercentage() + "%";
                } else if (res.getIdWaste() == 2) {
                    pctGreen = res.getPercentage() + "%";
                } else if (res.getIdWaste() == 3) {
                    pctBlack = res.getPercentage() + "%";
                }
            }
            tableModel.addRow(new Object[]{
                    q.getIdQuartering(),
                    "Inspección #" + q.getIdRecord(),
                    q.getInitialWeight() + " kg",
                    q.getSampleWeight() + " kg",
                    q.calculateTotalClassifiedWeight() + " kg",
                    q.calculateSampleLoss() + " kg",
                    pctWhite,
                    pctGreen,
                    pctBlack
            });
        }
    }

    private Quartering buildQuarteringFromForm() {
        String selected = (String) recordCombo.getSelectedItem();
        int idRecord = 0;
        if (selected != null && selected.contains(" - ")) {
            idRecord = Integer.parseInt(selected.split(" - ")[0].trim());
        }

        BigDecimal initialW = new BigDecimal(initialWeightField.getText().trim());
        BigDecimal sampleW = new BigDecimal(sampleWeightField.getText().trim());

        Quartering q = new Quartering(0, idRecord, initialW, sampleW, observationsField.getText());
        q.addResult(new QuarteringResult(0, 1, 1,
                new BigDecimal(whiteWeightField.getText().trim()),
                Integer.parseInt(whiteQtyField.getText().trim()),
                BigDecimal.ZERO));
        q.addResult(new QuarteringResult(0, 2, 2,
                new BigDecimal(greenWeightField.getText().trim()),
                Integer.parseInt(greenQtyField.getText().trim()),
                BigDecimal.ZERO));
        q.addResult(new QuarteringResult(0, 3, 3,
                new BigDecimal(blackWeightField.getText().trim()),
                Integer.parseInt(blackQtyField.getText().trim()),
                BigDecimal.ZERO));

        q.calculateCompositionPercentages();
        return q;
    }

    private void previewCalculations() {
        try {
            Quartering q = buildQuarteringFromForm();
            whitePctLabel.setText(q.getResults().get(0).getPercentage() + " %");
            greenPctLabel.setText(q.getResults().get(1).getPercentage() + " %");
            blackPctLabel.setText(q.getResults().get(2).getPercentage() + " %");
            summaryCalcLabel.setText("Suma Clasificada: " + q.calculateTotalClassifiedWeight()
                    + " kg | Muestra: " + q.getSampleWeight()
                    + " kg | Merma Física: " + q.calculateSampleLoss() + " kg");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Verifique que todos los pesos y cantidades sean números válidos.",
                    "Formato Numérico Inválido",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registerQuartering() {
        try {
            Quartering q = buildQuarteringFromForm();
            previewCalculations();
            Quartering saved = quarteringService.registerQuartering(q);
            refreshAll();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
            JOptionPane.showMessageDialog(this,
                    "Procedimiento de Cuarteo #" + saved.getIdQuartering()
                            + " registrado con éxito.\nPorcentajes calculados: Blanco "
                            + saved.getResults().get(0).getPercentage() + "%, Verde "
                            + saved.getResults().get(1).getPercentage() + "%, Negro "
                            + saved.getResults().get(2).getPercentage() + "%.",
                    "Cuarteo Registrado",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Los pesos y cantidades deben ser valores numéricos válidos.",
                    "Formato Numérico Inválido",
                    JOptionPane.ERROR_MESSAGE);
        } catch (EcoPointException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Regla de Negocio / Validación (" + ex.getErrorCode() + ")",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedQuartering() {
        int row = quarteringTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero un registro de cuarteo en la tabla.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idQuartering = (Integer) tableModel.getValueAt(row, 0);
            quarteringService.deleteQuartering(idQuartering);
            refreshAll();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (EcoPointException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error (" + ex.getErrorCode() + ")",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
