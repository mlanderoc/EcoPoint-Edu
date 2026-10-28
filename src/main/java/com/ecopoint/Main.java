package com.ecopoint;

import com.ecopoint.model.Container;
import com.ecopoint.model.ContainerDetail;
import com.ecopoint.model.EcologicalPoint;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.InspectionDay;
import com.ecopoint.model.InspectionRecord;
import com.ecopoint.model.Institution;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.User;
import com.ecopoint.model.Zone;
import com.ecopoint.service.AlertService;
import com.ecopoint.service.EcologicalPointService;
import com.ecopoint.service.InspectionService;
import com.ecopoint.service.QuarteringService;
import com.ecopoint.service.UserService;
import com.ecopoint.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Application entry point for EcoPoint Edu.
 * Initializes domain services backed by flat-file persistence (Protected Variations),
 * seeds initial institutional records if the flat files are empty, and launches
 * the Java Swing graphical user interface.
 *
 * @author craulgomez
 * @version 1.0
 */
public class Main {

    /**
     * Main method launching the EcoPoint Edu desktop application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        UserService userService = new UserService();
        EcologicalPointService pointService = new EcologicalPointService();
        AlertService alertService = new AlertService();
        InspectionService inspectionService = new InspectionService(pointService, userService, alertService);
        QuarteringService quarteringService = new QuarteringService(inspectionService);

        seedInitialDataIfEmpty(userService, pointService, inspectionService, quarteringService);

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(
                    userService, pointService, inspectionService, quarteringService, alertService);
            loginFrame.setVisible(true);
        });
    }

    private static void seedInitialDataIfEmpty(UserService userService,
                                               EcologicalPointService pointService,
                                               InspectionService inspectionService,
                                               QuarteringService quarteringService) {
        try {
            if (userService.listAllUsers().isEmpty()) {
                userService.registerUser(new User(
                        0, "Merlín Yelenis", "Landero Cuello",
                        "mlandero@unicesar.edu.co", "Coordinadora Ambiental"));
                userService.registerUser(new User(
                        0, "Estefanía", "Márquez Martínez",
                        "estefaniamarquez@unicesar.edu.co", "Inspectora Ambiental"));
                userService.registerUser(new User(
                        0, "Carlos Raúl", "Gómez Galvis",
                        "craulgomez@unicesar.edu.co", "Docente Supervisor"));
            }

            if (pointService.listAllInstitutions().isEmpty()) {
                Institution inst = pointService.registerInstitution(new Institution(
                        0, "Universidad Popular del Cesar — Sede Sabanas",
                        "Diagonal 21 No. 29-56, Valledupar, Cesar"));

                Zone zCafeteria = pointService.registerZone(new Zone(
                        0, inst.getIdInstitution(), "Cafetería Central",
                        "Zona de alta concurrencia y generación de residuos orgánicos y plásticos"));
                Zone zAulas = pointService.registerZone(new Zone(
                        0, inst.getIdInstitution(), "Bloque de Aulas Norte",
                        "Pasillos académicos de aulas de ingeniería"));

                EcologicalPoint p1 = new EcologicalPoint(
                        0, zCafeteria.getIdZone(), "Entrada Principal Cafetería",
                        "ACTIVO", "BUENO", new BigDecimal("65.50"), new BigDecimal("25.00"),
                        "Punto principal bajo techo junto a zona de mesas");
                p1.addContainer(new Container(0, 1, "Aprovechables (Plástico, Papel)", "BLANCO", new BigDecimal("55.00")));
                p1.addContainer(new Container(0, 2, "Orgánicos Aprovechables", "VERDE", new BigDecimal("55.00")));
                p1.addContainer(new Container(0, 3, "No Aprovechables", "NEGRO", new BigDecimal("55.00")));
                EcologicalPoint savedP1 = pointService.registerEcologicalPoint(p1);

                EcologicalPoint p2 = new EcologicalPoint(
                        0, zAulas.getIdZone(), "Pasillo Primer Piso Bloque Norte",
                        "ACTIVO", "BUENO", new BigDecimal("24.00"), new BigDecimal("22.50"),
                        "Punto ecológico frente a escaleras principales");
                p2.addContainer(new Container(0, 1, "Aprovechables (Papel, Cartón)", "BLANCO", new BigDecimal("45.00")));
                p2.addContainer(new Container(0, 2, "Orgánicos Aprovechables", "VERDE", new BigDecimal("45.00")));
                p2.addContainer(new Container(0, 3, "No Aprovechables", "NEGRO", new BigDecimal("45.00")));
                pointService.registerEcologicalPoint(p2);

                InspectionDay day = inspectionService.createInspectionDay(new InspectionDay(
                        0, LocalDate.now(), "Jornada inicial de diagnóstico institucional"));

                InspectionRecord rec = new InspectionRecord(
                        0, day.getIdDay(), savedP1.getIdPoint(), 1,
                        LocalDate.now(), "CRITICO",
                        "Alta generación tras jornada de almuerzo estudiantil");
                rec.addContainerDetail(new ContainerDetail(
                        0, 0, savedP1.getIdPoint(), 1, "ALTO", "BUENO", "BUENO", true,
                        "Se detectaron servilletas usadas en contenedor blanco"));
                rec.addContainerDetail(new ContainerDetail(
                        0, 0, savedP1.getIdPoint(), 2, "DESBORDADO", "BUENO", "REGULAR", false,
                        "Restos de alimentos rebosando capacidad"));
                rec.addContainerDetail(new ContainerDetail(
                        0, 0, savedP1.getIdPoint(), 3, "MEDIO", "BUENO", "BUENO", false,
                        "Sin novedad"));
                rec.addEvidence(new Evidence(
                        0, 0, "evidencia_cafeteria_dia1.jpg",
                        "Fotografía de punto en cafetería central", LocalDateTime.now()));

                InspectionRecord savedRec = inspectionService.registerPointInspection(rec);

                Quartering q = new Quartering(
                        0, savedRec.getIdRecord(),
                        new BigDecimal("24.00"), new BigDecimal("12.00"),
                        "Cuarteo ejecutado sobre muestra homogeneizada de Cafetería");
                q.addResult(new QuarteringResult(0, 1, 1, new BigDecimal("4.20"), 35, BigDecimal.ZERO));
                q.addResult(new QuarteringResult(0, 2, 2, new BigDecimal("5.40"), 28, BigDecimal.ZERO));
                q.addResult(new QuarteringResult(0, 3, 3, new BigDecimal("2.10"), 19, BigDecimal.ZERO));
                quarteringService.registerQuartering(q);
            }
        } catch (Exception ignored) {
        }
    }
}
