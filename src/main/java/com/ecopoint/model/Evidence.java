package com.ecopoint.model;

import java.time.LocalDateTime;

/**
 * Supporting file attached to a field record: a photo of the scale, of the
 * bag or of the paper form (RN-090). The file itself is stored by the
 * infrastructure; the domain keeps its metadata.
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
public class Evidence {

    private Long id;
    private final EvidenceTarget target;
    private final Long targetId;
    private final String filePath;
    private final String mimeType;
    private final long sizeBytes;
    private String description;
    private final Long uploadedBy;
    private final LocalDateTime uploadedAt;

    /**
     * Creates the metadata of an evidence file.
     *
     * @param id          identifier, or null if not saved yet
     * @param target      kind of record it belongs to
     * @param targetId    identifier of that record
     * @param filePath    path or key of the stored file
     * @param mimeType    media type, e.g. image/jpeg
     * @param sizeBytes   file size in bytes
     * @param description optional description in Spanish
     * @param uploadedBy  user who uploaded the file
     * @param uploadedAt  date and time of the upload
     */
    public Evidence(Long id, EvidenceTarget target, Long targetId, String filePath, String mimeType,
                    long sizeBytes, String description, Long uploadedBy, LocalDateTime uploadedAt) {
        this.id = id;
        this.target = target;
        this.targetId = targetId;
        this.filePath = filePath;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.description = description;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EvidenceTarget getTarget() {
        return target;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
