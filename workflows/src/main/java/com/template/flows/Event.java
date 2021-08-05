package com.template.flows;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class Event {
    private String targetContract;
    private String targetBlockchain;
    private EventData data;

    public void setTargetContract(String targetContract) {
        this.targetContract = targetContract;
    }

    public void setTargetBlockchain(String targetBlockchain) {
        this.targetBlockchain = targetBlockchain;
    }

    public void setData(EventData data) {
        this.data = data;
    }

    public String getTargetContract() {
        return targetContract;
    }

    public String getTargetBlockchain() {
        return targetBlockchain;
    }

    public EventData getData() {
        return data;
    }
}
