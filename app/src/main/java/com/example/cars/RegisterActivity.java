package com.example.cars;
//Created and written by Nurlybek Karshiga

//Modified by Ruslan Shakirov
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Objects;

import io.fabric.sdk.android.Fabric;

public class RegisterActivity extends AppCompatActivity {

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;

    //widgets
    private EditText mEmail, mName, mAge, mPassword, mConfirmPassword;
    private Button mRegister;
    private ProgressBar mProgressBar;

    //vars
    private Context mContext;
    private String email, age, name, password;
    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        //Hide titlebar
        getSupportActionBar().hide();
        setContentView(R.layout.activity_register);
        mRegister = (Button) findViewById(R.id.btn_register);
        mEmail = (EditText) findViewById(R.id.input_email);
        mPassword = (EditText) findViewById(R.id.input_password);
        mConfirmPassword = (EditText) findViewById(R.id.input_confirm_password);
        mName = (EditText) findViewById(R.id.input_name);
        mAge = (EditText)findViewById(R.id.input_age);
        mContext = RegisterActivity.this;
        mUser = new User();
        initProgressBar();
        setupFirebaseAuth();
        init();

        hideSoftKeyboard();

    }

    private void init(){


        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                email = mEmail.getText().toString();
                name = mName.getText().toString();
                password = mPassword.getText().toString();
                age = mAge.getText().toString();

                if (checkInputs(email, name, age, password, mConfirmPassword.getText().toString())) {
                    if(doStringsMatch(password, mConfirmPassword.getText().toString())){
                        registerNewEmail(email, password);
                    }else{
                        Toast.makeText(mContext, "passwords do not match", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(mContext, "All fields must be filled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void selectDate(View view) {
        DialogFragment newFragment = new RegisterActivity.SelectDateFragment();
        newFragment.show(getSupportFragmentManager(), "DatePicker");
    }
    public void populateSetDate(int year, int month, int day) {

        mAge.setText(MessageFormat.format("{0}/{1}/{2}", month, day, year));
    }
    @SuppressLint("ValidFragment")
    public class SelectDateFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar calendar = Calendar.getInstance();
            int yy = calendar.get(Calendar.YEAR);
            int mm = calendar.get(Calendar.MONTH);
            int dd = calendar.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(Objects.requireNonNull(getActivity()), this, yy, mm, dd);
        }

        public void onDateSet(DatePicker view, int yy, int mm, int dd) {
            populateSetDate(yy, mm+1, dd);
        }
    }


    /**
     * Return true if @param 's1' matches @param 's2'
     * @param s1
     * @param s2
     * @return
     */
    private boolean doStringsMatch(String s1, String s2){
        return s1.equals(s2);
    }


    /**
     * Checks all the input fields for null
     * @param email
     * @param username
     * @param password
     * @return
     */
    private boolean checkInputs(String email, String username, String age, String password, String confirmPassword){

        if(email.equals("") || username.equals("") || age.equals("")|| password.equals("") || confirmPassword.equals("")){
            Toast.makeText(mContext, "Oops. Required fields are empty.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    private void showProgressBar(){
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        mProgressBar.setVisibility(View.GONE);
    }

    private void initProgressBar(){
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
    }


    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

      /*
    ---------------------------Firebase-----------------------------------------
     */


    private void setupFirebaseAuth(){

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is authenticated
                    name = user.getDisplayName();
                    email = user.getEmail();
                } else {
                    // User is signed out
                }
                // ...
            }};}
    /**
     * Register a new email and password to Firebase Authentication
     * @param email
     * @param password
     */
    public void registerNewEmail(final String email, String password){

        showProgressBar();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {


                        if (task.isSuccessful()){
                            //send email verificaiton
                            sendVerificationEmail();

                            //add user details to firebase database
                            addNewUser();
                        }
                        if (!task.isSuccessful()) {
                            Toast.makeText(mContext, "Someone with that email already exists",
                                    Toast.LENGTH_SHORT).show();
                            hideProgressBar();

                        }
                        hideProgressBar();
                        // ...
                    }
                });
    }

    /**
     * Adds data to the node: "users"
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void addNewUser(){

        //add data to the "users" node
        String userid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();


        mUser.setName(name);
        mUser.setAge(Integer.valueOf(age));
        mUser.setUser_id(userid);


        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users").child(userid);

        //insert into users node
        reference.child("name")
                .setValue(mName.getText().toString());
        reference.child("age")
                .setValue(mAge.getText().toString());


        FirebaseAuth.getInstance().signOut();
        redirectLoginScreen();
    }

    /**
     * sends an email verification link to the user
     */
    public void sendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) { }
                            else{
                                Toast.makeText(mContext, "couldn't send email", Toast.LENGTH_SHORT).show();
                                hideProgressBar();} }
                    }); } }

    /**
     * Redirects the user to the login screen
     */
    private void redirectLoginScreen(){

        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
    }
}
