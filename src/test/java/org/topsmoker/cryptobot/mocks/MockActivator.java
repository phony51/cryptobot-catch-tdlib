package org.topsmoker.cryptobot.mocks;

import org.topsmoker.cryptobot.cheques.Activator;

import java.util.concurrent.Phaser;

public class MockActivator implements Activator {
    public MockActivator() {
    }

    @Override
    public void activate(String chequeId) {}
}
