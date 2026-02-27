package ocean.RedWhale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 設定画面のリスト（RecyclerView）に各設定項目（SettingsItem）を
 * どのように並べて表示するかを管理する「アダプター」クラスです。
 */
public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {

    // 表示する設定項目のデータのリスト
    private final List<SettingsItem> settingsItems;
    private final Context context;

    // 項目がタップされたときの動作を外（SettingsFragment）に伝えるための「耳（リスナー）」
    public interface OnItemClickListener {
        void onItemClick(SettingsItem item);
    }
    private OnItemClickListener listener;

    /**
     * コンストラクタ。アダプターの準備をします。
     *
     * @param context       画面の情報
     * @param settingsItems リストに表示するデータ
     */
    public SettingsAdapter(Context context, List<SettingsItem> settingsItems) {
        this.context = context;
        this.settingsItems = settingsItems;
    }

    /**
     * リストがタップされたときに動く処理（リスナー）を登録します。
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 新しい項目の「見た目（ビュー）」を作る必要があるときに呼ばれます。
     * 1行分のデザイン（list_item_setting.xml）を読み込んでビューホルダーを返します。
     */
    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_setting, parent, false);
        return new SettingsViewHolder(view);
    }

    /**
     * 作った「見た目（ビュー）」に、実際の「データ（タイトルやアイコン）」を当てはめる処理です。
     */
    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        // 現在の行のデータを取得します
        SettingsItem item = settingsItems.get(position);
        
        // 画面部品（ImageViewやTextView）にデータをセットします
        holder.icon.setImageResource(item.getIconResId());
        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubtitle());

        // 説明文（サブタイトル）がない場合は、空白を詰めて非表示にします
        if (item.getSubtitle() == null || item.getSubtitle().isEmpty()) {
            holder.subtitle.setVisibility(View.GONE);
        } else {
            holder.subtitle.setVisibility(View.VISIBLE);
        }

        // この行（項目）がタップされた時の処理です
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // 外側（SettingsFragment）に「これがタップされましたよ」と伝えます
                listener.onItemClick(item);
            }
        });
    }

    /**
     * リストに表示するデータが全部でいくつあるかを返します。
     */
    @Override
    public int getItemCount() {
        return settingsItems.size();
    }

    /**
     * リストの1行分の画面部品を裏側で保持（キャッシュ）しておくためのクラスです。
     * 毎回探す（findViewById）と動きが遅くなるため、これを使って高速化しています。
     */
    public static class SettingsViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, subtitle;

        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.setting_icon);
            title = itemView.findViewById(R.id.setting_title);
            subtitle = itemView.findViewById(R.id.setting_subtitle);
        }
    }
}