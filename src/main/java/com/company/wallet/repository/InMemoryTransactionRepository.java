package com.company.wallet.repository;

import com.company.wallet.domain.Transaction;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryTransactionRepository implements TransactionRepository{
    private final List<Transaction> transactions = new CopyOnWriteArrayList<>();
    @Override
    public void save(Transaction transaction){
        Objects.requireNonNull(transaction, "Transaction không được null");
        transactions.add(transaction);
    }
    @Override
    public List<Transaction> findAll() {
        return List.copyOf(transactions);
    }
    @Override
    public List<Transaction> findByWalletId(String walletId) {
        return transactions.stream().filter(transaction -> walletId.equals(transaction.getDestinationWalletId()) || walletId.equals(transaction.getSourceWalletId())).toList();
    }
}
