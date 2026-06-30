package com.aidflow.campaign.application;

import com.aidflow.campaign.domain.model.Campaign;

public interface CampaignEventRecorder {
    void recordCampaignCreated(Campaign campaign);
}
