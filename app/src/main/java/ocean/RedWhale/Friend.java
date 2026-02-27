package ocean.RedWhale;

/**
 * ユーザーの友達リスト（連絡先）内の1人を表すクラスです。
 * Bluetoothデバイスの基本情報と、暗号化に使われる公開鍵（アイデンティティ）を含みます。
 */
public class Friend {
    private int id; // データベース上のID
    private String name; // 表示名
    private String address; // BluetoothのMACアドレス
    private String identityAddress; // 暗号化用の公開鍵（アドレス）

    /**
     * すべての項目を指定するコンストラクタです。
     */
    public Friend(int id, String name, String address, String identityAddress) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.identityAddress = identityAddress;
    }

    /**
     * IDを指定しないコンストラクタ（データベースに保存する前の新規作成用）です。
     */
    public Friend(String name, String address, String identityAddress) {
        this.name = name;
        this.address = address;
        this.identityAddress = identityAddress;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getIdentityAddress() {
        return identityAddress;
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

    public void setIdentityAddress(String identityAddress) {
        this.identityAddress = identityAddress;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", identityAddress='" + identityAddress + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return address != null ? address.equals(friend.address) : friend.address == null;
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}
