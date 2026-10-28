package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.Zone;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat-file implementation of ZoneRepository storing records in data/zones.txt.
 *
 * @author mlanderoc
 * @version 1.0
 */
public class FileZoneRepository implements ZoneRepository {

    private static final String FILE_NAME = "zones.txt";

    /**
     * Constructs a FileZoneRepository and initializes the flat file.
     */
    public FileZoneRepository() {
        FileStorageHelper.resolveDataFile(FILE_NAME);
    }

    @Override
    public Zone save(Zone zone) {
        List<Zone> all = findAll();
        if (zone.getIdZone() <= 0) {
            int maxId = 0;
            for (Zone existing : all) {
                if (existing.getIdZone() > maxId) {
                    maxId = existing.getIdZone();
                }
            }
            zone.setIdZone(maxId + 1);
            all.add(zone);
        } else {
            boolean updated = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getIdZone() == zone.getIdZone()) {
                    all.set(i, zone);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                all.add(zone);
            }
        }
        writeAll(all);
        return zone;
    }

    @Override
    public Zone findById(int idZone) {
        for (Zone zone : findAll()) {
            if (zone.getIdZone() == idZone) {
                return zone;
            }
        }
        return null;
    }

    @Override
    public List<Zone> findAll() {
        List<String> lines = FileStorageHelper.readLines(FILE_NAME);
        List<Zone> list = new ArrayList<>();
        for (String line : lines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 4) {
                    list.add(new Zone(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            parts[2].trim(),
                            parts[3].trim()
                    ));
                }
            } catch (NumberFormatException ex) {
                throw new PersistenceException("Formato inválido en archivo " + FILE_NAME, ex);
            }
        }
        return list;
    }

    @Override
    public boolean deleteById(int idZone) {
        List<Zone> all = findAll();
        boolean removed = all.removeIf(z -> z.getIdZone() == idZone);
        if (removed) {
            writeAll(all);
        }
        return removed;
    }

    private void writeAll(List<Zone> list) {
        List<String> lines = new ArrayList<>();
        for (Zone z : list) {
            lines.add(z.getIdZone() + FileStorageHelper.FIELD_SEPARATOR
                    + z.getIdInstitution() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(z.getName()) + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(z.getDescription()));
        }
        FileStorageHelper.writeLines(FILE_NAME, lines);
    }
}
