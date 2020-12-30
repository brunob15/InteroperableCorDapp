package com.template.webserver;

import com.template.flows.Utils;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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
    private final String gatewayPartyString = "O=Gateway,L=Montevideo,C=UY";
    private final Party gatewayParty;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

        CordaX500Name gatewayPartyName = CordaX500Name.parse(gatewayPartyString);
        this.gatewayParty = proxy.wellKnownPartyFromX500Name(gatewayPartyName);
    }

    @PostMapping(value = "/transactions", produces = TEXT_PLAIN_VALUE, headers = "Content-Type=application/x-www-form-urlencoded")
    private ResponseEntity<String> recordExchange(HttpServletRequest request) throws IllegalArgumentException {
        String sourceTxId = request.getParameter("sourceTxId");
        String sourceBlockchain = request.getParameter("sourceBlockchain");
        String sourceContract = request.getParameter("sourceContract");
        String targetTx = request.getParameter("targetTx");
        String exchangeType = request.getParameter("exchangeType");
        String messageType = request.getParameter("messageType");

        // TODO: GET ALL PARAMETERS AND PASS THEM TO THE FLOW
        Map<String, String[]> parameters = request.getParameterMap();

        // Create a new state using the parameters given
        try {
            Class<? extends FlowLogic<SignedTransaction>> flowClass = Utils.getMappedFlow(targetTx);

            if (flowClass != null) {
                // Start the flow. It blocks and waits for the flow to return.
                SignedTransaction result =
                        proxy.startTrackedFlowDynamic(
                            flowClass,
                            sourceTxId,
                            sourceBlockchain,
                            sourceContract,
                            exchangeType,
                            messageType,
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
}
