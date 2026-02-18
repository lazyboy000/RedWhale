package ocean.RedWhale;

/**
 * ユーザーの友達リスト内の連絡先を表すクラスです。
 * Bluetoothデバイスの連絡先に関する基本情報を含みます。
 */
public class Friend {
    private int id; // データベース上のID
    private String name; // 表示名
    private String address; // BluetoothのMACアドレス

    /**
     * すべてのフィールドを持つコンストラクタ。
     * @param id 友達のデータベースID
     * @param name 友達の表示名
     * @param address 友達のデバイスのBluetooth MACアドレス
     */
    public Friend(int id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    /**
     * IDなしのコンストラクタ（データベースに挿入する前の新しい友達を作成するため）。
     * @param name 友達の表示名
     * @param address 友達のデバイスのBluetooth MACアドレス
     */
    public Friend(String name, String address) {
        this.name = name;
        this.address = address;
    }

    //Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

    /**
     * MACアドレスに基づいて二つのFriendオブジェクトが等しいかを判断します。
     * @param o 比較対象のオブジェクト
     * @return アドレスが同じであればtrue
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Friend friend = (Friend) o;

        return address != null ? address.equals(friend.address) : friend.address == null;
    }

    /**
     * オブジェクトのハッシュコードを返します。MACアドレスに基づいています。
     * @return ハッシュコード
     */
    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}