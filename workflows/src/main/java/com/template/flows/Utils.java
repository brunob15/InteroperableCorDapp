package com.template.flows;

import com.template.flows.SSFlow;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;

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
}
