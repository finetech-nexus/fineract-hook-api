package com.nexusbank.corebanking.hook.model;

import lombok.Data;

@Data
public class TransactionPayload {

    public String accountId;
    public String clientId;
    public String transactionId;
    public String transactionType;
    public String amount;
    public String currency;
    public String note;
    public String date;
    public String status;
}
