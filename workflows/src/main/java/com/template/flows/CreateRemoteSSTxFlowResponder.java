package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;

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
        subFlow(new ReceiveFinalityFlow(otherPartySession));
        return null;
    }
}
