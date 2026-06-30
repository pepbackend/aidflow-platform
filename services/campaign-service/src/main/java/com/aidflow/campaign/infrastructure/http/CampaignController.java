package com.aidflow.campaign.infrastructure.http;

import com.aidflow.campaign.application.AuthenticatedUser;
import com.aidflow.campaign.application.CreateCampaignCommand;
import com.aidflow.campaign.application.CreateCampaignUseCase;
import com.aidflow.campaign.application.GetCampaignByIdUseCase;
import com.aidflow.campaign.application.GetCampaignsUseCase;
import com.aidflow.campaign.infrastructure.http.dto.CampaignResponse;
import com.aidflow.campaign.infrastructure.http.dto.CreateCampaignRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CampaignController {
    private final CreateCampaignUseCase createCampaignUseCase;
    private final GetCampaignsUseCase getCampaignsUseCase;
    private final GetCampaignByIdUseCase getCampaignByIdUseCase;

    public CampaignController(
            CreateCampaignUseCase createCampaignUseCase,
            GetCampaignsUseCase getCampaignsUseCase,
            GetCampaignByIdUseCase getCampaignByIdUseCase
    ) {
        this.createCampaignUseCase = createCampaignUseCase;
        this.getCampaignsUseCase = getCampaignsUseCase;
        this.getCampaignByIdUseCase = getCampaignByIdUseCase;
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse create(
            @Valid @RequestBody CreateCampaignRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Roles") String userRoles
    ) {
        return CampaignResponse.from(createCampaignUseCase.execute(new CreateCampaignCommand(
                request.name(),
                request.description(),
                request.location(),
                request.priority(),
                new AuthenticatedUser(userId, userEmail, parseRoles(userRoles))
        )));
    }

    @GetMapping("/campaigns")
    public List<CampaignResponse> findAll() {
        return getCampaignsUseCase.execute().stream()
                .map(CampaignResponse::from)
                .toList();
    }

    @GetMapping("/campaigns/{id}")
    public CampaignResponse findById(@PathVariable UUID id) {
        return CampaignResponse.from(getCampaignByIdUseCase.execute(id));
    }

    private Set<String> parseRoles(String roles) {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .collect(Collectors.toSet());
    }
}
