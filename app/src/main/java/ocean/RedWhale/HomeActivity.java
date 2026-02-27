package ocean.RedWhale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
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

import android.os.PowerManager;
import android.provider.Settings;
import android.net.Uri;

/**
 * アプリのメイン画面（ハブ）です。
 * ナビゲーションメニューと、チャット一覧・設定画面の切り替えを管理します。
 */
public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;

    private static final int SELECT_DEVICE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // ツールバー（画面上部のバー）の設定
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // ナビゲーションメニュー（横から出るメニュー）の設定
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (drawerLayout != null && toolbar != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            // ユーザー情報（名前・Node ID）を非同期で読み込み
            loadNavHeaderData();
        }

        // 最初に表示する画面として「チャット一覧」をセット
        if (savedInstanceState == null && findViewById(R.id.fragment_container) != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatsFragment())
                    .commit();
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.nav_chats);
            }
            if (toolbar != null) {
                toolbar.setTitle("Chats");
            }
        }
        
        // バッテリー最適化設定のチェック（少し遅らせて実行）
        getWindow().getDecorView().postDelayed(this::checkBatteryOptimization, 1000);
    }

    /**
     * P2P通信を安定させるため、バッテリー制限の解除をユーザーに依頼します。
     */
    private void checkBatteryOptimization() {
        if (isFinishing()) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    // 失敗しても無視
                }
            }
        }
    }

    /**
     * メニューの上部にユーザー名とNode IDを表示します。
     */
    private void loadNavHeaderData() {
        if (navigationView == null) return;
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        TextView tvName = headerView.findViewById(R.id.nav_header_name);
        TextView tvAddress = headerView.findViewById(R.id.nav_header_address);

        new Thread(() -> {
            // 保存されている情報を取得
            SharedPreferences prefs = getSharedPreferences("RedWhalePrefs", MODE_PRIVATE);
            String displayName = prefs.getString("display_name", "User");
            
            IdentityManager identityManager = new IdentityManager(this);
            String nodeInfo;
            if (identityManager.exists()) {
                String address = identityManager.getIdentityAddress();
                if (address != null) {
                    if (address.length() > 16) {
                        // 長いアドレスを短縮して表示
                        nodeInfo = "Node ID: " + address.substring(0, 8) + "..." + address.substring(address.length() - 8);
                    } else {
                        nodeInfo = "Node ID: " + address;
                    }
                } else {
                    nodeInfo = "Node ID: Unknown";
                }
            } else {
                nodeInfo = "Node ID: Not Set";
            }

            // メインスレッドでUIを更新
            runOnUiThread(() -> {
                if (tvName != null) tvName.setText(displayName);
                if (tvAddress != null) tvAddress.setText(nodeInfo);
            });
        }).start();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        // 選択されたメニューに応じて画面（フラグメント）を切り替え
        if (itemId == R.id.nav_chats) {
            selectedFragment = new ChatsFragment();
            if (toolbar != null) toolbar.setTitle("チャット");
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
            if (toolbar != null) toolbar.setTitle("設定");
        }

        if (selectedFragment != null && !isFinishing()) {
            try {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            } catch (Exception e) {
                // エラー時は無視
            }
        }

        // メニューを閉じる
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // メニューが開いていれば閉じる
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // デバイス選択画面から戻ってきた時の処理
        if (requestCode == SELECT_DEVICE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String address = data.getStringExtra("deviceAddress");
                String name = data.getStringExtra("CHAT_NAME");
                // チャット画面（MainActivity）を開始
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("deviceAddress", address);
                intent.putExtra("CHAT_NAME", name);
                startActivity(intent);
            }
        }
    }
}