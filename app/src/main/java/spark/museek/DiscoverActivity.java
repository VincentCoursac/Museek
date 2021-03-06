package spark.museek;


import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;

import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.spotify.sdk.android.player.Config;

import java.util.Set;

import spark.museek.fragments.PlayerFragment;

import spark.museek.spotify.SpotifyRecommander;

import spark.museek.spotify.SpotifyUser;

public class DiscoverActivity extends AppCompatActivity implements ConnectionStateCallback {


    private PlayerFragment playerFragment;
    private Player player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        this.playerFragment = new PlayerFragment();
        transaction.add(R.id.container_player, playerFragment);
        transaction.commit();

        // We set the toolbar up
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Discover");
        getSupportActionBar().setIcon(R.drawable.ic_audiotrack_white_24dp);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        setupDefaultPreferences();



        Config playerConfig = new Config(this, SpotifyUser.getInstance().getAccessToken(), SpotifyUser.getInstance().getClientID());
        playerConfig.useCache(false);

        Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer spotifyPlayer) {
                player = spotifyPlayer;
                player.addConnectionStateCallback(DiscoverActivity.this);

            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
            }
        });


    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // If the player is playing music from likedPlayerFragment
        if (SpotifyUser.getInstance().getIsPlaying()) {
            SpotifyUser.getInstance().getPlayer().pause(new Player.OperationCallback() {
                @Override
                public void onSuccess() {
                    playerFragment.resumeCurrentSong();
                }

                @Override
                public void onError(Error error) {
                    Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                }
            });
        }

        // if the player is NOT playing music
        else if (!SpotifyUser.getInstance().getPlayer().getPlaybackState().isPlaying)
            playerFragment.prepareRestartCurrentSong();

        if (SpotifyUser.getInstance().hasParameterChanged()) {
            SpotifyRecommander.getInstance().requestSong(playerFragment, this.getApplicationContext());
        }
    }

    // If there are no preferences yet, it will create ones
    private void setupDefaultPreferences() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> selections = prefs.getStringSet("genre", null);

        // If there is no preferences yet
        if (!prefs.getBoolean("checkbox_releases", false)
                && !prefs.getBoolean("checkbox_suggestions", false)
                && selections == null) {

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("checkbox_releases", true);
            editor.putBoolean("checkbox_suggestions", false);
            Set<String> selects = new ArraySet<>();
            selects.add("hard-rock");
            selects.add("pop");
            selects.add("hip-hop");
            selects.add("electro");
            selects.add("classical");
            editor.putStringSet("genre", selects);
            editor.commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.discover_toolbar, menu);

        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:

                Intent newintent = new Intent(this, DiscoverPreferences.class);
                startActivity(newintent);

                return true;

            case R.id.action_show_likes:

                Intent likedIntent = new Intent(this, LikedActivity.class);
                startActivity(likedIntent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onLoggedIn() {
        SpotifyUser.getInstance().setPlayer(player);
        this.playerFragment.onPlayerLoaded();
    }

    @Override
    public void onLoggedOut() {
        System.out.println("=====  LOGGED OUT  =======");
    }

    @Override
    public void onLoginFailed(Error error) {
        System.out.println("=====  LOGIN FAILED  =======");
    }

    @Override
    public void onTemporaryError() {
        System.out.println("=====  TEMPORARY ERROR  =======");
    }

    @Override
    public void onConnectionMessage(String s) {
        System.out.println("=====  CONNECTION MESSAGE  =======");
    }
}
