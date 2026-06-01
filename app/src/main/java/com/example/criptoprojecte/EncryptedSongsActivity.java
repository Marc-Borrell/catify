package com.example.criptoprojecte;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EncryptedSongsActivity extends AppCompatActivity {

    private ListView listViewSongs;
    private List<SongItem> songItems = new ArrayList<>();
    private SongAdapter adapter;
    private boolean isDecryptMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypted_songs);

        // Verifica si estamos en modo decrypt
        isDecryptMode = getIntent().hasExtra("mode") && "decrypt".equals(getIntent().getStringExtra("mode"));

        listViewSongs = findViewById(R.id.listViewSongs);

        // Crear adaptador
        adapter = new SongAdapter(this, songItems, isDecryptMode);
        listViewSongs.setAdapter(adapter);

        // Cargar canciones
        loadEncryptedSongs();

        // Establecer el listener de clic para los elementos de la lista
        listViewSongs.setOnItemClickListener((parent, view, position, id) -> {
            SongItem selectedSong = songItems.get(position);
            if (isDecryptMode) {
                // En modo decrypt, devolver el nombre del archivo seleccionado
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedFile", selectedSong.getFileName());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                // En modo normal, abrir la actividad de descifrado
                openDecryptActivity(selectedSong.getFileName());
            }
        });
    }

    private void loadEncryptedSongs() {
        songItems.clear();
        File encryptedDir = new File(getFilesDir(), "encrypted");

        if (encryptedDir.exists()) {
            File[] encFiles = encryptedDir.listFiles((dir, name) -> name.endsWith(".enc"));

            if (encFiles != null) {
                for (File file : encFiles) {
                    String baseName = file.getName().replace(".enc", "");
                    boolean isSigned = new File(getFilesDir() + "/signed", baseName + ".sig").exists();
                    songItems.add(new SongItem(file.getName(), isSigned));
                }
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadEncryptedSongs();
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();


        // Para arreglar error en Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                    refreshReceiver,
                    new IntentFilter("REFRESH_SONGS_LIST"),
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(refreshReceiver, new IntentFilter("REFRESH_SONGS_LIST"));
        }

        loadEncryptedSongs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(refreshReceiver);
    }

    public void sendSong(SongItem song) {
        File encryptedFile = new File(getFilesDir() + "/encrypted", song.getFileName());
        if (!encryptedFile.exists()) {
            Toast.makeText(this, "Archivo no encontrado: " + song.getFileName(), Toast.LENGTH_SHORT).show();
            return;
        }
        openDecryptActivity(song.getFileName());
    }

    private void openDecryptActivity(String fileName) {
        Intent intent = new Intent(this, DecryptSongActivity.class);
        intent.putExtra("songFileName", fileName);
        startActivity(intent);
    }
}