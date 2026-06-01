package com.example.criptoprojecte;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptSongActivity extends AppCompatActivity {

    private static final String AES_MODE = "AES/CBC/PKCS5Padding";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";

    private TextView tvSongName;
    private Button btnPlay;
    private Button btnVerify;

    private MediaPlayer mediaPlayer;
    private File decryptedFile;
    private String songFileName;
    private SecretKey symmetricKey;
    private KeyPair keyPair;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt_song);

        // Initialize views
        tvSongName = findViewById(R.id.tvSongName);
        btnPlay = findViewById(R.id.btnPlay);
        btnVerify = findViewById(R.id.btnVerify);

        btnPlay.setEnabled(false);
        btnVerify.setEnabled(false);

        // Get song filename from intent
        songFileName = getIntent().getStringExtra("songFileName");
        if (songFileName == null || songFileName.isEmpty()) {
            Toast.makeText(this, "Error: No song file specified", Toast.LENGTH_SHORT).show();
            Log.e("DecryptSongActivity", "No song filename provided in intent");
            finish();
            return;
        }
        tvSongName.setText(songFileName);

        retrieveKeys();

        new Thread(this::decryptSongFile).start();

        // Set up listeners
        btnPlay.setOnClickListener(v -> playSong());
        btnVerify.setOnClickListener(v -> verifySongSignature());
    }

    private void retrieveKeys() {
        try {
            symmetricKey = CryptoUtils.getSymmetricKey(this);
            keyPair = CryptoUtils.getKeyPair(this);
            Log.d("DecryptSongActivity", "Keys retrieved successfully");
        } catch (Exception e) {
            Toast.makeText(this, "Error retrieving keys: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptSongFile() {
        try {
            String baseName = songFileName.replace(".enc", "");

            File encryptedDir = new File(getFilesDir(), "encrypted");
            File encryptedFile = new File(encryptedDir, songFileName);
            File ivFile = new File(encryptedDir, baseName + ".iv");
            File keyFile = new File(encryptedDir, baseName + ".key");
            File hashFile = new File(encryptedDir, baseName + ".hash");

            if (!encryptedFile.exists() || !ivFile.exists() ||
                    !keyFile.exists() || !hashFile.exists()) {
                runOnUiThread(() -> Toast.makeText(this, "Faltan archivos requeridos para la desencriptación",
                        Toast.LENGTH_SHORT).show());
                return;
            }

            byte[] encryptedData = readFile(encryptedFile);
            byte[] ivData = readFile(ivFile);
            byte[] wrappedKeyData = readFile(keyFile);
            byte[] originalHash = readFile(hashFile);

            // Desempaquetar la clave AES con la clave privada RSA
            Cipher rsaCipher = Cipher.getInstance(RSA_MODE);
            rsaCipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
            SecretKey unwrappedKey = (SecretKey) rsaCipher.unwrap(wrappedKeyData, "AES", Cipher.SECRET_KEY);

            // Desencriptar con AES
            Cipher aesCipher = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivSpec = new IvParameterSpec(ivData);
            aesCipher.init(Cipher.DECRYPT_MODE, unwrappedKey, ivSpec);
            byte[] decryptedData = aesCipher.doFinal(encryptedData);

            // Verificar la integridad
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] newHash = digest.digest(decryptedData);
            if (!MessageDigest.isEqual(originalHash, newHash)) {
                runOnUiThread(() -> Toast.makeText(this, "¡Atención! El archivo puede haber sido modificado",
                        Toast.LENGTH_SHORT).show());
            }

            // Guardar el archivo desencriptado
            File outputDir = new File(getFilesDir(), "temp");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            decryptedFile = new File(outputDir, baseName);
            try (FileOutputStream fos = new FileOutputStream(decryptedFile)) {
                fos.write(decryptedData);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Archivo desencriptado correctamente", Toast.LENGTH_SHORT).show();
                btnPlay.setEnabled(true);
                btnVerify.setEnabled(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error al desencriptar: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show());
        }
    }



    private void playSong() {
        try {
            if (decryptedFile == null || !decryptedFile.exists()) {
                Toast.makeText(this, "Error: Decrypted file not available",
                        Toast.LENGTH_SHORT).show();
                Log.e("DecryptSongActivity", "Decrypted file is null or doesn't exist in playSong");
                return;
            }

            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    Toast.makeText(this, "Canción pausada", Toast.LENGTH_SHORT).show();
                    btnPlay.setText("Reproducir");
                    return;
                } else if (mediaPlayer.getCurrentPosition() > 0) {
                    mediaPlayer.start();
                    Toast.makeText(this, "Reproduciendo canción", Toast.LENGTH_SHORT).show();
                    btnPlay.setText("Pausar");
                    return;
                } else {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }

            mediaPlayer = new MediaPlayer();

            Log.d("DecryptSongActivity", "Playing file: " + decryptedFile.getAbsolutePath());

            mediaPlayer.setDataSource(decryptedFile.getAbsolutePath());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("DecryptSongActivity", "MediaPlayer error: what=" + what + ", extra=" + extra);
                Toast.makeText(this, "Media player error: " + what, Toast.LENGTH_SHORT).show();
                return false;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                btnPlay.setText("Reproducir");
            });

            mediaPlayer.prepare();
            mediaPlayer.start();
            btnPlay.setText("Pausar");
            Toast.makeText(this, "Reproduciendo canción", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error playing file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void verifySongSignature() {
        try {
            // Obtener el nombre base sin la extension .enc
            String baseName = songFileName.replace(".enc", "");

            // Comprobar si el archivo esta firmado
            File signedDir = new File(getFilesDir(), "signed");
            File originalFile = new File(signedDir, baseName);
            File signatureFile = new File(signedDir, baseName + ".sig");

            if (!originalFile.exists() || !signatureFile.exists()) {
                Toast.makeText(this, "Signature files not found",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Leer archivos
            byte[] originalData = readFile(originalFile);
            byte[] signatureData = readFile(signatureFile);

            // Verificar signatura
            java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initVerify(keyPair.getPublic());
            signature.update(originalData);
            boolean verified = signature.verify(signatureData);

            if (verified) {
                Toast.makeText(this, "Signature valid! File integrity verified",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid signature! File may have been modified",
                        Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Error verifying signature: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (decryptedFile != null && decryptedFile.exists()) {
            decryptedFile.delete();
        }
    }
}