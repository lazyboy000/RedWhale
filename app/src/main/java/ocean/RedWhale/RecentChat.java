package ocean.RedWhale;

/**
 * 最近のチャットリストの項目を表すデータモデルクラスです。
 */
public class RecentChat {
    private String friendName;    // 友達の名前
    private String lastMessage;   // 最後のメッセージ
    private long timestamp;       // 最後のメッセージのタイムスタンプ
    private String friendAddress; // 友達のBluetoothアドレス

    public RecentChat(String friendName, String lastMessage, long timestamp, String friendAddress) {
        this.friendName = friendName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.friendAddress = friendAddress;
    }

    public String getFriendName() {
        return friendName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFriendAddress() {
        return friendAddress;
    }
}
