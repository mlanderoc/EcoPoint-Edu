package com.ecopoint.controller;

import com.ecopoint.dto.BagRequest;
import com.ecopoint.dto.BagResponse;
import com.ecopoint.dto.PageResponse;
import com.ecopoint.dto.PopulationDto;
import com.ecopoint.dto.ShiftResponse;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.WasteBag;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.ShiftService;
import com.ecopoint.service.WasteBagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A shift and what is recorded in it: close and reopen, population, bags and
 * "no generation" marks (spec 003, section 5.2).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/shifts/{shiftId}")
public class ShiftController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.ofEntries(
            Map.entry("estudiantes", "students"), Map.entry("docentes", "teachers"), Map.entry("personal", "staff"),
            Map.entry("pesoBruto", "grossWeight"), Map.entry("tara", "tare"), Map.entry("volumen", "volume"),
            Map.entry("fuente", "sourceId"));

    private final ShiftService shifts;
    private final WasteBagService bags;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param shifts      shift service
     * @param bags        bag service
     * @param currentUser user of the session
     */
    public ShiftController(ShiftService shifts, WasteBagService bags, CurrentUser currentUser) {
        this.shifts = shifts;
        this.bags = bags;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Shows the shift with the state of each participating source (R-345).
     *
     * @param shiftId shift id
     * @return the shift
     */
    @GetMapping
    public ShiftResponse get(@PathVariable Long shiftId) {
        return toResponse(shifts.get(shiftId));
    }

    /**
     * Closes the shift (RN-024, R-346).
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @return the closed shift
     */
    @PostMapping("/close")
    public ShiftResponse close(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long shiftId) {
        return toResponse(shifts.close(currentUser.of(principal), shiftId));
    }

    /**
     * Reopens the shift (RN-025).
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @return the reopened shift
     */
    @PostMapping("/reopen")
    public ShiftResponse reopen(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long shiftId) {
        return toResponse(shifts.reopen(currentUser.of(principal), shiftId));
    }

    /**
     * Shows the population of the shift.
     *
     * @param shiftId shift id
     * @return the population, or 204 if it was not registered yet
     */
    @GetMapping("/population")
    public ResponseEntity<PopulationDto> population(@PathVariable Long shiftId) {
        PopulationDto population = PopulationDto.from(shifts.get(shiftId).getPopulation());
        return population == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(population);
    }

    /**
     * Registers the population of the shift (RN-022, R-342).
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @param request   students, teachers and staff
     * @return the registered population
     */
    @PutMapping("/population")
    public PopulationDto registerPopulation(@AuthenticationPrincipal EcoPointUserDetails principal,
                                            @PathVariable Long shiftId, @RequestBody PopulationDto request) {
        SamplingShift shift = shifts.registerPopulation(currentUser.of(principal), shiftId, request.students(),
                request.teachers(), request.staff());
        return PopulationDto.from(shift.getPopulation());
    }

    /**
     * Lists the bags of the shift, one page at a time (R-103).
     *
     * @param shiftId shift id
     * @param page    page, starting at 0
     * @param size    page size
     * @return one page of bags
     */
    @GetMapping("/bags")
    public PageResponse<BagResponse> bags(@PathVariable Long shiftId, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(bags.list(shiftId), page, size, bag -> BagResponse.from(shiftId, bag));
    }

    /**
     * Registers a bag (R-343, R-344).
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @param request   source and weights
     * @return the bag with its label, net weight, density and atypical mark
     */
    @PostMapping("/bags")
    @ResponseStatus(HttpStatus.CREATED)
    public BagResponse registerBag(@AuthenticationPrincipal EcoPointUserDetails principal,
                                   @PathVariable Long shiftId, @RequestBody BagRequest request) {
        WasteBag bag = bags.register(currentUser.of(principal), shiftId, request.sourceId(), request.grossWeight(),
                request.tare(), request.volume());
        return BagResponse.from(shiftId, bag);
    }

    /**
     * Marks a source as having generated no waste in the shift (RN-024).
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @param sourceId  source id
     * @return the shift
     */
    @PostMapping("/sources/{sourceId}/no-generation")
    public ShiftResponse markNoGeneration(@AuthenticationPrincipal EcoPointUserDetails principal,
                                          @PathVariable Long shiftId, @PathVariable Long sourceId) {
        return toResponse(shifts.setNoGeneration(currentUser.of(principal), shiftId, sourceId, true));
    }

    /**
     * Removes the "no generation" mark of a source.
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @param sourceId  source id
     * @return the shift
     */
    @DeleteMapping("/sources/{sourceId}/no-generation")
    public ShiftResponse unmarkNoGeneration(@AuthenticationPrincipal EcoPointUserDetails principal,
                                            @PathVariable Long shiftId, @PathVariable Long sourceId) {
        return toResponse(shifts.setNoGeneration(currentUser.of(principal), shiftId, sourceId, false));
    }

    private ShiftResponse toResponse(SamplingShift shift) {
        return ShiftResponse.from(shift, shifts.progress(shift));
    }
}
