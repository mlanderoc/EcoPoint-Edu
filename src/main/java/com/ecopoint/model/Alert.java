package com.ecopoint.model;

import java.time.LocalDateTime;

/**
 * Represents an operational alert automatically generated when a container
 * reaches an overflow state ({@code DESBORDADO}) or exhibits improper waste separation.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
public class Alert {

    public static final String TYPE_OVERFLOW = "DESBORDAMIENTO";
    public static final String TYPE_IMPROPER_SEPARATION = "MALA_SEPARACION";

    private int idAlert;
    private int idContainerDetail;
    private String type;
    private boolean attended;
    private String description;
    private LocalDateTime generatedAt;

    /**
     * Default constructor.
     */
    public Alert() {
    }

    /**
     * Constructs an Alert with all attributes.
     *
     * @param idAlert           unique identifier of the alert
     * @param idContainerDetail identifier of the container detail that triggered the alert
     * @param type              alert category (DESBORDAMIENTO or MALA_SEPARACION)
     * @param attended          whether the alert has already been resolved
     * @param description       descriptive message in Spanish explaining the alert
     * @param generatedAt       timestamp when the alert was generated
     */
    public Alert(int idAlert, int idContainerDetail, String type, boolean attended,
                 String description, LocalDateTime generatedAt) {
        this.idAlert = idAlert;
        this.idContainerDetail = idContainerDetail;
        this.type = type;
        this.attended = attended;
        this.description = description;
        this.generatedAt = generatedAt;
    }

    /**
     * Marks this alert as attended/resolved (GRASP Information Expert).
     */
    public void markAsAttended() {
        this.attended = true;
    }

    public int getIdAlert() {
        return idAlert;
    }

    public void setIdAlert(int idAlert) {
        this.idAlert = idAlert;
    }

    public int getIdContainerDetail() {
        return idContainerDetail;
    }

    public void setIdContainerDetail(int idContainerDetail) {
        this.idContainerDetail = idContainerDetail;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAttended() {
        return attended;
    }

    public void setAttended(boolean attended) {
        this.attended = attended;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}

