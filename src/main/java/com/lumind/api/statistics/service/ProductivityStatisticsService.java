package com.lumind.api.statistics.service;

import com.lumind.api.statistics.dto.request.StatisticsPeriodQuery;
import com.lumind.api.statistics.dto.response.DailyCountResponse;
import com.lumind.api.statistics.dto.response.DailyMinutesResponse;
import com.lumind.api.statistics.dto.response.HabitStatisticsResponse;
import com.lumind.api.statistics.dto.response.PomodoroStatisticsResponse;
import com.lumind.api.statistics.dto.response.ProductivityOverviewResponse;
import com.lumind.api.statistics.dto.response.TaskStatisticsResponse;
import com.lumind.api.statistics.exception.InvalidStatisticsPeriodException;
import com.lumind.api.statistics.repository.ProductivityStatisticsRepository;
import com.lumind.api.statistics.repository.projection.DailyCountAggregation;
import com.lumind.api.statistics.repository.projection.DailyMinutesAggregation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ProductivityStatisticsService {

    private static final int DEFAULT_PERIOD_DAYS = 30;
    private static final int MAX_PERIOD_DAYS = 366;

    private final ProductivityStatisticsRepository productivityStatisticsRepository;

    public ProductivityStatisticsService(ProductivityStatisticsRepository productivityStatisticsRepository) {
        this.productivityStatisticsRepository = productivityStatisticsRepository;
    }

    @Transactional(readOnly = true)
    public ProductivityOverviewResponse getOverview(UUID userId, StatisticsPeriodQuery query) {
        ResolvedPeriod period = resolvePeriod(query);

        long tasksCreated = productivityStatisticsRepository.countTasksCreated(
                userId, period.from(), period.to());
        long tasksCompleted = productivityStatisticsRepository.countTasksCompleted(
                userId, period.from(), period.to());

        long sessionsStarted = productivityStatisticsRepository.countPomodoroSessionsStarted(
                userId, period.from(), period.to());
        long sessionsCompleted = productivityStatisticsRepository.countPomodoroSessionsCompleted(
                userId, period.from(), period.to());
        long totalFocusMinutes = productivityStatisticsRepository.sumPomodoroFocusMinutes(
                userId, period.from(), period.to());

        long totalHabits = productivityStatisticsRepository.countTotalHabits(userId);
        long habitsCreatedInPeriod = productivityStatisticsRepository.countHabitsCreatedInPeriod(
                userId, period.from(), period.to());

        return new ProductivityOverviewResponse(
                period.from(),
                period.to(),
                new ProductivityOverviewResponse.TaskOverviewMetrics(
                        tasksCreated,
                        tasksCompleted,
                        calculateCompletionRate(tasksCompleted, tasksCreated)
                ),
                new ProductivityOverviewResponse.PomodoroOverviewMetrics(
                        sessionsStarted,
                        sessionsCompleted,
                        totalFocusMinutes,
                        calculateCompletionRate(sessionsCompleted, sessionsStarted)
                ),
                new ProductivityOverviewResponse.HabitOverviewMetrics(
                        totalHabits,
                        habitsCreatedInPeriod
                )
        );
    }

    @Transactional(readOnly = true)
    public TaskStatisticsResponse getTaskStatistics(UUID userId, StatisticsPeriodQuery query) {
        ResolvedPeriod period = resolvePeriod(query);

        long created = productivityStatisticsRepository.countTasksCreated(
                userId, period.from(), period.to());
        long completed = productivityStatisticsRepository.countTasksCompleted(
                userId, period.from(), period.to());
        long pendingCreatedInPeriod = productivityStatisticsRepository.countTasksPendingCreatedInPeriod(
                userId, period.from(), period.to());

        return new TaskStatisticsResponse(
                period.from(),
                period.to(),
                created,
                completed,
                pendingCreatedInPeriod,
                calculateCompletionRate(completed, created),
                mapDailyCounts(productivityStatisticsRepository.findTasksCompletedByDay(
                        userId, period.from(), period.to()))
        );
    }

    @Transactional(readOnly = true)
    public PomodoroStatisticsResponse getPomodoroStatistics(UUID userId, StatisticsPeriodQuery query) {
        ResolvedPeriod period = resolvePeriod(query);

        long sessionsStarted = productivityStatisticsRepository.countPomodoroSessionsStarted(
                userId, period.from(), period.to());
        long sessionsCompleted = productivityStatisticsRepository.countPomodoroSessionsCompleted(
                userId, period.from(), period.to());
        long totalFocusMinutes = productivityStatisticsRepository.sumPomodoroFocusMinutes(
                userId, period.from(), period.to());

        return new PomodoroStatisticsResponse(
                period.from(),
                period.to(),
                sessionsStarted,
                sessionsCompleted,
                totalFocusMinutes,
                calculateAverageFocusMinutesPerCompletedSession(totalFocusMinutes, sessionsCompleted),
                calculateCompletionRate(sessionsCompleted, sessionsStarted),
                mapDailyMinutes(productivityStatisticsRepository.findPomodoroFocusMinutesByDay(
                        userId, period.from(), period.to()))
        );
    }

    @Transactional(readOnly = true)
    public HabitStatisticsResponse getHabitStatistics(UUID userId, StatisticsPeriodQuery query) {
        ResolvedPeriod period = resolvePeriod(query);

        long totalHabits = productivityStatisticsRepository.countTotalHabits(userId);
        long createdInPeriod = productivityStatisticsRepository.countHabitsCreatedInPeriod(
                userId, period.from(), period.to());

        return new HabitStatisticsResponse(
                period.from(),
                period.to(),
                totalHabits,
                createdInPeriod
        );
    }

    private ResolvedPeriod resolvePeriod(StatisticsPeriodQuery query) {
        Instant to = query.to() != null ? query.to() : Instant.now();
        Instant from = query.from() != null
                ? query.from()
                : defaultFromInstant();

        validatePeriod(from, to);
        return new ResolvedPeriod(from, to);
    }

    private Instant defaultFromInstant() {
        return Instant.now()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .minusDays(DEFAULT_PERIOD_DAYS)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    private void validatePeriod(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new InvalidStatisticsPeriodException();
        }

        long periodDays = ChronoUnit.DAYS.between(
                from.atZone(ZoneOffset.UTC).toLocalDate(),
                to.atZone(ZoneOffset.UTC).toLocalDate()
        );
        if (periodDays > MAX_PERIOD_DAYS) {
            throw new InvalidStatisticsPeriodException();
        }
    }

    private double calculateCompletionRate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return (double) numerator / denominator;
    }

    private long calculateAverageFocusMinutesPerCompletedSession(long totalFocusMinutes, long sessionsCompleted) {
        if (sessionsCompleted == 0) {
            return 0L;
        }
        return totalFocusMinutes / sessionsCompleted;
    }

    private List<DailyCountResponse> mapDailyCounts(List<DailyCountAggregation> aggregations) {
        if (aggregations.isEmpty()) {
            return List.of();
        }
        return aggregations.stream()
                .map(aggregation -> new DailyCountResponse(aggregation.date(), aggregation.count()))
                .toList();
    }

    private List<DailyMinutesResponse> mapDailyMinutes(List<DailyMinutesAggregation> aggregations) {
        if (aggregations.isEmpty()) {
            return List.of();
        }
        return aggregations.stream()
                .map(aggregation -> new DailyMinutesResponse(aggregation.date(), aggregation.minutes()))
                .toList();
    }

    private record ResolvedPeriod(Instant from, Instant to) {
    }
}
