package ocean.RedWhale;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;


public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Default fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ChatsFragment())
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_chats) {
                selectedFragment = new ChatsFragment();
                toolbar.setTitle("Chats");
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

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
}