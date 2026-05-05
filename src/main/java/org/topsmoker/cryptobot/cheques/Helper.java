package org.topsmoker.cryptobot.cheques;

import org.drinkless.tdlib.TdApi;

public class Helper {
    protected static final int CHEQUE_URL_LENGTH = 40;
    protected static final int CHEQUE_ID_LENGTH = 12;
    protected static final int CHEQUE_ID_OFFSET = CHEQUE_URL_LENGTH - CHEQUE_ID_LENGTH;
    private final static int ACTIVATED_URL_LENGTH = 34;


    protected static boolean isViaCryptobot(TdApi.Message message) {
        return message.viaBotUserId == Cryptobot.USER_ID;
    }


    protected static String extractChequeId(String url) {
        if (url.length() == CHEQUE_URL_LENGTH &&
                url.charAt(CHEQUE_ID_OFFSET) == 'C') {
            return url.substring(CHEQUE_ID_OFFSET);
        }
        return null;
    }
}
