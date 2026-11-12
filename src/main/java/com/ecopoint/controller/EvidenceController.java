package com.ecopoint.controller;

import com.ecopoint.dto.EvidenceResponse;
import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.EvidenceTarget;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.EvidenceService;
import com.ecopoint.service.EvidenceService.EvidenceFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Photos that support a shift, a bag or a quartering (spec 003, section 5.3; RN-090).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/evidences")
public class EvidenceController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "formato", "file", "tamano", "file", "archivo", "file", "registro", "targetId",
            "descripcion", "description");

    private final EvidenceService evidences;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param evidences   evidence service
     * @param currentUser user of the session
     */
    public EvidenceController(EvidenceService evidences, CurrentUser currentUser) {
        this.evidences = evidences;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Uploads a photo of a record (multipart form with {@code file}).
     *
     * @param principal   user of the session
     * @param targetType  SHIFT, BAG or QUARTERING
     * @param targetId    record id
     * @param file        JPG or PNG up to 5 MB
     * @param description optional description
     * @return the saved evidence
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenceResponse upload(@AuthenticationPrincipal EcoPointUserDetails principal,
                                   @RequestParam EvidenceTarget targetType, @RequestParam Long targetId,
                                   @RequestParam MultipartFile file,
                                   @RequestParam(required = false) String description) {
        try {
            return EvidenceResponse.from(evidences.upload(currentUser.of(principal), targetType, targetId,
                    file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream(),
                    description));
        } catch (IOException failure) {
            throw new PersistenceException("leer la foto recibida", failure);
        }
    }

    /**
     * Lists the photos of a record.
     *
     * @param targetType SHIFT, BAG or QUARTERING
     * @param targetId   record id
     * @return evidences
     */
    @GetMapping
    public List<EvidenceResponse> list(@RequestParam EvidenceTarget targetType, @RequestParam Long targetId) {
        return evidences.list(targetType, targetId).stream().map(EvidenceResponse::from).toList();
    }

    /**
     * Downloads the file of a photo.
     *
     * @param id evidence id
     * @return the image
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        EvidenceFile stored = evidences.file(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.evidence().getMimeType()))
                .body(new FileSystemResource(stored.path()));
    }
}
