package com.example.decision.repository;

import com.example.decision.model.DecisionEntity;
import com.example.decision.model.DecisionOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DecisionRepository extends JpaRepository<DecisionEntity, Long> {
    
    Optional<DecisionEntity> findByTransactionId(String transactionId);
    
    List<DecisionEntity> findByUserIdOrderByTimestampDesc(String userId);
    
    List<DecisionEntity> findByOutcome(DecisionOutcome outcome);
    
    @Query("SELECT d FROM DecisionEntity d WHERE d.userId = :userId AND d.timestamp >= :since")
    List<DecisionEntity> findRecentByUserId(@Param("userId") String userId, @Param("since") Instant since);
    
    @Query("SELECT COUNT(d) FROM DecisionEntity d WHERE d.userId = :userId AND d.timestamp >= :since")
    long countRecentByUserId(@Param("userId") String userId, @Param("since") Instant since);
    
    @Query("SELECT AVG(d.latencyMs) FROM DecisionEntity d WHERE d.timestamp >= :since")
    Double getAverageLatencySince(@Param("since") Instant since);
}
