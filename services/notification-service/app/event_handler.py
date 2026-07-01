import logging

from pydantic import ValidationError

from app.models import CampaignCreatedPayload, EventEnvelope

logger = logging.getLogger(__name__)


class EventHandler:
    def handle(self, event: EventEnvelope) -> None:
        if event.event_type == "CampaignCreated":
            self._handle_campaign_created(event)
            return

        logger.info("Ignored event with unsupported type: %s", event.event_type)

    def _handle_campaign_created(self, event: EventEnvelope) -> None:
        try:
            payload = CampaignCreatedPayload.model_validate(event.payload)
        except ValidationError:
            logger.exception("Invalid CampaignCreated payload")
            raise

        logger.info(
            "Simulated notification: campaign '%s' was created in %s with priority %s",
            payload.name,
            payload.location,
            payload.priority,
        )
