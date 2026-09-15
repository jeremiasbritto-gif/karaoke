package br.com.karaokestudio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

public class RoomActivity extends Activity {
    private static final int PICK_SERVER_SONG = 40;
    private final int navy = Color.rgb(5, 22, 65);
    private final int blue = Color.rgb(18, 92, 210);
    private final int cyan = Color.rgb(39, 202, 255);
    private String roomCode;
    private String roomUrl;
    private TextView queue;
    private LinearLayout chatArea;
    private int coins = 100;
    private boolean adminMode;
    private String artist;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        roomCode = getIntent().getStringExtra("room_code");
        roomUrl = getIntent().getStringExtra("room_url");
        artist = getIntent().getStringExtra("artist");
        if (artist == null || artist.trim().isEmpty()) artist = "Britto";
        if (roomCode == null) roomCode = "VIP2026";
        if (roomUrl == null) roomUrl = "https://meet.jit.si/KaraokeStudio-" + roomCode;
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(Color.rgb(2, 12, 38));
        setContentView(buildRoom());
    }

    private View buildRoom() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(18), dp(16), dp(18));
        root.setBackground(gradient(new int[]{Color.rgb(2, 15, 55), Color.rgb(13, 77, 180), Color.rgb(2, 23, 76)}, 0, 0));
        scroll.addView(root);

        LinearLayout header = row();
        TextView back = label("‹", 40, Color.WHITE);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        LinearLayout names = column();
        TextView title = label("🎤  SALA OS VIPS", 21, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        names.addView(title);
        names.addView(label("ID: " + roomCode, 12, Color.rgb(176, 224, 255)));
        header.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView people = pill("👥 1", Color.argb(120, 0, 25, 80));
        header.addView(people);
        root.addView(header, match());

        LinearLayout stats = row();
        stats.addView(pill("🏆 Ranking #1", Color.argb(150, 10, 48, 120)), weighted());
        TextView wallet = pill("💎 " + coins + " moedas", Color.argb(150, 13, 63, 145));
        stats.addView(wallet, weighted());
        root.addView(stats, margins(match(), 0, 12, 0, 12));

        LinearLayout stage = column();
        stage.setGravity(Gravity.CENTER);
        stage.setPadding(dp(18), dp(24), dp(18), dp(24));
        stage.setBackground(gradient(new int[]{Color.rgb(9, 38, 100), Color.rgb(22, 112, 225), Color.rgb(4, 28, 83)}, dp(24), 90));
        stage.addView(label("✦      🔦     ✦     🔦      ✦", 24, Color.rgb(155, 226, 255)), match());
        TextView mic = label("🎙️", 66, Color.WHITE); mic.setGravity(Gravity.CENTER);
        stage.addView(mic, margins(match(), 0, 22, 0, 12));
        TextView motto = label("Aqui respeito e amizade\né o nosso lema.", 25, Color.WHITE);
        motto.setGravity(Gravity.CENTER); motto.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        stage.addView(motto, match());
        TextView welcome = label("Escolha uma música e venha cantar!", 15, Color.rgb(190, 237, 255));
        welcome.setGravity(Gravity.CENTER);
        stage.addView(welcome, margins(match(), 0, 12, 0, 14));
        stage.addView(pill("🎵  PALCO PRINCIPAL", Color.argb(150, 0, 15, 55)));
        root.addView(stage, margins(match(), 0, 0, 0, 12));

        LinearLayout queueBar = row();
        queueBar.setGravity(Gravity.CENTER_VERTICAL);
        queueBar.setPadding(dp(16), dp(10), dp(10), dp(10));
        queueBar.setBackground(gradient(new int[]{Color.rgb(7, 36, 97), Color.rgb(10, 55, 137)}, dp(22), 0));
        queue = label("🎵 Na lista (0)\nEscolha uma música", 17, Color.WHITE);
        queue.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        queueBar.addView(queue, new LinearLayout.LayoutParams(0, -2, 1));
        Button sing = action("CANTAR");
        sing.setOnClickListener(v -> {
            queue.setText("🎵 Na lista (1)\nVocê é o próximo a cantar");
            Toast.makeText(this, "Você entrou na fila!", Toast.LENGTH_SHORT).show();
        });
        queueBar.addView(sing, new LinearLayout.LayoutParams(dp(122), dp(54)));
        root.addView(queueBar, match());

        String suppliedLyrics = getIntent().getStringExtra("lyrics");
        if (suppliedLyrics != null && !suppliedLyrics.trim().isEmpty()) {
            TextView lyricCard = chat("🎼 LETRA DA MÚSICA", suppliedLyrics);
            lyricCard.setGravity(Gravity.CENTER);
            root.addView(lyricCard, margins(match(), 8, 14, 8, 5));
        }

        chatArea = column();
        chatArea.addView(chat("💎 APRESENTADOR", "Bem-vindo! Escolha sua música."), margins(match(), 8, 18, 40, 5));
        chatArea.addView(chat("🎤 " + artist.toUpperCase(), "Respeito é bom e todos nós desejamos."), margins(match(), 42, 5, 8, 8));
        root.addView(chatArea, match());

        LinearLayout composer = row();
        EditText message = new EditText(this);
        message.setHint("Digite no chat..."); message.setTextColor(Color.WHITE); message.setHintTextColor(Color.LTGRAY);
        composer.addView(message, new LinearLayout.LayoutParams(0, dp(56), 1));
        Button send = action("Enviar");
        send.setOnClickListener(v -> {
            String value = message.getText().toString().trim();
            if (!value.isEmpty()) { chatArea.addView(chat("🎤 " + artist.toUpperCase(), value), margins(match(), 42, 5, 8, 5)); message.setText(""); }
        });
        composer.addView(send, new LinearLayout.LayoutParams(dp(100), dp(54)));
        root.addView(composer, margins(match(), 0, 4, 0, 12));

        LinearLayout gifts = row();
        String[] giftNames = {"🌹 Rosa 5", "🏆 Troféu 20", "👑 Coroa 50"};
        for (String giftName : giftNames) {
            TextView gift = pill(giftName, Color.argb(180, 8, 54, 130));
            gift.setOnClickListener(v -> {
                int cost = giftName.contains("50") ? 50 : giftName.contains("20") ? 20 : 5;
                if (coins < cost) { Toast.makeText(this, "Moedas insuficientes", Toast.LENGTH_SHORT).show(); return; }
                coins -= cost; wallet.setText("💎 " + coins + " moedas");
                chatArea.addView(chat("🎁 PRESENTE", artist + " enviou " + giftName));
            });
            gifts.addView(gift, margins(weighted(), 2, 2, 2, 2));
        }
        root.addView(gifts, margins(match(), 0, 0, 0, 12));

        Button admin = action("🛡️ Ativar controles de administrador");
        admin.setOnClickListener(v -> {
            adminMode = !adminMode;
            admin.setText(adminMode ? "🛡️ Administrador ativo: silenciar e remover" : "🛡️ Ativar controles de administrador");
            Toast.makeText(this, adminMode ? "Controles ativados" : "Controles desativados", Toast.LENGTH_SHORT).show();
        });
        root.addView(admin, margins(new LinearLayout.LayoutParams(-1, dp(54)), 0, 0, 0, 10));

        Button live = action("📹  ABRIR ÁUDIO E VÍDEO AO VIVO");
        live.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(roomUrl))));
        root.addView(live, margins(new LinearLayout.LayoutParams(-1, dp(62)), 0, 0, 0, 10));

        LinearLayout bottom = row();
        String[] controls = {"💬 Chat", "🎵 Música", "👥 Amigos", "🔗 Convidar"};
        for (String control : controls) {
            TextView item = label(control, 13, Color.WHITE);
            item.setGravity(Gravity.CENTER);
            if (control.contains("Convidar")) item.setOnClickListener(v -> share());
            if (control.contains("Música")) item.setOnClickListener(v -> {
                Intent pick = new Intent(this, CatalogActivity.class);
                startActivityForResult(pick, PICK_SERVER_SONG);
            });
            bottom.addView(item, weighted());
        }
        root.addView(bottom, margins(match(), 0, 5, 0, 8));
        root.addView(center("Sala de teste • câmera e microfone usam o Jitsi gratuito", 11, Color.rgb(167, 213, 255)), match());
        return scroll;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SERVER_SONG && resultCode == RESULT_OK && data != null) {
            String song = data.getStringExtra("song_title");
            String singer = data.getStringExtra("song_artist");
            String lyrics = data.getStringExtra("song_lyrics");
            String audio = data.getStringExtra("song_audio");
            queue.setText("🎵 Na lista (1)\n" + artist + " — " + song);
            chatArea.addView(chat("📋 FILA", artist + " escolheu " + song + " • " + singer));
            if (lyrics != null && !lyrics.isEmpty()) chatArea.addView(chat("🎼 LETRA", lyrics));
            if (audio != null && !audio.isEmpty()) {
                queue.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(audio))));
                Toast.makeText(this, "Música adicionada. Toque na fila para abrir o playback.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void share() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "Venha cantar comigo na sala " + roomCode + ": " + roomUrl);
        startActivity(Intent.createChooser(i, "Convidar amigos"));
    }

    private TextView chat(String name, String message) {
        TextView view = label(name + "\n" + message, 15, Color.WHITE);
        view.setPadding(dp(14), dp(10), dp(14), dp(10));
        view.setBackground(gradient(new int[]{Color.argb(170, 3, 25, 75), Color.argb(170, 9, 62, 145)}, dp(18), 0));
        return view;
    }

    private Button action(String text) {
        Button b = new Button(this);
        b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(gradient(new int[]{Color.rgb(15, 148, 244), Color.rgb(35, 207, 255)}, dp(28), 0));
        return b;
    }

    private TextView pill(String text, int color) {
        TextView v = label(text, 14, Color.WHITE); v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(20));
        v.setBackground(bg); return v;
    }

    private GradientDrawable gradient(int[] colors, int radius, int angle) {
        GradientDrawable.Orientation orientation = angle == 90 ? GradientDrawable.Orientation.TOP_BOTTOM : GradientDrawable.Orientation.LEFT_RIGHT;
        GradientDrawable d = new GradientDrawable(orientation, colors);
        d.setCornerRadius(radius); d.setStroke(dp(1), Color.argb(90, 125, 220, 255)); return d;
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); return v; }
    private TextView label(String s, int size, int color) { TextView v = new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color); return v; }
    private TextView center(String s, int size, int color) { TextView v = label(s, size, color); v.setGravity(Gravity.CENTER); return v; }
    private LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private LinearLayout.LayoutParams margins(LinearLayout.LayoutParams p, int l, int t, int r, int b) { p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
