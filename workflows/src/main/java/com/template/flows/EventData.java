package com.template.flows;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class EventData {
    private String sourceBlockchain;
    private String sourceContract;
    private String requestID;
    private String senderInstitution;
    private String receiverInstitution;
    private String messageType;
    private String requestType;
    private String status;
    private String requestDate;
    private String expectedReplyDate;
    private String responseDate;
    private String packageHash;

    public void setSourceBlockchain(String sourceBlockchain) {
        this.sourceBlockchain = sourceBlockchain;
    }
    public void setSourceContract(String sourceContract) {
        this.sourceContract = sourceContract;
    }
    public void setRequestID(String sourceTxId) {
        this.requestID = sourceTxId;
    }
    public void setSenderInstitution(String senderInstitution) {
        this.senderInstitution = senderInstitution;
    }
    public void setReceiverInstitution(String receiverInstitution) { this.receiverInstitution = receiverInstitution; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public void setStatus(String status) { this.status = status; }
    public void setRequestDate(String requestDate) { this.requestDate = requestDate; }
    public void setExpectedReplyDate(String expectedReplyDate) { this.expectedReplyDate = expectedReplyDate; }
    public void setResponseDate(String responseDate) { this.responseDate = responseDate; }
    public void setPackageHash(String packageHash) { this.packageHash = packageHash; }

    public String getSourceBlockchain() {
        return sourceBlockchain;
    }
    public String getSourceContract() {
        return sourceContract;
    }
    public String getRequestID() {
        return requestID;
    }
    public String getSenderInstitution() {
        return senderInstitution;
    }
    public String getReceiverInstitution() { return receiverInstitution; }
    public String getMessageType() {
        return messageType;
    }
    public String getRequestType() {
        return requestType;
    }
    public String getStatus() { return status; }
    public String getRequestDate() { return requestDate; }
    public String getExpectedReplyDate() { return expectedReplyDate; }
    public String getResponseDate() { return responseDate; }
    public String getPackageHash() { return packageHash; }
}
