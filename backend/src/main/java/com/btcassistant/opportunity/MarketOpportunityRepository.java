package com.btcassistant.opportunity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Somente leitura e inserção — opportunities é imutável, uma linha por avaliação (SDD Fase 3 §3). */
public interface MarketOpportunityRepository extends JpaRepository<MarketOpportunityEntity, UUID> {

    List<MarketOpportunityEntity> findTop50BySymbolOrderByCreatedAtDesc(String symbol);
}
