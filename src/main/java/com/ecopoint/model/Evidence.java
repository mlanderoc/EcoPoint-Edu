package com.ecopoint.model;

import java.time.LocalDateTime;

/**
 * Represents a photographic evidence item attached to an {@link InspectionRecord}.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class Evidence {

    private int idEvidence;
    private int idRecord;
    private String filePath;
    private String description;
    private LocalDateTime uploadedAt;

    /**
     * Default constructor.
     */
    public Evidence() {
    }

    /**
     * Constructs an Evidence item with all attributes.
     *
     * @param idEvidence  unique identifier of the evidence
     * @param idRecord    identifier of the parent inspection record
     * @param filePath    relative path or URI of the image file
     * @param description description of what the photograph shows
     * @param uploadedAt  timestamp when the evidence was uploaded
     */
    public Evidence(int idEvidence, int idRecord, String filePath, String description, LocalDateTime uploadedAt) {
        this.idEvidence = idEvidence;
        this.idRecord = idRecord;
        this.filePath = filePath;
        this.description = description;
        this.uploadedAt = uploadedAt;
    }

    public int getIdEvidence() {
        return idEvidence;
    }

    public void setIdEvidence(int idEvidence) {
        this.idEvidence = idEvidence;
    }

    public int getIdRecord() {
        return idRecord;
    }

    public void setIdRecord(int idRecord) {
        this.idRecord = idRecord;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
