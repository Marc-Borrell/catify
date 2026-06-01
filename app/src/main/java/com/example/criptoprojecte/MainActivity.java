package com.example.criptoprojecte;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.ActionBarDrawerToggle;


public class MainActivity extends AppCompatActivity {

    private Button btnEncrypt, btnDecrypt, btnSign, btnVerify;
    private SecretKey symmetricKey;
    private KeyPair keyPair;
    private byte[] wrappedKey;
    private byte[] iv;
    private ExecutorService executorService;
    private byte[] symmetricKeyBytes;
    private ClipboardManager clipboardManager;
    private static final int SERVER_PORT = 9090;
    private static final String AES_MODE = "AES/CBC/PKCS5Padding";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int PICK_FILE_REQUEST_ENCRYPT = 1;
    private static final int PICK_FILE_REQUEST_SIGN = 2;
    private static final int PICK_FILE_REQUEST_VERIFY = 3;
    private static final int REQUEST_CODE_DECRYPT = 1001;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;


    private String currentFileName;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Inicialización de vistas
        btnEncrypt = findViewById(R.id.buttonEncrypt);
        btnDecrypt = findViewById(R.id.buttonDecrypt);
        btnSign = findViewById(R.id.buttonSign);
        btnVerify = findViewById(R.id.buttonVerify);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        if (getIntent() != null && "decrypt".equals(getIntent().getStringExtra("action"))) {
            String fileName = getIntent().getStringExtra("fileName");
            if (fileName != null) {
                decryptFile(fileName);
            }
            // Limpiar el intent para evitar repeticiones
            getIntent().removeExtra("action");
            getIntent().removeExtra("fileName");
        }

