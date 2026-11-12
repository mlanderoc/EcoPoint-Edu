package com.ecopoint.controller;

import com.ecopoint.dto.DensityDto;
import com.ecopoint.dto.QuarteringRequest;
import com.ecopoint.dto.QuarteringResponse;
import com.ecopoint.dto.ReasonRequest;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.DensityService;
import com.ecopoint.service.QuarteringService;
import com.ecopoint.service.QuarteringService.ResultInput;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Quartering and density of a shift (spec 003, section 5.3).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/shifts/{shiftId}")
public class QualityController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "pesoSubmuestra", "sampleWeight", "resultados", "results", "peso", "results", "motivo", "reason",
            "volumenRecipiente", "containerVolume", "pesoNeto", "netWeight");

    private final QuarteringService quarterings;
    private final DensityService densities;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param quarterings quartering service
     * @param densities   density service
     * @param currentUser user of the session
     */
    public QualityController(QuarteringService quarterings, DensityService densities, CurrentUser currentUser) {
        this.quarterings = quarterings;
        this.densities = densities;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Shows the quartering of the shift.
     *
     * @param shiftId shift id
     * @return the quartering with its quality control and composition
     */
    @GetMapping("/quartering")
    public QuarteringResponse quartering(@PathVariable Long shiftId) {
        return QuarteringResponse.from(quarterings.get(shiftId));
    }

    /**
     * Registers the quartering of the shift (RN-050 to RN-056).
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @param request   subsample and subcategory weights
     * @return the quartering with its quality status
     */
    @PostMapping("/quartering")
    @ResponseStatus(HttpStatus.CREATED)
    public QuarteringResponse registerQuartering(@AuthenticationPrincipal EcoPointUserDetails principal,
                                                 @PathVariable Long shiftId, @RequestBody QuarteringRequest request) {
        List<ResultInput> inputs = request.results() == null ? List.of() : request.results().stream()
                .map(result -> new ResultInput(result.categoryId(), result.weight()))
                .toList();
        return QuarteringResponse.from(quarterings.register(currentUser.of(principal), shiftId,
                request.sampleWeight(), inputs));
    }

    /**
     * Annuls a non-conforming quartering with a reason (RN-052).
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @param request   reason
     * @return the annulled quartering
     */
    @PostMapping("/quartering/annul")
    public QuarteringResponse annul(@AuthenticationPrincipal EcoPointUserDetails principal,
                                    @PathVariable Long shiftId, @RequestBody ReasonRequest request) {
        return QuarteringResponse.from(quarterings.annul(currentUser.of(principal), shiftId, request.reason()));
    }

    /**
     * Shows the density measurement of the shift.
     *
     * @param shiftId shift id
     * @return the measurement
     */
    @GetMapping("/density")
    public DensityDto density(@PathVariable Long shiftId) {
        return DensityDto.from(densities.get(shiftId));
    }

    /**
     * Registers the density measurement of the shift (RN-057).
     *
     * @param principal user of the session
     * @param shiftId   shift id
     * @param request   container volume and net weight
     * @return the measurement with its density
     */
    @PostMapping("/density")
    @ResponseStatus(HttpStatus.CREATED)
    public DensityDto registerDensity(@AuthenticationPrincipal EcoPointUserDetails principal,
                                      @PathVariable Long shiftId, @RequestBody DensityDto request) {
        return DensityDto.from(densities.register(currentUser.of(principal), shiftId, request.containerVolume(),
                request.netWeight()));
    }
}
