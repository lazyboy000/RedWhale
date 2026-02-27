package ocean.RedWhale;

/**
 * チャット一覧画面に表示する、1つの会話（履歴）の情報を表すクラスです。
 * 相手の名前、最後のメッセージ、送信時間などをまとめて保持します。
 */
public class RecentChat {
    private String friendName;     // 友達の表示名
    private String lastMessage;    // 最後にやり取りしたメッセージ
    private long timestamp;        // メッセージが送られた時刻（ミリ秒）
    private String friendAddress;  // BluetoothのMACアドレス
    private String identityAddress; // 相手の公開鍵（アイデンティティ）

    /**
     * コンストラクタ。チャットの履歴データをセットします。
     */
    public RecentChat(String friendName, String lastMessage, long timestamp, String friendAddress, String identityAddress) {
        this.friendName = friendName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.friendAddress = friendAddress;
        this.identityAddress = identityAddress;
    }

    /**
     * @return 相手の名前を返します
     */
    public String getFriendName() {
        return friendName;
    }

    /**
     * @return 最後のメッセージ内容を返します
     */
    public String getLastMessage() {
        return lastMessage;
    }

    /**
     * @return メッセージの時刻を返します
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return 相手のMACアドレスを返します
     */
    public String getFriendAddress() {
        return friendAddress;
    }

    /**
     * @return 相手の公開鍵を返します
     */
    public String getIdentityAddress() {
        return identityAddress;
    }
}
