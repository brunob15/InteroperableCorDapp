package com.template.flows;

import com.template.contracts.TemplateContract;
import com.template.states.SSState;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.contracts.Command;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@InitiatingFlow
@StartableByRPC
public class SSFlow extends FlowLogic<SignedTransaction> {
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

    public SSFlow(
        Map<String, String[]> parameters,
        Party otherParty
    ) {
        this.sourceTxId = parameters.get("sourceTxId")[0];
        this.sourceBlockchain = parameters.get("sourceBlockchain")[0];
        this.sourceContract = parameters.get("sourceContract")[0];
        this.exchangeType = parameters.get("exchangeType")[0];
        this.messageType = parameters.get("messageType")[0];
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

        // Since there is no counter party we pass an empty list
        List<FlowSession> sessions = Collections.EMPTY_LIST;

        // We finalise the transaction and then send it to the counterparty.
        subFlow(new FinalityFlow(signedTx, sessions));

        return signedTx;
    }
}
