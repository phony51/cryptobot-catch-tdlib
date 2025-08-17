package org.topsmoker.cryptobot.cases;


import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.UpdateNewMessage;
import org.drinkless.tdlib.TdApi.UpdateMessageEdited;
import org.topsmoker.cryptobot.cheques.Cryptobot;

public class Updates {
    static final String CHEQUE_ID = "CQkueAMl2POI";

    public static UpdateNewMessage getRegexCheque() {
        TdApi.Message m = new TdApi.Message();
        TdApi.MessageText t = new TdApi.MessageText();
        t.text = new TdApi.FormattedText();
        t.text.text = """
                \u200B (https://imggen.send.tg/checks/image?asset=USDT&asset_amount=0.01999&fiat=RUB&fiat_amount=1.59&main=asset&v2)Чек
                
                Сумма: \uD83E\uDE99 0.019994 USDT (1.59 RUB)
                
                Любой может активировать этот чек.
                
                Скопируйте ссылку, чтобы поделиться чеком:
                t.me/send?start="""+CHEQUE_ID+"""
                
                ⚠️ Никогда не делайте скриншот вашего чека и не отправляйте его никому! Ссылку на чек могут использовать мошенники, чтобы получить доступ к вашим средствам.""";
        m.content = t;
        return new UpdateNewMessage(m);
    }

    public static UpdateNewMessage getForwardedCheque() {
        TdApi.ReplyMarkupInlineKeyboard rm = new TdApi.ReplyMarkupInlineKeyboard();
        rm.rows = new TdApi.InlineKeyboardButton[][] {
                new TdApi.InlineKeyboardButton[] {
                        new TdApi.InlineKeyboardButton("Чек", new TdApi.InlineKeyboardButtonTypeUrl("http://t.me/send?start="+CHEQUE_ID))
                }
        };
        TdApi.Message m = new TdApi.Message();
        m.viaBotUserId = Cryptobot.USER_ID;
        m.replyMarkup = rm;
        return new UpdateNewMessage(m);
    }

    public static UpdateMessageEdited getInlineCheque() {
        TdApi.ReplyMarkupInlineKeyboard rm = new TdApi.ReplyMarkupInlineKeyboard();
        rm.rows = new TdApi.InlineKeyboardButton[][] {
                new TdApi.InlineKeyboardButton[] {
                        new TdApi.InlineKeyboardButton("Чек", new TdApi.InlineKeyboardButtonTypeUrl("http://t.me/send?start="+CHEQUE_ID))
                }
        };
        UpdateMessageEdited u = new UpdateMessageEdited();
        u.replyMarkup = rm;
        return u;
    }
}
