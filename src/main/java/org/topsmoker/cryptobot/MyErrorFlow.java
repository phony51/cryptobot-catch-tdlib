package org.topsmoker.cryptobot;

import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.utils.ClientAuth;

public class MyErrorFlow implements ClientAuth.ErrorFlow {

    @Override
    public void onInvalidCode() {
        System.out.println("Entered invalid code");
    }

}
