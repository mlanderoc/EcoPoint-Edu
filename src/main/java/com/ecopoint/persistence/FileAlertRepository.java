package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.Alert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Flat-file implementation of AlertRepository storing records in data/alerts.txt.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class FileAlertRepository implements AlertRepository {

    private static final String FILE_NAME = "alerts.txt";

    /**
     * Constructs a FileAlertRepository and initializes the flat file.
     */
    public FileAlertRepository() {
        FileStorageHelper.resolveDataFile(FILE_NAME);
    }

    @Override
    public Alert save(Alert alert) {
        List<Alert> all = findAll();
        if (alert.getIdAlert() <= 0) {
            int maxId = 0;
            for (Alert existing : all) {
                if (existing.getIdAlert() > maxId) {
                    maxId = existing.getIdAlert();
                }
            }
            alert.setIdAlert(maxId + 1);
            if (alert.getGeneratedAt() == null) {
                alert.setGeneratedAt(LocalDateTime.now());
            }
            all.add(alert);
        } else {
            boolean updated = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getIdAlert() == alert.getIdAlert()) {
                    all.set(i, alert);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                all.add(alert);
            }
        }
        writeAll(all);
        return alert;
    }

    @Override
    public Alert findById(int idAlert) {
        for (Alert alert : findAll()) {
            if (alert.getIdAlert() == idAlert) {
                return alert;
            }
        }
        return null;
    }

    @Override
    public List<Alert> findAll() {
        List<String> lines = FileStorageHelper.readLines(FILE_NAME);
        List<Alert> alerts = new ArrayList<>();
        for (String line : lines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 6) {
                    alerts.add(new Alert(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            parts[2].trim(),
                            Boolean.parseBoolean(parts[3].trim()),
                            parts[4].trim(),
                            LocalDateTime.parse(parts[5].trim())
                    ));
                }
            } catch (Exception ex) {
                throw new PersistenceException("Formato inválido en archivo " + FILE_NAME, ex);
            }
        }
        return alerts;
    }

    @Override
    public boolean deleteById(int idAlert) {
        List<Alert> all = findAll();
        boolean removed = all.removeIf(a -> a.getIdAlert() == idAlert);
        if (removed) {
            writeAll(all);
        }
        return removed;
    }

    private void writeAll(List<Alert> alerts) {
        List<String> lines = new ArrayList<>();
        for (Alert a : alerts) {
            lines.add(a.getIdAlert() + FileStorageHelper.FIELD_SEPARATOR
                    + a.getIdContainerDetail() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(a.getType()) + FileStorageHelper.FIELD_SEPARATOR
                    + a.isAttended() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(a.getDescription()) + FileStorageHelper.FIELD_SEPARATOR
                    + a.getGeneratedAt());
        }
        FileStorageHelper.writeLines(FILE_NAME, lines);
    }
}
