package com.lumind.api.statistics;

import com.lumind.api.auth.support.AuthTestData;
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
import com.lumind.api.statistics.service.ProductivityStatisticsService;
import com.lumind.api.statistics.support.ProductivityStatisticsTestData;
import com.lumind.api.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductivityStatisticsServiceTest {

    @Mock
    private ProductivityStatisticsRepository productivityStatisticsRepository;

    @InjectMocks
    private ProductivityStatisticsService productivityStatisticsService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        user = AuthTestData.activeUser();
        userId = user.getId();
    }

    @Test
    void getOverview_customPeriod_returnsAggregatedMetrics() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        stubOverviewRepositoryCalls(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO
        );

        ProductivityOverviewResponse response = productivityStatisticsService.getOverview(userId, query);

        assertThat(response.from()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM);
        assertThat(response.to()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        assertThat(response.tasks().created()).isEqualTo(ProductivityStatisticsTestData.TASKS_CREATED);
        assertThat(response.tasks().completed()).isEqualTo(ProductivityStatisticsTestData.TASKS_COMPLETED);
        assertThat(response.tasks().completionRate())
                .isEqualTo(ProductivityStatisticsTestData.expectedTaskCompletionRate());
        assertThat(response.pomodoroSessions().sessionsStarted())
                .isEqualTo(ProductivityStatisticsTestData.POMODORO_SESSIONS_STARTED);
        assertThat(response.pomodoroSessions().sessionsCompleted())
                .isEqualTo(ProductivityStatisticsTestData.POMODORO_SESSIONS_COMPLETED);
        assertThat(response.pomodoroSessions().totalFocusMinutes())
                .isEqualTo(ProductivityStatisticsTestData.POMODORO_FOCUS_MINUTES);
        assertThat(response.pomodoroSessions().completionRate())
                .isEqualTo(ProductivityStatisticsTestData.expectedPomodoroCompletionRate());
        assertThat(response.habits().totalHabits()).isEqualTo(ProductivityStatisticsTestData.TOTAL_HABITS);
        assertThat(response.habits().createdInPeriod())
                .isEqualTo(ProductivityStatisticsTestData.HABITS_CREATED_IN_PERIOD);

        verifyOverviewRepositoryCalls(userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
    }

    @Test
    void getOverview_defaultPeriod_resolvesThirtyDayWindow() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.emptyPeriodQuery();
        when(productivityStatisticsRepository.countTasksCreated(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.countTasksCompleted(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.countPomodoroSessionsStarted(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.countPomodoroSessionsCompleted(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.sumPomodoroFocusMinutes(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.countTotalHabits(userId)).thenReturn(0L);
        when(productivityStatisticsRepository.countHabitsCreatedInPeriod(eq(userId), any(), any())).thenReturn(0L);

        productivityStatisticsService.getOverview(userId, query);

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(productivityStatisticsRepository).countTasksCreated(eq(userId), fromCaptor.capture(), toCaptor.capture());

        assertThat(fromCaptor.getValue()).isEqualTo(ProductivityStatisticsTestData.expectedDefaultFromInstant());
        assertThat(toCaptor.getValue()).isCloseTo(Instant.now(), within(2, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void getOverview_noData_returnsZeroMetrics() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        stubOverviewRepositoryCallsWithZeros(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO
        );

        ProductivityOverviewResponse response = productivityStatisticsService.getOverview(userId, query);

        assertThat(response.tasks().created()).isZero();
        assertThat(response.tasks().completed()).isZero();
        assertThat(response.tasks().completionRate()).isZero();
        assertThat(response.pomodoroSessions().sessionsStarted()).isZero();
        assertThat(response.pomodoroSessions().sessionsCompleted()).isZero();
        assertThat(response.pomodoroSessions().totalFocusMinutes()).isZero();
        assertThat(response.pomodoroSessions().completionRate()).isZero();
        assertThat(response.habits().totalHabits()).isZero();
        assertThat(response.habits().createdInPeriod()).isZero();
    }

    @Test
    void getOverview_zeroTaskDenominator_returnsZeroCompletionRate() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        stubOverviewRepositoryCalls(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                0L,
                0L,
                ProductivityStatisticsTestData.POMODORO_SESSIONS_STARTED,
                ProductivityStatisticsTestData.POMODORO_SESSIONS_COMPLETED,
                ProductivityStatisticsTestData.POMODORO_FOCUS_MINUTES,
                ProductivityStatisticsTestData.TOTAL_HABITS,
                ProductivityStatisticsTestData.HABITS_CREATED_IN_PERIOD
        );

        ProductivityOverviewResponse response = productivityStatisticsService.getOverview(userId, query);

        assertThat(response.tasks().completionRate()).isZero();
    }

    @Test
    void getTaskStatistics_customPeriod_returnsMetricsAndDailyTrend() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        List<DailyCountAggregation> dailyCounts = ProductivityStatisticsTestData.sampleTaskDailyCounts();
        stubTaskRepositoryCalls(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                dailyCounts
        );

        TaskStatisticsResponse response = productivityStatisticsService.getTaskStatistics(userId, query);

        assertThat(response.from()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM);
        assertThat(response.to()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        assertThat(response.created()).isEqualTo(ProductivityStatisticsTestData.TASKS_CREATED);
        assertThat(response.completed()).isEqualTo(ProductivityStatisticsTestData.TASKS_COMPLETED);
        assertThat(response.pendingCreatedInPeriod())
                .isEqualTo(ProductivityStatisticsTestData.TASKS_PENDING_IN_PERIOD);
        assertThat(response.completionRate()).isEqualTo(ProductivityStatisticsTestData.expectedTaskCompletionRate());
        assertThat(response.completedByDay()).containsExactly(
                new DailyCountResponse(ProductivityStatisticsTestData.DAILY_COUNT_DATE,
                        ProductivityStatisticsTestData.DAILY_TASK_COUNT)
        );

        verify(productivityStatisticsRepository).countTasksCreated(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        verify(productivityStatisticsRepository).countTasksCompleted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        verify(productivityStatisticsRepository).countTasksPendingCreatedInPeriod(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        verify(productivityStatisticsRepository).findTasksCompletedByDay(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
    }

    @Test
    void getTaskStatistics_defaultPeriod_callsRepositoryWithResolvedPeriod() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.emptyPeriodQuery();
        when(productivityStatisticsRepository.countTasksCreated(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.countTasksCompleted(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.countTasksPendingCreatedInPeriod(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.findTasksCompletedByDay(eq(userId), any(), any())).thenReturn(List.of());

        productivityStatisticsService.getTaskStatistics(userId, query);

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(productivityStatisticsRepository).findTasksCompletedByDay(eq(userId), fromCaptor.capture(), any());
        assertThat(fromCaptor.getValue()).isEqualTo(ProductivityStatisticsTestData.expectedDefaultFromInstant());
    }

    @Test
    void getTaskStatistics_noData_returnsEmptyDailyList() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        stubTaskRepositoryCalls(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                List.of()
        );
        when(productivityStatisticsRepository.countTasksCreated(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.countTasksCompleted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.countTasksPendingCreatedInPeriod(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);

        TaskStatisticsResponse response = productivityStatisticsService.getTaskStatistics(userId, query);

        assertThat(response.created()).isZero();
        assertThat(response.completed()).isZero();
        assertThat(response.pendingCreatedInPeriod()).isZero();
        assertThat(response.completionRate()).isZero();
        assertThat(response.completedByDay()).isEmpty();
    }

    @Test
    void getTaskStatistics_zeroCreated_returnsZeroCompletionRate() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        when(productivityStatisticsRepository.countTasksCreated(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.countTasksCompleted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.countTasksPendingCreatedInPeriod(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.findTasksCompletedByDay(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(List.of());

        TaskStatisticsResponse response = productivityStatisticsService.getTaskStatistics(userId, query);

        assertThat(response.completionRate()).isZero();
    }

    @Test
    void getPomodoroStatistics_customPeriod_returnsMetricsAndDailyTrend() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        List<DailyMinutesAggregation> dailyMinutes = ProductivityStatisticsTestData.samplePomodoroDailyMinutes();
        stubPomodoroRepositoryCalls(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                dailyMinutes
        );

        PomodoroStatisticsResponse response = productivityStatisticsService.getPomodoroStatistics(userId, query);

        assertThat(response.from()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM);
        assertThat(response.to()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        assertThat(response.sessionsStarted()).isEqualTo(ProductivityStatisticsTestData.POMODORO_SESSIONS_STARTED);
        assertThat(response.sessionsCompleted()).isEqualTo(ProductivityStatisticsTestData.POMODORO_SESSIONS_COMPLETED);
        assertThat(response.totalFocusMinutes()).isEqualTo(ProductivityStatisticsTestData.POMODORO_FOCUS_MINUTES);
        assertThat(response.averageFocusMinutesPerCompletedSession())
                .isEqualTo(ProductivityStatisticsTestData.expectedAverageFocusMinutes());
        assertThat(response.completionRate()).isEqualTo(ProductivityStatisticsTestData.expectedPomodoroCompletionRate());
        assertThat(response.focusMinutesByDay()).containsExactly(
                new DailyMinutesResponse(ProductivityStatisticsTestData.DAILY_MINUTES_DATE,
                        ProductivityStatisticsTestData.DAILY_FOCUS_MINUTES)
        );

        verify(productivityStatisticsRepository).countPomodoroSessionsStarted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        verify(productivityStatisticsRepository).countPomodoroSessionsCompleted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        verify(productivityStatisticsRepository).sumPomodoroFocusMinutes(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        verify(productivityStatisticsRepository).findPomodoroFocusMinutesByDay(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
    }

    @Test
    void getPomodoroStatistics_defaultPeriod_callsRepositoryWithResolvedPeriod() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.emptyPeriodQuery();
        when(productivityStatisticsRepository.countPomodoroSessionsStarted(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.countPomodoroSessionsCompleted(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.sumPomodoroFocusMinutes(eq(userId), any(), any())).thenReturn(0L);
        when(productivityStatisticsRepository.findPomodoroFocusMinutesByDay(eq(userId), any(), any())).thenReturn(List.of());

        productivityStatisticsService.getPomodoroStatistics(userId, query);

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(productivityStatisticsRepository).sumPomodoroFocusMinutes(eq(userId), fromCaptor.capture(), any());
        assertThat(fromCaptor.getValue()).isEqualTo(ProductivityStatisticsTestData.expectedDefaultFromInstant());
    }

    @Test
    void getPomodoroStatistics_noData_returnsEmptyDailyList() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        stubPomodoroRepositoryCalls(
                ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM,
                ProductivityStatisticsTestData.CUSTOM_PERIOD_TO,
                List.of()
        );
        when(productivityStatisticsRepository.countPomodoroSessionsStarted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.countPomodoroSessionsCompleted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.sumPomodoroFocusMinutes(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);

        PomodoroStatisticsResponse response = productivityStatisticsService.getPomodoroStatistics(userId, query);

        assertThat(response.sessionsStarted()).isZero();
        assertThat(response.sessionsCompleted()).isZero();
        assertThat(response.totalFocusMinutes()).isZero();
        assertThat(response.averageFocusMinutesPerCompletedSession()).isZero();
        assertThat(response.completionRate()).isZero();
        assertThat(response.focusMinutesByDay()).isEmpty();
    }

    @Test
    void getPomodoroStatistics_zeroCompletedSessions_returnsZeroAverageFocusMinutes() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        when(productivityStatisticsRepository.countPomodoroSessionsStarted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(ProductivityStatisticsTestData.POMODORO_SESSIONS_STARTED);
        when(productivityStatisticsRepository.countPomodoroSessionsCompleted(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.sumPomodoroFocusMinutes(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);
        when(productivityStatisticsRepository.findPomodoroFocusMinutesByDay(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(List.of());

        PomodoroStatisticsResponse response = productivityStatisticsService.getPomodoroStatistics(userId, query);

        assertThat(response.averageFocusMinutesPerCompletedSession()).isZero();
        assertThat(response.completionRate()).isZero();
    }

    @Test
    void getHabitStatistics_customPeriod_returnsMetrics() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        when(productivityStatisticsRepository.countTotalHabits(userId))
                .thenReturn(ProductivityStatisticsTestData.TOTAL_HABITS);
        when(productivityStatisticsRepository.countHabitsCreatedInPeriod(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(ProductivityStatisticsTestData.HABITS_CREATED_IN_PERIOD);

        HabitStatisticsResponse response = productivityStatisticsService.getHabitStatistics(userId, query);

        assertThat(response.from()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM);
        assertThat(response.to()).isEqualTo(ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
        assertThat(response.totalHabits()).isEqualTo(ProductivityStatisticsTestData.TOTAL_HABITS);
        assertThat(response.createdInPeriod()).isEqualTo(ProductivityStatisticsTestData.HABITS_CREATED_IN_PERIOD);

        verify(productivityStatisticsRepository).countTotalHabits(userId);
        verify(productivityStatisticsRepository).countHabitsCreatedInPeriod(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO);
    }

    @Test
    void getHabitStatistics_defaultPeriod_callsRepositoryWithResolvedPeriod() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.emptyPeriodQuery();
        when(productivityStatisticsRepository.countTotalHabits(userId)).thenReturn(0L);
        when(productivityStatisticsRepository.countHabitsCreatedInPeriod(eq(userId), any(), any())).thenReturn(0L);

        productivityStatisticsService.getHabitStatistics(userId, query);

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(productivityStatisticsRepository).countHabitsCreatedInPeriod(eq(userId), fromCaptor.capture(), any());
        assertThat(fromCaptor.getValue()).isEqualTo(ProductivityStatisticsTestData.expectedDefaultFromInstant());
    }

    @Test
    void getHabitStatistics_noData_returnsZeroMetrics() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.customPeriodQuery();
        when(productivityStatisticsRepository.countTotalHabits(userId)).thenReturn(0L);
        when(productivityStatisticsRepository.countHabitsCreatedInPeriod(
                userId, ProductivityStatisticsTestData.CUSTOM_PERIOD_FROM, ProductivityStatisticsTestData.CUSTOM_PERIOD_TO))
                .thenReturn(0L);

        HabitStatisticsResponse response = productivityStatisticsService.getHabitStatistics(userId, query);

        assertThat(response.totalHabits()).isZero();
        assertThat(response.createdInPeriod()).isZero();
    }

    @Test
    void invalidPeriod_fromAfterTo_throwsInvalidStatisticsPeriodException() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.invalidFromAfterToQuery();

        assertInvalidPeriodOnAllMethods(query);
        verify(productivityStatisticsRepository, never()).countTasksCreated(any(), any(), any());
    }

    @Test
    void invalidPeriod_exceeds366Days_throwsInvalidStatisticsPeriodException() {
        StatisticsPeriodQuery query = ProductivityStatisticsTestData.periodTooLongQuery();

        assertInvalidPeriodOnAllMethods(query);
        verify(productivityStatisticsRepository, never()).countTotalHabits(any());
    }

    private void assertInvalidPeriodOnAllMethods(StatisticsPeriodQuery query) {
        assertThatThrownBy(() -> productivityStatisticsService.getOverview(userId, query))
                .isInstanceOf(InvalidStatisticsPeriodException.class);
        assertThatThrownBy(() -> productivityStatisticsService.getTaskStatistics(userId, query))
                .isInstanceOf(InvalidStatisticsPeriodException.class);
        assertThatThrownBy(() -> productivityStatisticsService.getPomodoroStatistics(userId, query))
                .isInstanceOf(InvalidStatisticsPeriodException.class);
        assertThatThrownBy(() -> productivityStatisticsService.getHabitStatistics(userId, query))
                .isInstanceOf(InvalidStatisticsPeriodException.class);
    }

    private void stubOverviewRepositoryCalls(Instant from, Instant to) {
        stubOverviewRepositoryCalls(
                from,
                to,
                ProductivityStatisticsTestData.TASKS_CREATED,
                ProductivityStatisticsTestData.TASKS_COMPLETED,
                ProductivityStatisticsTestData.POMODORO_SESSIONS_STARTED,
                ProductivityStatisticsTestData.POMODORO_SESSIONS_COMPLETED,
                ProductivityStatisticsTestData.POMODORO_FOCUS_MINUTES,
                ProductivityStatisticsTestData.TOTAL_HABITS,
                ProductivityStatisticsTestData.HABITS_CREATED_IN_PERIOD
        );
    }

    private void stubOverviewRepositoryCalls(
            Instant from,
            Instant to,
            long tasksCreated,
            long tasksCompleted,
            long sessionsStarted,
            long sessionsCompleted,
            long focusMinutes,
            long totalHabits,
            long habitsCreatedInPeriod
    ) {
        when(productivityStatisticsRepository.countTasksCreated(userId, from, to)).thenReturn(tasksCreated);
        when(productivityStatisticsRepository.countTasksCompleted(userId, from, to)).thenReturn(tasksCompleted);
        when(productivityStatisticsRepository.countPomodoroSessionsStarted(userId, from, to)).thenReturn(sessionsStarted);
        when(productivityStatisticsRepository.countPomodoroSessionsCompleted(userId, from, to)).thenReturn(sessionsCompleted);
        when(productivityStatisticsRepository.sumPomodoroFocusMinutes(userId, from, to)).thenReturn(focusMinutes);
        when(productivityStatisticsRepository.countTotalHabits(userId)).thenReturn(totalHabits);
        when(productivityStatisticsRepository.countHabitsCreatedInPeriod(userId, from, to))
                .thenReturn(habitsCreatedInPeriod);
    }

    private void stubOverviewRepositoryCallsWithZeros(Instant from, Instant to) {
        stubOverviewRepositoryCalls(from, to, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private void verifyOverviewRepositoryCalls(UUID userId, Instant from, Instant to) {
        verify(productivityStatisticsRepository).countTasksCreated(userId, from, to);
        verify(productivityStatisticsRepository).countTasksCompleted(userId, from, to);
        verify(productivityStatisticsRepository).countPomodoroSessionsStarted(userId, from, to);
        verify(productivityStatisticsRepository).countPomodoroSessionsCompleted(userId, from, to);
        verify(productivityStatisticsRepository).sumPomodoroFocusMinutes(userId, from, to);
        verify(productivityStatisticsRepository).countTotalHabits(userId);
        verify(productivityStatisticsRepository).countHabitsCreatedInPeriod(userId, from, to);
    }

    private void stubTaskRepositoryCalls(Instant from, Instant to, List<DailyCountAggregation> dailyCounts) {
        when(productivityStatisticsRepository.countTasksCreated(userId, from, to))
                .thenReturn(ProductivityStatisticsTestData.TASKS_CREATED);
        when(productivityStatisticsRepository.countTasksCompleted(userId, from, to))
                .thenReturn(ProductivityStatisticsTestData.TASKS_COMPLETED);
        when(productivityStatisticsRepository.countTasksPendingCreatedInPeriod(userId, from, to))
                .thenReturn(ProductivityStatisticsTestData.TASKS_PENDING_IN_PERIOD);
        when(productivityStatisticsRepository.findTasksCompletedByDay(userId, from, to)).thenReturn(dailyCounts);
    }

    private void stubPomodoroRepositoryCalls(Instant from, Instant to, List<DailyMinutesAggregation> dailyMinutes) {
        when(productivityStatisticsRepository.countPomodoroSessionsStarted(userId, from, to))
                .thenReturn(ProductivityStatisticsTestData.POMODORO_SESSIONS_STARTED);
        when(productivityStatisticsRepository.countPomodoroSessionsCompleted(userId, from, to))
                .thenReturn(ProductivityStatisticsTestData.POMODORO_SESSIONS_COMPLETED);
        when(productivityStatisticsRepository.sumPomodoroFocusMinutes(userId, from, to))
                .thenReturn(ProductivityStatisticsTestData.POMODORO_FOCUS_MINUTES);
        when(productivityStatisticsRepository.findPomodoroFocusMinutesByDay(userId, from, to)).thenReturn(dailyMinutes);
    }
}
