package com.template.flows;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class EventData {
    private String sourceBlockchain;
    private String sourceContract;
    private String sourceTxId;
    private String exchangeType;
    private String messageType;

    public void setSourceBlockchain(String sourceBlockchain) {
        this.sourceBlockchain = sourceBlockchain;
    }

    public void setSourceContract(String sourceContract) {
        this.sourceContract = sourceContract;
    }

    public void setSourceTxId(String sourceTxId) {
        this.sourceTxId = sourceTxId;
    }

    public void setExchangeType(String exchangeType) {
        this.exchangeType = exchangeType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSourceBlockchain() {
        return sourceBlockchain;
    }

    public String getSourceContract() {
        return sourceContract;
    }

    public String getSourceTxId() {
        return sourceTxId;
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public String getMessageType() {
        return messageType;
    }
}