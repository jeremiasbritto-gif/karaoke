package br.com.karaokestudio;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 10;
    private static final int MIC_PERMISSION = 11;
    private final int blue = Color.rgb(47, 128, 237);
    private final Handler meterHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer backingTrack, recordingPlayer;
    private MediaRecorder recorder;
    private Uri selectedTrack, savedRecording;
    private TextView trackLabel, status, scoreLabel;
    private Button recordButton;
    private EditText roomCode;
    private VoiceGraphView voiceGraph;
    private boolean recording;
    private int score, meterSamples;

    private final Runnable meterTask = new Runnable() {
        @Override public void run() {
            if (!recording || recorder == null) return;
            int amplitude = recorder.getMaxAmplitude();
            float level = Math.min(1f, amplitude / 18000f);
            voiceGraph.addLevel(level);
            if (amplitude > 1800) score += Math.max(1, Math.round(level * 12));
            meterSamples++;
            scoreLabel.setText(String.format(Locale.getDefault(), "Pontuação: %d", score));
            meterHandler.postDelayed(this, 100);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(30), dp(22), dp(36));
        root.setBackgroundColor(Color.rgb(247, 250, 255));
        scroll.addView(root);

        root.addView(text("🎤", 58, blue, Gravity.CENTER), matchWrap());
        TextView title = text("Karaoke Studio", 29, Color.rgb(20, 34, 58), Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, matchWrap());
        root.addView(text("Sua voz. Seu palco. Seus amigos.", 15, Color.DKGRAY, Gravity.CENTER),
                margins(matchWrap(), 0, 4, 0, 24));

        TextView localTitle = text("KARAOKÊ NO CELULAR", 14, blue, Gravity.START);
        localTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(localTitle, matchWrap());

        Button choose = button("🎵 Escolher playback armazenado");
        choose.setOnClickListener(v -> chooseTrack());
        root.addView(choose, margins(matchWrap(), 0, 8, 0, 0));
        trackLabel = text("Nenhuma música escolhida", 14, Color.DKGRAY, Gravity.CENTER);
        root.addView(trackLabel, margins(matchWrap(), 0, 8, 0, 12));

        root.addView(text("Volume do playback", 13, Color.GRAY, Gravity.START), matchWrap());
        SeekBar volume = new SeekBar(this);
        volume.setMax(100); volume.setProgress(70);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (backingTrack != null) backingTrack.setVolume(p / 100f, p / 100f);
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(volume, matchWrap());

        voiceGraph = new VoiceGraphView(this);
        root.addView(voiceGraph, margins(new LinearLayout.LayoutParams(-1, dp(120)), 0, 10, 0, 6));
        scoreLabel = text("Pontuação: 0", 21, blue, Gravity.CENTER);
        scoreLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(scoreLabel, matchWrap());

        recordButton = button("●  GRAVAR E CANTAR");
        recordButton.setTextSize(18); recordButton.setMinHeight(dp(62));
        recordButton.setOnClickListener(v -> toggleRecording());
        root.addView(recordButton, margins(matchWrap(), 0, 12, 0, 10));

        Button listen = button("▶ Ouvir última gravação");
        listen.setOnClickListener(v -> playRecording());
        root.addView(listen, matchWrap());
        status = text("Escolha um playback e aperte GRAVAR", 14, Color.rgb(70, 80, 95), Gravity.CENTER);
        root.addView(status, margins(matchWrap(), 0, 14, 0, 28));

        TextView liveTitle = text("SALA AO VIVO — TESTE GRATUITO", 14, blue, Gravity.START);
        liveTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(liveTitle, matchWrap());
        root.addView(text("Crie um código e envie aos amigos. A sala abre com câmera e microfone.",
                14, Color.DKGRAY, Gravity.START), margins(matchWrap(), 0, 5, 0, 8));

        roomCode = new EditText(this);
        roomCode.setHint("Código da sala");
        roomCode.setSingleLine(true);
        roomCode.setText("VIP" + (1000 + new Random().nextInt(9000)));
        root.addView(roomCode, matchWrap());

        Button enterRoom = button("📹 Entrar na sala com amigos");
        enterRoom.setOnClickListener(v -> openLiveRoom());
        root.addView(enterRoom, margins(matchWrap(), 0, 10, 0, 8));

        Button shareRoom = button("🔗 Compartilhar convite da sala");
        shareRoom.setOnClickListener(v -> shareRoom());
        root.addView(shareRoom, matchWrap());
        root.addView(text("O login Google será ativado depois do cadastro oficial do app.",
                12, Color.GRAY, Gravity.CENTER), margins(matchWrap(), 0, 12, 0, 0));
        return scroll;
    }

    private String roomUrl() {
        String code = roomCode.getText().toString().replaceAll("[^A-Za-z0-9]", "");
        if (code.length() < 3) code = "VIP" + (1000 + new Random().nextInt(9000));
        roomCode.setText(code);
        return "https://meet.jit.si/KaraokeStudio-" + code;
    }

    private void openLiveRoom() {
        String url = roomUrl();
        Intent i = new Intent(this, RoomActivity.class);
        i.putExtra("room_code", roomCode.getText().toString());
        i.putExtra("room_url", url);
        startActivity(i);
    }

    private void shareRoom() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "Venha cantar comigo no Karaoke Studio: " + roomUrl());
        startActivity(Intent.createChooser(i, "Enviar convite"));
    }

    private void chooseTrack() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_AUDIO);
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == PICK_AUDIO && result == RESULT_OK && data != null) {
            selectedTrack = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(selectedTrack,
                        data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {}
            trackLabel.setText("Playback carregado ✓");
            status.setText("Pronto para cantar!");
        }
    }

    private void toggleRecording() {
        if (recording) { stopRecording(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            return;
        }
        startRecording();
    }

    @Override public void onRequestPermissionsResult(int req, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(req, permissions, results);
        if (req == MIC_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            startRecording();
        else Toast.makeText(this, "Permita o microfone para cantar.", Toast.LENGTH_LONG).show();
    }

    private void startRecording() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, "Karaoke_" + System.currentTimeMillis() + ".m4a");
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Karaoke Studio");
            savedRecording = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            if (savedRecording == null) throw new IOException("Não foi possível criar a gravação");
            FileDescriptor fd = getContentResolver().openFileDescriptor(savedRecording, "w").getFileDescriptor();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(192000);
            recorder.setAudioSamplingRate(48000);
            recorder.setOutputFile(fd);
            recorder.prepare();
            recorder.start();

            if (selectedTrack != null) {
                backingTrack = MediaPlayer.create(this, selectedTrack);
                if (backingTrack != null) backingTrack.start();
            }
            score = 0; meterSamples = 0; voiceGraph.clear();
            recording = true;
            recordButton.setText("■  PARAR E SALVAR");
            recordButton.setBackgroundColor(Color.rgb(220, 65, 75));
            status.setText("Gravando… acompanhe o gráfico!");
            meterHandler.post(meterTask);
        } catch (Exception e) {
            cleanupRecorder();
            Toast.makeText(this, "Não consegui iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording() {
        meterHandler.removeCallbacks(meterTask);
        try { if (recorder != null) recorder.stop(); } catch (RuntimeException ignored) {}
        cleanupRecorder();
        if (backingTrack != null) { backingTrack.stop(); backingTrack.release(); backingTrack = null; }
        recording = false;
        recordButton.setText("●  GRAVAR E CANTAR");
        recordButton.setBackgroundColor(blue);
        int finalScore = meterSamples == 0 ? 0 : Math.min(100, score * 100 / Math.max(1, meterSamples * 8));
        scoreLabel.setText("Nota final: " + finalScore + " / 100");
        status.setText("Salvo em Música/Karaoke Studio ✓");
        Toast.makeText(this, "Gravação salva! Nota: " + finalScore, Toast.LENGTH_LONG).show();
    }

    private void playRecording() {
        if (savedRecording == null) {
            Toast.makeText(this, "Faça uma gravação primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (recordingPlayer != null) recordingPlayer.release();
        recordingPlayer = MediaPlayer.create(this, savedRecording);
        if (recordingPlayer == null) return;
        recordingPlayer.setOnCompletionListener(mp -> status.setText("Reprodução concluída"));
        recordingPlayer.start();
        status.setText("Ouvindo sua gravação…");
    }

    private void cleanupRecorder() {
        if (recorder != null) { recorder.release(); recorder = null; }
    }

    @Override protected void onDestroy() {
        meterHandler.removeCallbacks(meterTask);
        if (recording) stopRecording();
        if (recordingPlayer != null) recordingPlayer.release();
        super.onDestroy();
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(16);
        b.setAllCaps(false); b.setBackgroundColor(blue);
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        return b;
    }

    private TextView text(String value, int size, int color, int gravity) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(gravity);
        return t;
    }

    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams margins(LinearLayout.LayoutParams p, int l, int t, int r, int b) {
        p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p;
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private static class VoiceGraphView extends View {
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ArrayList<Float> levels = new ArrayList<>();
        VoiceGraphView(Activity context) {
            super(context);
            line.setColor(Color.rgb(47, 128, 237)); line.setStrokeWidth(6f); line.setStrokeCap(Paint.Cap.ROUND);
            grid.setColor(Color.rgb(220, 230, 242)); grid.setStrokeWidth(2f);
            setBackgroundColor(Color.WHITE);
        }
        void addLevel(float value) {
            levels.add(value);
            if (levels.size() > 80) levels.remove(0);
            invalidate();
        }
        void clear() { levels.clear(); invalidate(); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float mid = getHeight() / 2f;
            c.drawLine(0, mid, getWidth(), mid, grid);
            if (levels.isEmpty()) return;
            float step = getWidth() / 80f;
            float x = getWidth() - levels.size() * step;
            for (float v : levels) {
                float h = Math.max(4f, v * (getHeight() * .42f));
                c.drawLine(x, mid - h, x, mid + h, line);
                x += step;
            }
        }
    }
}
