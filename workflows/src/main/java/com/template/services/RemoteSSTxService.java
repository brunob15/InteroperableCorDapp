package com.template.services;

import com.template.states.SSState;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.SignedTransaction;
import org.json.simple.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@CordaService
public class RemoteSSTxService extends SingletonSerializeAsToken {
    AppServiceHub serviceHub;
    private static final String SECRET_KEY = "SEGURIDAD5678";

    RemoteSSTxService(AppServiceHub serviceHub) {
        this.serviceHub = serviceHub;
    }

    public void notifyRemoteTx(SSState state, SignedTransaction tx) {
        try {
            URL url = new URL("http://localhost:3003");
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection)con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);

            byte[] body = buildBody(state, tx);
            int length = body.length;
            String signature = buildSignature(body);

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            http.setRequestProperty("x-corda-signature", signature);
            http.connect();

            try(OutputStream os = http.getOutputStream()) {
                os.write(body);
            }
        } catch (Exception e) {}

        // Do something with http.getInputStream()
    }

    private byte[] buildBody(SSState state, SignedTransaction tx) {
        JSONObject body = new JSONObject();
        JSONObject data = new JSONObject();

        String messageType = state.getMessageType();
        String packageHash = messageType.equals("request") ? state.getRequestPackageHash() : state.getResponsePackageHash();

        body.put("targetBlockchain", "fabric");
        body.put("targetContract", "socialsecurityexchange");
        data.put("sourceBlockchain", "corda");
        data.put("sourceContract", "SocialSecurity");
        data.put("requestID", state.getRequestID());
        data.put("senderInstitution", state.getSenderInstitution());
        data.put("receiverInstitution", state.getReceiverInstitution());
        data.put("messageType", messageType);
        data.put("requestType", state.getRequestType());
        data.put("status", state.getStatus());
        data.put("requestDate", state.getRequestDate());
        data.put("responseDate", state.getResponseDate());
        data.put("expectedReplyDate", state.getExpectedReplyDate());
        data.put("packageHash", packageHash);
        body.put("data", data);

        String bodyString = body.toJSONString();
        return bodyString.getBytes(StandardCharsets.UTF_8);
    }

    private String buildSignature(byte[] body)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(body));
    }
}
