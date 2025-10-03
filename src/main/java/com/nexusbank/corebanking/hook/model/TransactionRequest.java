package com.nexusbank.corebanking.hook.model;

import lombok.Data;
@Data
public class TransactionRequest {
    private String transactionDate;
    private double transactionAmount;
    private double paymentTypeId;
    private String note;
    private String dateFormat;
    private String locale;

}