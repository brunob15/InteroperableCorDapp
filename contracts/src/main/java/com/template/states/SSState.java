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
    private final String exchangeType;
    private final String messageType;
    private final Party sender;
    private final Party receiver;

    public SSState(String exchangeType, String messageType, Party sender, Party receiver) {
        this.exchangeType = exchangeType;
        this.messageType = messageType;
        this.sender = sender;
        this.receiver = receiver;
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public String getMessageType() { return messageType; }

    public Party getSender() {
        return sender;
    }

    public Party getReceiver() {
        return receiver;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, receiver);
    }
}
