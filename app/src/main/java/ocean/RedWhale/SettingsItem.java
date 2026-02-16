package ocean.RedWhale;

/**
 * 設定リストの単一の項目を表すデータモデルクラスです。
 */
public class SettingsItem {
    private int iconResId;    // アイコンのリソースID
    private String title;     // 項目のタイトル
    private String subtitle;  // 項目のサブタイトル（説明）

    public SettingsItem(int iconResId, String title, String subtitle) {
        this.iconResId = iconResId;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}