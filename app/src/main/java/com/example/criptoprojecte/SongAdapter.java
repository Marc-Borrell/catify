package com.example.criptoprojecte;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

public class SongAdapter extends ArrayAdapter<SongItem> {
    private boolean isDecryptMode;

    public SongAdapter(Context context, List<SongItem> songs, boolean isDecryptMode) {
        super(context, 0, songs);
        this.isDecryptMode = isDecryptMode;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_song, parent, false);
        }

        SongItem song = getItem(position);
        TextView tvFileName = convertView.findViewById(R.id.tvFileName);
        TextView tvSignatureStatus = convertView.findViewById(R.id.tvSignatureStatus);
        Button btnSend = convertView.findViewById(R.id.btnSend);

        tvFileName.setText(song.getFileName());
        tvSignatureStatus.setText(song.isSigned() ? "Firmada" : "No firmada");

        // Cambiar la visibilidad del botón según el modo
        if (isDecryptMode) {
            btnSend.setVisibility(View.GONE); // Ocultar el botón en modo decrypt
        } else {
            btnSend.setVisibility(View.VISIBLE);
            btnSend.setOnClickListener(v -> {
                if (getContext() instanceof EncryptedSongsActivity) {
                    ((EncryptedSongsActivity) getContext()).sendSong(song);
                }
            });
        }

        return convertView;
    }
}