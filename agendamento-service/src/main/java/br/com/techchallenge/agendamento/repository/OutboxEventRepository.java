package br.com.techchallenge.agendamento.repository;

import br.com.techchallenge.agendamento.entity.OutboxEvent;
import br.com.techchallenge.agendamento.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from OutboxEvent o
            where o.status in :statuses
              and o.nextAttemptAt <= :nextAttemptAt
              and (o.lockedAt is null or o.lockedAt <= :staleLockThreshold)
            order by o.createdAt
            """)
    List<OutboxEvent> findClaimableForUpdate(
            @Param("statuses") Collection<OutboxStatus> statuses,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("staleLockThreshold") Instant staleLockThreshold,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent o
               set o.status = br.com.techchallenge.agendamento.entity.OutboxStatus.PUBLISHED,
                   o.publishedAt = :publishedAt,
                   o.lastError = null,
                   o.lockedAt = null,
                   o.lockedBy = null
             where o.id = :id
               and o.lockedBy = :lockedBy
               and o.claimToken = :claimToken
            """)
    int markPublished(
            @Param("id") Long id,
            @Param("lockedBy") String lockedBy,
            @Param("claimToken") Long claimToken,
            @Param("publishedAt") Instant publishedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboxEvent o
               set o.status = br.com.techchallenge.agendamento.entity.OutboxStatus.FAILED,
                   o.attempts = o.attempts + 1,
                   o.nextAttemptAt = :nextAttemptAt,
                   o.lastError = :lastError,
                   o.lockedAt = null,
                   o.lockedBy = null
             where o.id = :id
               and o.lockedBy = :lockedBy
               and o.claimToken = :claimToken
            """)
    int markFailed(
            @Param("id") Long id,
            @Param("lockedBy") String lockedBy,
            @Param("claimToken") Long claimToken,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError
    );
}
