package ocean.RedWhale;

/**
 * チャットのメッセージ1件分を表すデータクラスです。
 */
public class ChatMessage {
    private String message; // メッセージの本文（テキスト）
    private boolean isSentByUser; // 自分が送信したメッセージならtrue、相手から届いたならfalse

    /**
     * メッセージオブジェクトを作るためのコンストラクタです。
     * @param message メッセージの文字
     * @param isSentByUser 自分が送った場合はtrue、受け取った場合はfalse
     */
    public ChatMessage(String message, boolean isSentByUser) {
        this.message = message;
        this.isSentByUser = isSentByUser;
    }

    /**
     * @return メッセージの本文を返します
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return 自分の送信かどうかを返します
     */
    public boolean isSentByUser() {
        return isSentByUser;
    }
}
