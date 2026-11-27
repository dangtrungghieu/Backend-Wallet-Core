package com.company.wallet.domain;

public enum WalletStatus {
    ACTIVE(true),
    FROZEN(false),
    CLOSED(false);

    private final boolean canTransact;
    WalletStatus(boolean canTransact) {
        this.canTransact = canTransact;
    }
    public boolean canTransact() {
        return canTransact;
    }
}
