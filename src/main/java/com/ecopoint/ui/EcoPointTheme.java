package com.ecopoint.ui;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.io.InputStream;

/**
 * Centralized visual theme for the EcoPoint Edu Swing interface.
 * Applies the official EcoPoint Edu logo color palette and typography
 * aligned with recycling, environmental management, and information systems.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class EcoPointTheme {

    public static final Color FOREST_DARK = new Color(20, 90, 50);
    public static final Color ECO_GREEN = new Color(30, 132, 73);
    public static final Color LEAF_GREEN = new Color(104, 159, 56);
    public static final Color MINT_LIGHT = new Color(232, 245, 233);
    public static final Color INFO_BLUE = new Color(31, 78, 121);
    public static final Color BG_MAIN = new Color(244, 247, 245);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(28, 40, 38);
    public static final Color TEXT_MUTED = new Color(92, 111, 104);
    public static final Color BORDER_COLOR = new Color(213, 227, 220);

    public static final Color DANGER_RED = new Color(220, 38, 38);
    public static final Color WARNING_AMBER = new Color(217, 119, 6);
    public static final Color BIN_WHITE_BG = new Color(248, 250, 252);
    public static final Color BIN_GREEN_BG = new Color(30, 132, 73);
    public static final Color BIN_BLACK_BG = new Color(30, 41, 59);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.BOLD, 13);

    private EcoPointTheme() {
    }

    /**
     * Creates a styled primary button using the EcoPoint Edu palette.
     *
     * @param text    button label
     * @param bgColor background color
     * @return styled JButton
     */
    public static JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        return button;
    }

    /**
     * Creates a card panel with a subtle green-gray border and white background.
     *
     * @param title section title
     * @return styled JPanel
     */
    public static JPanel createCardPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        Border line = BorderFactory.createLineBorder(BORDER_COLOR, 1);
        Border titled = BorderFactory.createTitledBorder(
                line, " " + title + " ", 0, 0, FONT_SUBTITLE, FOREST_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
                titled, BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        return panel;
    }

    /**
     * Applies EcoPoint Edu visual styling to a JTable.
     *
     * @param table table to style
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(26);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(MINT_LIGHT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BUTTON);
        header.setBackground(FOREST_DARK);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 30));
    }

    /**
     * Creates a styled form label.
     *
     * @param text label text
     * @return styled JLabel
     */
    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    /**
     * Loads and scales the official EcoPoint Edu logo if available in resources or disk.
     *
     * @param width  desired width in pixels
     * @param height desired height in pixels
     * @return scaled ImageIcon or null if not found
     */
    public static ImageIcon loadLogoIcon(int width, int height) {
        try {
            InputStream stream = EcoPointTheme.class.getResourceAsStream("/logo_ecopoint.jpeg");
            Image img = null;
            if (stream != null) {
                img = ImageIO.read(stream);
            } else {
                File localFile = new File("src/main/resources/logo_ecopoint.jpeg");
                if (localFile.exists()) {
                    img = ImageIO.read(localFile);
                }
            }
            if (img != null) {
                Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
