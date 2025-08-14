package org.topsmoker.cryptobot;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.clients.Cryptobot;
import org.topsmoker.cryptobot.clients.ChequeHandler;
import org.topsmoker.cryptobot.config.Config;
import org.topsmoker.cryptobot.config.Credentials;
import org.topsmoker.cryptobot.config.ResourceLoader;
import org.topsmoker.cryptobot.utils.ClientSetup;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        Config config = (new ResourceLoader()).load();
        TdApi.SetTdlibParameters baseTdlibParameters = new TdApi.SetTdlibParameters();
        baseTdlibParameters.useFileDatabase = false;
        baseTdlibParameters.useChatInfoDatabase = false;
        baseTdlibParameters.useMessageDatabase = false;
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
        ClientSetup activatorClientSetup = new ClientSetup(activatorClient,
                new MyAuthFlow(
                        activatorCredentials.getPhone(),
                        activatorCredentials.getPassword()
                ),
                new MyErrorFlow(), baseTdlibParameters);
        activatorClientSetup.setOptimizedOptions();
        activatorClientSetup.auth();


        Cryptobot cryptobot = new Cryptobot(activatorClient, config.getActivator().getRetryCount());

        baseTdlibParameters.databaseDirectory = "db/catcher";
        Credentials catcherCredentials = config.getCatcher().getCredentials();

        ReentrantLock lock = new ReentrantLock();
        Condition liveCondition = lock.newCondition();

        try (ChequeHandler handler = new ChequeHandler(cryptobot, config.getCatcher().getPollingPeriodMs(), config.getCatcher().getPollingTimeoutMs())) {
            Client catcherClient = Client.create(handler, null, null);

            ClientSetup catcherClientSetup = new ClientSetup(catcherClient,
                    new MyAuthFlow(
                            catcherCredentials.getPhone(),
                            catcherCredentials.getPassword()
                    ),
                    new MyErrorFlow(), baseTdlibParameters);
            catcherClientSetup.setOptimizedOptions();
            catcherClientSetup.auth();
            handler.setClient(catcherClient);

            lock.lock();
            liveCondition.await(); // TODO
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
