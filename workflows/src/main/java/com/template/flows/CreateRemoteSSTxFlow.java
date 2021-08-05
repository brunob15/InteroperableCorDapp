package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.TemplateContract;
import com.template.services.RemoteSSTxService;
import com.template.states.SSState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.helpers.BasicMarker;

@InitiatingFlow
@StartableByRPC
public class CreateRemoteSSTxFlow extends FlowLogic<SignedTransaction> {
    public String sourceBlockchain = "Corda";
    public String sourceContract = "SocialSecurity";
    public String requestID;
    public String senderInstitution;
    public String receiverInstitution;
    public String messageType;
    public String requestType;
    public String status;
    public String requestDate;
    public String expectedReplyDate;
    public String responseDate;
    public String packageHash;
    public String requestPackageHash;
    public String responsePackageHash;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public CreateRemoteSSTxFlow(
        String requestID,
        String senderInstitution,
        String receiverInstitution,
        String messageType,
        String requestType,
        String status,
        String requestDate,
        String expectedReplyDate,
        String responseDate,
        String packageHash
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
        this.packageHash = packageHash;
    }

    public CreateRemoteSSTxFlow(EventData eventData) {
        this.requestID = eventData.getRequestID();
        this.senderInstitution = eventData.getSenderInstitution();
        this.receiverInstitution = eventData.getReceiverInstitution();
        this.messageType = eventData.getMessageType();
        this.requestType = eventData.getRequestType();
        this.status = eventData.getStatus();
        this.requestDate = eventData.getRequestDate();
        this.expectedReplyDate = eventData.getExpectedReplyDate();
        this.responseDate = eventData.getResponseDate();
        this.packageHash = eventData.getPackageHash();
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
        getLogger().info("Flow started");

        Utils utils = new Utils();
        ServiceHub serviceHub = getServiceHub();
        Party self = getOurIdentity();
        List<AbstractParty> allParties = utils.getAllParties(self.nameOrNull(), serviceHub);

        // We retrieve the notary identity from the network map.
        Party notary = serviceHub.getNetworkMapCache().getNotaryIdentities().get(0);
        StateAndRef<SSState> refRequestState = utils.populateResponse(messageType, requestID, this, serviceHub);

        // We create the transaction components.
        SSState outputState = new SSState(
            sourceBlockchain,
            sourceContract,
            requestID,
            senderInstitution,
            receiverInstitution,
            messageType,
            requestType,
            status,
            requestDate,
            expectedReplyDate,
            responseDate,
            requestPackageHash,
            responsePackageHash,
            allParties
        );

        Command<TemplateContract.Commands.Send> command = new Command<>(new TemplateContract.Commands.Send(), self.getOwningKey());

        getLogger().info("Building and signing transaction");
        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary);
        if (refRequestState != null) {
            txBuilder.addInputState(refRequestState);
        }
        txBuilder.addOutputState(outputState, TemplateContract.ID)
                .addCommand(command);

        // Signing the transaction.
        SignedTransaction signedTx = serviceHub.signInitialTransaction(txBuilder);

        List<FlowSession> sessions = new ArrayList<>();
        for (AbstractParty pty: allParties) {
            CordaX500Name partyName = pty.nameOrNull();
            if (partyName != null && !partyName.equals(self.getName())) {
                FlowSession session = initiateFlow(pty);
                sessions.add(session);
            }
        }

        try {
            // Finalise the transaction and then send it to the counterparty.
            getLogger().info("Forwarding transaction to peers");
            subFlow(new FinalityFlow(signedTx, sessions));
            getLogger().info("Transaction signed by all peers");
        } catch (FlowException e) {
            Exception er = e;
        }

        getLogger().info("Triggering cross-chain event");
        RemoteSSTxService service = serviceHub.cordaService(RemoteSSTxService.class);
        service.notifyRemoteTx(outputState, signedTx);

        return signedTx;
    }
}
