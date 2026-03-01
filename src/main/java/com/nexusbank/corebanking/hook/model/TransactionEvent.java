package com.nexusbank.corebanking.hook.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
@Data
public class TransactionEvent {

    private String tenantId;
    private String action;
    private String entity;
    private String resourceId;
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private TransactionPayload payload;

}
