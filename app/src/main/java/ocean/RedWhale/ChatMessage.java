package ocean.RedWhale;

/**
 * 単一のチャットメッセージを表すデータモデルクラスです。
 */
public class ChatMessage {
    private String message; // メッセージの本文
    private boolean isSentByUser; // このメッセージが現在のユーザーによって送信されたかどうか

    /**
     * ChatMessageのコンストラクタ
     * @param message メッセージのテキスト
     * @param isSentByUser 現在のユーザーが送信した場合はtrue、受信した場合はfalse
     */
    public ChatMessage(String message, boolean isSentByUser) {
        this.message = message;
        this.isSentByUser = isSentByUser;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSentByUser() {
        return isSentByUser;
    }
}
