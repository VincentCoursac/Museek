package spark.museek;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import spark.museek.spotify.SpotifyUser;

public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SpotifyUser.getInstance().setAccessToken(TryLoadFromCache("token"));

        if (SpotifyUser.getInstance().getAccessToken() != null) {

            if (!isTokenStillValid())
                return;

            Intent newintent = new Intent(this, DiscoverActivity.class);
            startActivity(newintent);
        }
    }

    public boolean isTokenStillValid() {

        String exp = TryLoadFromCache("expiration");

        if (exp == null)
            return false;

        String expParams[] = exp.split(" ");

        if (expParams.length != 6)
            return false;

        Calendar expDate = Calendar.getInstance();
        expDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(expParams[0]));
        expDate.set(Calendar.MONTH, Integer.parseInt(expParams[1]));
        expDate.set(Calendar.YEAR, Integer.parseInt(expParams[2]));
        expDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(expParams[3]));
        expDate.set(Calendar.MINUTE, Integer.parseInt(expParams[4]));
        expDate.set(Calendar.SECOND, Integer.parseInt(expParams[5]));

        Calendar rightNow = Calendar.getInstance();

        if (rightNow.getTimeInMillis() < expDate.getTimeInMillis())
            return true;

        return false;
    }

    public void connectUser(View v) {

        ProgressBar bar = findViewById(R.id.progressBar);
        bar.setVisibility(View.VISIBLE);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(SpotifyUser.getInstance().getClientID(),
                AuthenticationResponse.Type.TOKEN,
                SpotifyUser.getInstance().getRedirectUri());
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, SpotifyUser.getInstance().getRequestCode(), request);
    }

    public void quitApp(View v) {
        finish();
        System.exit(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == SpotifyUser.getInstance().getRequestCode()) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {

                SpotifyUser.getInstance().setAccessToken(response.getAccessToken());
                WritefromCache("token", SpotifyUser.getInstance().getAccessToken());

                Calendar rightNow = Calendar.getInstance();
                rightNow.add(Calendar.SECOND, response.getExpiresIn());
                String exp = rightNow.get(Calendar.DAY_OF_MONTH) + " " + rightNow.get(Calendar.MONTH) +
                        " " + rightNow.get(Calendar.YEAR) + " " + rightNow.get(Calendar.HOUR_OF_DAY) + " " + rightNow.get(Calendar.MINUTE) + " " + rightNow.get(Calendar.SECOND);

                WritefromCache("expiration", exp);

                ProgressBar bar = findViewById(R.id.progressBar);
                bar.setVisibility(View.INVISIBLE);

                Toast.makeText(getApplicationContext(), "Connection successful!", Toast.LENGTH_SHORT).show();

                Intent newintent = new Intent(this, DiscoverActivity.class);
                startActivity(newintent);

            }
            else
                Toast.makeText(getApplicationContext(), "Incorrect token!", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Connection failed!", Toast.LENGTH_SHORT).show();
    }


    // TEMPORARY
    public String TryLoadFromCache(String key) {
        String ret = null;
        try {

            InputStream inputStream = this.openFileInput("cache_" + key + ".txt");

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public void WritefromCache(String key, String value) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput("cache_" + key + ".txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(value);
            outputStreamWriter.close();


        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}