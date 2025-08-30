package org.topsmoker.cryptobot;

import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.cheques.Cryptobot;
import org.topsmoker.cryptobot.cheques.ChequeHandler;
import org.topsmoker.cryptobot.cheques.PollingService;
import org.topsmoker.cryptobot.config.Config;
import org.topsmoker.cryptobot.config.Credentials;
import org.topsmoker.cryptobot.config.ResourceLoader;
import org.topsmoker.cryptobot.utils.ClientSetup;
import org.topsmoker.cryptobot.utils.TDLibClient;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class Main {
    public static void main(String[] args) throws Exception {
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
        TDLibClient activatorClient = new TDLibClient(null);
        activatorClient.send(setLogVerbosityLevel, null);
        ClientSetup activatorClientSetup = new ClientSetup(activatorClient,
                new MyAuthFlow(
                        activatorCredentials.getPhone(),
                        activatorCredentials.getPassword()
                ),
                new MyErrorFlow(), baseTdlibParameters);
        activatorClientSetup.setOptimizedOptions();
        activatorClientSetup.auth();


        Cryptobot cryptobot = new Cryptobot(activatorClient);

        baseTdlibParameters.databaseDirectory = "db/catcher";
        Config.Catcher catcherConfig = config.getCatcher();

        Credentials catcherCredentials = catcherConfig.getCredentials();

        ReentrantLock lock = new ReentrantLock();
        Condition liveCondition = lock.newCondition();

        TDLibClient catcherClient = new TDLibClient(null);
        ClientSetup catcherClientSetup = new ClientSetup(catcherClient,
                new MyAuthFlow(
                        catcherCredentials.getPhone(),
                        catcherCredentials.getPassword()
                ),
                new MyErrorFlow(), baseTdlibParameters);

        catcherClientSetup.setOptimizedOptions();
        catcherClientSetup.auth();

        try (ChequeHandler handler = new ChequeHandler(cryptobot,
                new PollingService(catcherClient, cryptobot, catcherConfig.getPollingPeriodMs(), catcherConfig.getPollingTimeoutMs()))) {
            catcherClient.setUpdateHandler(handler);

            lock.lock();
            try {
                liveCondition.await(); // TODO
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }

    }
}
