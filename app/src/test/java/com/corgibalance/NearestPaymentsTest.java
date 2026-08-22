package com.corgibalance;

import com.corgibalance.controllers.views.OverviewController;
import com.corgibalance.controllers.views.OverviewController.NearestPayment;
import com.corgibalance.models.PlannedTransaction;
import com.corgibalance.models.RecurringTransaction;
import com.corgibalance.models.TransactionType;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NearestPaymentsTest {

    private PlannedTransaction planned(LocalDate date) {
        PlannedTransaction p = new PlannedTransaction();
        p.setPlannedDate(date);
        p.setAmount(100);
        p.setTransactionType(TransactionType.EXPENSE);
        return p;
    }

    private RecurringTransaction recurring(LocalDate date) {
        RecurringTransaction r = new RecurringTransaction();
        r.setNextDate(date);
        r.setAmount(200);
        r.setTransactionType(TransactionType.INCOME);
        return r;
    }

    @Test
    public void upcomingSortedByDateLimitedToFive() {
        LocalDate today = LocalDate.of(2026, 8, 20);
        List<PlannedTransaction> planned = List.of(planned(today.plusDays(3)), planned(today.plusDays(1)));
        List<RecurringTransaction> recurring = List.of(recurring(today.plusDays(2)), recurring(today.plusDays(4)));

        List<NearestPayment> result = OverviewController.nearestPayments(planned, recurring, today, 5);

        assertEquals(4, result.size());
        assertEquals(List.of(today.plusDays(1), today.plusDays(2), today.plusDays(3), today.plusDays(4)),
                result.stream().map(NearestPayment::date).toList());
    }

    @Test
    public void overdueComeFirstAndDoNotCountTowardLimit() {
        LocalDate today = LocalDate.of(2026, 8, 20);
        List<PlannedTransaction> planned = List.of(
                planned(today.plusDays(1)), planned(today.plusDays(2)), planned(today.plusDays(3)),
                planned(today.plusDays(4)), planned(today.plusDays(5)), planned(today.plusDays(6)),
                planned(today.minusDays(2)), planned(today.minusDays(5)));
        List<RecurringTransaction> recurring = List.of(recurring(today.minusDays(1)));

        List<NearestPayment> result = OverviewController.nearestPayments(planned, recurring, today, 5);

        assertEquals(8, result.size());
        List<LocalDate> dates = result.stream().map(NearestPayment::date).toList();
        assertEquals(today.minusDays(5), dates.get(0));
        assertEquals(today.minusDays(2), dates.get(1));
        assertEquals(today.minusDays(1), dates.get(2));
        assertEquals(today.plusDays(1), dates.get(3));
        assertEquals(today.plusDays(5), dates.get(7));
        assertTrue(dates.subList(3, 8).stream().allMatch(d -> !d.isBefore(today)));
    }

    @Test
    public void emptyWhenNothingDue() {
        LocalDate today = LocalDate.of(2026, 8, 20);
        List<NearestPayment> result = OverviewController.nearestPayments(List.of(), List.of(), today, 5);
        assertTrue(result.isEmpty());
    }
}