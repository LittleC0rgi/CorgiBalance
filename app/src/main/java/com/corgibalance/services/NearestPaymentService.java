package com.corgibalance.services;

import com.corgibalance.models.PlannedTransaction;
import com.corgibalance.models.RecurringTransaction;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.PlannedTransactionRepository;
import com.corgibalance.repositories.RecurringTransactionRepository;
import com.corgibalance.repositories.TransactionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NearestPaymentService {

    private final TransactionRepository transactionRepository;
    private final PlannedTransactionRepository plannedTransactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public NearestPaymentService() {
        this(new TransactionRepository(), new PlannedTransactionRepository(), new RecurringTransactionRepository());
    }

    public NearestPaymentService(TransactionRepository transactionRepository,
                                 PlannedTransactionRepository plannedTransactionRepository,
                                 RecurringTransactionRepository recurringTransactionRepository) {
        this.transactionRepository = transactionRepository;
        this.plannedTransactionRepository = plannedTransactionRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
    }

    public static List<NearestPayment> nearestPayments(List<PlannedTransaction> planned, List<RecurringTransaction> recurring,
                                                       LocalDate today, int limit) {
        List<NearestPayment> overdue = new ArrayList<>();
        List<NearestPayment> upcoming = new ArrayList<>();
        for (PlannedTransaction p : planned) {
            (p.getPlannedDate().isBefore(today) ? overdue : upcoming).add(NearestPayment.of(p));
        }
        for (RecurringTransaction r : recurring) {
            (r.getNextDate().isBefore(today) ? overdue : upcoming).add(NearestPayment.of(r));
        }
        overdue.sort(Comparator.comparing(NearestPayment::date));
        upcoming.sort(Comparator.comparing(NearestPayment::date));
        overdue.addAll(upcoming.subList(0, Math.min(limit, upcoming.size())));
        return overdue;
    }

    public List<NearestPayment> upcoming(LocalDate today, int limit) {
        return nearestPayments(plannedTransactionRepository.findAll(),
                recurringTransactionRepository.findActiveUpcoming(), today, limit);
    }

    public void confirm(NearestPayment payment) {
        if (payment.planned != null) {
            createTransaction(payment.planned.getAccountId(), payment.planned.getTagId(), payment.planned.getAmount(),
                    payment.planned.getDescription(), payment.planned.getTransactionType(), payment.planned.getPlannedDate());
            plannedTransactionRepository.delete(payment.planned);
        } else {
            RecurringTransaction recurring = payment.recurring;
            createTransaction(recurring.getAccountId(), recurring.getTagId(), recurring.getAmount(),
                    recurring.getDescription(), recurring.getTransactionType(), recurring.getNextDate());
            LocalDate next = recurring.getInterval().next(recurring.getNextDate());
            if (recurring.getEndDate() != null && next.isAfter(recurring.getEndDate())) {
                recurring.setActive(false);
            } else {
                recurring.setNextDate(next);
            }
            recurringTransactionRepository.update(recurring);
        }
    }

    public void delete(NearestPayment payment) {
        if (payment.planned != null) {
            plannedTransactionRepository.delete(payment.planned);
        } else {
            recurringTransactionRepository.delete(payment.recurring);
        }
    }

    private void createTransaction(Long accountId, Long tagId, long amount, String description,
                                   TransactionType type, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(accountId);
        transaction.setTagId(tagId);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionType(type);
        transaction.setTransactionDate(date);
        transactionRepository.create(transaction);
    }

    public record NearestPayment(PlannedTransaction planned, RecurringTransaction recurring) {
        public static NearestPayment of(PlannedTransaction p) {
            return new NearestPayment(p, null);
        }

        public static NearestPayment of(RecurringTransaction r) {
            return new NearestPayment(null, r);
        }

        public LocalDate date() {
            return planned != null ? planned.getPlannedDate() : recurring.getNextDate();
        }

        public long amount() {
            return planned != null ? planned.getAmount() : recurring.getAmount();
        }

        public Long accountId() {
            return planned != null ? planned.getAccountId() : recurring.getAccountId();
        }

        public Long tagId() {
            return planned != null ? planned.getTagId() : recurring.getTagId();
        }

        public TransactionType type() {
            return planned != null ? planned.getTransactionType() : recurring.getTransactionType();
        }

        public String description() {
            return planned != null ? planned.getDescription() : recurring.getDescription();
        }

        public boolean isRecurring() {
            return recurring != null;
        }
    }
}
