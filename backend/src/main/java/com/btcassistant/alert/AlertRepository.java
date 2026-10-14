package com.btcassistant.alert;

import com.btcassistant.opportunity.Direction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {

    Optional<AlertEntity> findTopBySymbolAndDirectionOrderByCreatedAtDesc(String symbol, Direction direction);

    List<AlertEntity> findTop50BySymbolOrderByCreatedAtDesc(String symbol);
}
