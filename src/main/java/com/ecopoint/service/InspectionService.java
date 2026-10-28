package com.ecopoint.service;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.model.ContainerDetail;
import com.ecopoint.model.EcologicalPoint;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.InspectionDay;
import com.ecopoint.model.InspectionRecord;
import com.ecopoint.persistence.FileInspectionRepository;
import com.ecopoint.persistence.InspectionRepository;
import com.ecopoint.validation.InspectionValidator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business service responsible for managing characterization days (InspectionDay)
 * and field inspection records (InspectionRecord).
 * Depends on InspectionRepository applying Protected Variations and SOLID DIP.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class InspectionService {

    private final EcologicalPointService pointService;
    private final UserService userService;
    private final AlertService alertService;
    private final InspectionRepository inspectionRepository;

    /**
     * Constructs an InspectionService using the default flat-file repository.
     *
     * @param pointService service used to verify ecological point existence
     * @param userService  service used to verify inspector user existence
     * @param alertService service used to trigger automatic alerts
     */
    public InspectionService(EcologicalPointService pointService,
                             UserService userService,
                             AlertService alertService) {
        this(pointService, userService, alertService, new FileInspectionRepository());
    }

    /**
     * Constructs an InspectionService injecting required services and repository contract.
     *
     * @param pointService         service used to verify ecological point existence
     * @param userService          service used to verify inspector user existence
     * @param alertService         service used to trigger automatic alerts
     * @param inspectionRepository inspection repository contract
     */
    public InspectionService(EcologicalPointService pointService,
                             UserService userService,
                             AlertService alertService,
                             InspectionRepository inspectionRepository) {
        this.pointService = pointService;
        this.userService = userService;
        this.alertService = alertService;
        this.inspectionRepository = inspectionRepository;
    }

    /**
     * Creates a new environmental characterization day.
     *
     * @param day inspection day to register
     * @return registered inspection day with assigned ID
     */
    public InspectionDay createInspectionDay(InspectionDay day) {
        InspectionValidator.validateInspectionDayData(day);
        day.setIdDay(0);
        if (day.getObservations() != null) {
            day.setObservations(day.getObservations().trim());
        }
        return inspectionRepository.saveDay(day);
    }

    /**
     * Updates an existing characterization day.
     *
     * @param day inspection day with updated fields
     * @return updated inspection day
     */
    public InspectionDay updateInspectionDay(InspectionDay day) {
        getInspectionDayById(day.getIdDay());
        InspectionValidator.validateInspectionDayData(day);
        if (day.getObservations() != null) {
            day.setObservations(day.getObservations().trim());
        }
        return inspectionRepository.saveDay(day);
    }

    /**
     * Deletes an inspection day if it has no registered field inspections.
     *
     * @param idDay identifier of the inspection day to delete
     */
    public void deleteInspectionDay(int idDay) {
        getInspectionDayById(idDay);
        for (InspectionRecord record : inspectionRepository.findAllRecords()) {
            if (record.getIdDay() == idDay) {
                throw new BusinessRuleException(
                        "No se puede eliminar la jornada porque ya tiene registros de inspección asociados.");
            }
        }
        inspectionRepository.deleteDayById(idDay);
    }

    /**
     * Registers a field inspection of an ecological point, updates point status if
     * critical, and triggers automatic alerts when needed.
     *
     * @param record inspection record to register
     * @return registered inspection record
     */
    public InspectionRecord registerPointInspection(InspectionRecord record) {
        InspectionValidator.validateInspectionRecordData(record);

        getInspectionDayById(record.getIdDay());
        EcologicalPoint point = pointService.getEcologicalPointById(record.getIdPoint());
        userService.getUserById(record.getIdUser());

        record.setIdRecord(0);
        record.setPointStatus(record.getPointStatus().trim().toUpperCase());

        for (ContainerDetail detail : record.getContainerDetails()) {
            detail.setFillLevel(detail.getFillLevel().trim().toUpperCase());
            detail.setContainerCondition(detail.getContainerCondition().trim().toUpperCase());
            detail.setSignageCondition(detail.getSignageCondition().trim().toUpperCase());
        }

        if (record.getEvidences() != null) {
            for (Evidence evidence : record.getEvidences()) {
                if (evidence.getUploadedAt() == null) {
                    evidence.setUploadedAt(LocalDateTime.now());
                }
            }
        }

        InspectionRecord saved = inspectionRepository.saveRecord(record);
        if (saved.hasCriticalIncidents()) {
            pointService.updatePointCoordinatesAndStatus(
                    point.getIdPoint(), point.getCoordX(), point.getCoordY(), "CRITICO");
        }
        alertService.evaluateAndGenerateAutomaticAlerts(saved);
        return saved;
    }

    /**
     * Deletes an inspection record by identifier.
     *
     * @param idRecord identifier of the inspection record to delete
     */
    public void deleteInspectionRecord(int idRecord) {
        getInspectionRecordById(idRecord);
        inspectionRepository.deleteRecordById(idRecord);
    }

    /**
     * Finds an inspection day by its identifier.
     *
     * @param idDay inspection day identifier
     * @return matching inspection day
     */
    public InspectionDay getInspectionDayById(int idDay) {
        InspectionDay found = inspectionRepository.findDayById(idDay);
        InspectionValidator.validateInspectionDayExists(found, idDay);
        return found;
    }

    /**
     * Finds an inspection record by its identifier.
     *
     * @param idRecord inspection record identifier
     * @return matching inspection record
     */
    public InspectionRecord getInspectionRecordById(int idRecord) {
        InspectionRecord found = inspectionRepository.findRecordById(idRecord);
        InspectionValidator.validateInspectionRecordExists(found, idRecord);
        return found;
    }

    /**
     * Returns all registered characterization days.
     *
     * @return list of inspection days
     */
    public List<InspectionDay> listInspectionDays() {
        return inspectionRepository.findAllDays();
    }

    /**
     * Returns all registered field inspection records.
     *
     * @return list of inspection records
     */
    public List<InspectionRecord> listAllInspectionRecords() {
        return inspectionRepository.findAllRecords();
    }
}
