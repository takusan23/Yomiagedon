package io.github.takusan23.yomiagedon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Scope;
import com.sys1yagi.mastodon4j.api.entity.auth.AccessToken;
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Apps;

import okhttp3.OkHttpClient;

public class LoginActivity extends AppCompatActivity {

    String client_id, client_secret, redirect_url;
    String instance_string, access_token_string;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        final EditText instance_edittext = findViewById(R.id.instance_edittext);
        final EditText code_edittext = findViewById(R.id.code_edittext);

        Button instance_button = findViewById(R.id.instance_button);
        Button code_button = findViewById(R.id.code_button);

        MastodonClient mastodonClient = new MastodonClient.Builder(instance_edittext.getText().toString(), new OkHttpClient.Builder(), new Gson()).build();
        final Apps apps = new Apps(mastodonClient);

        instance_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... aVoid) {
                        try {
                            AppRegistration appRegistration = apps.createApp(
                                    "Yomiagedon",
                                    "urn:ietf:wg:oauth:2.0:oob",
                                    new Scope(Scope.Name.ALL),
                                    "https://friends.nico/@takusan_23"
                            ).execute();

                            client_id = appRegistration.getClientId();
                            client_secret = appRegistration.getClientSecret();
                            redirect_url = appRegistration.getRedirectUri();

                            //ブラウザー
                            if (client_id != null) {
                                String url = "https://" + instance_edittext.getText().toString() + "/oauth/authorize?client_id=" + client_id + "&redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=code&scope=read%20write%20follow";
                                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                                CustomTabsIntent customTabsIntent = builder.build();
                                customTabsIntent.launchUrl(LoginActivity.this, Uri.parse(url));
                            }

                        } catch (Mastodon4jRequestException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });


        //アクセストークン
        code_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... aVoid) {
                        try {
                            AccessToken accessToken = apps.getAccessToken(
                                    client_id,
                                    client_secret,
                                    redirect_url,
                                    code_edittext.getText().toString(),
                                    "authorization_code"
                            ).execute();

                            instance_string = instance_edittext.getText().toString();
                            access_token_string = accessToken.getAccessToken();

                            //保存
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("access_token", access_token_string);
                            editor.putString("instance", instance_string);
                            editor.commit();

                            Intent intent = new Intent(LoginActivity.this, YomiageMain.class);
                            startActivity(intent);

                        } catch (Mastodon4jRequestException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

    }
}
