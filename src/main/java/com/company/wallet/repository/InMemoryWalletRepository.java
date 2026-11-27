package com.company.wallet.repository;

import com.company.wallet.domain.Wallet;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWalletRepository implements WalletRepository{
    private final ConcurrentHashMap<String, Wallet> wallets = new ConcurrentHashMap<>();
    @Override
    public void save(Wallet wallet) {
        Objects.requireNonNull(wallet, "Wallet không được null");
        wallets.put(wallet.getWalletId(), wallet);
    }
    @Override
    public Optional<Wallet> findById(String walletId){
        return Optional.ofNullable(wallets.get(walletId));
    }
    @Override
    public boolean existsById(String walletId){
        return wallets.containsKey(walletId);
    }
}
