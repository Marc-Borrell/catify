package com.example.criptoprojecte;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText editTextEmail;
    private Button buttonResetPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Referencias a los elementos
        editTextEmail = findViewById(R.id.editTextEmail);
        buttonResetPassword = findViewById(R.id.buttonResetPassword);

        // Acción del botón para recuperar la contraseña
        buttonResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = editTextEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa tu correo electrónico", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enviar correo de recuperación de contraseña
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Correo enviado. Revisa tu bandeja de entrada", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Error al enviar correo de recuperación", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
