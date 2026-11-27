package com.company.wallet.repository;

import com.company.wallet.domain.Wallet;
import java.util.Optional;

public interface WalletRepository {
    void save(Wallet wallet);
    Optional<Wallet> findById(String walletId);
    boolean existsById(String walletId);
}
