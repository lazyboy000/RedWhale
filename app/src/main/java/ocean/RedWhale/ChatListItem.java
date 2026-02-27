package ocean.RedWhale;

/**
 * チャット一覧の1項目（1つの会話）のデータを保持するクラスです。
 * 画面に表示するための名前やメッセージ、アイコン情報を持ちます。
 */
public class ChatListItem {
    private String name;        // 連絡先の名前
    private String lastMessage; // 最後に送受信したメッセージ
    private String timestamp;   // 最後のメッセージの時間（表示用）
    private int avatarResId;    // プロフィール画像のアイコン（リソースID）

    /**
     * コンストラクタ：チャット項目の情報をセットします。
     */
    public ChatListItem(String name, String lastMessage, String timestamp, int avatarResId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.avatarResId = avatarResId;
    }

    public String getName() {
        return name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getAvatarResId() {
        return avatarResId;
    }
}