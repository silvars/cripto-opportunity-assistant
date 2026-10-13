package com.btcassistant.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Somente leitura e inserção — nenhum método de update é declarado (SDD §21). */
public interface MarketEventRepository extends JpaRepository<MarketEventEntity, UUID> {

    List<MarketEventEntity> findTop50BySymbolOrderByEventTimeDesc(String symbol);
}
