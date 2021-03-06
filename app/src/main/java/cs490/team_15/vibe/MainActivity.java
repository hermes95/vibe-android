package cs490.team_15.vibe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import cs490.team_15.vibe.API.RequestAPI;
import cs490.team_15.vibe.API.UserAPI;
import cs490.team_15.vibe.API.models.User;

public class MainActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private Player mPlayer;
    private static String mAccessToken;
    private AuthenticationRequest mAuthRequest;

    private static boolean mLoggedIn = false;
    // TODO: 12/4/16 save user info into shared preferences
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mSharedPreferencesEditor;

    private static Resources mResources;
    private static volatile User currentUser;

    private Menu menu;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isLoggedIn() {
        return mLoggedIn;
    }

    private static final int NUM_TABS = 3;
    private static final int SEARCH_TAB_INDEX = 1;
    private static final int REQUEST_TAB_INDEX = 2;

    private static final String CLIENT_ID = "ff502d57cc2a464fbece5c9511763cea";
    private static final String REDIRECT_URI = "localhost://callback";
    private static final int REQUEST_CODE = 1337;

    public static String getAccessToken() {
        return mAccessToken;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*try {
            UserAPI.deleteUser(new User(30, "Austin Dewey", "adewey4", null), getApplicationContext());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }*/
        setContentView(R.layout.activity_main);
        mResources = getResources();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == REQUEST_TAB_INDEX)
                    RequestAPI.getAllRequests(getCurrentUser(), RequestFragment.getInstance().mRequestArrayAdapter);
            }
        });

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(mResources.getString(R.string.spotify_client_id),
                AuthenticationResponse.Type.TOKEN, mResources.getString(R.string.spotify_redirect_uri));
        builder.setScopes(mResources.getStringArray(R.array.spotify_scopes));
        this.mAuthRequest = builder.build();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Log the DJ in
        if (id == R.id.action_login && this.mLoggedIn == false) {
            AuthenticationClient.openLoginActivity(this, mResources.getInteger(R.integer.spotify_request_code), this.mAuthRequest);
            this.mLoggedIn = true;
            item.setTitle(getString(R.string.dj_login));
            return true;
        }
        // Log the DJ out
        else if (id == R.id.action_login && this.mLoggedIn == true) {
            mPlayer.logout();
            this.mLoggedIn = false;
            item.setTitle(getString(R.string.dj_login));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        MenuItem item = menu.findItem(R.id.action_login);
        item.setTitle("DJ Logout");
        // Create new DJ
        // User: First Name, Last Name, Spotify ID, email
        //User temp = UserAPI.generateRandomUser();
        User temp = UserAPI.generateLoggedInUser(this.mAccessToken);
        System.out.println("Printing" + temp);
        try {
            UserAPI.createNewUser(temp, getApplicationContext());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        /*User u = getCurrentUser();
        RequestFragment.getInstance().onLoggedIn(u.id);*/
        //mPlayer.playUri(null, "spotify:artist:5K4W6rqBFWDnAN6FQUkS6x", 0, 0);
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
        MenuItem item = menu.findItem(R.id.action_login);
        item.setTitle("DJ Login");
        try {
            UserAPI.deleteUser(getCurrentUser(), getApplicationContext());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        /*
        // Delete the DJ that logged out from the DB
        try {
            // Replace the first arg with the logged in DJ id number
            UserAPI.deleteUser(getCurrentUser(), getApplicationContext());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        */
    }

    @Override
    public void onLoginFailed(int i) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == mResources.getInteger(R.integer.spotify_request_code)) {
            // Get oauth access token for DJ functions
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            this.mAccessToken = response.getAccessToken();

            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), mResources.getString(R.string.spotify_client_id));
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 2) {
                if (RequestFragment.getInstance().mRequestArrayAdapter != null)
                    RequestAPI.getAllRequests(getCurrentUser(), RequestFragment.getInstance().mRequestArrayAdapter);
                return RequestFragment.getInstance();
            }
            if (position == 0) {
                return DjFragment.getInstance();
            }
            return SearchFragment.newInstance();
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "DJs";
                case 1:
                    return "Search";
                case 2:
                    return "Requests";
            }
            return null;
        }
    }
}
