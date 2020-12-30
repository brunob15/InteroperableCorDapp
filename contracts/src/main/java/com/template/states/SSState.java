package com.template.states;

import com.template.contracts.TemplateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(TemplateContract.class)
public class SSState implements ContractState {
    private final String sourceTxId;
    private final String sourceBlockchain;
    private final String sourceContract;
    private final String exchangeType;
    private final String messageType;
    private final Party receiver;

    public SSState(
        String sourceTxId,
        String sourceBlockchain,
        String sourceContract,
        String exchangeType,
        String messageType,
        Party receiver
    ) {
        this.sourceTxId = sourceTxId;
        this.sourceBlockchain = sourceBlockchain;
        this.sourceContract = sourceContract;
        this.exchangeType = exchangeType;
        this.messageType = messageType;
        this.receiver = receiver;
    }

    public String getSourceTxId() { return sourceTxId; }
    public String getSourceBlockchain() { return sourceBlockchain; }
    public String getSourceContract() { return sourceContract; }
    public String getExchangeType() { return exchangeType; }
    public String getMessageType() { return messageType; }
    public Party getReceiver() { return receiver; }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(receiver);
    }
}
