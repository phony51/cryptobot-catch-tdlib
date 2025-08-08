package org.topsmoker.cryptobot.utils;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class ClientAuth {
    private final SyncClient client;
    private final AuthFlow authFlow;
    private final ErrorFlow errorFlow;
    private final TdApi.SetTdlibParameters tdlibParameters;

    public ClientAuth(Client client,
                      AuthFlow authFlow, ErrorFlow errorFlow,
                      TdApi.SetTdlibParameters tdlibParameters) {
        this.client = new SyncClient(client);
        this.authFlow = authFlow;
        this.errorFlow = errorFlow;
        this.tdlibParameters = tdlibParameters;
    }

    public interface ErrorFlow {
        void onInvalidCode();
    }

    public interface AuthFlow {
        String getPhoneNumber();

        String getPassword();

        String getCode();

        void onReady();
    }

    private boolean handleAuthorizationState(TdApi.AuthorizationState state) throws SyncClient.ExecutionException {
        switch (state.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> client.execute(tdlibParameters);
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR ->
                    client.execute(new TdApi.SetAuthenticationPhoneNumber(authFlow.getPhoneNumber(), null));

            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                try {
                    client.execute(new TdApi.CheckAuthenticationCode(authFlow.getCode()));
                } catch (SyncClient.ExecutionException e) {
                    errorFlow.onInvalidCode();
                }
            }

            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR ->
                    client.execute(new TdApi.CheckAuthenticationPassword(authFlow.getPassword()));
            case TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                authFlow.onReady();
                return true;
            }
        }
        return false;
    }

    public void auth() throws SyncClient.ExecutionException {
        boolean authorized = false;
        while (!authorized) {
            authorized = handleAuthorizationState(
                    ((TdApi.AuthorizationState) client.execute(new TdApi.GetAuthorizationState()))
            );
        }
    }
}

