import logging

from pydantic import ValidationError

from app.models import CampaignCreatedPayload, EventEnvelope

logger = logging.getLogger(__name__)


class EventHandler:
    def handle(self, event: EventEnvelope) -> None:
        if event.event_type == "CampaignCreated":
            self._handle_campaign_created(event)
            return

        logger.info(
            "Ignored event with unsupported type",
            extra={
                "eventId": event.event_id,
                "eventType": event.event_type,
                "aggregateType": event.aggregate_type,
                "aggregateId": event.aggregate_id,
            },
        )

    def _handle_campaign_created(self, event: EventEnvelope) -> None:
        try:
            payload = CampaignCreatedPayload.model_validate(event.payload)
        except ValidationError:
            logger.exception(
                "Invalid CampaignCreated payload",
                extra={
                    "eventId": event.event_id,
                    "eventType": event.event_type,
                    "aggregateType": event.aggregate_type,
                    "aggregateId": event.aggregate_id,
                },
            )
            raise

        logger.info(
            "Simulated notification sent",
            extra={
                "eventId": event.event_id,
                "eventType": event.event_type,
                "aggregateType": event.aggregate_type,
                "aggregateId": event.aggregate_id,
                "campaignId": payload.campaign_id,
                "campaignName": payload.name,
                "location": payload.location,
                "priority": payload.priority,
                "createdBy": payload.created_by,
            },
        )
