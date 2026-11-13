package com.ecopoint.service;

import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.VerifiableRecord;

/**
 * A verifiable record together with its kind and its shift, as the verification
 * screens show it (spec 005, R-420, R-421 and R-430).
 *
 * @param kind   kind of record
 * @param record the record with its values and verification
 * @param shift  shift the record belongs to
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record RecordView(RecordKind kind, VerifiableRecord record, SamplingShift shift) {
}
