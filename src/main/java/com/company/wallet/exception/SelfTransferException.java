package com.company.wallet.exception;

public class SelfTransferException extends WalletException{
    public SelfTransferException(String message){
        super(message);
    }
}
