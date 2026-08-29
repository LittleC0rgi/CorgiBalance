package com.corgibalance.services;

import com.corgibalance.models.Account;
import com.corgibalance.models.Budget;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.BudgetRepository;
import com.corgibalance.repositories.TransactionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OverviewService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CurrencyConverter converter;

    public OverviewService(CurrencyConverter converter) {
        this(new AccountRepository(), new TransactionRepository(), new BudgetRepository(), converter);
    }

    public OverviewService(AccountRepository accountRepository, TransactionRepository transactionRepository,
                           BudgetRepository budgetRepository, CurrencyConverter converter) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.converter = converter;
    }

    public long totalBalance(Long baseCurrencyId) {
        long total = 0;
        for (Account account : accountRepository.findAll()) {
            if (account.isHidden()) {
                continue;
            }
            long balance = accountRepository.currentBalance(account.getId());
            total += converter.convert(balance, account.getCurrencyId(), baseCurrencyId);
        }
        return total;
    }

    public long income(int year, int month, Long baseCurrencyId) {
        return sumForPeriod(TransactionType.INCOME, year, month, baseCurrencyId);
    }

    public long expense(int year, int month, Long baseCurrencyId) {
        return sumForPeriod(TransactionType.EXPENSE, year, month, baseCurrencyId);
    }

    public List<Budget> budgets(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<Budget> result = new ArrayList<>();
        for (Budget budget : budgetRepository.findAll()) {
            if (budget.getStartDate().isAfter(to) || budget.getEndDate().isBefore(from)) {
                continue;
            }
            result.add(budget);
        }
        return result;
    }

    public long budgetSpent(Budget budget, Long baseCurrencyId) {
        Map<Long, Long> totals = transactionRepository.sumByCurrency(
                TransactionType.EXPENSE, budget.getTagId(), budget.getStartDate(), budget.getEndDate());
        long spent = 0;
        for (Map.Entry<Long, Long> entry : totals.entrySet()) {
            spent += converter.convert(Math.abs(entry.getValue()), entry.getKey(), baseCurrencyId);
        }
        return spent;
    }

    private long sumForPeriod(TransactionType type, int year, int month, Long baseCurrencyId) {
        Map<Long, Long> totals = transactionRepository.sumByCurrency(type, year, month);
        long result = 0;
        for (Map.Entry<Long, Long> entry : totals.entrySet()) {
            result += converter.convert(Math.abs(entry.getValue()), entry.getKey(), baseCurrencyId);
        }
        return result;
    }
}
