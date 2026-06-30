package com.aidflow.campaign.application;

public class ForbiddenCampaignOperationException extends RuntimeException {
    public ForbiddenCampaignOperationException() {
        super("User is not allowed to perform this campaign operation");
    }
}
