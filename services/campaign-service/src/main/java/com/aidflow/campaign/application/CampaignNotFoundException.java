package com.aidflow.campaign.application;

import java.util.UUID;

public class CampaignNotFoundException extends RuntimeException {
    public CampaignNotFoundException(UUID id) {
        super("Campaign not found: " + id);
    }
}
