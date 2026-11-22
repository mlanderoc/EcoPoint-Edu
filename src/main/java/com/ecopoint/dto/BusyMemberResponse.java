package com.ecopoint.dto;

/**
 * Student that cannot join a crew because of another open study (RN-090).
 *
 * @param userId  id of the student
 * @param studyId id of his open study
 * @author craulgomez
 * @version 1.0
 */
public record BusyMemberResponse(Long userId, Long studyId) {
}
