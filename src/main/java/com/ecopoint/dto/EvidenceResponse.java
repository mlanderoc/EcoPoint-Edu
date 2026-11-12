package com.ecopoint.dto;

import com.ecopoint.model.Evidence;
import com.ecopoint.model.EvidenceTarget;

import java.time.LocalDateTime;

/**
 * Data of a photo; the file itself is downloaded from {@code url} (RN-090).
 *
 * @param id          evidence id
 * @param targetType  SHIFT, BAG or QUARTERING
 * @param targetId    id of the record
 * @param mimeType    image/jpeg or image/png
 * @param sizeBytes   size of the file
 * @param description optional description
 * @param uploadedBy  user that uploaded it
 * @param uploadedAt  when it was uploaded
 * @param url         route to download the file
 * @author craulgomez
 * @version 1.0
 */
public record EvidenceResponse(Long id, EvidenceTarget targetType, Long targetId, String mimeType, long sizeBytes,
                               String description, Long uploadedBy, LocalDateTime uploadedAt, String url) {

    /**
     * Copies an evidence of the domain.
     *
     * @param evidence domain evidence
     * @return the response
     */
    public static EvidenceResponse from(Evidence evidence) {
        return new EvidenceResponse(evidence.getId(), evidence.getTarget(), evidence.getTargetId(),
                evidence.getMimeType(), evidence.getSizeBytes(), evidence.getDescription(), evidence.getUploadedBy(),
                evidence.getUploadedAt(), "/api/v1/evidences/" + evidence.getId() + "/file");
    }
}
