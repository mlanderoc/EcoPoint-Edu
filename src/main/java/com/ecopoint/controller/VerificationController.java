package com.ecopoint.controller;

import com.ecopoint.dto.CorrectionRequest;
import com.ecopoint.dto.CorrectionResponse;
import com.ecopoint.dto.RecordVerificationResponse;
import com.ecopoint.dto.VerifiableRecordResponse;
import com.ecopoint.dto.VerificationSummaryResponse;
import com.ecopoint.model.VerifiableRecord;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.RecordKind;
import com.ecopoint.service.VerificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Double entry: pending records, verification, correction and history
 * (spec 003, section 5.3).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
public class VerificationController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.ofEntries(
            Map.entry("motivo", "reason"), Map.entry("tara", "values.tare"),
            Map.entry("pesoBruto", "values.grossWeight"), Map.entry("volumen", "values.volume"),
            Map.entry("estudiantes", "values.students"), Map.entry("docentes", "values.teachers"),
            Map.entry("personal", "values.staff"), Map.entry("poblacion", "values"),
            Map.entry("pesoSubmuestra", "values.sampleWeight"), Map.entry("peso", "values"),
            Map.entry("volumenRecipiente", "values.containerVolume"), Map.entry("pesoNeto", "values.netWeight"));

    private final VerificationService verifications;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param verifications verification service
     * @param currentUser   user of the session
     */
    public VerificationController(VerificationService verifications, CurrentUser currentUser) {
        this.verifications = verifications;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Lists the records of a study, or of one of its shifts, still pending verification.
     *
     * @param principal user of the session
     * @param studyId   study id
     * @param shiftId   shift id, optional
     * @return pending records
     */
    @GetMapping("/api/v1/verifications")
    public List<RecordVerificationResponse> pending(@AuthenticationPrincipal EcoPointUserDetails principal,
                                                    @RequestParam Long studyId,
                                                    @RequestParam(required = false) Long shiftId) {
        return verifications.pending(currentUser.of(principal), studyId, shiftId).stream()
                .map(pending -> new RecordVerificationResponse(
                        RecordKind.fromRecordType(pending.recordType()).getPath(), pending.recordType(),
                        pending.recordId(), pending.shiftId(), null))
                .toList();
    }

    /**
     * Marks a record as matching the paper form (RN-062).
     *
     * @param principal  user of the session
     * @param recordType population, bag, quartering or density
     * @param recordId   record id
     * @return the record with its verification
     */
    @PostMapping("/api/v1/verifications/{recordType}/{recordId}")
    public RecordVerificationResponse verify(@AuthenticationPrincipal EcoPointUserDetails principal,
                                             @PathVariable String recordType, @PathVariable Long recordId) {
        RecordKind kind = RecordKind.fromPath(recordType);
        VerifiableRecord record = verifications.verify(currentUser.of(principal), kind, recordId);
        return RecordVerificationResponse.from(kind.getPath(), verifications.shiftOf(kind, recordId), record);
    }

    /**
     * Corrects a record and keeps the old values in its history (RN-063).
     *
     * @param principal  user of the session
     * @param recordType population, bag, quartering or density
     * @param recordId   record id
     * @param request    new values and reason
     * @return the record with its verification
     */
    @PostMapping("/api/v1/verifications/{recordType}/{recordId}/correct")
    public RecordVerificationResponse correct(@AuthenticationPrincipal EcoPointUserDetails principal,
                                              @PathVariable String recordType, @PathVariable Long recordId,
                                              @RequestBody CorrectionRequest request) {
        RecordKind kind = RecordKind.fromPath(recordType);
        VerifiableRecord record = verifications.correct(currentUser.of(principal), kind, recordId, request.values(),
                request.reason());
        return RecordVerificationResponse.from(kind.getPath(), verifications.shiftOf(kind, recordId), record);
    }

    /**
     * Shows the correction history of a record (RN-063).
     *
     * @param recordType population, bag, quartering or density
     * @param recordId   record id
     * @return corrections, oldest first
     */
    @GetMapping("/api/v1/records/{recordType}/{recordId}/history")
    public List<CorrectionResponse> history(@PathVariable String recordType, @PathVariable Long recordId) {
        return verifications.history(RecordKind.fromPath(recordType), recordId).stream()
                .map(CorrectionResponse::from)
                .toList();
    }

    /**
     * Summarizes the data cleaning of a study by shift (spec 005, R-424).
     *
     * @param principal user of the session
     * @param studyId   study id
     * @return pending, verified and corrected records, and whether the study can be closed
     */
    @GetMapping("/api/v1/studies/{studyId}/verification-summary")
    public VerificationSummaryResponse summary(@AuthenticationPrincipal EcoPointUserDetails principal,
                                               @PathVariable Long studyId) {
        return VerificationSummaryResponse.from(verifications.summary(currentUser.of(principal), studyId));
    }

    /**
     * Lists the verifiable records of a shift with their values (spec 005, R-420, R-421).
     *
     * @param shiftId shift id
     * @return population, bags, quartering and density of the shift
     */
    @GetMapping("/api/v1/shifts/{shiftId}/records")
    public List<VerifiableRecordResponse> shiftRecords(@PathVariable Long shiftId) {
        return verifications.shiftRecords(shiftId).stream().map(VerifiableRecordResponse::from).toList();
    }

    /**
     * Shows one verifiable record with its values and shift (spec 005, R-430).
     *
     * @param recordType population, bag, quartering or density
     * @param recordId   record id
     * @return the record
     */
    @GetMapping("/api/v1/records/{recordType}/{recordId}")
    public VerifiableRecordResponse record(@PathVariable String recordType, @PathVariable Long recordId) {
        return VerifiableRecordResponse.from(verifications.record(RecordKind.fromPath(recordType), recordId));
    }
}
