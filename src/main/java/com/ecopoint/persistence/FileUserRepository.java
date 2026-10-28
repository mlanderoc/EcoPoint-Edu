package com.ecopoint.persistence;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat-file implementation of UserRepository storing records in data/users.txt.
 *
 * @author mlanderoc
 * @version 1.0
 */
public class FileUserRepository implements UserRepository {

    private static final String FILE_NAME = "users.txt";

    /**
     * Constructs a FileUserRepository and initializes the flat file.
     */
    public FileUserRepository() {
        FileStorageHelper.resolveDataFile(FILE_NAME);
    }

    @Override
    public User save(User user) {
        List<User> all = findAll();
        if (user.getIdUser() <= 0) {
            int maxId = 0;
            for (User existing : all) {
                if (existing.getIdUser() > maxId) {
                    maxId = existing.getIdUser();
                }
            }
            user.setIdUser(maxId + 1);
            all.add(user);
        } else {
            boolean updated = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getIdUser() == user.getIdUser()) {
                    all.set(i, user);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                all.add(user);
            }
        }
        writeAll(all);
        return user;
    }

    @Override
    public User findById(int idUser) {
        for (User user : findAll()) {
            if (user.getIdUser() == idUser) {
                return user;
            }
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim();
        for (User user : findAll()) {
            if (user.getEmail() != null && user.getEmail().trim().equalsIgnoreCase(normalized)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public List<User> findAll() {
        List<String> lines = FileStorageHelper.readLines(FILE_NAME);
        List<User> users = new ArrayList<>();
        for (String line : lines) {
            try {
                String[] parts = line.split(FileStorageHelper.FIELD_SPLIT_REGEX, -1);
                if (parts.length >= 5) {
                    User u = new User(
                            Integer.parseInt(parts[0].trim()),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim(),
                            parts[4].trim()
                    );
                    users.add(u);
                }
            } catch (NumberFormatException ex) {
                throw new PersistenceException("Formato corrupto en archivo " + FILE_NAME, ex);
            }
        }
        return users;
    }

    @Override
    public boolean deleteById(int idUser) {
        List<User> all = findAll();
        boolean removed = all.removeIf(u -> u.getIdUser() == idUser);
        if (removed) {
            writeAll(all);
        }
        return removed;
    }

    private void writeAll(List<User> users) {
        List<String> lines = new ArrayList<>();
        for (User u : users) {
            String line = u.getIdUser() + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(u.getFirstName()) + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(u.getLastName()) + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(u.getEmail()) + FileStorageHelper.FIELD_SEPARATOR
                    + FileStorageHelper.clean(u.getRole());
            lines.add(line);
        }
        FileStorageHelper.writeLines(FILE_NAME, lines);
    }
}
