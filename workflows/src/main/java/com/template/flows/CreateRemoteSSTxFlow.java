package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.TemplateContract;
import com.template.states.SSState;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

@InitiatingFlow
@StartableByRPC
public class CreateRemoteSSTxFlow extends FlowLogic<SignedTransaction> {
    private final String sourceTxId;
    private final String sourceBlockchain;
    private final String sourceContract;
    private final String exchangeType;
    private final String messageType;
    private final Party otherParty;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public CreateRemoteSSTxFlow(
        String sourceTxId,
        String sourceBlockchain,
        String sourceContract,
        String exchangeType,
        String messageType,
        Party otherParty
    ) {
        this.sourceTxId = sourceTxId;
        this.sourceBlockchain = sourceBlockchain;
        this.sourceContract = sourceContract;
        this.exchangeType = exchangeType;
        this.messageType = messageType;
        this.otherParty = otherParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // We create the transaction components.
        SSState outputState = new SSState(
                sourceTxId,
                sourceBlockchain,
                sourceContract,
                exchangeType,
                messageType,
                getOurIdentity()
        );
        Command command = new Command<>(new TemplateContract.Commands.Send(), getOurIdentity().getOwningKey());

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, TemplateContract.ID)
                .addCommand(command);

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Creating a session with the other party.
        FlowSession otherPartySession = initiateFlow(otherParty);

        // We finalise the transaction and then send it to the counterparty.
        subFlow(new FinalityFlow(signedTx, otherPartySession));

        return signedTx;
    }
}
