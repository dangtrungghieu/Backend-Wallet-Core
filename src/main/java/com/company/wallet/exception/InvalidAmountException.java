package com.company.wallet.exception;

public class InvalidAmountException extends WalletException{
    public InvalidAmountException(String message){
        super(message);
    }
}
