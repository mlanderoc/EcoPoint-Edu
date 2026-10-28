package com.ecopoint.ui;

import com.ecopoint.exceptions.EcoPointException;
import com.ecopoint.model.Alert;
import com.ecopoint.model.EcologicalPoint;
import com.ecopoint.service.AlertService;
import com.ecopoint.service.EcologicalPointService;
import com.ecopoint.service.InspectionService;
import com.ecopoint.service.QuarteringService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Panel displaying consolidated environmental statistics, waste composition
 * bars by regulatory color, and the active operational alerts tray.
 *
 * @author craulgomez
 * @version 1.0
 */
public class DashboardAndAlertsPanel extends JPanel {

    private final EcologicalPointService pointService;
    private final InspectionService inspectionService;
    private final QuarteringService quarteringService;
    private final AlertService alertService;
    private final Runnable onDataChanged;

    private JLabel totalPointsValue;
    private JLabel criticalPointsValue;
    private JLabel totalInspectionsValue;
    private JLabel pendingAlertsValue;
    private JLabel totalSampleKgValue;

    private JProgressBar whiteProgressBar;
    private JProgressBar greenProgressBar;
    private JProgressBar blackProgressBar;

    private JCheckBox onlyPendingCheck;
    private JTable alertTable;
    private DefaultTableModel alertTableModel;

