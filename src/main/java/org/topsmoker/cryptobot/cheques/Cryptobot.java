package org.topsmoker.cryptobot.cheques;

import org.topsmoker.cryptobot.misc.Client;
import org.drinkless.tdlib.TdApi;

public class Cryptobot implements Activator {
    private final Client client;
    public static final long USER_ID = 1559501630;
    private final TdApi.SendMessage sendMessage;

    public Cryptobot(Client client) {
        this.client = client;
        this.sendMessage = new TdApi.SendMessage();
        client.execute(new TdApi.SearchPublicChat("@send"));
        sendMessage.chatId = USER_ID;
    }

    public void activate(String chequeId) {
        sendMessage.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText("/start " + chequeId, null),
                null,
                false
        );
        client.send(sendMessage, null);
        System.out.println("Activated: " + chequeId);
    }
}

