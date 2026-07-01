from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field


class EventEnvelope(BaseModel):
    event_id: str = Field(alias="eventId")
    event_type: str = Field(alias="eventType")
    aggregate_id: str = Field(alias="aggregateId")
    aggregate_type: str = Field(alias="aggregateType")
    occurred_at: datetime = Field(alias="occurredAt")
    version: int
    payload: dict[str, Any]


class CampaignCreatedPayload(BaseModel):
    campaign_id: str = Field(alias="campaignId")
    name: str
    description: str
    location: str
    priority: str
    status: str
    created_by: str = Field(alias="createdBy")


class DeadLetterEvent(BaseModel):
    original_event: str = Field(alias="originalEvent")
    error_type: str = Field(alias="errorType")
    error_message: str = Field(alias="errorMessage")
    failed_at: datetime = Field(alias="failedAt")
    service: str
    consumer_group: str = Field(alias="consumerGroup")
