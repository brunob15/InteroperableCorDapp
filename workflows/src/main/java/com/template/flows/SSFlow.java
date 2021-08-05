package com.template.flows;

import com.template.contracts.TemplateContract;
import com.template.schema.SSSchema;
import com.template.states.SSState;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.contracts.Command;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.FieldInfo;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.corda.core.node.services.vault.QueryCriteriaUtils.getField;

@InitiatingFlow
@StartableByRPC
public class SSFlow extends FlowLogic<SignedTransaction> {
    private String sourceBlockchain = "Corda";
    private String sourceContract = "SocialSecurity";
    private String requestID;
    private String senderInstitution;
    private String receiverInstitution;
    private String messageType;
    private String requestType;
    private String status;
    private String requestDate;
    private String expectedReplyDate;
    private String responseDate;
    private String packageHash;
    private String requestPackageHash;
    private String responsePackageHash;
    private final Party otherParty;

    /**
     * The progress tracker provides checkpoints indicating the progress of the flow to observers.
     */
    private final ProgressTracker progressTracker = new ProgressTracker();

    public SSFlow(EventData eventData, Party otherParty) {
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

        ServiceHub serviceHub = getServiceHub();
        String gatewayPartyString = "O=Gateway,L=Berlin,C=DE";
        CordaX500Name gatewayPartyName = CordaX500Name.parse(gatewayPartyString);
        Party gatewayParty = serviceHub.getIdentityService().wellKnownPartyFromX500Name(gatewayPartyName);

        List<AbstractParty> participants = new ArrayList<>();
        participants.add(getOurIdentity());
        participants.add(gatewayParty);

        StateAndRef<SSState> refRequestState = null;
        SSState requestState;
        if (messageType.equals("response")) {
            refRequestState = Utils.getStateByRequestId(requestID, getServiceHub());
            if (refRequestState != null) {
                requestState = refRequestState.getState().getData();
                this.senderInstitution = requestState.getReceiverInstitution();
                this.receiverInstitution = requestState.getSenderInstitution();
                this.requestType = requestState.getRequestType();
                this.status = "replied";
                this.requestDate = requestState.getRequestDate();
                this.expectedReplyDate = requestState.getExpectedReplyDate();
                this.requestPackageHash = requestState.getRequestPackageHash();
                this.responsePackageHash = this.packageHash;
            }
        } else {
            this.requestPackageHash = this.packageHash;
        }

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
            participants
        );
        Command command = new Command<>(new TemplateContract.Commands.Send(), getOurIdentity().getOwningKey());

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary);
        if (refRequestState != null) {
            txBuilder.addInputState(refRequestState);
        }
        txBuilder.addOutputState(outputState, TemplateContract.ID)
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
