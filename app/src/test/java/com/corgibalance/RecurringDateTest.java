package com.corgibalance;

import com.corgibalance.components.views.CalendarView;
import com.corgibalance.models.RecurrenceInterval;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class RecurringDateTest {

    @Test
    public void dailyRollsForwardByOneDay() {
        assertEquals(LocalDate.of(2026, 2, 1),
                CalendarView.nextOccurrence(LocalDate.of(2026, 1, 31), RecurrenceInterval.DAILY));
    }

    @Test
    public void weeklyRollsForwardBySevenDays() {
        assertEquals(LocalDate.of(2026, 1, 8),
                CalendarView.nextOccurrence(LocalDate.of(2026, 1, 1), RecurrenceInterval.WEEKLY));
    }

    @Test
    public void monthlyClampsShortMonths() {
        assertEquals(LocalDate.of(2026, 2, 28),
                CalendarView.nextOccurrence(LocalDate.of(2026, 1, 31), RecurrenceInterval.MONTHLY));
        assertEquals(LocalDate.of(2026, 4, 30),
                CalendarView.nextOccurrence(LocalDate.of(2026, 3, 31), RecurrenceInterval.MONTHLY));
    }

    @Test
    public void yearlyClampsLeapDay() {
        assertEquals(LocalDate.of(2025, 2, 28),
                CalendarView.nextOccurrence(LocalDate.of(2024, 2, 29), RecurrenceInterval.YEARLY));
    }
}