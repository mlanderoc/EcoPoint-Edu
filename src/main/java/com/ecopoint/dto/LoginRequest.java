package com.ecopoint.dto;

/**
 * Data sent to log in (spec 003, R-120).
 *
 * @param email    email of the user
 * @param password password typed by the user
 * @author craulgomez
 * @version 1.0
 */
public record LoginRequest(String email, String password) {
}
