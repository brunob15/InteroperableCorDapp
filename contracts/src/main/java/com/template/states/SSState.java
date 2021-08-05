package com.template.states;

import com.template.contracts.TemplateContract;
import com.template.schema.SSSchema;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@BelongsToContract(TemplateContract.class)
public class SSState implements ContractState, QueryableState {
    private final String sourceBlockchain;
    private final String sourceContract;
    private final String requestID;
    private final String senderInstitution;
    private final String receiverInstitution;
    private final String messageType;
    private final String requestType;
    private final String status;
    private final String requestDate;
    private final String expectedReplyDate;
    private final String responseDate;
    private final String requestPackageHash;
    private final String responsePackageHash;
    private final List<AbstractParty> participants;

    public SSState(
        String sourceBlockchain,
        String sourceContract,
        String requestID,
        String senderInstitution,
        String receiverInstitution,
        String messageType,
        String requestType,
        String status,
        String requestDate,
        String expectedReplyDate,
        String responseDate,
        String requestPackageHash,
        String responsePackageHash,
        List<AbstractParty> participants
    ) {
        this.sourceBlockchain = sourceBlockchain;
        this.sourceContract = sourceContract;
        this.requestID = requestID;
        this.senderInstitution = senderInstitution;
        this.receiverInstitution = receiverInstitution;
        this.messageType = messageType;
        this.requestType = requestType;
        this.status = status;
        this.requestDate = requestDate;
        this.expectedReplyDate = expectedReplyDate;
        this.responseDate = responseDate;
        this.requestPackageHash = requestPackageHash;
        this.responsePackageHash = responsePackageHash;
        this.participants = participants;
    }

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
    public String getRequestPackageHash() {
        return requestPackageHash;
    }
    public String getResponsePackageHash() {
        return responsePackageHash;
    }
    public String getResponseDate() { return responseDate; }

    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return  Collections.singleton(new SSSchema());
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if (schema instanceof SSSchema) {
            return new SSSchema.PersistentSS(
                this.requestID,
                this.senderInstitution,
                this.receiverInstitution,
                this.messageType,
                this.requestType,
                this.status,
                this.requestDate,
                this.expectedReplyDate,
                this.responseDate,
                this.sourceBlockchain,
                this.sourceContract
            );
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }
}
