package ocean.RedWhale;

/**
 * チャットリストの単一の項目（一つの会話）を表すデータモデルクラスです。
 */
public class ChatListItem {
    private String name; // 連絡先名
    private String lastMessage; // 最後のメッセージ
    private String timestamp; // 最後のメッセージのタイムスタンプ
    private int avatarResId; // アバター画像のリソースID

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