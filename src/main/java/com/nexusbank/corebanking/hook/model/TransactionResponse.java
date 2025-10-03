package com.nexusbank.corebanking.hook.model;

import lombok.Data;

import java.util.Map;

@Data
public class TransactionResponse {
    private int clientId;
    private int savingsId;
    private int resourceId;
    private Map<String, Object> changes;
}
