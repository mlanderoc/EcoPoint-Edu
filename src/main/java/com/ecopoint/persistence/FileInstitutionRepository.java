package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.Institution;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat-file implementation of InstitutionRepository storing records in data/institutions.txt.
 *
 * @author mlanderoc
 * @version 1.0
 */
public class FileInstitutionRepository implements InstitutionRepository {

    private static final String FILE_NAME = "institutions.txt";

    /**
     * Constructs a FileInstitutionRepository and initializes the flat file.
     */
    public FileInstitutionRepository() {
        FileStorageHelper.resolveDataFile(FILE_NAME);
    }

    @Override
    public Institution save(Institution institution) {
        List<Institution> all = findAll();
        if (institution.getIdInstitution() <= 0) {
            int maxId = 0;
            for (Institution existing : all) {
                if (existing.getIdInstitution() > maxId) {
                    maxId = existing.getIdInstitution();
                }
            }
            institution.setIdInstitution(maxId + 1);
            all.add(institution);
        } else {
            boolean updated = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getIdInstitution() == institution.getIdInstitution()) {
                    all.set(i, institution);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                all.add(institution);
            }
        }
        writeAll(all);
        return institution;
    }

    @Override
    public Institution findById(int idInstitution) {
        for (Institution inst : findAll()) {
            if (inst.getIdInstitution() == idInstitution) {
                return inst;
            }
        }
        return null;
    }

    @Override
    public List<Institution> findAll() {
        List<String> lines = FileStorageHelper.readLines(FILE_NAME);
        List<Institution> list = new ArrayList<>();
        for (String line : lines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 3) {
                    list.add(new Institution(
                            Integer.parseInt(parts[0].trim()),
                            parts[1].trim(),
                            parts[2].trim()
                    ));
                }
            } catch (NumberFormatException ex) {
                throw new PersistenceException("Formato inválido en archivo " + FILE_NAME, ex);
            }
        }
        return list;
    }

    @Override
    public boolean deleteById(int idInstitution) {
        List<Institution> all = findAll();
        boolean removed = all.removeIf(i -> i.getIdInstitution() == idInstitution);
        if (removed) {
            writeAll(all);
        }
        return removed;
    }

    private void writeAll(List<Institution> list) {
        List<String> lines = new ArrayList<>();
        for (Institution inst : list) {
            lines.add(inst.getIdInstitution() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(inst.getName()) + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(inst.getAddress()));
        }
        FileStorageHelper.writeLines(FILE_NAME, lines);
    }
}
