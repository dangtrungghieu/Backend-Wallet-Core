package com.company.wallet.repository;

import com.company.wallet.domain.Transaction;

import java.util.List;

public interface TransactionRepository {
    void save(Transaction transaction);
    List<Transaction> findAll();
    List<Transaction> findByWalletId(String walletId);
}
