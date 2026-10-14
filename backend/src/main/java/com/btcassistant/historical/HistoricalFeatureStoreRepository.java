package com.btcassistant.historical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistoricalFeatureStoreRepository extends JpaRepository<HistoricalFeatureStoreEntity, Long> {

    Optional<HistoricalFeatureStoreEntity> findByEventId(UUID eventId);

    /**
     * Candidatos elegíveis (SDD §31, regra estrita): {@code eventTime < asOf}, nunca {@code <=}.
     * Não há parâmetro de exclusão por {@code eventId} — como {@code asOf} é sempre o
     * {@code eventTime} do evento avaliado, nenhuma linha com o mesmo {@code eventTime} (incluindo
     * a do próprio evento) jamais passa no filtro estrito (ver {@link HistoricalAnalyzerImpl}).
     * Ordenados por {@code eventTime} apenas para determinismo — a ordenação por similaridade
     * acontece em memória no {@link HistoricalAnalyzerImpl}.
     */
    @Query("select h from HistoricalFeatureStoreEntity h "
            + "where h.symbol = :symbol and h.eventType = :eventType and h.eventTime < :asOf "
            + "order by h.eventTime asc")
    List<HistoricalFeatureStoreEntity> findCandidates(@Param("symbol") String symbol,
                                                       @Param("eventType") String eventType,
                                                       @Param("asOf") Instant asOf);
}
