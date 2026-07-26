package com.lumind.api.statistics.support;

import com.lumind.api.statistics.dto.request.StatisticsPeriodQuery;
import com.lumind.api.statistics.repository.projection.DailyCountAggregation;
import com.lumind.api.statistics.repository.projection.DailyMinutesAggregation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class ProductivityStatisticsTestData {

    public static final Instant CUSTOM_PERIOD_FROM = Instant.parse("2026-07-01T00:00:00Z");
    public static final Instant CUSTOM_PERIOD_TO = Instant.parse("2026-07-26T23:59:59Z");

    public static final Instant INVALID_FROM_AFTER_TO_FROM = Instant.parse("2026-07-26T00:00:00Z");
    public static final Instant INVALID_FROM_AFTER_TO_TO = Instant.parse("2026-07-01T00:00:00Z");

    public static final Instant PERIOD_TOO_LONG_FROM = Instant.parse("2025-01-01T00:00:00Z");
    public static final Instant PERIOD_TOO_LONG_TO = Instant.parse("2026-07-26T00:00:00Z");

    public static final long TASKS_CREATED = 10L;
    public static final long TASKS_COMPLETED = 7L;
    public static final long TASKS_PENDING_IN_PERIOD = 3L;
    public static final long POMODORO_SESSIONS_STARTED = 8L;
    public static final long POMODORO_SESSIONS_COMPLETED = 5L;
    public static final long POMODORO_FOCUS_MINUTES = 125L;
    public static final long TOTAL_HABITS = 4L;
    public static final long HABITS_CREATED_IN_PERIOD = 2L;

    public static final LocalDate DAILY_COUNT_DATE = LocalDate.of(2026, 7, 14);
    public static final long DAILY_TASK_COUNT = 3L;
    public static final LocalDate DAILY_MINUTES_DATE = LocalDate.of(2026, 7, 15);
    public static final long DAILY_FOCUS_MINUTES = 50L;

    private ProductivityStatisticsTestData() {
    }

    public static StatisticsPeriodQuery emptyPeriodQuery() {
        return new StatisticsPeriodQuery(null, null);
    }

    public static StatisticsPeriodQuery customPeriodQuery() {
        return new StatisticsPeriodQuery(CUSTOM_PERIOD_FROM, CUSTOM_PERIOD_TO);
    }

    public static StatisticsPeriodQuery invalidFromAfterToQuery() {
        return new StatisticsPeriodQuery(INVALID_FROM_AFTER_TO_FROM, INVALID_FROM_AFTER_TO_TO);
    }

    public static StatisticsPeriodQuery periodTooLongQuery() {
        return new StatisticsPeriodQuery(PERIOD_TOO_LONG_FROM, PERIOD_TOO_LONG_TO);
    }

    public static StatisticsPeriodQuery recentPeriodQuery() {
        Instant to = Instant.now();
        Instant from = to.minus(7, ChronoUnit.DAYS);
        return new StatisticsPeriodQuery(from, to);
    }

    public static String periodQueryParams(Instant from, Instant to) {
        return "from=" + from + "&to=" + to;
    }

    public static String customPeriodQueryParams() {
        return periodQueryParams(CUSTOM_PERIOD_FROM, CUSTOM_PERIOD_TO);
    }

    public static String invalidFromAfterToQueryParams() {
        return periodQueryParams(INVALID_FROM_AFTER_TO_FROM, INVALID_FROM_AFTER_TO_TO);
    }

    public static String periodTooLongQueryParams() {
        return periodQueryParams(PERIOD_TOO_LONG_FROM, PERIOD_TOO_LONG_TO);
    }

    public static String recentPeriodQueryParams() {
        StatisticsPeriodQuery query = recentPeriodQuery();
        return periodQueryParams(query.from(), query.to());
    }

    public static Instant expectedDefaultFromInstant() {
        return Instant.now()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .minusDays(30)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    public static List<DailyCountAggregation> sampleTaskDailyCounts() {
        return List.of(new DailyCountAggregation(DAILY_COUNT_DATE, DAILY_TASK_COUNT));
    }

    public static List<DailyMinutesAggregation> samplePomodoroDailyMinutes() {
        return List.of(new DailyMinutesAggregation(DAILY_MINUTES_DATE, DAILY_FOCUS_MINUTES));
    }

    public static double expectedTaskCompletionRate() {
        return (double) TASKS_COMPLETED / TASKS_CREATED;
    }

    public static double expectedPomodoroCompletionRate() {
        return (double) POMODORO_SESSIONS_COMPLETED / POMODORO_SESSIONS_STARTED;
    }

    public static long expectedAverageFocusMinutes() {
        return POMODORO_FOCUS_MINUTES / POMODORO_SESSIONS_COMPLETED;
    }
}