    /**
     * Constructs the DashboardAndAlertsPanel.
     *
     * @param pointService      ecological point service
     * @param inspectionService inspection service
     * @param quarteringService quartering service
     * @param alertService      alert service
     * @param onDataChanged     callback invoked when data changes
     */
    public DashboardAndAlertsPanel(EcologicalPointService pointService,
                                   InspectionService inspectionService,
                                   QuarteringService quarteringService,
                                   AlertService alertService,
                                   Runnable onDataChanged) {
        this.pointService = pointService;
        this.inspectionService = inspectionService;
        this.quarteringService = quarteringService;
        this.alertService = alertService;
        this.onDataChanged = onDataChanged;

        initComponents();
        refreshAll();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(EcoPointTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topWrapper = new JPanel(new BorderLayout(10, 10));
        topWrapper.setBackground(EcoPointTheme.BG_MAIN);

        JPanel kpiGrid = new JPanel(new GridLayout(1, 5, 10, 10));
        kpiGrid.setBackground(EcoPointTheme.BG_MAIN);

        totalPointsValue = createKpiValueLabel(EcoPointTheme.FOREST_DARK);
        criticalPointsValue = createKpiValueLabel(EcoPointTheme.DANGER_RED);
        totalInspectionsValue = createKpiValueLabel(EcoPointTheme.INFO_BLUE);
        pendingAlertsValue = createKpiValueLabel(EcoPointTheme.WARNING_AMBER);
        totalSampleKgValue = createKpiValueLabel(EcoPointTheme.ECO_GREEN);

        kpiGrid.add(wrapKpiCard("Puntos Ecológicos", totalPointsValue));
        kpiGrid.add(wrapKpiCard("Puntos en Estado Crítico", criticalPointsValue));
        kpiGrid.add(wrapKpiCard("Inspecciones Realizadas", totalInspectionsValue));
        kpiGrid.add(wrapKpiCard("Alertas Pendientes", pendingAlertsValue));
        kpiGrid.add(wrapKpiCard("Muestra Caracterizada", totalSampleKgValue));

        JPanel compositionCard = EcoPointTheme.createCardPanel(
                "Estadísticas Generales de Composición por Código de Colores (Resolución 2184 de 2019)");
        compositionCard.setLayout(new GridLayout(3, 2, 10, 8));

        whiteProgressBar = createStyledBar(EcoPointTheme.INFO_BLUE);
        greenProgressBar = createStyledBar(EcoPointTheme.ECO_GREEN);
        blackProgressBar = createStyledBar(EcoPointTheme.BIN_BLACK_BG);

        compositionCard.add(EcoPointTheme.createLabel("BLANCO — Residuos Aprovechables (Plástico, Papel, Vidrio):"));
        compositionCard.add(whiteProgressBar);
        compositionCard.add(EcoPointTheme.createLabel("VERDE — Residuos Orgánicos Aprovechables:"));
        compositionCard.add(greenProgressBar);
        compositionCard.add(EcoPointTheme.createLabel("NEGRO — Residuos No Aprovechables:"));
        compositionCard.add(blackProgressBar);

        topWrapper.add(kpiGrid, BorderLayout.NORTH);
        topWrapper.add(compositionCard, BorderLayout.CENTER);

        JPanel alertCard = EcoPointTheme.createCardPanel(
                "Centro de Alertas Operativas Automáticas (data/alerts.txt)");
        alertCard.setLayout(new BorderLayout(8, 8));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        actionRow.setBackground(EcoPointTheme.CARD_BG);

        onlyPendingCheck = new JCheckBox("Mostrar solo alertas pendientes de atención", false);
        onlyPendingCheck.setFont(EcoPointTheme.FONT_BODY);
        onlyPendingCheck.setOpaque(false);
        onlyPendingCheck.addActionListener(e -> refreshAll());

        JButton attendBtn = EcoPointTheme.createButton(
                "Marcar Alerta Seleccionada como Atendida", EcoPointTheme.ECO_GREEN);
        attendBtn.addActionListener(e -> attendSelectedAlert());

        JButton deleteAlertBtn = EcoPointTheme.createButton(
                "Eliminar Alerta", EcoPointTheme.DANGER_RED);
        deleteAlertBtn.addActionListener(e -> deleteSelectedAlert());

        actionRow.add(onlyPendingCheck);
        actionRow.add(attendBtn);
        actionRow.add(deleteAlertBtn);

        alertTableModel = new DefaultTableModel(new Object[]{
                "ID Alerta", "ID Detalle Recipiente", "Tipo de Alerta",
                "Estado", "Descripción Operativa", "Fecha y Hora"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        alertTable = new JTable(alertTableModel);
        EcoPointTheme.styleTable(alertTable);

        alertCard.add(actionRow, BorderLayout.NORTH);
        alertCard.add(new JScrollPane(alertTable), BorderLayout.CENTER);

        add(topWrapper, BorderLayout.NORTH);
        add(alertCard, BorderLayout.CENTER);
    }

    private JLabel createKpiValueLabel(Color color) {
        JLabel label = new JLabel("0", SwingConstants.CENTER);
        label.setFont(EcoPointTheme.FONT_TITLE);
        label.setForeground(color);
        return label;
    }

    private JPanel wrapKpiCard(String title, JLabel valueLabel) {
        JPanel card = EcoPointTheme.createCardPanel(title);
        card.setLayout(new BorderLayout());
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JProgressBar createStyledBar(Color fgColor) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setFont(EcoPointTheme.FONT_MONO);
        bar.setForeground(fgColor);
        bar.setBackground(EcoPointTheme.BG_MAIN);
        return bar;
    }

    /**
     * Recalculates all dashboard metrics and reloads the alerts table.
     */
    public void refreshAll() {
        List<EcologicalPoint> points = pointService.listPointsByZone(null);
        int criticalCount = 0;
        for (EcologicalPoint p : points) {
            if ("CRITICO".equalsIgnoreCase(p.getStatus())) {
                criticalCount++;
            }
        }

        int inspectionsCount = inspectionService.listAllInspectionRecords().size();
        int pendingAlertsCount = alertService.listPendingAlerts().size();

        Map<String, BigDecimal> stats = quarteringService.calculateConsolidatedStatistics();
        BigDecimal totalSample = stats.getOrDefault("totalSampleWeight", BigDecimal.ZERO);
        BigDecimal whiteKg = stats.getOrDefault("totalWhiteWeight", BigDecimal.ZERO);
        BigDecimal greenKg = stats.getOrDefault("totalGreenWeight", BigDecimal.ZERO);
        BigDecimal blackKg = stats.getOrDefault("totalBlackWeight", BigDecimal.ZERO);

        BigDecimal whitePct = stats.getOrDefault("whitePercentage", BigDecimal.ZERO);
        BigDecimal greenPct = stats.getOrDefault("greenPercentage", BigDecimal.ZERO);
        BigDecimal blackPct = stats.getOrDefault("blackPercentage", BigDecimal.ZERO);

        totalPointsValue.setText(String.valueOf(points.size()));
        criticalPointsValue.setText(String.valueOf(criticalCount));
        totalInspectionsValue.setText(String.valueOf(inspectionsCount));
        pendingAlertsValue.setText(String.valueOf(pendingAlertsCount));
        totalSampleKgValue.setText(totalSample + " kg");

        whiteProgressBar.setValue(whitePct.intValue());
        whiteProgressBar.setString(whitePct + "% (" + whiteKg + " kg)");

        greenProgressBar.setValue(greenPct.intValue());
        greenProgressBar.setString(greenPct + "% (" + greenKg + " kg)");

        blackProgressBar.setValue(blackPct.intValue());
        blackProgressBar.setString(blackPct + "% (" + blackKg + " kg)");

        alertTableModel.setRowCount(0);
        List<Alert> alerts = onlyPendingCheck.isSelected()
                ? alertService.listPendingAlerts()
                : alertService.listAllAlerts();

        for (Alert a : alerts) {
            alertTableModel.addRow(new Object[]{
                    a.getIdAlert(),
                    "Detalle #" + a.getIdContainerDetail(),
                    a.getType(),
                    a.isAttended() ? "ATENDIDA" : "PENDIENTE",
                    a.getDescription(),
                    a.getGeneratedAt()
            });
        }
    }

    private void attendSelectedAlert() {
        int row = alertTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero una alerta en la tabla para marcarla como atendida.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idAlert = (Integer) alertTableModel.getValueAt(row, 0);
            alertService.markAlertAsAttended(idAlert);
            refreshAll();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
            JOptionPane.showMessageDialog(this,
                    "Alerta #" + idAlert + " marcada como ATENDIDA correctamente.",
                    "Alerta Atendida",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (EcoPointException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Regla de Negocio (" + ex.getErrorCode() + ")",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedAlert() {
        int row = alertTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero una alerta en la tabla para eliminar.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idAlert = (Integer) alertTableModel.getValueAt(row, 0);
            alertService.deleteAlert(idAlert);
            refreshAll();
        } catch (EcoPointException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error (" + ex.getErrorCode() + ")",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
