package org.topsmoker.cryptobot;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.clients.Cryptobot;
import org.topsmoker.cryptobot.clients.InlineChequeHandler;
import org.topsmoker.cryptobot.config.Config;
import org.topsmoker.cryptobot.config.Credentials;
import org.topsmoker.cryptobot.config.ResourceLoader;
import org.topsmoker.cryptobot.utils.ClientAuth;
import org.topsmoker.cryptobot.utils.SyncClient;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class Main {
    public static void main(String[] args) throws SyncClient.ExecutionException, InterruptedException {
        Config config = (new ResourceLoader()).load();

        TdApi.SetTdlibParameters baseTdlibParameters = new TdApi.SetTdlibParameters();
        baseTdlibParameters.filesDirectory = "tdlib";
        baseTdlibParameters.useFileDatabase = true;
        baseTdlibParameters.useSecretChats = false;
        baseTdlibParameters.apiId = config.getApiId();
        baseTdlibParameters.apiHash = config.getApiHash();
        baseTdlibParameters.systemLanguageCode = "en";
        baseTdlibParameters.deviceModel = "iPhone 12 Mini";
        baseTdlibParameters.systemVersion = "18.6";
        baseTdlibParameters.applicationVersion = "1.0.0";

        baseTdlibParameters.databaseDirectory = "db/activator";

        TdApi.SetLogVerbosityLevel setLogVerbosityLevel = new TdApi.SetLogVerbosityLevel(1);

        Credentials activatorCredentials = config.getActivator().getCredentials();
        Client activatorClient = Client.create(null, null, null);
        activatorClient.send(setLogVerbosityLevel, null);
        new ClientAuth(activatorClient, new MyAuthFlow(activatorCredentials.getPhone(), activatorCredentials.getPassword()),
                new MyErrorFlow(), baseTdlibParameters).auth();


        Cryptobot cryptobot = new Cryptobot(activatorClient);

        baseTdlibParameters.databaseDirectory = "db/catcher";
        Credentials catcherCredentials = config.getCatcher().getCredentials();
        InlineChequeHandler handler = new InlineChequeHandler(cryptobot);
        Client catcherClient = Client.create(handler, null, null);

        new ClientAuth(catcherClient, new MyAuthFlow(catcherCredentials.getPhone(), catcherCredentials.getPassword()),
                new MyErrorFlow(), baseTdlibParameters).auth();
        handler.setClient(catcherClient);

        ReentrantLock lock = new ReentrantLock();
        Condition loopCondition = lock.newCondition();
        lock.lock();
        try {
            loopCondition.await();
        }  finally {
            lock.unlock();
        }
    }
}
