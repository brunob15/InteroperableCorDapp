package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(CreateRemoteSSTxFlow.class)
public class CreateRemoteSSTxFlowResponder extends FlowLogic<Void> {
    private final FlowSession otherPartySession;

    public CreateRemoteSSTxFlowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
//        String otherPartyName = otherPartySession.getCounterparty().getName().toString();
//        logger.info("Receiving transaction from: " + otherPartyName);
        getLogger().info("Receiving transaction");
        SignedTransaction tx = subFlow(new ReceiveFinalityFlow(otherPartySession));
        getLogger().info("Transaction signed");
        return null;
    }
}
