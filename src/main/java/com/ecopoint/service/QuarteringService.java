package com.ecopoint.service;

import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.persistence.FileQuarteringRepository;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.validation.QuarteringValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Business service responsible for executing and registering solid waste
 * characterization procedures using the Quartering Method and computing
 * general waste composition statistics.
 * Depends on QuarteringRepository applying Protected Variations and SOLID DIP.
 *
 * @author craulgomez
 * @version 1.0
 */
public class QuarteringService {

    private final InspectionService inspectionService;
    private final QuarteringRepository quarteringRepository;

    /**
     * Constructs a QuarteringService using the default flat-file repository.
     *
     * @param inspectionService service used to verify the originating inspection record
     */
    public QuarteringService(InspectionService inspectionService) {
        this(inspectionService, new FileQuarteringRepository());
    }

    /**
     * Constructs a QuarteringService injecting the InspectionService and QuarteringRepository.
     *
     * @param inspectionService    service used to verify the originating inspection record
     * @param quarteringRepository quartering repository contract
     */
    public QuarteringService(InspectionService inspectionService,
                             QuarteringRepository quarteringRepository) {
        this.inspectionService = inspectionService;
        this.quarteringRepository = quarteringRepository;
    }

    /**
     * Validates, calculates percentages, and registers a new Quartering procedure.
     *
     * @param quartering quartering procedure to register
     * @return registered quartering with computed composition percentages
     */
    public Quartering registerQuartering(Quartering quartering) {
        QuarteringValidator.validateQuarteringData(quartering);
        QuarteringValidator.validateUniqueQuarteringPerRecord(
                quartering.getIdRecord(), quarteringRepository.findAll());

        inspectionService.getInspectionRecordById(quartering.getIdRecord());

        quartering.setIdQuartering(0);
        quartering.calculateCompositionPercentages();
        return quarteringRepository.save(quartering);
    }

    /**
     * Deletes a quartering procedure by identifier.
     *
     * @param idQuartering identifier of the quartering to delete
     */
    public void deleteQuartering(int idQuartering) {
        getQuarteringById(idQuartering);
        quarteringRepository.deleteById(idQuartering);
    }

    /**
     * Finds a quartering procedure by its identifier.
     *
     * @param idQuartering quartering identifier
     * @return matching quartering instance
     */
    public Quartering getQuarteringById(int idQuartering) {
        Quartering found = quarteringRepository.findById(idQuartering);
        QuarteringValidator.validateQuarteringExists(found, idQuartering);
        return found;
    }

    /**
     * Returns all registered quartering procedures.
     *
     * @return list of quartering procedures
     */
    public List<Quartering> listAllQuarterings() {
        return quarteringRepository.findAll();
    }

    /**
     * Calculates consolidated waste statistics across all registered quarterings:
     * total sample weight, total classified weight, and global composition percentages
     * for waste categories 1 (BLANCO), 2 (VERDE), and 3 (NEGRO).
     *
     * @return map of consolidated metric names to BigDecimal values
     */
    public Map<String, BigDecimal> calculateConsolidatedStatistics() {
        List<Quartering> all = quarteringRepository.findAll();
        BigDecimal totalInitial = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSample = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalWhite = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGreen = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBlack = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Quartering q : all) {
            if (q.getInitialWeight() != null) {
                totalInitial = totalInitial.add(q.getInitialWeight());
            }
            if (q.getSampleWeight() != null) {
                totalSample = totalSample.add(q.getSampleWeight());
            }
            if (q.getResults() != null) {
                for (QuarteringResult r : q.getResults()) {
                    if (r.getWeight() == null) {
                        continue;
                    }
                    if (r.getIdWaste() == 1) {
                        totalWhite = totalWhite.add(r.getWeight());
                    } else if (r.getIdWaste() == 2) {
                        totalGreen = totalGreen.add(r.getWeight());
                    } else if (r.getIdWaste() == 3) {
                        totalBlack = totalBlack.add(r.getWeight());
                    }
                }
            }
        }

        BigDecimal hundred = new BigDecimal("100.00");
        BigDecimal pctWhite = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pctGreen = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pctBlack = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (totalSample.compareTo(BigDecimal.ZERO) > 0) {
            pctWhite = totalWhite.multiply(hundred).divide(totalSample, 2, RoundingMode.HALF_UP);
            pctGreen = totalGreen.multiply(hundred).divide(totalSample, 2, RoundingMode.HALF_UP);
            pctBlack = totalBlack.multiply(hundred).divide(totalSample, 2, RoundingMode.HALF_UP);
        }

        Map<String, BigDecimal> stats = new LinkedHashMap<>();
        stats.put("totalInitialWeight", totalInitial.setScale(2, RoundingMode.HALF_UP));
        stats.put("totalSampleWeight", totalSample.setScale(2, RoundingMode.HALF_UP));
        stats.put("totalWhiteWeight", totalWhite.setScale(2, RoundingMode.HALF_UP));
        stats.put("totalGreenWeight", totalGreen.setScale(2, RoundingMode.HALF_UP));
        stats.put("totalBlackWeight", totalBlack.setScale(2, RoundingMode.HALF_UP));
        stats.put("whitePercentage", pctWhite);
        stats.put("greenPercentage", pctGreen);
        stats.put("blackPercentage", pctBlack);
        return stats;
    }
}
