package com.lumind.api.statistics.repository;

import com.lumind.api.statistics.repository.projection.DailyCountAggregation;
import com.lumind.api.statistics.repository.projection.DailyMinutesAggregation;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class ProductivityStatisticsRepository {

    private final EntityManager entityManager;

    public ProductivityStatisticsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countTasksCreated(UUID userId, Instant from, Instant to) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(t) FROM Task t
                        WHERE t.user.id = :userId
                          AND t.createdAt >= :from
                          AND t.createdAt <= :to
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long countTasksCompleted(UUID userId, Instant from, Instant to) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(t) FROM Task t
                        WHERE t.user.id = :userId
                          AND t.completed = true
                          AND t.updatedAt >= :from
                          AND t.updatedAt <= :to
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long countTasksPendingCreatedInPeriod(UUID userId, Instant from, Instant to) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(t) FROM Task t
                        WHERE t.user.id = :userId
                          AND t.completed = false
                          AND t.createdAt >= :from
                          AND t.createdAt <= :to
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public List<DailyCountAggregation> findTasksCompletedByDay(UUID userId, Instant from, Instant to) {
        List<Object[]> rows = entityManager.createQuery(
                        """
                        SELECT CAST(t.updatedAt AS localdate), COUNT(t) FROM Task t
                        WHERE t.user.id = :userId
                          AND t.completed = true
                          AND t.updatedAt >= :from
                          AND t.updatedAt <= :to
                        GROUP BY CAST(t.updatedAt AS localdate)
                        ORDER BY CAST(t.updatedAt AS localdate) ASC
                        """,
                        Object[].class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        return rows.stream()
                .map(row -> new DailyCountAggregation(toLocalDate(row[0]), (Long) row[1]))
                .toList();
    }

    public long countPomodoroSessionsStarted(UUID userId, Instant from, Instant to) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(p) FROM PomodoroSession p
                        WHERE p.user.id = :userId
                          AND p.startedAt >= :from
                          AND p.startedAt <= :to
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long countPomodoroSessionsCompleted(UUID userId, Instant from, Instant to) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(p) FROM PomodoroSession p
                        WHERE p.user.id = :userId
                          AND p.completed = true
                          AND p.finishedAt >= :from
                          AND p.finishedAt <= :to
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long sumPomodoroFocusMinutes(UUID userId, Instant from, Instant to) {
        Long total = entityManager.createQuery(
                        """
                        SELECT COALESCE(SUM(p.completedMinutes), 0L) FROM PomodoroSession p
                        WHERE p.user.id = :userId
                          AND p.completed = true
                          AND p.finishedAt >= :from
                          AND p.finishedAt <= :to
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return total;
    }

    public List<DailyMinutesAggregation> findPomodoroFocusMinutesByDay(UUID userId, Instant from, Instant to) {
        List<Object[]> rows = entityManager.createQuery(
                        """
                        SELECT CAST(p.finishedAt AS localdate), COALESCE(SUM(p.completedMinutes), 0L)
                        FROM PomodoroSession p
                        WHERE p.user.id = :userId
                          AND p.completed = true
                          AND p.finishedAt >= :from
                          AND p.finishedAt <= :to
                        GROUP BY CAST(p.finishedAt AS localdate)
                        ORDER BY CAST(p.finishedAt AS localdate) ASC
                        """,
                        Object[].class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        return rows.stream()
                .map(row -> new DailyMinutesAggregation(toLocalDate(row[0]), (Long) row[1]))
                .toList();
    }

    public long countTotalHabits(UUID userId) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(h) FROM Habit h
                        WHERE h.user.id = :userId
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .getSingleResult();
    }

    public long countHabitsCreatedInPeriod(UUID userId, Instant from, Instant to) {
        return entityManager.createQuery(
                        """
                        SELECT COUNT(h) FROM Habit h
                        WHERE h.user.id = :userId
                          AND h.createdAt >= :from
                          AND h.createdAt <= :to
                        """,
                        Long.class
                )
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        throw new IllegalStateException("Unexpected date type in aggregation result: " + value.getClass());
    }
}
