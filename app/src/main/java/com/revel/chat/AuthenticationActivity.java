package com.revel.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class AuthenticationActivity extends AppCompatActivity {
    EditText et;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks;
    private FirebaseAuth mAuth;
    public static String phoneUser;
    Button bt;
    ImageView logo;
    LinearLayout up, down;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticaton);
        et = findViewById(R.id.txtPhone);
        mAuth = FirebaseAuth.getInstance();
        SharedPreferences sp = getSharedPreferences("phoneUser", MODE_PRIVATE);
        phoneUser = sp.getString("phoneUser", "");

        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(AuthenticationActivity.this, FragmentMainPage.class);
            startActivity(intent);
            finish();
        }

        up = findViewById(R.id.up);
        down = findViewById(R.id.down);

        logo = findViewById(R.id.logo);
        Glide.with(this).load(R.drawable.logo).circleCrop().into(logo);

        up.animate().setDuration(1000).translationY(0);
        down.animate().setDuration(500).translationY(0);

        bt = findViewById(R.id.btnLogin);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phoneUser = et.getText().toString();
                SharedPreferences sp = getSharedPreferences("phoneUser", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("phoneUser", phoneUser);
                editor.apply();
                if (phoneUser.trim().length() != 0) {
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            "+91" + phoneUser,        // Phone number to verify
                            60,                 // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            AuthenticationActivity.this,               // Activity (for callback binding)
                            mCallBacks);        // OnVerificationStateChangedCallbacks
                } else {
                    et.setError("Enter phoneFriend number here");
                    et.requestFocus();
                }
            }
        });

        mCallBacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                DatabaseReference creteUser = FirebaseDatabase.getInstance().getReference("Registered number");
                creteUser.child(phoneUser).setValue("num");
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(AuthenticationActivity.this, "Invalid request", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(AuthenticationActivity.this, "Limit for this project is exceeded", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                Intent intent = new Intent(AuthenticationActivity.this, CodeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra("vi", s);
                startActivity(intent);
                finish();
            }
        };

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Intent intent = new Intent(AuthenticationActivity.this, FragmentMainPage.class);
                            startActivity(intent);
                            finish();
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            Toast.makeText(AuthenticationActivity.this, "signInWithCredential:failure", Toast.LENGTH_SHORT).show();
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(AuthenticationActivity.this, "Wrong code", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
}
