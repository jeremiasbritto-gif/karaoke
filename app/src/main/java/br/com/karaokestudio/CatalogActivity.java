package br.com.karaokestudio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class CatalogActivity extends Activity {
    private static final String CATALOG_URL = "https://raw.githubusercontent.com/jeremiasbritto-gif/karaoke/main/catalog/catalog.json";
    private final ArrayList<Song> songs = new ArrayList<>();
    private LinearLayout results;
    private EditText search;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(4, 25, 78));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(20));
        root.setBackgroundColor(Color.rgb(5, 31, 91));

        TextView title = text("🎵 Catálogo de músicas", 25, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Pesquise e escolha para entrar na fila", 14, Color.rgb(174, 222, 255)));

        search = new EditText(this);
        search.setHint("Digite o nome da música ou cantor");
        search.setHintTextColor(Color.LTGRAY); search.setTextColor(Color.WHITE); search.setSingleLine(true);
        root.addView(search, margins(new LinearLayout.LayoutParams(-1, dp(58)), 0, 18, 0, 6));

        Button find = button("🔎 PESQUISAR");
        find.setOnClickListener(v -> showMatches());
        root.addView(find, new LinearLayout.LayoutParams(-1, dp(54)));
        status = text("Conectando ao catálogo…", 13, Color.rgb(174, 222, 255));
        root.addView(status, margins(new LinearLayout.LayoutParams(-1, -2), 0, 10, 0, 8));

        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        loadCatalog();
    }

    private void loadCatalog() {
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(CATALOG_URL).openConnection();
                connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder json = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();
                JSONArray array = new JSONArray(json.toString());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject value = array.getJSONObject(i);
                    songs.add(new Song(value.optString("title"), value.optString("artist"),
                            value.optString("audioUrl"), value.optString("lyrics")));
                }
                runOnUiThread(() -> { status.setText(songs.size() + " música(s) disponíveis"); showMatches(); });
            } catch (Exception error) {
                runOnUiThread(() -> status.setText("Catálogo vazio ou sem internet. Cadastre músicas no GitHub."));
            }
        }).start();
    }

    private void showMatches() {
        results.removeAllViews();
        String term = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (Song song : songs) {
            if (!term.isEmpty() && !(song.title + " " + song.artist).toLowerCase(Locale.ROOT).contains(term)) continue;
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(12, 62, 145)); bg.setCornerRadius(dp(18));
            card.setBackground(bg);
            TextView name = text("🎤 " + song.title, 18, Color.WHITE); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(name); card.addView(text(song.artist, 14, Color.rgb(182, 226, 255)));
            Button add = button("ENTRAR NA FILA");
            add.setOnClickListener(v -> choose(song));
            card.addView(add, margins(new LinearLayout.LayoutParams(-1, dp(50)), 0, 8, 0, 0));
            results.addView(card, margins(new LinearLayout.LayoutParams(-1, -2), 0, 5, 0, 8));
            count++;
        }
        if (count == 0) results.addView(text("Nenhuma música encontrada.", 16, Color.WHITE));
    }

    private void choose(Song song) {
        Intent result = new Intent();
        result.putExtra("song_title", song.title); result.putExtra("song_artist", song.artist);
        result.putExtra("song_audio", song.audio); result.putExtra("song_lyrics", song.lyrics);
        setResult(RESULT_OK, result); finish();
    }

    private Button button(String value) { Button b = new Button(this); b.setText(value); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setAllCaps(false); b.setBackgroundColor(Color.rgb(20, 155, 240)); return b; }
    private TextView text(String value, int size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); return v; }
    private LinearLayout.LayoutParams margins(LinearLayout.LayoutParams p, int l, int t, int r, int b) { p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private static class Song { final String title, artist, audio, lyrics; Song(String t, String a, String u, String l) { title=t; artist=a; audio=u; lyrics=l; } }
}
