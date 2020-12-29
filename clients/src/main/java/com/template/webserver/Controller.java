package com.template.webserver;

import com.template.flows.SSFlow;
import com.template.states.SSState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/socialsecurity") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final CordaX500Name me;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);
    private final String gatewayPartyString = "O=Gateway,L=Montevideo,C=UY";
    private final Party gatewayParty;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

        CordaX500Name gatewayPartyName = CordaX500Name.parse(gatewayPartyString);
        this.gatewayParty = proxy.wellKnownPartyFromX500Name(gatewayPartyName);
    }

    @GetMapping(value = "/me", produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami() {
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    @GetMapping(value = "/exchanges", produces = APPLICATION_JSON_VALUE)
    private List<StateAndRef<SSState>> getExchanges() {
        return proxy.vaultQuery(SSState.class).getStates();
    }

    @PostMapping(value = "/exchanges", produces = TEXT_PLAIN_VALUE, headers = "Content-Type=application/x-www-form-urlencoded")
    private ResponseEntity<String> recordExchange(HttpServletRequest request) throws IllegalArgumentException {
        String exchangeType = request.getParameter("exchangeType");
        String messageType = request.getParameter("messageType");

        // Create a new SSState using the parameters given
        try {
            // Start the SSFlow. It blocks and waits for the flow to return.
            SignedTransaction result =
                proxy.startTrackedFlowDynamic(SSFlow.class, exchangeType, messageType, gatewayParty)
                     .getReturnValue()
                     .get();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id " + result.getId() + " committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}
