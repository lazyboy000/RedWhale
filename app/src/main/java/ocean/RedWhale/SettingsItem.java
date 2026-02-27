package ocean.RedWhale;

/**
 * 設定画面のリスト（RecyclerView）に表示する、1つ1つの「設定項目」のデータをまとめたクラスです。
 * アイコン、タイトル、サブタイトル（説明）の3つの情報を持ちます。
 */
public class SettingsItem {
    // 画面に表示するアイコンの画像リソースID（例：R.drawable.ic_menu_preferences）
    private int iconResId;    
    
    // 項目の大きな文字（例："Language" や "Security"）
    private String title;     
    
    // 項目の下にある小さな説明文（例："アプリの言語を変更します"）
    private String subtitle;  

    /**
     * コンストラクタ：設定項目を新しく作るときに情報をセットします。
     *
     * @param iconResId アイコン画像のID
     * @param title     項目のタイトル
     * @param subtitle  項目の説明
     */
    public SettingsItem(int iconResId, String title, String subtitle) {
        this.iconResId = iconResId;
        this.title = title;
        this.subtitle = subtitle;
    }

    /**
     * @return アイコン画像のIDを返します
     */
    public int getIconResId() {
        return iconResId;
    }

    /**
     * @return 項目のタイトルを返します
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return 項目の説明文を返します
     */
    public String getSubtitle() {
        return subtitle;
    }
}