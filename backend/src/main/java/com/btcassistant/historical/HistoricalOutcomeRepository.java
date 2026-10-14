package com.btcassistant.historical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistoricalOutcomeRepository extends JpaRepository<HistoricalOutcomeEntity, Long> {

    Optional<HistoricalOutcomeEntity> findByEventIdAndHorizon(UUID eventId, String horizon);

    /**
     * Outcomes elegíveis para agregação (SDD §31): só os já completamente conhecidos em
     * {@code asOf} — {@code evaluationEnd <= asOf}, nunca {@code evaluationEnd > asOf}.
     */
    @Query("select o from HistoricalOutcomeEntity o "
            + "where o.eventId in :eventIds and o.horizon = :horizon and o.evaluationEnd <= :asOf")
    List<HistoricalOutcomeEntity> findKnownAsOf(@Param("eventIds") Collection<UUID> eventIds,
                                                 @Param("horizon") String horizon,
                                                 @Param("asOf") Instant asOf);
}
