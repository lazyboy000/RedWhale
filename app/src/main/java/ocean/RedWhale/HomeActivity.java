package ocean.RedWhale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

/**
 * メインのホーム画面となるアクティビティです。
 * ナビゲーションドロワーとフラグメントの管理を担当します。
 */
public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;

    // デバイス選択画面からの結果を識別するためのリクエストコード
    private static final int SELECT_DEVICE = 102;

    /**
     * アクティビティが初めて作成されるときに呼び出されます。
     * ツールバー、ドロワー、ナビゲーションビューを設定します。
     * @param savedInstanceState 以前に保存されたアクティビティの状態
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // ドロワーのトグルを設定し、ツールバーとドロワーレイアウトをリンクします。
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ナビゲーションビューの項目クリックリスナーを設定します。
        navigationView.setNavigationItemSelectedListener(this);

        // アクティビティが新しく作成された場合、デフォルトでChatsFragmentを表示します。
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatsFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_chats);
            toolbar.setTitle("Chats");
        }
    }

    /**
     * ナビゲーションドロワーの項目が選択されたときに呼び出されます。
     * 選択された項目に応じてフラグメントを切り替えます。
     * @param item 選択されたメニュー項目
     * @return イベントが処理された場合はtrue
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        // 選択された項目に基づいて表示するフラグメントを決定します。
        if (itemId == R.id.nav_chats) {
            selectedFragment = new ChatsFragment();
            toolbar.setTitle("Chats");
        } else if (itemId == R.id.nav_new_group) {
            Toast.makeText(this, "New Group - Coming Soon!", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.nav_calls) {
            selectedFragment = new CallsFragment();
            toolbar.setTitle("Calls");
        } else if (itemId == R.id.nav_contacts) {
            selectedFragment = new ContactsFragment();
            toolbar.setTitle("Contacts");
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
            toolbar.setTitle("Settings");
        }

        // フラグメントが選択された場合、現在のフラグメントを新しいものに置き換えます。
        if (selectedFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }

        // ナビゲーションドロワーを閉じます。
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * バックボタンが押されたときの処理。
     * ドロワーが開いていれば閉じ、そうでなければデフォルトの動作をします。
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 他のアクティビティから結果を受け取ったときに呼び出されます。
     * この場合、DeviceListActivityからの結果を処理します。
     * @param requestCode アクティビティを開始したときのリクエストコード
     * @param resultCode アクティビティからの結果コード
     * @param data 結果データを含むインテント
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_DEVICE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // DeviceListActivityからデバイスが選択されたら、MainActivityを開始してチャットします。
                String address = data.getStringExtra("deviceAddress");
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("deviceAddress", address);
                startActivity(intent);
            }
        }
    }
}