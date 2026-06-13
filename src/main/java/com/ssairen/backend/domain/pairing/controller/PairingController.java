package com.ssairen.backend.domain.pairing.controller;

import com.ssairen.backend.domain.pairing.dto.GuardianResponse;
import com.ssairen.backend.domain.pairing.service.PairingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pairings")
@Tag(
        name = "보호자 페어링",
        description = "피해자와 보호자의 페어링 정보를 조회하는 API"
)
public class PairingController {

    private final PairingService pairingService;

    public PairingController(PairingService pairingService) {
        this.pairingService = pairingService;
    }

    @GetMapping("/{victimId}/guardians")
    @Operation(
            summary = "피해자의 보호자 목록 조회",
            description = "피해자와 페어링된 보호자 목록을 조회합니다."
    )
    public List<GuardianResponse> getGuardians(
            @Parameter(description = "조회할 피해자 사용자 ID", example = "1001")
            @PathVariable Long victimId
    ) {
        return pairingService.getGuardians(victimId);
    }
}