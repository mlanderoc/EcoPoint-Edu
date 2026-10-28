package com.ecopoint.ui;

import com.ecopoint.exceptions.EcoPointException;
import com.ecopoint.model.User;
import com.ecopoint.service.AlertService;
import com.ecopoint.service.EcologicalPointService;
import com.ecopoint.service.InspectionService;
import com.ecopoint.service.QuarteringService;
import com.ecopoint.service.UserService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Login window for the EcoPoint Edu desktop demonstration.
 * Allows users to authenticate using their institutional email address
 * or register a new user account directly.
 *
 * @author craulgomez
 * @version 1.0
 */
public class LoginFrame extends JFrame {

    private final UserService userService;
    private final EcologicalPointService pointService;
    private final InspectionService inspectionService;
    private final QuarteringService quarteringService;
    private final AlertService alertService;

    private JTextField emailField;
    private JComboBox<String> quickUsersCombo;

    /**
     * Constructs the LoginFrame injecting all application services.
     *
     * @param userService       user management service
     * @param pointService      ecological point service
     * @param inspectionService inspection service
     * @param quarteringService quartering service
     * @param alertService      alert service
     */
    public LoginFrame(UserService userService,
                      EcologicalPointService pointService,
                      InspectionService inspectionService,
                      QuarteringService quarteringService,
                      AlertService alertService) {
        this.userService = userService;
        this.pointService = pointService;
        this.inspectionService = inspectionService;
        this.quarteringService = quarteringService;
        this.alertService = alertService;

        initComponents();
        refreshQuickUsers();
    }

    private void initComponents() {
        setTitle("EcoPoint Edu — Inicio de Sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 470);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(EcoPointTheme.BG_MAIN);

        JPanel header = new JPanel();
        header.setBackground(EcoPointTheme.FOREST_DARK);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        ImageIcon logo = EcoPointTheme.loadLogoIcon(68, 68);
        if (logo != null) {
            JLabel logoLabel = new JLabel(logo);
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            header.add(logoLabel);
            header.add(Box.createVerticalStrut(8));
        }

        JLabel titleLabel = new JLabel("ECOPOINT EDU", SwingConstants.CENTER);
        titleLabel.setFont(EcoPointTheme.FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(
                "Sistema de Información para Gestión y Caracterización de Puntos Ecológicos",
                SwingConstants.CENTER);
        subtitleLabel.setFont(EcoPointTheme.FONT_SMALL);
        subtitleLabel.setForeground(EcoPointTheme.MINT_LIGHT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);

        JPanel card = EcoPointTheme.createCardPanel("Acceso al Sistema");
        card.setLayout(new GridLayout(0, 1, 6, 8));

        card.add(EcoPointTheme.createLabel("Correo Electrónico Institucional:"));
        emailField = new JTextField();
        emailField.setFont(EcoPointTheme.FONT_BODY);
        card.add(emailField);

        card.add(EcoPointTheme.createLabel("Selección Rápida de Usuarios Registrados (Demo):"));
        quickUsersCombo = new JComboBox<>();
        quickUsersCombo.setFont(EcoPointTheme.FONT_BODY);
        quickUsersCombo.addActionListener(e -> {
            String selected = (String) quickUsersCombo.getSelectedItem();
            if (selected != null && selected.contains("<") && selected.contains(">")) {
                int start = selected.indexOf('<') + 1;
                int end = selected.indexOf('>');
                emailField.setText(selected.substring(start, end));
            }
        });
        card.add(quickUsersCombo);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        buttonRow.setBackground(EcoPointTheme.CARD_BG);

        JButton loginButton = EcoPointTheme.createButton("Iniciar Sesión", EcoPointTheme.ECO_GREEN);
        loginButton.setPreferredSize(new Dimension(170, 36));
        loginButton.addActionListener(e -> performLogin());

        JButton quickRegisterButton = EcoPointTheme.createButton("Crear Nuevo Usuario", EcoPointTheme.INFO_BLUE);
        quickRegisterButton.setPreferredSize(new Dimension(170, 36));
        quickRegisterButton.addActionListener(e -> showQuickRegisterDialog());

        buttonRow.add(loginButton);
        buttonRow.add(quickRegisterButton);
        card.add(buttonRow);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(EcoPointTheme.BG_MAIN);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));
        centerWrapper.add(card, BorderLayout.CENTER);

        JLabel footerLabel = new JLabel(
                "Resolución 2184 de 2019 (Blanco · Verde · Negro) | Persistencia en Archivos Planos",
                SwingConstants.CENTER);
        footerLabel.setFont(EcoPointTheme.FONT_SMALL);
        footerLabel.setForeground(EcoPointTheme.TEXT_MUTED);
        footerLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 12, 10));

        root.add(header, BorderLayout.NORTH);
        root.add(centerWrapper, BorderLayout.CENTER);
        root.add(footerLabel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void refreshQuickUsers() {
        quickUsersCombo.removeAllItems();
        List<User> users = userService.listAllUsers();
        for (User u : users) {
            quickUsersCombo.addItem(u.getFullName() + " (" + u.getRole() + ") <" + u.getEmail() + ">");
        }
        if (!users.isEmpty()) {
            emailField.setText(users.get(0).getEmail());
        }
    }

    private void performLogin() {
        try {
            User loggedUser = userService.authenticateByEmail(emailField.getText());
            MainFrame mainFrame = new MainFrame(
                    loggedUser, userService, pointService, inspectionService, quarteringService, alertService);
            mainFrame.setVisible(true);
            dispose();
        } catch (EcoPointException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error de Autenticación (" + ex.getErrorCode() + ")",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showQuickRegisterDialog() {
        JTextField firstNameInput = new JTextField();
        JTextField lastNameInput = new JTextField();
        JTextField emailInput = new JTextField();
        JTextField roleInput = new JTextField("Inspector Ambiental");

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Nombre (sin números):"));
        panel.add(firstNameInput);
        panel.add(new JLabel("Apellido (sin números):"));
        panel.add(lastNameInput);
        panel.add(new JLabel("Correo Electrónico (con @):"));
        panel.add(emailInput);
        panel.add(new JLabel("Rol en la Institución (solo descriptivo):"));
        panel.add(roleInput);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Registro Rápido de Usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                User created = userService.registerUser(new User(
                        0,
                        firstNameInput.getText(),
                        lastNameInput.getText(),
                        emailInput.getText(),
                        roleInput.getText()
                ));
                refreshQuickUsers();
                emailField.setText(created.getEmail());
                JOptionPane.showMessageDialog(this,
                        "Usuario registrado exitosamente con ID #" + created.getIdUser(),
                        "Registro Exitoso",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (EcoPointException ex) {
                JOptionPane.showMessageDialog(this,
                        ex.getMessage(),
                        "Error de Validación (" + ex.getErrorCode() + ")",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
