package ocean.RedWhale;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * ユーザーがプロフィールやアプリの設定（言語、ストレージなど）を変更するための「設定画面（フラグメント）」です。
 */
public class SettingsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingsItem> settingsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        setupProfileInfo(view);

        recyclerView = view.findViewById(R.id.recycler_view_settings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize empty list and attach adapter immediately to prevent "No adapter attached" error
        settingsList = new ArrayList<>();
        adapter = new SettingsAdapter(getContext(), settingsList);
        recyclerView.setAdapter(adapter);

        // Load settings items asynchronously (though they are static, good practice for consistency)
        loadSettingsItems();

        adapter.setOnItemClickListener(item -> {
            if (getString(R.string.settings_language).equals(item.getTitle())) {
                showLanguagePicker();
            } else {
                Toast.makeText(getContext(), item.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadSettingsItems() {
        settingsList.clear();
        settingsList.add(new SettingsItem(android.R.drawable.ic_menu_sort_alphabetically, getString(R.string.settings_language), getString(R.string.settings_language_desc)));
        settingsList.add(new SettingsItem(android.R.drawable.ic_menu_save, getString(R.string.settings_storage), getString(R.string.settings_storage_desc)));
        settingsList.add(new SettingsItem(android.R.drawable.ic_lock_lock, getString(R.string.settings_security), getString(R.string.settings_security_desc)));
        adapter.notifyDataSetChanged();
    }

    private void setupProfileInfo(View view) {
        TextView tvName = view.findViewById(R.id.settings_name);
        if (tvName == null || getContext() == null) return;
        
        SharedPreferences prefs = getContext().getSharedPreferences("RedWhalePrefs", MODE_PRIVATE);
        String displayName = prefs.getString("display_name", "User Name");
        tvName.setText(displayName);
    }

    private void showLanguagePicker() {
        if (getContext() == null) return;

        String[] languages = {"English", "日本語 (Japanese)", "සිංහල (Sinhala)", "नेपाली (Nepali)", "မြန်မာ (Myanmar)"};
        String[] languageTags = {"en", "ja", "si", "ne", "my"};

        new AlertDialog.Builder(getContext())
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    String selectedTag = languageTags[which];
                    LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(selectedTag);
                    AppCompatDelegate.setApplicationLocales(appLocale);
                })
                .show();
    }
}