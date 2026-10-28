package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.Container;
import com.ecopoint.model.EcologicalPoint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Flat-file implementation of EcologicalPointRepository storing points in data/points.txt
 * and their associated containers in data/containers.txt.
 *
 * @author mlanderoc
 * @version 1.0
 */
public class FileEcologicalPointRepository implements EcologicalPointRepository {

    private static final String POINTS_FILE = "points.txt";
    private static final String CONTAINERS_FILE = "containers.txt";

    /**
     * Constructs a FileEcologicalPointRepository and initializes the flat files.
     */
    public FileEcologicalPointRepository() {
        FileStorageHelper.resolveDataFile(POINTS_FILE);
        FileStorageHelper.resolveDataFile(CONTAINERS_FILE);
    }

    @Override
    public EcologicalPoint save(EcologicalPoint point) {
        List<EcologicalPoint> all = findAll();
        if (point.getIdPoint() <= 0) {
            int maxId = 0;
            for (EcologicalPoint existing : all) {
                if (existing.getIdPoint() > maxId) {
                    maxId = existing.getIdPoint();
                }
            }
            point.setIdPoint(maxId + 1);
            assignContainerIds(point);
            all.add(point);
        } else {
            assignContainerIds(point);
            boolean updated = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getIdPoint() == point.getIdPoint()) {
                    all.set(i, point);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                all.add(point);
            }
        }
        writeAll(all);
        return point;
    }

    @Override
    public EcologicalPoint findById(int idPoint) {
        for (EcologicalPoint point : findAll()) {
            if (point.getIdPoint() == idPoint) {
                return point;
            }
        }
        return null;
    }

    @Override
    public List<EcologicalPoint> findAll() {
        List<String> pointLines = FileStorageHelper.readLines(POINTS_FILE);
        List<String> containerLines = FileStorageHelper.readLines(CONTAINERS_FILE);
        List<EcologicalPoint> points = new ArrayList<>();

        for (String line : pointLines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 8) {
                    EcologicalPoint p = new EcologicalPoint(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            parts[2].trim(),
                            parts[3].trim(),
                            parts[4].trim(),
                            new BigDecimal(parts[5].trim()),
                            new BigDecimal(parts[6].trim()),
                            parts[7].trim()
                    );
                    points.add(p);
                }
            } catch (NumberFormatException ex) {
                throw new PersistenceException("Formato inválido en archivo " + POINTS_FILE, ex);
            }
        }

        for (String line : containerLines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 5) {
                    Container c = new Container(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            parts[2].trim(),
                            parts[3].trim(),
                            new BigDecimal(parts[4].trim())
                    );
                    for (EcologicalPoint p : points) {
                        if (p.getIdPoint() == c.getIdPoint()) {
                            p.addContainer(c);
                            break;
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                throw new PersistenceException("Formato inválido en archivo " + CONTAINERS_FILE, ex);
            }
        }

        return points;
    }

    @Override
    public boolean deleteById(int idPoint) {
        List<EcologicalPoint> all = findAll();
        boolean removed = all.removeIf(p -> p.getIdPoint() == idPoint);
        if (removed) {
            writeAll(all);
        }
        return removed;
    }

    private void assignContainerIds(EcologicalPoint point) {
        if (point.getContainers() == null) {
            return;
        }
        int seq = 1;
        for (Container c : point.getContainers()) {
            if (c != null) {
                c.setIdPoint(point.getIdPoint());
                c.setIdContainer(seq++);
            }
        }
    }

    private void writeAll(List<EcologicalPoint> points) {
        List<String> pointLines = new ArrayList<>();
        List<String> containerLines = new ArrayList<>();

        for (EcologicalPoint p : points) {
            pointLines.add(p.getIdPoint() + FileStorageHelper.FIELD_SEPARATOR
                    + p.getIdZone() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(p.getLocation()) + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(p.getStatus()) + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(p.getInitialStatus()) + FileStorageHelper.FIELD_SEPARATOR
                    + p.getCoordX() + FileStorageHelper.FIELD_SEPARATOR
                    + p.getCoordY() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(p.getObservations()));

            if (p.getContainers() != null) {
                for (Container c : p.getContainers()) {
                    containerLines.add(p.getIdPoint() + FileStorageHelper.FIELD_SEPARATOR
                            + c.getIdContainer() + FileStorageHelper.FIELD_SEPARATOR
                            + FileStorageHelper.clean(c.getWasteType()) + FileStorageHelper.FIELD_SEPARATOR
                            + FileStorageHelper.clean(c.getColor()) + FileStorageHelper.FIELD_SEPARATOR
                            + c.getCapacity());
                }
            }
        }

        FileStorageHelper.writeLines(POINTS_FILE, pointLines);
        FileStorageHelper.writeLines(CONTAINERS_FILE, containerLines);
    }
}
