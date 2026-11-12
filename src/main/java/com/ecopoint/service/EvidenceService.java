package com.ecopoint.service;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Evidence;
import com.ecopoint.model.EvidenceTarget;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.persistence.EvidenceRepository;
import com.ecopoint.persistence.QuarteringRepository;
import com.ecopoint.persistence.ShiftRepository;
import com.ecopoint.persistence.WasteBagRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.EvidenceValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Photos that support a shift, a bag or a quartering (RN-090; spec 003,
 * section 5.3). The files are kept in a local folder outside the repository and
 * the database keeps their data (decision T5 of the plan, risk R5).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class EvidenceService {

    /**
     * Stored file of an evidence.
     *
     * @param evidence data of the evidence
     * @param path     file on disk
     */
    public record EvidenceFile(Evidence evidence, Path path) {
    }

    private final EvidenceRepository evidences;
    private final ShiftRepository shifts;
    private final WasteBagRepository bags;
    private final QuarteringRepository quarterings;
    private final Path uploadDir;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param evidences   evidence repository
     * @param shifts      shift repository
     * @param bags        bag repository
     * @param quarterings quartering repository
     * @param uploadDir   folder where the photos are kept, from {@code ECOPOINT_UPLOAD_DIR}
     * @param clock       clock that dates the uploads
     */
    public EvidenceService(EvidenceRepository evidences, ShiftRepository shifts, WasteBagRepository bags,
                           QuarteringRepository quarterings, @Value("${ecopoint.upload-dir}") String uploadDir,
                           Clock clock) {
        this.evidences = evidences;
        this.shifts = shifts;
        this.bags = bags;
        this.quarterings = quarterings;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.clock = clock;
    }

    /**
     * Lists the photos of a record.
     *
     * @param target   kind of record
     * @param targetId record id
     * @return evidences, oldest first
     */
    public List<Evidence> list(EvidenceTarget target, Long targetId) {
        return evidences.findByTarget(target, targetId);
    }

    /**
     * Stores a photo of a record (RN-090: JPG or PNG up to 5 MB). The user needs
     * the permission to register that kind of record.
     *
     * @param actor        user that uploads it
     * @param target       kind of record
     * @param targetId     record id
     * @param originalName name of the uploaded file, used for its extension
     * @param mimeType     media type sent by the browser
     * @param sizeBytes    size of the file
     * @param content      content of the file
     * @param description  optional description
     * @return the saved evidence
     */
    public Evidence upload(User actor, EvidenceTarget target, Long targetId, String originalName, String mimeType,
                           long sizeBytes, InputStream content, String description) {
        AccessPolicy.require(actor, permissionFor(target));
        requireRecord(target, targetId);
        String key = target.name().toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID() + extensionOf(originalName);
        Evidence evidence = new Evidence(null, target, targetId, key, mimeType, sizeBytes, description,
                actor.getId(), LocalDateTime.now(clock));
        EvidenceValidator.validate(evidence);
        Path file = resolve(key);
        write(file, content);
        try {
            return evidences.save(evidence);
        } catch (RuntimeException failure) {
            deleteQuietly(file);
            throw failure;
        }
    }

    /**
     * Returns the data and the file of a photo.
     *
     * @param id evidence id
     * @return the evidence with its file
     * @throws ResourceNotFoundException if the evidence or its file does not exist
     */
    public EvidenceFile file(Long id) {
        Evidence evidence = evidences.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evidencia", id));
        Path file = resolve(evidence.getFilePath());
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Archivo de la evidencia", id);
        }
        return new EvidenceFile(evidence, file);
    }

    private static Permission permissionFor(EvidenceTarget target) {
        if (target == null) {
            return Permission.REGISTER_BAGS;
        }
        return switch (target) {
            case SHIFT -> Permission.REGISTER_POPULATION;
            case BAG -> Permission.REGISTER_BAGS;
            case QUARTERING -> Permission.REGISTER_QUARTERING;
        };
    }

    private void requireRecord(EvidenceTarget target, Long targetId) {
        if (target == null || targetId == null) {
            return;
        }
        boolean exists = switch (target) {
            case SHIFT -> shifts.findById(targetId).isPresent();
            case BAG -> bags.findById(targetId).isPresent();
            case QUARTERING -> quarterings.findById(targetId).isPresent();
        };
        if (!exists) {
            throw new ResourceNotFoundException(target.getDisplayName(), targetId);
        }
    }

    private Path resolve(String key) {
        Path file = uploadDir.resolve(key).normalize();
        if (!file.startsWith(uploadDir)) {
            throw new ResourceNotFoundException("Archivo de la evidencia", key);
        }
        return file;
    }

    private static void write(Path file, InputStream content) {
        try (InputStream input = content) {
            Files.createDirectories(file.getParent());
            Files.copy(input, file);
        } catch (IOException failure) {
            deleteQuietly(file);
            throw new PersistenceException("guardar la foto de evidencia", failure);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // The file could not be removed; it is only an orphan photo on disk.
        }
    }

    private static String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        return dot < 0 ? "" : originalName.substring(dot).toLowerCase(Locale.ROOT);
    }
}
