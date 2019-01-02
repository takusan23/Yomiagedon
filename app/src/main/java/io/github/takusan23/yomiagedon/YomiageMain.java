package io.github.takusan23.yomiagedon;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Shutdownable;
import com.sys1yagi.mastodon4j.api.entity.Notification;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Streaming;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import aqkanji2koe.AqKanji2Koe;
import aquestalk.AquesTalk;
import okhttp3.OkHttpClient;

public class YomiageMain extends AppCompatActivity {

    boolean home = false, notification = false, local = false;
    Shutdownable shutdownable_home, shutdownable_local;
    String yomiage_dic;
    int count = 0;

    AquesTalk aquestalk;
    AudioTrack audioTrack;
    private static final int FS = 8000;    //[Hz]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yomiage_main);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        aquestalk = new AquesTalk();

        String access_token = sharedPreferences.getString("instance", "");
        String instance = sharedPreferences.getString("access_token", "");

        if (access_token.isEmpty()) {
            Intent intent = new Intent(YomiageMain.this, LoginActivity.class);
            startActivity(intent);
        }

        Switch home_Switch = findViewById(R.id.home_swich);
        Switch notification_Switch = findViewById(R.id.notification_swich);
        Switch local_Swich = findViewById(R.id.local_swich);

        Button re_login = findViewById(R.id.re_login);

        home_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    home = true;
                } else {
                    home = false;
                }
            }
        });
        notification_Switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    notification = true;
                } else {
                    notification = false;
                }
            }
        });
        local_Swich.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    local = true;
                } else {
                    local = false;
                }
            }
        });

        copyDic();
        yomiage_dic = getFilesDir().toString();