        // Configurar Toolbar y ActionBarDrawerToggle
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_show_public_key) {
            }
            drawerLayout.closeDrawers();
            return true;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_encrypted_songs) {
                try {
                    // Obtener la clave simétrica desde CryptoUtils
                    SecretKey currentKey = CryptoUtils.getSymmetricKey(MainActivity.this);

                    // Crear el Intent y pasar la clave como extra
                    Intent intent = new Intent(MainActivity.this, EncryptedSongsActivity.class);
                    intent.putExtra("symmetricKey", currentKey.getEncoded());

                    // Iniciar la actividad
                    startActivity(intent);

                } catch (Exception e) {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            else if (id == R.id.nav_show_public_key) {
                showKeyDialog("Clave Pública", getPublicKeyString(), false);
            }
            else if (id == R.id.nav_show_private_key) {
                showKeyDialog("Clave Privada", getPrivateKeyString(), true);
            }
            else if (id == R.id.nav_start_server) {
                startSocketServer();
            }
            else if (id == R.id.nav_send_file) {
                showIpDialog();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // Inicializar servicio de hilos y generar claves en un hilo separado
        executorService = Executors.newFixedThreadPool(2);
        executorService.execute(this::generateKeys);

        btnEncrypt.setOnClickListener(v -> openFileChooser(PICK_FILE_REQUEST_ENCRYPT));
        btnDecrypt.setOnClickListener(v -> {
            // Abrir actividad de canciones cifradas para selección
            Intent intent = new Intent(this, EncryptedSongsActivity.class);
            intent.putExtra("mode", "decrypt");
            startActivityForResult(intent, REQUEST_CODE_DECRYPT);
        });
        btnSign.setOnClickListener(v -> openFileChooser(PICK_FILE_REQUEST_SIGN));
        btnVerify.setOnClickListener(v -> openFileChooser(PICK_FILE_REQUEST_VERIFY));


    }

    private void showIpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enviar archivo cifrado");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Ingresa la IP del destinatario");
        builder.setView(input);

        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String ip = input.getText().toString().trim();
            if (!ip.isEmpty()) {
                File encryptedDir = new File(getFilesDir(), "encrypted");
                File[] files = encryptedDir.listFiles((dir, name) -> name.endsWith(".enc"));

                if (files != null && files.length > 0) {
                    sendFileViaSocket(ip, files[files.length - 1]); // Envía el último archivo cifrado
                } else {
                    Toast.makeText(this, "No hay archivos cifrados para enviar", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private String getPublicKeyString() {
        try {
            KeyPair keyPair = CryptoUtils.getKeyPair(this);
            return Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al obtener clave pública";
        }
    }

    private String getPrivateKeyString() {
        try {
            KeyPair keyPair = CryptoUtils.getKeyPair(this);
            return Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al obtener clave privada";
        }
    }

    private void showKeyDialog(String title, String key, boolean isSensitive) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(key);
        input.setSelection(0, key.length());

        if (isSensitive) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);

        if (isSensitive) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText("Mostrar clave privada");
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    input.setTransformationMethod(null);
                } else {
                    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                input.setSelection(input.getText().length());
            });
            layout.addView(checkBox);
        }

        builder.setView(layout);

        builder.setPositiveButton("Copiar", (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(title, key);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, title + " copiada al portapapeles", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cerrar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Añadir advertencia para claves privadas
        if (isSensitive) {
            new AlertDialog.Builder(this)
                    .setTitle("¡Advertencia de Seguridad!")
                    .setMessage("Compartir tu clave privada compromete la seguridad. ¿Estás seguro de que quieres verla?")
                    .setPositiveButton("Sí", (d, w) -> dialog.show())
                    .setNegativeButton("No", (d, w) -> dialog.cancel())
                    .show();
        } else {
            dialog.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }



    private void openFileChooser(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");  // Solo archivos de audio
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_DECRYPT) {
                // Obtener el nombre del archivo seleccionado
                String selectedFile = data.getStringExtra("selectedFile");
                if (selectedFile != null && !selectedFile.isEmpty()) {
                    decryptFile(selectedFile);
                }
                return;
            }
            final Uri uri = data.getData();


            if (uri != null) {
                currentFileName = getFileName(uri);

                // Usamos hilos para las operaciones con archivos
                executorService.execute(() -> {
                    try {
                        byte[] fileData = readFileFromUri(uri);

                        switch (requestCode) {
                            case PICK_FILE_REQUEST_ENCRYPT:
                                encryptFile(fileData, currentFileName);
                                break;
                            case PICK_FILE_REQUEST_SIGN:
                                signFile(fileData, currentFileName);
                                break;
                            case PICK_FILE_REQUEST_VERIFY:
                                promptForSignatureFile(fileData);
                                break;
                        }
                    } catch (IOException e) {
                        showToast("Error al leer el archivo: " + e.getMessage());
                    }
                });
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private byte[] readFileFromUri(Uri uri) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("No se pudo abrir el archivo");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return outputStream.toByteArray();
    }

    private void generateKeys() {
        try {
            // Comprobar si las claves ya estan generadas
            try {
                symmetricKey = CryptoUtils.getSymmetricKey(this);
                keyPair = CryptoUtils.getKeyPair(this);
                iv = CryptoUtils.getIV(this);

                // Las llaves existen y mostramos un texto para que el usuario lo sepa
                showToast("Keys already generated and loaded");

                // Cogemos los bytes de la clave simetrica para usar en otras partes de la app
                symmetricKeyBytes = symmetricKey.getEncoded();

                // Generamos la clave embolcallada
                Cipher cipher = Cipher.getInstance(RSA_MODE);
                cipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
                wrappedKey = cipher.wrap(symmetricKey);

                return;
            } catch (Exception e) {
                // Si las claves no existen continuamos con la generacion de las claves
            }

            // Genera nuevas claves usando una funcion de la clase CryptoUtils
            keyPair = CryptoUtils.generateAndSaveKeys(this);

            symmetricKey = CryptoUtils.getSymmetricKey(this);
            iv = CryptoUtils.getIV(this);

            // Conseguimos los bytes de la clave simetrica
            symmetricKeyBytes = symmetricKey.getEncoded();

            // Generamos la clave embolcallada
            Cipher cipher = Cipher.getInstance(RSA_MODE);
            cipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
            wrappedKey = cipher.wrap(symmetricKey);

            // Indicamos al usuario que las claves se han generado
            showToast("Keys generated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error generating keys: " + e.getMessage());
        }
    }




    // Función auxiliar para convertir bytes a hexadecimal
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    private void encryptFile(byte[] fileData, String fileName) {
        try {
            // Calcular hash del archivo original
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileHash = digest.digest(fileData);

            // Configurar cifrado AES
            Cipher cipher = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, ivSpec);
            byte[] encryptedData = cipher.doFinal(fileData);

            // Guardar el archivo cifrado, IV y clave envuelta
            File outputDir = new File(getFilesDir(), "encrypted");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Guardar datos encriptados
            File encryptedFile = new File(outputDir, fileName + ".enc");
            try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
                fos.write(encryptedData);
            }

            // Guardar IV
            File ivFile = new File(outputDir, fileName + ".iv");
            try (FileOutputStream fos = new FileOutputStream(ivFile)) {
                fos.write(iv);
            }

            // Guardar clave envuelta
            File keyFile = new File(outputDir, fileName + ".key");
            try (FileOutputStream fos = new FileOutputStream(keyFile)) {
                fos.write(wrappedKey);
            }

            // Guardar hash
            File hashFile = new File(outputDir, fileName + ".hash");
            try (FileOutputStream fos = new FileOutputStream(hashFile)) {
                fos.write(fileHash);
            }

            showToast("Archivo cifrado con éxito: " + encryptedFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error al cifrar: " + e.getMessage());
        }
    }

    public void decryptFile(String encryptedFileName) {
        try {
            File encryptedDir = new File(getFilesDir(), "encrypted");

            // Obtener archivo seleccionado
            File encryptedFile = new File(encryptedDir, encryptedFileName);
            String baseName = encryptedFileName.replace(".enc", "");

            // Obtener archivos relacionados
            File ivFile = new File(encryptedDir, baseName + ".iv");
            File keyFile = new File(encryptedDir, baseName + ".key");
            File hashFile = new File(encryptedDir, baseName + ".hash");

            if (!encryptedFile.exists() || !ivFile.exists() || !keyFile.exists() || !hashFile.exists()) {
                showToast("Faltan archivos necesarios para el descifrado");
                return;
            }

            // Leer datos necesarios
            byte[] encryptedData = readFile(encryptedFile);
            byte[] ivData = readFile(ivFile);
            byte[] wrappedKeyData = readFile(keyFile);
            byte[] originalHash = readFile(hashFile);

            // Desenvolver la clave simétrica
            Cipher rsaCipher = Cipher.getInstance(RSA_MODE);
            rsaCipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
            SecretKey unwrappedKey = (SecretKey) rsaCipher.unwrap(wrappedKeyData, "AES", Cipher.SECRET_KEY);

            // Descifrar con AES
            Cipher aesCipher = Cipher.getInstance(AES_MODE);
            IvParameterSpec ivSpec = new IvParameterSpec(ivData);
            aesCipher.init(Cipher.DECRYPT_MODE, unwrappedKey, ivSpec);
            byte[] decryptedData = aesCipher.doFinal(encryptedData);

            // Verificar integridad
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] newHash = digest.digest(decryptedData);

            if (!MessageDigest.isEqual(originalHash, newHash)) {
                showToast("Advertencia: El archivo parece haber sido modificado");
            }

            // Guardar archivo descifrado
            File outputDir = new File(getFilesDir(), "decrypted");
            if (!outputDir.exists()) outputDir.mkdirs();

            File decryptedFile = new File(outputDir, baseName);
            try (FileOutputStream fos = new FileOutputStream(decryptedFile)) {
                fos.write(decryptedData);
            }

            // Eliminar archivos relacionados del cifrado
            new File(encryptedDir, encryptedFileName).delete();
            new File(encryptedDir, baseName + ".iv").delete();
            new File(encryptedDir, baseName + ".key").delete();
            new File(encryptedDir, baseName + ".hash").delete();

            showToast("Archivo descifrado y eliminado de la lista");

            // Notificar para actualizar lista
            sendBroadcast(new Intent("REFRESH_SONGS_LIST"));

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error al descifrar: " + e.getMessage());
        }
    }

    private void signFile(byte[] fileData, String fileName) {
        try {
            // Crear la firma con la clave privada RSA
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(keyPair.getPrivate());
            signature.update(fileData);
            byte[] digitalSignature = signature.sign();

            // Guardar la firma digital
            File outputDir = new File(getFilesDir(), "signed");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Guardar el archivo original
            File originalFile = new File(outputDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(originalFile)) {
                fos.write(fileData);
            }

            // Guardar la firma
            File signatureFile = new File(outputDir, fileName + ".sig");
            try (FileOutputStream fos = new FileOutputStream(signatureFile)) {
                fos.write(digitalSignature);
            }

            showToast("Firma digital generada con éxito: " + signatureFile.getAbsolutePath());
            Intent refreshIntent = new Intent("REFRESH_SONGS_LIST");
            sendBroadcast(refreshIntent);
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error al firmar el archivo: " + e.getMessage());
        }
    }

    private void promptForSignatureFile(byte[] originalFileData) {
        try {
            File signedDir = new File(getFilesDir(), "signed");
            if (!signedDir.exists()) {
                showToast("No hay archivos firmados disponibles");
                return;
            }

            // Buscar el archivo de firma correspondiente
            File signatureFile = new File(signedDir, currentFileName + ".sig");
            if (!signatureFile.exists()) {
                showToast("No se encontró la firma para este archivo");
                return;
            }

            byte[] signatureData = readFile(signatureFile);
            verifySignature(originalFileData, signatureData);

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error al buscar la firma: " + e.getMessage());
        }
    }

    private void verifySignature(byte[] fileData, byte[] signatureData) {
        try {
            // Verificar la firma con la clave pública RSA
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(keyPair.getPublic());
            signature.update(fileData);
            boolean verified = signature.verify(signatureData);

            if (verified) {
                showToast("¡Firma válida! La integridad del archivo está verificada");
            } else {
                showToast("¡Firma inválida! El archivo puede haber sido modificado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error al verificar la firma: " + e.getMessage());
        }
    }

    private byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        java.io.FileInputStream inputStream = new java.io.FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }

    private void startSocketServer() {
        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(9090);
                runOnUiThread(() -> Toast.makeText(this, "Servidor iniciado en puerto 9090", Toast.LENGTH_SHORT).show());

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        String clientAddress = clientSocket.getInetAddress().getHostAddress();
                        runOnUiThread(() -> Toast.makeText(this, "Conectado a: " + clientAddress, Toast.LENGTH_SHORT).show());

                        // Crear nombres únicos
                        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        String fileName = "received_" + timestamp + ".enc";

                        // Crear y verificar que exista el directorio de archivos encriptados
                        File encryptedDir = new File(getFilesDir(), "encrypted");
                        if (!encryptedDir.exists()) {
                            encryptedDir.mkdirs();
                        }

                        // Guardar el archivo en la carpeta de encriptados
                        File receivedFile = new File(encryptedDir, fileName);

                        InputStream is = clientSocket.getInputStream();
                        FileOutputStream fos = new FileOutputStream(receivedFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }

                        fos.close();
                        is.close();
                        clientSocket.close();

                        // Notificar a la interfaz de usuario para actualizar la lista de canciones
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Archivo recibido: " + fileName, Toast.LENGTH_SHORT).show();
                            // Enviar broadcast para actualizar la lista de canciones encriptadas
                            sendBroadcast(new Intent("REFRESH_SONGS_LIST"));
                        });

                    } catch (IOException e) {
                        runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (IOException e) {
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> Toast.makeText(this, "Error al iniciar servidor: " + errorMsg,
                        Toast.LENGTH_SHORT).show());
            } finally {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {

                    }
                }
            }
        }).start();
    }



    public void sendFileViaSocket(String ip, File file) {
        executorService.execute(() -> {
            try {
                Socket socket = new Socket(ip, 9090);
                OutputStream os = socket.getOutputStream();
                FileInputStream fis = new FileInputStream(file);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                fis.close();
                os.close();

                runOnUiThread(() ->
                        Toast.makeText(this, "Archivo enviado: " + file.getName(), Toast.LENGTH_SHORT).show()
                );

            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

}