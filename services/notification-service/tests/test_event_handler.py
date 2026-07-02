import logging
from datetime import UTC, datetime

import pytest

from app.event_handler import EventHandler
from app.models import EventEnvelope


def test_campaign_created_event_logs_simulated_notification(caplog):
    event = EventEnvelope(
        eventId="event-1",
        eventType="CampaignCreated",
        aggregateId="campaign-1",
        aggregateType="Campaign",
        occurredAt=datetime.now(UTC),
        version=1,
        payload={
            "campaignId": "campaign-1",
            "name": "Flood response in Granollers",
            "description": "Volunteer coordination after heavy rain flooding",
            "location": "Granollers",
            "priority": "HIGH",
            "status": "ACTIVE",
            "createdBy": "user-1",
        },
    )

    with caplog.at_level(logging.INFO):
        EventHandler().handle(event)

    assert "Simulated notification sent" in caplog.text


def test_unknown_event_type_is_ignored(caplog):
    event = EventEnvelope(
        eventId="event-1",
        eventType="SomethingElse",
        aggregateId="campaign-1",
        aggregateType="Campaign",
        occurredAt=datetime.now(UTC),
        version=1,
        payload={},
    )

    with caplog.at_level(logging.INFO):
        EventHandler().handle(event)

    assert "Ignored event with unsupported type" in caplog.text


def test_invalid_campaign_created_payload_raises():
    event = EventEnvelope(
        eventId="event-1",
        eventType="CampaignCreated",
        aggregateId="campaign-1",
        aggregateType="Campaign",
        occurredAt=datetime.now(UTC),
        version=1,
        payload={"campaignId": "campaign-1"},
    )

    with pytest.raises(Exception):
        EventHandler().handle(event)
