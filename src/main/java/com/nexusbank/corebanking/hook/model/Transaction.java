package com.nexusbank.corebanking.hook.model;

import lombok.Data;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.Map;

@Data

public class Transaction {
    private String createdByName;
    private TransactionRequest request;
    private int clientId;
    private int createdBy;
    private int officeId;
    private String entityName;
    private TransactionResponse response;
    private String createdByFullName;
    private String actionName;
    private ZonedDateTime timestamp;

}

