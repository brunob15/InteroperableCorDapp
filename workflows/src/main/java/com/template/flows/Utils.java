package com.template.flows;

import com.template.contracts.TemplateContract;
import com.template.schema.SSSchema;
import com.template.states.SSState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Utils {
    /*
    * This method takes a string which represents a transaction to be executed
    * and returns the class of the flow that executes that particular transaction.
    * */
    public static Class<? extends FlowLogic<SignedTransaction>> getMappedFlow(String targetTx) {
        switch (targetTx) {
            case "SocialSecurityExchange":
                return SSFlow.class;
            default:
                return null;
        }
    }

    public static StateAndRef<SSState> getStateByRequestId(String requestID, ServiceHub serviceHub) {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        Field requestIDField;
        try {
            requestIDField = SSSchema.PersistentSS.class.getDeclaredField("requestID");
            CriteriaExpression<Object, Boolean> requestIDIndex = Builder.equal(requestIDField, requestID);
            QueryCriteria customCriteria = new QueryCriteria.VaultCustomQueryCriteria(requestIDIndex);
            QueryCriteria criteria = generalCriteria.and(customCriteria);
            VaultService vaultService = serviceHub.getVaultService();
            Vault.Page<SSState> results = vaultService.queryBy(SSState.class, criteria);
            List<StateAndRef<SSState>> states = results.getStates();

            if (states.size() > 0) {
                return states.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<AbstractParty> getAllParties(CordaX500Name ownName, ServiceHub serviceHub) {
        Iterable<PartyAndCertificate> identities = serviceHub.getIdentityService().getAllIdentities();
        Iterator<PartyAndCertificate> iterator = identities.iterator();

        List<AbstractParty> allParties = new ArrayList<>();
        PartyAndCertificate party;
        while (iterator.hasNext()) {
            party = iterator.next();
            allParties.add(party.getParty());
        }

        return allParties;
    }

//    public SignedTransaction buildSignedTx(
//        StateAndRef<SSState> refRequestState,
//        SSState outputState,
//        PublicKey publicKey,
//        Party notary,
//        ServiceHub serviceHub
//    ) {
//        Command<TemplateContract.Commands.Send> command = new Command<>(new TemplateContract.Commands.Send(), publicKey);
//
//        // We create a transaction builder and add the components.
//        TransactionBuilder txBuilder = new TransactionBuilder(notary);
//        if (refRequestState != null) {
//            txBuilder.addInputState(refRequestState);
//        }
//        txBuilder.addOutputState(outputState, TemplateContract.ID)
//                .addCommand(command);
//
//        // Signing the transaction.
//        return serviceHub.signInitialTransaction(txBuilder);
//    }

    public StateAndRef<SSState> populateResponse(
            String messageType,
            String requestID,
            CreateRemoteSSTxFlow flow,
            ServiceHub serviceHub
    ) {
        StateAndRef<SSState> refRequestState = null;
        SSState requestState;
        if (messageType.equals("response")) {
            refRequestState = Utils.getStateByRequestId(requestID, serviceHub);
            if (refRequestState != null) {
                requestState = refRequestState.getState().getData();
                flow.senderInstitution = requestState.getReceiverInstitution();
                flow.receiverInstitution = requestState.getSenderInstitution();
                flow.requestType = requestState.getRequestType();
                flow.status = "replied";
                flow.requestDate = requestState.getRequestDate();
                flow.expectedReplyDate = requestState.getExpectedReplyDate();
                flow.requestPackageHash = requestState.getRequestPackageHash();
                flow.responsePackageHash = flow.packageHash;
            }
        } else {
            flow.requestPackageHash = flow.packageHash;
        }

        return refRequestState;
    }
}
