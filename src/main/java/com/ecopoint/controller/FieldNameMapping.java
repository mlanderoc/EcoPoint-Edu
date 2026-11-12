package com.ecopoint.controller;

import java.util.Map;

/**
 * Implemented by controllers whose validators name fields in Spanish, to translate
 * them to the JSON field of the request in {@code fieldErrors} (spec 003, CA-205).
 *
 * @author craulgomez
 * @version 1.0
 */
public interface FieldNameMapping {

    /**
     * Returns the translation from the field name used by the validators to the
     * JSON field name, for example {@code "tara" -> "tare"}.
     *
     * @return dictionary of field names
     */
    Map<String, String> fieldNames();
}