/*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
*/

        final MastodonClient mastodonClient = new MastodonClient.Builder(sharedPreferences.getString("instance", ""), new OkHttpClient.Builder(), new Gson())
                .accessToken(sharedPreferences.getString("access_token", ""))
                .useStreamingApi()
                .build();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "読み上げを初めますね？", Snackbar.LENGTH_LONG)
                        .setAction("スタート", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                //ストリーミング済みなら一旦切る


                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... aVoid) {

                                        if (home || notification) {
                                            com.sys1yagi.mastodon4j.api.Handler handler = new com.sys1yagi.mastodon4j.api.Handler() {
                                                @Override
                                                public void onStatus(@NotNull final com.sys1yagi.mastodon4j.api.entity.Status status) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            String toot = Html.fromHtml(status.getContent(), Html.FROM_HTML_MODE_COMPACT).toString();
                                                            String yomiage_text = AqKanji2Koe.convert(yomiage_dic, toot);
                                                            onPlayBtn(yomiage_text, 100);
                                                            //Toast.makeText(YomiageMain.this,toot,Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onNotification(@NotNull final Notification notification) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            String notification_type = notification.getType();
                                                            if (notification_type.equals("mention")) {
                                                                notification_type = "さんが返信しました";
                                                            }
                                                            if (notification_type.equals("reblog")) {
                                                                notification_type = "さんがブーストしました";
                                                            }
                                                            if (notification_type.equals("favourite")) {
                                                                notification_type = "さんが二コしました";

                                                            }
                                                            if (notification_type.equals("follow")) {
                                                                notification_type = "さんがフォローしました";
                                                            }
                                                            String notification_string = notification.getAccount().getDisplayName() + notification_type;
                                                            String yomiage_text = AqKanji2Koe.convert(yomiage_dic, notification_string);
                                                            onPlayBtn(yomiage_text, 100);
                                                            //Toast.makeText(YomiageMain.this,toot,Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                                }

                                                @Override
                                                public void onDelete(long l) {

                                                }
                                            };
                                            try {
                                                Streaming streaming_home = new Streaming(mastodonClient);
                                                shutdownable_home = streaming_home.user(handler);
                                            } catch (Mastodon4jRequestException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        if (local) {
                                            com.sys1yagi.mastodon4j.api.Handler handler1 = new com.sys1yagi.mastodon4j.api.Handler() {
                                                @Override
                                                public void onStatus(@NotNull final com.sys1yagi.mastodon4j.api.entity.Status status) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            String toot = Html.fromHtml(status.getContent(), Html.FROM_HTML_MODE_COMPACT).toString();
                                                            String yomiage_text = AqKanji2Koe.convert(yomiage_dic, toot);
                                                            //Toast.makeText(YomiageMain.this,yomiage_text,Toast.LENGTH_SHORT).show();
                                                            onPlayBtn(yomiage_text, 100);
                                                            //Toast.makeText(YomiageMain.this,toot,Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onNotification(@NotNull Notification notification) {

                                                }

                                                @Override
                                                public void onDelete(long l) {

                                                }
                                            };
                                            try {
                                                Streaming streaming_local = new Streaming(mastodonClient);
                                                shutdownable_local = streaming_local.localPublic(handler1);
                                            } catch (Mastodon4jRequestException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        return null;
                                    }
                                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        }).show();


            }
        });

        re_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(YomiageMain.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onDestroy() {

        if (shutdownable_home != null) {
            shutdownable_home.shutdown();
        }
        if (shutdownable_local != null) {
            shutdownable_local.shutdown();
        }
        super.onDestroy();
    }

    private void onPlayBtn(String koe, int speed) {
        // 音声合成
        final byte[] wav = aquestalk.syntheWav(koe, speed);

        if (wav.length == 1) {//生成エラー時には,長さ１で、先頭にエラーコードが返される
            Log.v("AQTKAPP", "AquesTalk Synthe ERROR:" + wav[0]);
            Toast.makeText(YomiageMain.this, "音声記号列が正しい？：" + wav[0], Toast.LENGTH_LONG).show();
        } else {    // 音声出力



            if (audioTrack != null) {// インスタンスがあれば停止/解放
                audioTrack.stop();
                audioTrack.release();
            }



            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    FS,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    wav.length - 44,
                    AudioTrack.MODE_STATIC);

            audioTrack.write(wav, 44, wav.length - 44);// 44:wavのヘッダサイズ

            audioTrack.play();

            audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack track) {
                    audioTrack.stop();
                    audioTrack.release();
                }

                @Override
                public void onPeriodicNotification(AudioTrack track) {

                }
            });

        }



    }


    private void copyDic() {
        try {
            // すでに展開済み？
            String filepath = this.getFilesDir().getAbsolutePath() + "/" + "copyed.dat";
            File file = new File(filepath);
            boolean isExists = file.exists();

            if (!isExists) {//展開済みでなかったら（初期起動時）
                SnackberProgress(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AssetManager am = getResources().getAssets();
                            InputStream is = am.open("aq_dic.zip", AssetManager.ACCESS_STREAMING);
                            ZipInputStream zis = new ZipInputStream(is);
                            ZipEntry ze = zis.getNextEntry();

                            int totalSize = 0;
                            for (; ze != null; ) {
                                String path = getFilesDir().toString() + "/" + ze.getName();
                                FileOutputStream fos = new FileOutputStream(path, false);
                                byte[] buf = new byte[8192];
                                int size = 0;
                                int posLast = 0;
                                while ((size = zis.read(buf, 0, buf.length)) > -1) {
                                    fos.write(buf, 0, size);
                                    totalSize += size;
                                    int pos = totalSize * 100 / 27220452 + 1;
                                    if (posLast != pos) {
                                        //dialog.setProgress(pos);
                                        posLast = pos;
                                    }
                                }
                                fos.close();
                                zis.closeEntry();
                                ze = zis.getNextEntry();
                            }
                            zis.close();
                            {// コピー完了のマークとして、copyed.datを作成
                                String filepath = getFilesDir().getAbsolutePath() + "/" + "copyed.dat";
                                FileOutputStream fos = new FileOutputStream(filepath, false);
                                byte[] buf = new byte[1];
                                buf[0] = '*';
                                fos.write(buf, 0, 1);
                                fos.close();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                        SnackberProgress(true);
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SnackberProgress(boolean stop) {
        //くるくる
        View view = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, "初期化処理中", Snackbar.LENGTH_INDEFINITE);
        ViewGroup snackBer_viewGrop = (ViewGroup) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text).getParent();
        //SnackBerを複数行対応させる
        TextView snackBer_textView = (TextView) snackBer_viewGrop.findViewById(android.support.design.R.id.snackbar_text);
        snackBer_textView.setMaxLines(2);
        //複数行対応させたおかげでずれたので修正
        ProgressBar progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressBer_layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressBer_layoutParams.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(progressBer_layoutParams);
        snackBer_viewGrop.addView(progressBar, 0);
        if (stop) {
            snackbar.dismiss();
        } else {
            snackbar.show();
        }
    }
}
