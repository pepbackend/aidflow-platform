import logging
from dataclasses import dataclass

from pydantic import ValidationError

from app.models import CampaignCreatedPayload, EventEnvelope

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class ProcessingResult:
    handler: str
    processing_result: str
    notification_sent: bool = False


class EventHandler:
    def handle(self, event: EventEnvelope) -> ProcessingResult:
        if event.event_type == "CampaignCreated":
            return self._handle_campaign_created(event)

        logger.info(
            "Ignored event with unsupported type",
            extra={
                "eventId": event.event_id,
                "eventType": event.event_type,
                "aggregateType": event.aggregate_type,
                "aggregateId": event.aggregate_id,
            },
        )
        return ProcessingResult(
            handler="unsupported_event",
            processing_result="ignored",
        )

    def _handle_campaign_created(self, event: EventEnvelope) -> ProcessingResult:
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
        return ProcessingResult(
            handler="CampaignCreatedNotificationHandler",
            processing_result="notification_sent",
            notification_sent=True,
        )
