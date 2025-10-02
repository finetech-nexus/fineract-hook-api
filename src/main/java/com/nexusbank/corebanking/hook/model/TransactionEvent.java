package com.nexusbank.corebanking.hook.model;
import lombok.Data;
@Data
public class TransactionEvent {

    private String tenantIdentifier;
    private String action;
    private String entity;
    private String resourceId;
    private TransactionPayload payload;

}
