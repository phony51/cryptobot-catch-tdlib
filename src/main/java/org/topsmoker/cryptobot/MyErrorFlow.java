package org.topsmoker.cryptobot;

import org.topsmoker.cryptobot.utils.ClientSetup;

public class MyErrorFlow implements ClientSetup.ErrorFlow {

    @Override
    public void onInvalidCode() {
        System.out.println("Entered invalid code");
    }

}
