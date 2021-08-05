package com.template.webserver;

import com.template.flows.*;
import com.template.schema.SSSchema;
import com.template.states.SSState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.json.JsonArray;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/crosschain") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final CordaX500Name me;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);
    private final String gatewayPartyString = "O=Gateway,L=Berlin,C=DE";
    private final Party gatewayParty;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

        CordaX500Name gatewayPartyName = CordaX500Name.parse(gatewayPartyString);
        this.gatewayParty = proxy.wellKnownPartyFromX500Name(gatewayPartyName);
    }

    @PostMapping(value = "/transactions")
    private ResponseEntity<String> recordExchange(@RequestBody Event event) throws IllegalArgumentException {
        String targetContract = event.getTargetContract();
        EventData data = event.getData();

        // Create a new state using the parameters given
        try {
            Class<? extends FlowLogic<SignedTransaction>> flowClass = Utils.getMappedFlow(targetContract);

            if (flowClass != null) {
                // Start the flow. It blocks and waits for the flow to return.
                SignedTransaction result =
                        proxy.startTrackedFlowDynamic(
                            flowClass,
                            data,
                            gatewayParty
                        ).getReturnValue().get();

                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body("Transaction id " + result.getId() + " committed to ledger.\n " + result.getTx().getOutput(0));
            } else {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("The specified target transaction could not be found.");
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PostMapping(value = "/requests")
    private ResponseEntity<String> recordRequest(@RequestBody EventData data) throws IllegalArgumentException {
        data.setMessageType("request");
        return triggerTransaction(data);
    }

    @PostMapping(value = "/responses")
    private ResponseEntity<String> recordResponse(@RequestBody EventData data) throws IllegalArgumentException {
        data.setMessageType("response");
        return triggerTransaction(data);
    }

    @GetMapping(value = "/states")
    private ResponseEntity<String> getStates() throws IllegalArgumentException {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<SSState>> results = proxy.vaultQueryByCriteria(generalCriteria, SSState.class).getStates();

        JSONArray states = new JSONArray();
        for (StateAndRef<SSState> result: results) {
            SSState state = result.getState().getData();
            JSONObject stateJson = new JSONObject();
            stateJson.put("sourceBlockchain", state.getSourceBlockchain());
            stateJson.put("requestID", state.getRequestID());
            stateJson.put("receiverInstitution", state.getReceiverInstitution());
            stateJson.put("senderInstitution", state.getSenderInstitution());
            stateJson.put("messageType", state.getMessageType());
            stateJson.put("requestType", state.getRequestType());
            stateJson.put("status", state.getStatus());
            stateJson.put("requestDate", state.getRequestDate());
            stateJson.put("expectedReplyDate", state.getExpectedReplyDate());
            stateJson.put("responseDate", state.getResponseDate());
            stateJson.put("requestPackageHash", state.getRequestPackageHash());
            stateJson.put("responsePackageHash", state.getResponsePackageHash());
            states.add(stateJson);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(states.toJSONString());
    }

    @GetMapping(value = "/transactions")
    private ResponseEntity<String> getTransactions() throws IllegalArgumentException {
        List<SignedTransaction> transactions = proxy.internalVerifiedTransactionsSnapshot();

        JSONArray transactionsArray = new JSONArray();
        for (SignedTransaction tx: transactions) {
            SSState inputState = getInputState(transactions, tx);
            SSState outputState = getOutputState(tx);

            JSONObject inputStateJson = null;
            if (inputState != null) {
                inputStateJson = stateToJson(inputState);
            }
            JSONObject outputStateJson = stateToJson(outputState);
            JSONObject txJson = buildTxJson(tx, inputStateJson, outputStateJson);
            transactionsArray.add(txJson);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(transactionsArray.toJSONString());
    }

    private JSONObject stateToJson(SSState state) {
        JSONObject json = new JSONObject();
        json.put("sourceBlockchain", state.getSourceBlockchain());
        json.put("requestID", state.getRequestID());
        json.put("receiverInstitution", state.getReceiverInstitution());
        json.put("senderInstitution", state.getSenderInstitution());
        json.put("messageType", state.getMessageType());
        json.put("status", state.getStatus());
        json.put("requestDate", state.getRequestDate());
        json.put("expectedReplyDate", state.getExpectedReplyDate());
        json.put("responseDate", state.getResponseDate());
        json.put("requestPackageHash", state.getRequestPackageHash());
        json.put("responsePackageHash", state.getResponsePackageHash());

        return json;
    }

    private JSONObject buildTxJson(SignedTransaction tx, JSONObject inputState, JSONObject outputState) {
        JSONObject txJson = new JSONObject();
        txJson.put("hash", tx.getId().toString());
        txJson.put("previousTxHash", getPreviousTxHash(tx));
        txJson.put("inputState", inputState);
        txJson.put("outputState", outputState);
        return txJson;
    }

    private String getPreviousTxHash(SignedTransaction tx) {
        List<StateRef> inputs = tx.getInputs();
        return inputs.size() > 0 ? tx.getInputs().get(0).getTxhash().toString() : "";
    }

    private SSState getInputState(List<SignedTransaction> transactions, SignedTransaction tx) {
        List<StateRef> inputStates = tx.getInputs();
        if (inputStates.size() > 0) {
            SecureHash inputTxHash = inputStates.get(0).getTxhash();
            int inputStateIndex = inputStates.get(0).getIndex();
            SignedTransaction inputTx = findTxById(transactions, inputTxHash);
            if (inputTx != null) {
                return (SSState) inputTx.getTx().getOutput(inputStateIndex);
            }
        }
        return null;
    }

    private SSState getOutputState(SignedTransaction tx) {
        return (SSState) tx.getTx().getOutput(0);
    }

    private SignedTransaction findTxById(List<SignedTransaction> transactions, SecureHash txId) {
        List<SignedTransaction> txs =
            transactions.stream()
                .filter(t -> t.getId().equals(txId))
                .collect(Collectors.toList());

        return (txs.size() > 0) ? txs.get(0) : null;
    }

    private ResponseEntity<String> triggerTransaction(EventData data) {
        // Create a new state using the parameters given
        try {
            // Start the flow. It blocks and waits for the flow to return.
            SignedTransaction result =
                    proxy.startTrackedFlowDynamic(
                            CreateRemoteSSTxFlow.class,
                            data
                    ).getReturnValue().get();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id " + result.getId() + " committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(getStackTrace(e.getCause()));
        }
    }
}
