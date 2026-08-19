package com.corgibalance;

import com.corgibalance.components.views.CalendarView;
import org.junit.Test;

import java.time.YearMonth;

import static org.junit.Assert.assertEquals;

public class CalendarGridTest {

    @Test
    public void firstColumnMatchesMondayBasedOffset() {
        assertEquals(3, CalendarView.firstColumn(YearMonth.of(2026, 1)));
        assertEquals(5, CalendarView.firstColumn(YearMonth.of(2026, 8)));
        assertEquals(6, CalendarView.firstColumn(YearMonth.of(2026, 3)));
    }

    @Test
    public void daysInMonthMatchYearMonth() {
        assertEquals(28, YearMonth.of(2026, 2).lengthOfMonth());
        assertEquals(29, YearMonth.of(2028, 2).lengthOfMonth());
        assertEquals(31, YearMonth.of(2026, 8).lengthOfMonth());
        assertEquals(30, YearMonth.of(2026, 4).lengthOfMonth());
    }
}
