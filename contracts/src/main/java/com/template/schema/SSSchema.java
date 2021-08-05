package com.template.schema;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Collections;

public class SSSchema extends MappedSchema {
    public SSSchema() {
        super(SSSchema.class, 1, Collections.singleton(PersistentSS.class));
    }

    @Entity
    @Table(name = "social_security_states")
    public static class PersistentSS extends PersistentState {
        @Column(name = "requestID") private final String requestID;
        @Column(name = "senderInstitution") private final String senderInstitution;
        @Column(name = "receiverInstitution") private final String receiverInstitution;
        @Column(name = "messageType") private final String messageType;
        @Column(name = "requestType") private final String requestType;
        @Column(name = "status") private final String status;
        @Column(name = "requestDate") private final String requestDate;
        @Column(name = "expectedReplyDate") private final String expectedReplyDate;
        @Column(name = "responseDate") private final String responseDate;
        @Column(name = "sourceBlockchain") private final String sourceBlockchain;
        @Column(name = "sourceContract") private final String sourceContract;

        public PersistentSS(
            String requestID,
            String senderInstitution,
            String receiverInstitution,
            String messageType,
            String requestType,
            String status,
            String requestDate,
            String expectedReplyDate,
            String responseDate,
            String sourceBlockchain,
            String sourceContract
        ) {
            this.requestID = requestID;
            this.senderInstitution = senderInstitution;
            this.receiverInstitution = receiverInstitution;
            this.messageType = messageType;
            this.requestType = requestType;
            this.status = status;
            this.requestDate = requestDate;
            this.expectedReplyDate = expectedReplyDate;
            this.responseDate = responseDate;
            this.sourceBlockchain = sourceBlockchain;
            this.sourceContract = sourceContract;
        }

        // Default constructor required by hibernate.
        public PersistentSS() {
            this.requestID = null;
            this.senderInstitution = null;
            this.receiverInstitution = null;
            this.messageType = null;
            this.requestType = null;
            this.status = null;
            this.requestDate = null;
            this.expectedReplyDate = null;
            this.responseDate = null;
            this.sourceBlockchain = null;
            this.sourceContract = null;
        }

        public String getRequestID() {
            return requestID;
        }

        public String getSenderInstitution() {
            return senderInstitution;
        }

        public String getReceiverInstitution() {
            return receiverInstitution;
        }

        public String getMessageType() {
            return messageType;
        }

        public String getRequestType() {
            return requestType;
        }

        public String getStatus() {
            return status;
        }

        public String getRequestDate() {
            return requestDate;
        }

        public String getExpectedReplyDate() {
            return expectedReplyDate;
        }

        public String getSourceBlockchain() {
            return sourceBlockchain;
        }

        public String getSourceContract() {
            return sourceContract;
        }

        public String getResponseDate() { return responseDate; }
    }
}