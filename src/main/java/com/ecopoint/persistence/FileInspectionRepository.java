package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.ContainerDetail;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.InspectionDay;
import com.ecopoint.model.InspectionRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Flat-file implementation of InspectionRepository storing characterization days,
 * field inspection records, container evaluations, and evidences in flat files.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class FileInspectionRepository implements InspectionRepository {

    private static final String DAYS_FILE = "inspection_days.txt";
    private static final String RECORDS_FILE = "inspection_records.txt";
    private static final String DETAILS_FILE = "container_details.txt";
    private static final String EVIDENCES_FILE = "evidences.txt";

    /**
     * Constructs a FileInspectionRepository and initializes all flat files.
     */
    public FileInspectionRepository() {
        FileStorageHelper.resolveDataFile(DAYS_FILE);
        FileStorageHelper.resolveDataFile(RECORDS_FILE);
        FileStorageHelper.resolveDataFile(DETAILS_FILE);
        FileStorageHelper.resolveDataFile(EVIDENCES_FILE);
    }

    @Override
    public InspectionDay saveDay(InspectionDay day) {
        List<InspectionDay> all = findAllDays();
        if (day.getIdDay() <= 0) {
            int maxId = 0;
            for (InspectionDay existing : all) {
                if (existing.getIdDay() > maxId) {
                    maxId = existing.getIdDay();
                }
            }
            day.setIdDay(maxId + 1);
            all.add(day);
        } else {
            boolean updated = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getIdDay() == day.getIdDay()) {
                    all.set(i, day);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                all.add(day);
            }
        }
        writeDays(all);
        return day;
    }

    @Override
    public InspectionDay findDayById(int idDay) {
        for (InspectionDay day : findAllDays()) {
            if (day.getIdDay() == idDay) {
                return day;
            }
        }
        return null;
    }

    @Override
    public List<InspectionDay> findAllDays() {
        List<String> lines = FileStorageHelper.readLines(DAYS_FILE);
        List<InspectionDay> days = new ArrayList<>();
        for (String line : lines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 3) {
                    days.add(new InspectionDay(
                            Integer.parseInt(parts[0].trim()),
                            LocalDate.parse(parts[1].trim()),
                            parts[2].trim()
                    ));
                }
            } catch (Exception ex) {
                throw new PersistenceException("Formato inválido en archivo " + DAYS_FILE, ex);
            }
        }
        List<InspectionRecord> records = findAllRecords();
        for (InspectionDay day : days) {
            for (InspectionRecord record : records) {
                if (record.getIdDay() == day.getIdDay()) {
                    day.addInspectionRecord(record);
                }
            }
        }
        return days;
    }

    @Override
    public boolean deleteDayById(int idDay) {
        List<InspectionDay> days = findAllDays();
        boolean removed = days.removeIf(d -> d.getIdDay() == idDay);
        if (removed) {
            writeDays(days);
        }
        return removed;
    }

    @Override
    public InspectionRecord saveRecord(InspectionRecord record) {
        List<InspectionRecord> all = findAllRecords();
        int maxRecordId = 0;
        int maxDetailId = 0;
        int maxEvidenceId = 0;

        for (InspectionRecord existing : all) {
            if (existing.getIdRecord() > maxRecordId) {
                maxRecordId = existing.getIdRecord();
            }
            if (existing.getContainerDetails() != null) {
                for (ContainerDetail d : existing.getContainerDetails()) {
                    if (d.getIdContainerDetail() > maxDetailId) {
                        maxDetailId = d.getIdContainerDetail();
                    }
                }
            }
            if (existing.getEvidences() != null) {
                for (Evidence ev : existing.getEvidences()) {
                    if (ev.getIdEvidence() > maxEvidenceId) {
                        maxEvidenceId = ev.getIdEvidence();
                    }
                }
            }
        }

        if (record.getIdRecord() <= 0) {
            record.setIdRecord(maxRecordId + 1);
            assignChildIds(record, maxDetailId, maxEvidenceId);
            all.add(record);
        } else {
            assignChildIds(record, maxDetailId, maxEvidenceId);
            boolean updated = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getIdRecord() == record.getIdRecord()) {
                    all.set(i, record);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                all.add(record);
            }
        }
        writeRecords(all);
        return record;
    }

    @Override
    public InspectionRecord findRecordById(int idRecord) {
        for (InspectionRecord record : findAllRecords()) {
            if (record.getIdRecord() == idRecord) {
                return record;
            }
        }
        return null;
    }

    @Override
    public List<InspectionRecord> findAllRecords() {
        List<String> recordLines = FileStorageHelper.readLines(RECORDS_FILE);
        List<String> detailLines = FileStorageHelper.readLines(DETAILS_FILE);
        List<String> evidenceLines = FileStorageHelper.readLines(EVIDENCES_FILE);

        List<InspectionRecord> records = new ArrayList<>();
        for (String line : recordLines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 7) {
                    records.add(new InspectionRecord(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()),
                            Integer.parseInt(parts[3].trim()),
                            LocalDate.parse(parts[4].trim()),
                            parts[5].trim(),
                            parts[6].trim()
                    ));
                }
            } catch (Exception ex) {
                throw new PersistenceException("Formato inválido en archivo " + RECORDS_FILE, ex);
            }
        }

        for (String line : detailLines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 9) {
                    ContainerDetail d = new ContainerDetail(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()),
                            Integer.parseInt(parts[3].trim()),
                            parts[4].trim(),
                            parts[5].trim(),
                            parts[6].trim(),
                            Boolean.parseBoolean(parts[7].trim()),
                            parts[8].trim()
                    );
                    for (InspectionRecord r : records) {
                        if (r.getIdRecord() == d.getIdRecord()) {
                            r.addContainerDetail(d);
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                throw new PersistenceException("Formato inválido en archivo " + DETAILS_FILE, ex);
            }
        }

        for (String line : evidenceLines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 5) {
                    Evidence ev = new Evidence(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            parts[2].trim(),
                            parts[3].trim(),
                            LocalDateTime.parse(parts[4].trim())
                    );
                    for (InspectionRecord r : records) {
                        if (r.getIdRecord() == ev.getIdRecord()) {
                            r.addEvidence(ev);
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                throw new PersistenceException("Formato inválido en archivo " + EVIDENCES_FILE, ex);
            }
        }

        return records;
    }

    @Override
    public boolean deleteRecordById(int idRecord) {
        List<InspectionRecord> all = findAllRecords();
        boolean removed = all.removeIf(r -> r.getIdRecord() == idRecord);
        if (removed) {
            writeRecords(all);
        }
        return removed;
    }

    private void assignChildIds(InspectionRecord record, int maxDetailId, int maxEvidenceId) {
        int nextDetail = maxDetailId + 1;
        if (record.getContainerDetails() != null) {
            for (ContainerDetail d : record.getContainerDetails()) {
                if (d.getIdContainerDetail() <= 0) {
                    d.setIdContainerDetail(nextDetail++);
                }
                d.setIdRecord(record.getIdRecord());
                d.setIdPoint(record.getIdPoint());
            }
        }
        int nextEv = maxEvidenceId + 1;
        if (record.getEvidences() != null) {
            for (Evidence ev : record.getEvidences()) {
                if (ev.getIdEvidence() <= 0) {
                    ev.setIdEvidence(nextEv++);
                }
                ev.setIdRecord(record.getIdRecord());
                if (ev.getUploadedAt() == null) {
                    ev.setUploadedAt(LocalDateTime.now());
                }
            }
        }
    }

    private void writeDays(List<InspectionDay> days) {
        List<String> lines = new ArrayList<>();
        for (InspectionDay d : days) {
            lines.add(d.getIdDay() + FileStorageHelper.FIELD_SEPARATOR
                    + d.getDate() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(d.getObservations()));
        }
        FileStorageHelper.writeLines(DAYS_FILE, lines);
    }

    private void writeRecords(List<InspectionRecord> records) {
        List<String> recordLines = new ArrayList<>();
        List<String> detailLines = new ArrayList<>();
        List<String> evidenceLines = new ArrayList<>();

        for (InspectionRecord r : records) {
            recordLines.add(r.getIdRecord() + FileStorageHelper.FIELD_SEPARATOR
                    + r.getIdDay() + FileStorageHelper.FIELD_SEPARATOR
                    + r.getIdPoint() + FileStorageHelper.FIELD_SEPARATOR
                    + r.getIdUser() + FileStorageHelper.FIELD_SEPARATOR
                    + r.getCaptureDate() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(r.getPointStatus()) + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(r.getObservations()));

            if (r.getContainerDetails() != null) {
                for (ContainerDetail d : r.getContainerDetails()) {
                    detailLines.add(d.getIdContainerDetail() + FileStorageHelper.FIELD_SEPARATOR
                            + r.getIdRecord() + FileStorageHelper.FIELD_SEPARATOR
                            + r.getIdPoint() + FileStorageHelper.FIELD_SEPARATOR
                            + d.getIdContainer() + FileStorageHelper.FIELD_SEPARATOR
                            + FileStorageHelper.clean(d.getFillLevel()) + FileStorageHelper.FIELD_SEPARATOR
                            + FileStorageHelper.clean(d.getContainerCondition()) + FileStorageHelper.FIELD_SEPARATOR
                            + FileStorageHelper.clean(d.getSignageCondition()) + FileStorageHelper.FIELD_SEPARATOR
                            + d.isImproperSeparation() + FileStorageHelper.FIELD_SEPARATOR
                            + FileStorageHelper.clean(d.getObservations()));
                }
            }

            if (r.getEvidences() != null) {
                for (Evidence ev : r.getEvidences()) {
                    evidenceLines.add(ev.getIdEvidence() + FileStorageHelper.FIELD_SEPARATOR
                            + r.getIdRecord() + FileStorageHelper.FIELD_SEPARATOR
                            + FileStorageHelper.clean(ev.getFilePath()) + FileStorageHelper.FIELD_SEPARATOR
                            + FileStorageHelper.clean(ev.getDescription()) + FileStorageHelper.FIELD_SEPARATOR
                            + ev.getUploadedAt());
                }
            }
        }

        FileStorageHelper.writeLines(RECORDS_FILE, recordLines);
        FileStorageHelper.writeLines(DETAILS_FILE, detailLines);
        FileStorageHelper.writeLines(EVIDENCES_FILE, evidenceLines);
    }
}
