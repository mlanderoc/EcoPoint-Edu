package com.ecopoint.dto;

import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.VerifiableRecord;
import com.ecopoint.model.WasteBag;
import com.ecopoint.service.RecordView;

import java.util.ArrayList;
import java.util.List;

/**
 * A verifiable record with its typed values, ready to compare with the paper
 * form and to correct (spec 005, R-420 to R-422, R-430).
 *
 * @param recordType   kind in the routes: population, bag, quartering or density
 * @param recordName   kind shown to the user, e.g. "Bolsa"
 * @param recordId     record id
 * @param shiftId      shift of the record
 * @param shiftCode    code of the shift, e.g. {@code D3-M}
 * @param title        short title, e.g. "Bolsa AU-D3-M"
 * @param fields       typed and calculated values; the editable ones are the fields the correction accepts
 * @param verification double-entry data
 * @author craulgomez
 * @version 1.0
 */
public record VerifiableRecordResponse(String recordType, String recordName, Long recordId, Long shiftId,
                                       String shiftCode, String title, List<RecordField> fields,
                                       VerificationDto verification) {

    /**
     * One value of the record.
     *
     * @param field    name of the field in a correction, e.g. {@code grossWeight} or {@code result:3}
     * @param label    name shown to the user
     * @param value    current value
     * @param editable whether it can be corrected (calculated values cannot)
     */
    public record RecordField(String field, String label, Object value, boolean editable) {
    }

    /**
     * Copies a record found by the verification service.
     *
     * @param view record with its kind and shift
     * @return the response
     */
    public static VerifiableRecordResponse from(RecordView view) {
        VerifiableRecord record = view.record();
        SamplingShift shift = view.shift();
        return new VerifiableRecordResponse(view.kind().getPath(), record.getRecordType(), record.getId(),
                shift.getId(), shift.getCode(), titleOf(record, shift), fieldsOf(record),
                VerificationDto.from(record.getVerification()));
    }

    private static String titleOf(VerifiableRecord record, SamplingShift shift) {
        if (record instanceof WasteBag bag) {
            return "Bolsa " + bag.getLabelText();
        }
        return record.getRecordType() + " " + shift.getCode();
    }

    private static List<RecordField> fieldsOf(VerifiableRecord record) {
        List<RecordField> fields = new ArrayList<>();
        if (record instanceof ShiftPopulation population) {
            fields.add(new RecordField("students", "Estudiantes", population.getStudents(), true));
            fields.add(new RecordField("teachers", "Docentes", population.getTeachers(), true));
            fields.add(new RecordField("staff", "Personal", population.getStaff(), true));
            fields.add(new RecordField("total", "Total", population.getTotal(), false));
        } else if (record instanceof WasteBag bag) {
            fields.add(new RecordField("source", "Fuente", bag.getSourceCode(), false));
            fields.add(new RecordField("grossWeight", "Peso bruto (kg)", bag.getGrossWeight(), true));
            fields.add(new RecordField("tare", "Tara (kg)", bag.getTare(), true));
            fields.add(new RecordField("volume", "Volumen (m³)", bag.getVolume(), true));
            fields.add(new RecordField("netWeight", "Peso neto (kg)", Decimals.show(bag.getNetWeight()), false));
            fields.add(new RecordField("density", "Densidad (kg/m³)", Decimals.show(bag.getDensity()), false));
            fields.add(new RecordField("atypical", "Valor atípico", bag.isAtypical() ? "Sí" : "No", false));
        } else if (record instanceof Quartering quartering) {
            fields.add(new RecordField("sampleWeight", "Submuestra (kg)", quartering.getSampleWeight(), true));
            quartering.getResults().forEach(result -> fields.add(new RecordField("result:" + result.getCategoryId(),
                    result.getCategoryName() + " (kg)", result.getWeight(), true)));
            fields.add(new RecordField("differencePercent", "Diferencia (%)",
                    Decimals.show(quartering.calculateDifferencePercent()), false));
            fields.add(new RecordField("qualityStatus", "Control del 2 %",
                    quartering.getQualityStatus() == null ? null : quartering.getQualityStatus().getDisplayName(), false));
        } else if (record instanceof DensityMeasurement density) {
            fields.add(new RecordField("containerVolume", "Volumen del recipiente (m³)", density.getContainerVolume(), true));
            fields.add(new RecordField("netWeight", "Peso neto (kg)", density.getNetWeight(), true));
            fields.add(new RecordField("density", "Densidad (kg/m³)", Decimals.show(density.getDensity()), false));
        }
        return fields;
    }
}
