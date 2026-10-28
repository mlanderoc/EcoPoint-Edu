package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Flat-file implementation of QuarteringRepository storing procedures in
 * data/quarterings.txt and their results in data/quartering_results.txt.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class FileQuarteringRepository implements QuarteringRepository {

    private static final String QUARTERINGS_FILE = "quarterings.txt";
    private static final String RESULTS_FILE = "quartering_results.txt";

    /**
     * Constructs a FileQuarteringRepository and initializes the flat files.
     */
    public FileQuarteringRepository() {
        FileStorageHelper.resolveDataFile(QUARTERINGS_FILE);
        FileStorageHelper.resolveDataFile(RESULTS_FILE);
    }

    @Override
    public Quartering save(Quartering quartering) {
        List<Quartering> all = findAll();
        if (quartering.getIdQuartering() <= 0) {
            int maxId = 0;
            for (Quartering existing : all) {
                if (existing.getIdQuartering() > maxId) {
                    maxId = existing.getIdQuartering();
                }
            }
            quartering.setIdQuartering(maxId + 1);
            assignResultIds(quartering);
            all.add(quartering);
        } else {
            assignResultIds(quartering);
            boolean updated = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getIdQuartering() == quartering.getIdQuartering()) {
                    all.set(i, quartering);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                all.add(quartering);
            }
        }
        writeAll(all);
        return quartering;
    }

    @Override
    public Quartering findById(int idQuartering) {
        for (Quartering q : findAll()) {
            if (q.getIdQuartering() == idQuartering) {
                return q;
            }
        }
        return null;
    }

    @Override
    public List<Quartering> findAll() {
        List<String> qLines = FileStorageHelper.readLines(QUARTERINGS_FILE);
        List<String> rLines = FileStorageHelper.readLines(RESULTS_FILE);
        List<Quartering> list = new ArrayList<>();

        for (String line : qLines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 5) {
                    list.add(new Quartering(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            new BigDecimal(parts[2].trim()),
                            new BigDecimal(parts[3].trim()),
                            parts[4].trim()
                    ));
                }
            } catch (Exception ex) {
                throw new PersistenceException("Formato inválido en archivo " + QUARTERINGS_FILE, ex);
            }
        }

        for (String line : rLines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 6) {
                    QuarteringResult res = new QuarteringResult(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()),
                            new BigDecimal(parts[3].trim()),
                            Integer.parseInt(parts[4].trim()),
                            new BigDecimal(parts[5].trim())
                    );
                    for (Quartering q : list) {
                        if (q.getIdQuartering() == res.getIdQuartering()) {
                            q.addResult(res);
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                throw new PersistenceException("Formato inválido en archivo " + RESULTS_FILE, ex);
            }
        }

        return list;
    }

    @Override
    public boolean deleteById(int idQuartering) {
        List<Quartering> all = findAll();
        boolean removed = all.removeIf(q -> q.getIdQuartering() == idQuartering);
        if (removed) {
            writeAll(all);
        }
        return removed;
    }

    private void assignResultIds(Quartering quartering) {
        if (quartering.getResults() == null) {
            return;
        }
        int seq = 1;
        for (QuarteringResult res : quartering.getResults()) {
            res.setIdQuartering(quartering.getIdQuartering());
            res.setIdResult(seq++);
        }
    }

    private void writeAll(List<Quartering> list) {
        List<String> qLines = new ArrayList<>();
        List<String> rLines = new ArrayList<>();

        for (Quartering q : list) {
            qLines.add(q.getIdQuartering() + FileStorageHelper.FIELD_SEPARATOR
                    + q.getIdRecord() + FileStorageHelper.FIELD_SEPARATOR
                    + q.getInitialWeight() + FileStorageHelper.FIELD_SEPARATOR
                    + q.getSampleWeight() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(q.getObservations()));

            if (q.getResults() != null) {
                for (QuarteringResult res : q.getResults()) {
                    rLines.add(q.getIdQuartering() + FileStorageHelper.FIELD_SEPARATOR
                            + res.getIdResult() + FileStorageHelper.FIELD_SEPARATOR
                            + res.getIdWaste() + FileStorageHelper.FIELD_SEPARATOR
                            + res.getWeight() + FileStorageHelper.FIELD_SEPARATOR
                            + res.getQuantity() + FileStorageHelper.FIELD_SEPARATOR
                            + res.getPercentage());
                }
            }
        }

        FileStorageHelper.writeLines(QUARTERINGS_FILE, qLines);
        FileStorageHelper.writeLines(RESULTS_FILE, rLines);
    }
}
