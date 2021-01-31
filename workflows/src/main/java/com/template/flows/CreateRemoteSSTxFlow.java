package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.TemplateContract;
import com.template.services.RemoteSSTxService;
import com.template.states.SSState;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

@InitiatingFlow
@StartableByRPC
public class CreateRemoteSSTxFlow extends FlowLogic<SignedTransaction> {
    private final String sourceBlockchain = "Corda";
    private final String sourceContract = "SocialSecurity";
    private final String exchangeType;
    private final String messageType;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public CreateRemoteSSTxFlow(String exchangeType, String messageType) {
        this.exchangeType = exchangeType;
        this.messageType = messageType;
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
        ServiceHub serviceHub = getServiceHub();

        String gatewayPartyString = "O=Gateway,L=Montevideo,C=UY";
        CordaX500Name gatewayPartyName = CordaX500Name.parse(gatewayPartyString);
        Party gatewayParty = serviceHub.getIdentityService().wellKnownPartyFromX500Name(gatewayPartyName);

        // We retrieve the notary identity from the network map.
        Party notary = serviceHub.getNetworkMapCache().getNotaryIdentities().get(0);

        // We create the transaction components.
        String txId = null;
        SSState outputState = new SSState(
            txId,
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
        FlowSession otherPartySession = initiateFlow(gatewayParty);

        // We finalise the transaction and then send it to the counterparty.
        subFlow(new FinalityFlow(signedTx, otherPartySession));

        RemoteSSTxService service = serviceHub.cordaService(RemoteSSTxService.class);
        service.notifyRemoteTx(outputState, signedTx);

        return signedTx;
    }
}
