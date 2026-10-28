package com.ecopoint.ui;

import com.ecopoint.exceptions.EcoPointException;
import com.ecopoint.model.User;
import com.ecopoint.service.UserService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Panel providing full CRUD administration for User entities.
 * Demonstrates UserValidator rules such as names without numbers,
 * email format with the at symbol, and duplicate email prevention.
 *
 * @author craulgomez
 * @version 1.0
 */
public class UserManagementPanel extends JPanel {

    private final UserService userService;
    private final Runnable onDataChanged;

    private JTextField idField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField roleField;
    private JTextField searchEmailField;

    private JTable userTable;
    private DefaultTableModel tableModel;

    /**
     * Constructs the UserManagementPanel.
     *
     * @param userService   user business service
     * @param onDataChanged callback invoked whenever user data is modified
     */
    public UserManagementPanel(UserService userService, Runnable onDataChanged) {
        this.userService = userService;
        this.onDataChanged = onDataChanged;
        initComponents();
        refreshTable();
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setBackground(EcoPointTheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formCard = EcoPointTheme.createCardPanel("Formulario CRUD de Usuarios");
        formCard.setLayout(new BorderLayout(8, 8));

        JPanel fieldsGrid = new JPanel(new GridLayout(5, 2, 8, 8));
        fieldsGrid.setBackground(EcoPointTheme.CARD_BG);

        idField = new JTextField();
        idField.setEditable(false);
        idField.setFont(EcoPointTheme.FONT_MONO);
        idField.setBackground(EcoPointTheme.BG_MAIN);

        firstNameField = new JTextField();
        firstNameField.setFont(EcoPointTheme.FONT_BODY);

        lastNameField = new JTextField();
        lastNameField.setFont(EcoPointTheme.FONT_BODY);

        emailField = new JTextField();
        emailField.setFont(EcoPointTheme.FONT_BODY);

        roleField = new JTextField();
        roleField.setFont(EcoPointTheme.FONT_BODY);

        fieldsGrid.add(EcoPointTheme.createLabel("ID Usuario (Automático):"));
        fieldsGrid.add(idField);
        fieldsGrid.add(EcoPointTheme.createLabel("Nombre (sin números):"));
        fieldsGrid.add(firstNameField);
        fieldsGrid.add(EcoPointTheme.createLabel("Apellido (sin números):"));
        fieldsGrid.add(lastNameField);
        fieldsGrid.add(EcoPointTheme.createLabel("Correo Electrónico (con @ y único):"));
        fieldsGrid.add(emailField);
        fieldsGrid.add(EcoPointTheme.createLabel("Rol en la Institución (descriptivo):"));
        fieldsGrid.add(roleField);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        buttonRow.setBackground(EcoPointTheme.CARD_BG);

        JButton createBtn = EcoPointTheme.createButton("Registrar Usuario", EcoPointTheme.ECO_GREEN);
        createBtn.addActionListener(e -> createUser());

        JButton updateBtn = EcoPointTheme.createButton("Actualizar Seleccionado", EcoPointTheme.INFO_BLUE);
        updateBtn.addActionListener(e -> updateUser());

        JButton deleteBtn = EcoPointTheme.createButton("Eliminar Seleccionado", EcoPointTheme.DANGER_RED);
        deleteBtn.addActionListener(e -> deleteUser());

        JButton clearBtn = EcoPointTheme.createButton("Limpiar Campos", EcoPointTheme.TEXT_MUTED);
        clearBtn.addActionListener(e -> clearForm());

        buttonRow.add(createBtn);
        buttonRow.add(updateBtn);
        buttonRow.add(deleteBtn);
        buttonRow.add(clearBtn);

        formCard.add(fieldsGrid, BorderLayout.CENTER);
        formCard.add(buttonRow, BorderLayout.SOUTH);

        JPanel tableCard = EcoPointTheme.createCardPanel("Directorio de Usuarios Registrados (data/users.txt)");
        tableCard.setLayout(new BorderLayout(8, 8));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.setBackground(EcoPointTheme.CARD_BG);
        searchBar.add(EcoPointTheme.createLabel("Buscar por Correo:"));
        searchEmailField = new JTextField(24);
        searchEmailField.setFont(EcoPointTheme.FONT_BODY);
        searchBar.add(searchEmailField);

        JButton searchBtn = EcoPointTheme.createButton("Buscar", EcoPointTheme.FOREST_DARK);
        searchBtn.addActionListener(e -> searchByEmail());
        searchBar.add(searchBtn);

        JButton showAllBtn = EcoPointTheme.createButton("Ver Todos", EcoPointTheme.LEAF_GREEN);
        showAllBtn.addActionListener(e -> refreshTable());
        searchBar.add(showAllBtn);

        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Apellido", "Correo Electrónico", "Rol Institucional"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new JTable(tableModel);
        EcoPointTheme.styleTable(userTable);
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userTable.getSelectedRow() >= 0) {
                int row = userTable.getSelectedRow();
                idField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
                firstNameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
                lastNameField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
                emailField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
                roleField.setText(String.valueOf(tableModel.getValueAt(row, 4)));
            }
        });

        tableCard.add(searchBar, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(userTable), BorderLayout.CENTER);

        add(formCard, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
    }

    /**
     * Reloads the user table from the service.
     */
    public void refreshTable() {
        tableModel.setRowCount(0);
        List<User> users = userService.listAllUsers();
        for (User u : users) {
            tableModel.addRow(new Object[]{
                    u.getIdUser(),
                    u.getFirstName(),
                    u.getLastName(),
                    u.getEmail(),
                    u.getRole()
            });
        }
    }

    private void createUser() {
        try {
            User user = new User(
                    0,
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    roleField.getText()
            );
            User saved = userService.registerUser(user);
            refreshTable();
            clearForm();
            notifyDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Usuario '" + saved.getFullName() + "' registrado con éxito (ID #" + saved.getIdUser() + ").",
                    "Registro Exitoso",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void updateUser() {
        if (idField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero un usuario de la tabla para actualizar.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int id = Integer.parseInt(idField.getText().trim());
            User user = new User(
                    id,
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    roleField.getText()
            );
            userService.updateUser(user);
            refreshTable();
            clearForm();
            notifyDataChanged();
            JOptionPane.showMessageDialog(this,
                    "Usuario #" + id + " actualizado correctamente.",
                    "Actualización Exitosa",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void deleteUser() {
        if (idField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione primero un usuario de la tabla para eliminar.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int id = Integer.parseInt(idField.getText().trim());
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de eliminar el usuario #" + id + "?",
                    "Confirmar Eliminación",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                userService.deleteUser(id);
                refreshTable();
                clearForm();
                notifyDataChanged();
                JOptionPane.showMessageDialog(this,
                        "Usuario eliminado correctamente.",
                        "Eliminado",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void searchByEmail() {
        try {
            User found = userService.getUserByEmail(searchEmailField.getText());
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{
                    found.getIdUser(),
                    found.getFirstName(),
                    found.getLastName(),
                    found.getEmail(),
                    found.getRole()
            });
        } catch (EcoPointException ex) {
            showError(ex);
        }
    }

    private void clearForm() {
        idField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        roleField.setText("");
        userTable.clearSelection();
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
