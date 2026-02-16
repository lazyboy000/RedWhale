package ocean.RedWhale;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 設定項目のリストをRecyclerViewに表示するためのアダプターです。
 */
public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {

    private final List<SettingsItem> settingsItems;
    private final Context context;

    public SettingsAdapter(Context context, List<SettingsItem> settingsItems) {
        this.context = context;
        this.settingsItems = settingsItems;
    }

    /**
     * 新しいViewHolderが作成されるときに呼び出されます。
     * @param parent 親ビューグループ
     * @param viewType ビュータイプ
     * @return 新しいSettingsViewHolder
     */
    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_setting, parent, false);
        return new SettingsViewHolder(view);
    }

    /**
     * ViewHolderにデータをバインド（設定）するときに呼び出されます。
     * @param holder データを設定するViewHolder
     * @param position データリスト内の位置
     */
    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        SettingsItem item = settingsItems.get(position);
        holder.icon.setImageResource(item.getIconResId());
        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubtitle());

        // サブタイトルが空の場合は非表示にする
        if (item.getSubtitle() == null || item.getSubtitle().isEmpty()) {
            holder.subtitle.setVisibility(View.GONE);
        } else {
            holder.subtitle.setVisibility(View.VISIBLE);
        }

        // アイテムクリック時のダミー処理
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, item.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * リスト内のアイテムの総数を返します。
     * @return アイテムの総数
     */
    @Override
    public int getItemCount() {
        return settingsItems.size();
    }

    /**
     * 設定リストの各アイテムのビューを保持するためのViewHolderクラスです。
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