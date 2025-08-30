package org.topsmoker.cryptobot.utils;

import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.misc.Client;
import org.topsmoker.cryptobot.misc.ExecutionException;


public record ClientSetup(Client client, AuthFlow authFlow, ErrorFlow errorFlow,
                          TdApi.SetTdlibParameters tdlibParameters) {

    public interface ErrorFlow {
        void onInvalidCode();
    }

    public interface AuthFlow {
        String getPhoneNumber();

        String getPassword();

        String getCode();

        void onReady();
    }

    private boolean handleAuthorizationState(TdApi.AuthorizationState state) throws ExecutionException {
        switch (state.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> client.execute(tdlibParameters);
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR ->
                    client.execute(new TdApi.SetAuthenticationPhoneNumber(authFlow.getPhoneNumber(), null));

            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                TdApi.Object result = client.execute(new TdApi.CheckAuthenticationCode(authFlow.getCode()));
                if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
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

    public void setOptimizedOptions() {
        client.send(new TdApi.SetOption("always_parse_markdown", new TdApi.OptionValueBoolean(false)), null);
        client.send(new TdApi.SetOption("ignore_sensitive_content_restrictions", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("disable_top_chats", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("disable_time_adjustment_protection", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("disable_sent_scheduled_message_notifications", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("disable_contact_registered_notifications", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("disable_animated_emoji", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("ignore_background_updates", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("disable_persistent_network_statistics", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("disable_network_statistics", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("ignore_file_names", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("use_storage_optimizer", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("ignore_inline_thumbnails", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("use_quick_ack", new TdApi.OptionValueBoolean(true)), null);
        client.send(new TdApi.SetOption("message_unload_delay", new TdApi.OptionValueInteger(0)), null);
        client.send(new TdApi.SetOption("use_pfs", new TdApi.OptionValueBoolean(false)), null);
        client.send(new TdApi.SetOption("online", new TdApi.OptionValueBoolean(false)), null);
    }

    public void auth() throws InterruptedException {
        boolean authorized = false;
        while (!authorized) {
            try {
                TdApi.Object result = client.execute(new TdApi.GetAuthorizationState());
                authorized = handleAuthorizationState(
                        ((TdApi.AuthorizationState) result)
                );
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

        }
    }
}

