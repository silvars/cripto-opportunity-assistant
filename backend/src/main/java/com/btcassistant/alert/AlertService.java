package com.btcassistant.alert;

import com.btcassistant.event.DetectedEvent;
import com.btcassistant.historical.HistoricalContext;
import com.btcassistant.opportunity.Classification;
import com.btcassistant.opportunity.MarketOpportunityEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Cooldown + deduplicação (SDD §41/§43). Nunca duplica alertas durante um movimento prolongado:
 * se já existe um alerta ACTIVE (ou criado há menos que {@code dedup-window}) para o mesmo
 * símbolo+direção, a ocorrência é suprimida (nenhuma linha nova) — a oportunidade em si já foi
 * registrada de forma imutável em {@code opportunities}, nada é perdido para auditoria.
 *
 * <p>Expiração é avaliada de forma preguiçosa, a cada novo evento (sem scheduler em background) —
 * suficiente para esta fase (ver docs/phases/FASE-3-opportunity-risk.md §11).</p>
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository repository;
    private final AlertProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    public AlertService(AlertRepository repository, AlertProperties properties, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    public Optional<AlertEntity> process(MarketOpportunityEntity opportunity, DetectedEvent event,
                                         Optional<HistoricalContext> historicalContext) {
        if (opportunity.getClassification() == Classification.NONE) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        Optional<AlertEntity> mostRecent = repository.findTopBySymbolAndDirectionOrderByCreatedAtDesc(
                opportunity.getSymbol(), opportunity.getDirection());

        if (mostRecent.isPresent() && shouldSuppress(mostRecent.get(), now)) {
            log.info("alert_deduplicated symbol={} direction={} existingAlertId={}",
                    opportunity.getSymbol(), opportunity.getDirection(), mostRecent.get().getAlertId());
            return Optional.empty();
        }

        AlertEntity created = AlertEntity.from(opportunity, event, now.plus(properties.getCooldown()));
        created.transitionTo(AlertStatus.ANALYZING);
        created.transitionTo(AlertStatus.CREATED);
        created.transitionTo(AlertStatus.ACTIVE);
        repository.save(created);
        log.info("alert_created symbol={} direction={} alertId={} classification={} score={}",
                created.getSymbol(), created.getDirection(), created.getAlertId(), created.getClassification(), created.getScore());
        eventPublisher.publishEvent(new AlertCreatedEvent(created, event, historicalContext));
        return Optional.of(created);
    }

    private boolean shouldSuppress(AlertEntity existing, Instant now) {
        boolean pastExpiry = existing.getExpiresAt() != null && !existing.getExpiresAt().isAfter(now);
        if (existing.getStatus() == AlertStatus.ACTIVE) {
            if (pastExpiry) {
                existing.transitionTo(AlertStatus.EXPIRED);
                repository.save(existing);
                log.info("alert_expired symbol={} direction={} alertId={}",
                        existing.getSymbol(), existing.getDirection(), existing.getAlertId());
            } else {
                return true;
            }
        }

        Duration sinceCreated = Duration.between(existing.getCreatedAt(), now);
        return sinceCreated.compareTo(properties.getDedupWindow()) < 0;
    }
}
