package org.topsmoker.cryptobot.clients;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

public class Cryptobot {
    private final Client client;
    public static final long USER_ID = 1559501630;

    public Cryptobot(Client client) {
        this.client = client;
        client.send(new TdApi.GetChat(USER_ID), null);
    }

    public void activate(String chequeId) {
        TdApi.SendMessage sendMessage = new TdApi.SendMessage();
        sendMessage.chatId = USER_ID;
        sendMessage.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("/start " + chequeId, null),
                null,
                false
        );
        client.send(sendMessage, null); // TODO
    }
}
