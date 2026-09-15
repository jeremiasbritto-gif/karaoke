package br.com.karaokestudio;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 10;
    private static final int MIC_PERMISSION = 11;
    private final int blue = Color.rgb(47, 128, 237);
    private MediaPlayer backingTrack;
    private MediaPlayer recordingPlayer;
    private MediaRecorder recorder;
    private Uri selectedTrack;
    private Uri savedRecording;
    private TextView trackLabel;
    private TextView status;
    private Button recordButton;
    private boolean recording;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(42), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(247, 250, 255));

        TextView logo = text("🎤", 64, blue, Gravity.CENTER);
        root.addView(logo, matchWrap());
        TextView title = text("Karaoke Studio", 30, Color.rgb(20, 34, 58), Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, matchWrap());
        TextView subtitle = text("Sua voz. Seu palco.", 16, Color.DKGRAY, Gravity.CENTER);
        root.addView(subtitle, withMargins(matchWrap(), 0, 4, 0, 26));

        Button choose = button("Escolher playback do celular");
        choose.setOnClickListener(v -> chooseTrack());
        root.addView(choose, matchWrap());

        trackLabel = text("Nenhuma música escolhida", 15, Color.DKGRAY, Gravity.CENTER);
        root.addView(trackLabel, withMargins(matchWrap(), 0, 12, 0, 18));

        SeekBar volume = new SeekBar(this);
        volume.setMax(100); volume.setProgress(75);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (backingTrack != null) backingTrack.setVolume(p / 100f, p / 100f);
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(text("Volume do playback", 14, Color.GRAY, Gravity.START), matchWrap());
        root.addView(volume, matchWrap());

        recordButton = button("●  GRAVAR MINHA VOZ");
        recordButton.setTextSize(18); recordButton.setMinHeight(dp(62));
        recordButton.setOnClickListener(v -> toggleRecording());
        root.addView(recordButton, withMargins(matchWrap(), 0, 28, 0, 14));

        Button listen = button("▶ Ouvir última gravação");
        listen.setOnClickListener(v -> playRecording());
        root.addView(listen, matchWrap());

        status = text("Escolha uma música e aperte GRAVAR", 15, Color.rgb(70, 80, 95), Gravity.CENTER);
        root.addView(status, withMargins(matchWrap(), 0, 22, 0, 0));
        return root;
    }

    private void chooseTrack() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*"); intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_AUDIO);
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == PICK_AUDIO && result == RESULT_OK && data != null) {
            selectedTrack = data.getData();
            getContentResolver().takePersistableUriPermission(selectedTrack,
                    data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            trackLabel.setText("Playback carregado ✓"); status.setText("Pronto para cantar!");
        }
    }

    private void toggleRecording() {
        if (recording) { stopRecording(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION); return;
        }
        startRecording();
    }

    @Override public void onRequestPermissionsResult(int req, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(req, permissions, results);
        if (req == MIC_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startRecording();
        else Toast.makeText(this, "O microfone precisa ser permitido.", Toast.LENGTH_LONG).show();
    }

    private void startRecording() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, "Karaoke_" + System.currentTimeMillis() + ".m4a");
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Karaoke Studio");
            savedRecording = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            if (savedRecording == null) throw new IOException("Não foi possível criar o arquivo");
            FileDescriptor fd = getContentResolver().openFileDescriptor(savedRecording, "w").getFileDescriptor();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(192000);
            recorder.setAudioSamplingRate(48000);
            recorder.setOutputFile(fd);
            recorder.prepare(); recorder.start();

            if (selectedTrack != null) {
                backingTrack = MediaPlayer.create(this, selectedTrack);
                if (backingTrack != null) backingTrack.start();
            }
            recording = true;
            recordButton.setText("■  PARAR E SALVAR"); recordButton.setBackgroundColor(Color.rgb(220, 65, 75));
            status.setText("Gravando… cante agora!");
        } catch (Exception e) {
            cleanupRecorder();
            Toast.makeText(this, "Não consegui iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording() {
        try { if (recorder != null) recorder.stop(); }
        catch (RuntimeException ignored) {}
        cleanupRecorder();
        if (backingTrack != null) { backingTrack.stop(); backingTrack.release(); backingTrack = null; }
        recording = false;
        recordButton.setText("●  GRAVAR MINHA VOZ"); recordButton.setBackgroundColor(blue);
        status.setText("Salvo em Música/Karaoke Studio ✓");
        Toast.makeText(this, "Gravação salva no celular!", Toast.LENGTH_LONG).show();
    }

    private void playRecording() {
        if (savedRecording == null) { Toast.makeText(this, "Faça uma gravação primeiro.", Toast.LENGTH_SHORT).show(); return; }
        try {
            if (recordingPlayer != null) recordingPlayer.release();
            recordingPlayer = MediaPlayer.create(this, savedRecording);
            recordingPlayer.setOnCompletionListener(mp -> status.setText("Reprodução concluída"));
            recordingPlayer.start(); status.setText("Ouvindo sua gravação…");
        } catch (Exception e) { Toast.makeText(this, "Não foi possível reproduzir.", Toast.LENGTH_SHORT).show(); }
    }

    private void cleanupRecorder() { if (recorder != null) { recorder.release(); recorder = null; } }
    @Override protected void onDestroy() {
        if (recording) stopRecording();
        if (recordingPlayer != null) recordingPlayer.release();
        super.onDestroy();
    }

    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(16);
        b.setAllCaps(false); b.setBackgroundColor(blue); b.setPadding(dp(16), dp(10), dp(16), dp(10)); return b;
    }
    private TextView text(String value, int size, int color, int gravity) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(gravity); return t;
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams withMargins(LinearLayout.LayoutParams p, int l, int t, int r, int b) { p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
