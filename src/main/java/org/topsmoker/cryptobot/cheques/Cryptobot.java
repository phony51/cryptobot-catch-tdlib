package org.topsmoker.cryptobot.cheques;

import org.topsmoker.cryptobot.misc.Client;
import org.drinkless.tdlib.TdApi;

public class Cryptobot implements Activator {
    private final Client client;
    private final int retryCount;
    public static final long USER_ID = 1559501630;
    private final TdApi.SendMessage sendMessage;

    public Cryptobot(Client client, int retryCount) {
        this.client = client;
        this.retryCount = retryCount;
        this.sendMessage = new TdApi.SendMessage();
        client.send(new TdApi.SearchPublicChat("@send"), null);
        sendMessage.chatId = USER_ID;
    }

    public void activate(String chequeId) {
        StringBuilder startBuilder = new StringBuilder("/start ");
        sendMessage.inputMessageContent = new TdApi.InputMessageText(new TdApi.FormattedText(startBuilder.append(chequeId).toString(), null),
                null,
                false
        );
        for (int i = 0; i < retryCount; i++) {
            client.send(sendMessage, null);
        }
        System.out.println("Activated: " + chequeId);
    }
}

