package com.ecopoint.controller;

import com.ecopoint.dto.BagRequest;
import com.ecopoint.dto.BagResponse;
import com.ecopoint.model.WasteBag;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.WasteBagService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * One bag: see it and correct it while it is pending verification (spec 003,
 * section 5.2; R-348).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/bags/{bagId}")
public class BagController implements FieldNameMapping {

    private static final Map<String, String> FIELDS = Map.of(
            "pesoBruto", "grossWeight", "tara", "tare", "volumen", "volume");

    private final WasteBagService bags;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param bags        bag service
     * @param currentUser user of the session
     */
    public BagController(WasteBagService bags, CurrentUser currentUser) {
        this.bags = bags;
        this.currentUser = currentUser;
    }

    @Override
    public Map<String, String> fieldNames() {
        return FIELDS;
    }

    /**
     * Shows a bag.
     *
     * @param bagId bag id
     * @return the bag
     */
    @GetMapping
    public BagResponse get(@PathVariable Long bagId) {
        return BagResponse.from(bags.shiftOf(bagId), bags.get(bagId));
    }

    /**
     * Corrects the weights of a bag still pending verification (R-348).
     *
     * @param principal user of the session
     * @param bagId     bag id
     * @param request   new weights
     * @return the saved bag
     */
    @PutMapping
    public BagResponse correct(@AuthenticationPrincipal EcoPointUserDetails principal, @PathVariable Long bagId,
                               @RequestBody BagRequest request) {
        WasteBag bag = bags.correctPending(currentUser.of(principal), bagId, request.grossWeight(), request.tare(),
                request.volume());
        return BagResponse.from(bags.shiftOf(bagId), bag);
    }
}
