package com.ecopoint.service;

import com.ecopoint.model.ContainerDetail;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.InspectionDay;
import com.ecopoint.model.InspectionRecord;
import com.ecopoint.validation.InspectionValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Business service responsible for managing characterization days ({@link InspectionDay})
 * and field inspection records ({@link InspectionRecord}).
 * Coordinates precondition validation via {@link InspectionValidator}, verifies
 * referenced entities, and triggers automatic alerts through {@link AlertService}.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class InspectionService {

    private final EcologicalPointService pointService;
    private final UserService userService;
    private final AlertService alertService;
    private final List<InspectionDay> inspectionDays;
    private final List<InspectionRecord> inspectionRecords;
    private int nextDayId;
    private int nextRecordId;
    private int nextContainerDetailId;
    private int nextEvidenceId;

    /**
     * Constructs an InspectionService injecting required domain services.
     *
     * @param pointService service used to verify ecological point existence
     * @param userService  service used to verify inspector user existence
     * @param alertService service used to trigger automatic alerts
     */
    public InspectionService(EcologicalPointService pointService,
                             UserService userService,
                             AlertService alertService) {
        this.pointService = pointService;
        this.userService = userService;
        this.alertService = alertService;
        this.inspectionDays = new ArrayList<>();
        this.inspectionRecords = new ArrayList<>();
        this.nextDayId = 1;
        this.nextRecordId = 1;
        this.nextContainerDetailId = 1;
        this.nextEvidenceId = 1;
    }

    /**
     * Creates a new environmental characterization day.
     *
     * @param day inspection day to register
     * @return registered inspection day with assigned ID
     */
    public InspectionDay createInspectionDay(InspectionDay day) {
        InspectionValidator.validateInspectionDayData(day);
        day.setIdDay(nextDayId++);
        inspectionDays.add(day);
        return day;
    }

    /**
     * Registers a field inspection of an ecological point, assigns identifiers to
     * container evaluations and evidences, and triggers automatic alerts when needed.
     *
     * @param record inspection record to register
     * @return registered inspection record
     */
    public InspectionRecord registerPointInspection(InspectionRecord record) {
        InspectionValidator.validateInspectionRecordData(record);

        InspectionDay day = getInspectionDayById(record.getIdDay());
        pointService.getEcologicalPointById(record.getIdPoint());
        userService.getUserById(record.getIdUser());

        record.setIdRecord(nextRecordId++);
        record.setPointStatus(record.getPointStatus().trim().toUpperCase());

        for (ContainerDetail detail : record.getContainerDetails()) {
            detail.setIdContainerDetail(nextContainerDetailId++);
            detail.setIdRecord(record.getIdRecord());
            detail.setIdPoint(record.getIdPoint());
            detail.setFillLevel(detail.getFillLevel().trim().toUpperCase());
            detail.setContainerCondition(detail.getContainerCondition().trim().toUpperCase());
            detail.setSignageCondition(detail.getSignageCondition().trim().toUpperCase());
        }

        if (record.getEvidences() != null) {
            for (Evidence evidence : record.getEvidences()) {
                evidence.setIdEvidence(nextEvidenceId++);
                evidence.setIdRecord(record.getIdRecord());
                if (evidence.getUploadedAt() == null) {
                    evidence.setUploadedAt(LocalDateTime.now());
                }
            }
        }

        day.addInspectionRecord(record);
        inspectionRecords.add(record);

        alertService.evaluateAndGenerateAutomaticAlerts(record);
        return record;
    }

    /**
     * Finds an inspection day by its identifier.
     *
     * @param idDay inspection day identifier
     * @return matching inspection day
     */
    public InspectionDay getInspectionDayById(int idDay) {
        InspectionDay found = null;
        for (InspectionDay day : inspectionDays) {
            if (day.getIdDay() == idDay) {
                found = day;
                break;
            }
        }
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
        InspectionRecord found = null;
        for (InspectionRecord record : inspectionRecords) {
            if (record.getIdRecord() == idRecord) {
                found = record;
                break;
            }
        }
        InspectionValidator.validateInspectionRecordExists(found, idRecord);
        return found;
    }

    /**
     * Returns all registered characterization days.
     *
     * @return unmodifiable list of inspection days
     */
    public List<InspectionDay> listInspectionDays() {
        return Collections.unmodifiableList(inspectionDays);
    }
}
