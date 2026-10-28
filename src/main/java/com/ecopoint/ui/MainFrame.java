package com.ecopoint.ui;

import com.ecopoint.model.User;
import com.ecopoint.service.AlertService;
import com.ecopoint.service.EcologicalPointService;
import com.ecopoint.service.InspectionService;
import com.ecopoint.service.QuarteringService;
import com.ecopoint.service.UserService;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

/**
 * Main application window of EcoPoint Edu hosting the institutional header
 * and the tabbed modules for Dashboard, Campus Map and Points, Inspections,
 * Quartering Method, and User CRUD Administration.
 *
 * @author craulgomez
 * @version 1.0
 */
public class MainFrame extends JFrame {

    private final User currentUser;
    private final UserService userService;
    private final EcologicalPointService pointService;
    private final InspectionService inspectionService;
    private final QuarteringService quarteringService;
    private final AlertService alertService;

    private DashboardAndAlertsPanel dashboardPanel;
    private CampusAndPointPanel campusPanel;
    private InspectionManagementPanel inspectionPanel;
    private QuarteringManagementPanel quarteringPanel;
    private UserManagementPanel userPanel;

    /**
     * Constructs the MainFrame for the authenticated user.
     *
     * @param currentUser       logged-in user
     * @param userService       user management service
     * @param pointService      ecological point service
     * @param inspectionService inspection service
     * @param quarteringService quartering service
     * @param alertService      alert service
     */
    public MainFrame(User currentUser,
                     UserService userService,
                     EcologicalPointService pointService,
                     InspectionService inspectionService,
                     QuarteringService quarteringService,
                     AlertService alertService) {
        this.currentUser = currentUser;
        this.userService = userService;
        this.pointService = pointService;
        this.inspectionService = inspectionService;
        this.quarteringService = quarteringService;
        this.alertService = alertService;

        initComponents();
    }

    private void initComponents() {
        setTitle("EcoPoint Edu — Sistema de Información Ambiental (Sesión: "
                + currentUser.getFullName() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EcoPointTheme.BG_MAIN);

        JPanel topBar = new JPanel(new BorderLayout(12, 8));
        topBar.setBackground(EcoPointTheme.FOREST_DARK);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setOpaque(false);

        ImageIcon logo = EcoPointTheme.loadLogoIcon(42, 42);
        if (logo != null) {
            brandPanel.add(new JLabel(logo));
        }

        JLabel appTitle = new JLabel("ECOPOINT EDU — Gestión y Caracterización de Residuos Sólidos");
        appTitle.setFont(EcoPointTheme.FONT_SUBTITLE);
        appTitle.setForeground(Color.WHITE);
        brandPanel.add(appTitle);

        JPanel sessionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        sessionPanel.setOpaque(false);

        JLabel userBadge = new JLabel("Usuario: " + currentUser.getFullName()
                + " [" + currentUser.getRole() + "]");
        userBadge.setFont(EcoPointTheme.FONT_BODY);
        userBadge.setForeground(EcoPointTheme.MINT_LIGHT);

        JButton refreshBtn = EcoPointTheme.createButton("Actualizar Vistas", EcoPointTheme.LEAF_GREEN);
        refreshBtn.addActionListener(e -> refreshAllPanels());

        JButton logoutBtn = EcoPointTheme.createButton("Cerrar Sesión", EcoPointTheme.INFO_BLUE);
        logoutBtn.addActionListener(e -> logout());

        sessionPanel.add(userBadge);
        sessionPanel.add(refreshBtn);
        sessionPanel.add(logoutBtn);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(sessionPanel, BorderLayout.EAST);

        Runnable refreshCallback = this::refreshAllPanels;

        dashboardPanel = new DashboardAndAlertsPanel(
                pointService, inspectionService, quarteringService, alertService, refreshCallback);
        campusPanel = new CampusAndPointPanel(pointService, refreshCallback);
        inspectionPanel = new InspectionManagementPanel(
                inspectionService, pointService, userService, alertService, refreshCallback);
        quarteringPanel = new QuarteringManagementPanel(
                quarteringService, inspectionService, refreshCallback);
        userPanel = new UserManagementPanel(userService, refreshCallback);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(EcoPointTheme.FONT_BUTTON);
        tabs.setBackground(Color.WHITE);

        tabs.addTab("1. Estadísticas y Centro de Alertas", dashboardPanel);
        tabs.addTab("2. Zonas, Puntos Ecológicos y Mapa (%)", campusPanel);
        tabs.addTab("3. Jornadas e Inspecciones de Campo", inspectionPanel);
        tabs.addTab("4. Método del Cuarteo (Cálculos %)", quarteringPanel);
        tabs.addTab("5. Administración CRUD de Usuarios", userPanel);

        root.add(topBar, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void refreshAllPanels() {
        dashboardPanel.refreshAll();
        campusPanel.refreshAll();
        inspectionPanel.refreshAll();
        quarteringPanel.refreshAll();
        userPanel.refreshTable();
    }

    private void logout() {
        LoginFrame login = new LoginFrame(
                userService, pointService, inspectionService, quarteringService, alertService);
        login.setVisible(true);
        dispose();
    }
}
