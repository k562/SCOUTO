package com.kaoshik.scouto;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {


    public static final String TAG = "TAG";
    EditText mFullName, mEmail, mPassword, mPhone;
    Button mRegister;
    TextView mLoginbtn, mtermc, mpprivacypolicy;
    ProgressBar progressBar;
    CheckBox mcheckbox;




    FirebaseFirestore fstore;
    String userID;
    FirebaseAuth fAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        mFullName = findViewById(R.id.FullName);
        mEmail = findViewById(R.id.Email);
        mPassword = findViewById(R.id.Password);
        mPhone = findViewById(R.id.Phone);
        mRegister = findViewById(R.id.Register);
        mLoginbtn = findViewById(R.id.Loginbtn);
        progressBar = findViewById(R.id.progressBar);
        mcheckbox = findViewById(R.id.checkbox);
        mtermc = findViewById(R.id.termsandcondition);
        mpprivacypolicy = findViewById(R.id.pprivacypolicy);


        fAuth = FirebaseAuth.getInstance();
        fstore = FirebaseFirestore.getInstance();


        if (fAuth.getCurrentUser() != null) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }


        mLoginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Login.class));
            }
        });







        mcheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {


                    mRegister.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {


                            final String Email = mEmail.getText().toString().trim();
                            String Password = mPassword.getText().toString().trim();
                            final String FullName = mFullName.getText().toString();
                            final String Phone = mPhone.getText().toString();






                            if (TextUtils.isEmpty(Email)) {
                                mEmail.setError("Email is Required");
                                return;
                            }
                            if (TextUtils.isEmpty(Password)) {
                                mPassword.setError("Password is Required");
                                return;
                            }

                            if (Password.length() < 6) {
                                mPassword.setError("Password Must Be Less Then 6 Character");
                            }


                            progressBar.setVisibility(VISIBLE);


                            fAuth.createUserWithEmailAndPassword(Email, Password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {


                                        FirebaseUser fuser = fAuth.getCurrentUser();
                                        fuser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(getApplicationContext(), "Register Succesful", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d(TAG, "Onfailure: Email Not Sent" + e.getMessage());
                                            }
                                        });


                                        Toast.makeText(getApplicationContext(), "User Created", Toast.LENGTH_SHORT).show();
                                        userID = fAuth.getCurrentUser().getUid();
                                        DocumentReference documentReference = fstore.collection("user").document(userID);
                                        Map<String, Object> user = new HashMap<>();
                                        user.put("FullName", FullName);
                                        user.put("Email", Email);
                                        user.put("Phone", Phone);
                                        user.put("PASSWORD", Password);
                                        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {

                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d(TAG, "onfailure : " + e.toString());
                                            }
                                        });

                                        startActivity(new Intent(getApplicationContext(), MainActivity.class));


                                    } else {
                                        Toast.makeText(Register.this, "Error Please Try Again", Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(GONE);
                                        mtermc.setTextColor(Integer.parseInt("#FF0000"));
                                    }
                                }
                            });


                        }
                    });

                } else {
                    mRegister.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(Register.this, "AGREE TERMS AND CONDITIONS TO CONTINUE", Toast.LENGTH_SHORT).show();
                        }
                    });


                }
            }
        });


    }
}