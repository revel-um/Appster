package com.revel.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.revel.chat.AuthenticationActivity.phoneUser;

public class CodeActivity extends AppCompatActivity {
    LinearLayout up, down;
    Button bt;
    EditText et;
    String code;
    FirebaseAuth mAuth;
    ImageView logo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);
        Bundle bundle = getIntent().getExtras();
        final String verificationId = bundle.getString("vi");
        up = findViewById(R.id.up);
        down = findViewById(R.id.down);
        et = findViewById(R.id.txtPhone);
        bt = findViewById(R.id.btnLogin);
        mAuth = FirebaseAuth.getInstance();

        logo = findViewById(R.id.logo);
        Glide.with(this).load(R.drawable.logo).circleCrop().into(logo);

        up.animate().setDuration(1000).translationY(0);
        down.animate().setDuration(500).translationY(0);

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                code = et.getText().toString();
                if (code.trim().length() != 0) {
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                    signInWithPhoneAuthCredential(credential);
                } else {
                    et.setError("Enter code here");
                    et.requestFocus();
                }
            }
        });


    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            DatabaseReference creteUser = FirebaseDatabase.getInstance().getReference("Registered number");
                            creteUser.child(phoneUser).setValue("num");
                            Intent intent = new Intent(CodeActivity.this, FragmentMainPage.class);
                            startActivity(intent);
                            finish();
                            // ...
                        } else {
                            Intent intent = new Intent(CodeActivity.this,AuthenticationActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            // Sign in failed, display a message and update the UI
                            Toast.makeText(CodeActivity.this, "signInWithCredential:failure", Toast.LENGTH_SHORT).show();
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(CodeActivity.this, "Wrong code", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

}
