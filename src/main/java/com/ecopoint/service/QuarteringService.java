package com.ecopoint.service;

import com.ecopoint.model.InspectionRecord;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.validation.QuarteringValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Business service responsible for executing and registering solid waste
 * characterization procedures using the Quartering Method ({@link Quartering}).
 * <p>
 * Delegates weight coherence checks to {@link QuarteringValidator} and triggers
 * automatic composition percentage calculation on the {@link Quartering} domain model.
 * </p>
 *
 * @author craulgomez
 * @version 1.0
 */
public class QuarteringService {

    private final InspectionService inspectionService;
    private final List<Quartering> quarterings;
    private int nextQuarteringId;

    /**
     * Constructs a QuarteringService linked to the {@link InspectionService}.
     *
     * @param inspectionService service used to verify the originating inspection record
     */
    public QuarteringService(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
        this.quarterings = new ArrayList<>();
        this.nextQuarteringId = 1;
    }

    /**
     * Validates, calculates percentages, and registers a new {@link Quartering} procedure.
     *
     * @param quartering quartering procedure to register
     * @return registered quartering with computed composition percentages
     */
    public Quartering registerQuartering(Quartering quartering) {
        QuarteringValidator.validateQuarteringData(quartering);
        QuarteringValidator.validateUniqueQuarteringPerRecord(quartering.getIdRecord(), quarterings);

        InspectionRecord record = inspectionService.getInspectionRecordById(quartering.getIdRecord());

        quartering.setIdQuartering(nextQuarteringId++);
        int sequence = 1;
        for (QuarteringResult result : quartering.getResults()) {
            result.setIdQuartering(quartering.getIdQuartering());
            result.setIdResult(sequence++);
        }

        quartering.calculateCompositionPercentages();
        record.setQuartering(quartering);
        quarterings.add(quartering);
        return quartering;
    }

    /**
     * Finds a quartering procedure by its identifier.
     *
     * @param idQuartering quartering identifier
     * @return matching quartering instance
     */
    public Quartering getQuarteringById(int idQuartering) {
        Quartering found = null;
        for (Quartering q : quarterings) {
            if (q.getIdQuartering() == idQuartering) {
                found = q;
                break;
            }
        }
        QuarteringValidator.validateQuarteringExists(found, idQuartering);
        return found;
    }

    /**
     * Returns all registered quartering procedures.
     *
     * @return unmodifiable list of quartering procedures
     */
    public List<Quartering> listAllQuarterings() {
        return Collections.unmodifiableList(quarterings);
    }
}