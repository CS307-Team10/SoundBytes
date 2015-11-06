package com.soundbytes;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button bLogout, bAddFriend, bSend;
    EditText etName, etAge, etUsername, etAddFriend;
    UserLocalStore userLocalStore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etName = (EditText)findViewById(R.id.etName);
        etAge = (EditText)findViewById(R.id.etAge);
        etUsername = (EditText)findViewById(R.id.etUsername);
        etAddFriend = (EditText)findViewById(R.id.etAddFriend);
        bLogout = (Button)findViewById(R.id.bLogout);
        bLogout.setOnClickListener(this);
        bAddFriend = (Button)findViewById(R.id.bAddFriend);
        bAddFriend.setOnClickListener(this);
        bSend = (Button)findViewById(R.id.bSend);
        bSend.setOnClickListener(this);
        userLocalStore = new UserLocalStore(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("loginStatus: " + userLocalStore.getUserLoggedIn());
        if(authenticate()){
            displayUserDetails();
        } else {
            startActivity(new Intent(MainActivity.this, Login.class));
        }
    }

    private boolean authenticate(){
        if (!userLocalStore.getUserLoggedIn() || userLocalStore.getLoggedInUser() == null) {
            System.out.println("user not logged in");
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            return false;
        }
        return true;
    }

    private void displayUserDetails(){
        User user = userLocalStore.getLoggedInUser();
        etUsername.setText(user.username);
        etName.setText(user.name);
        etAge.setText(user.age + "");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bLogout:
                userLocalStore.clearUserData();
                userLocalStore.setUserLoggedin(false);
                startActivity(new Intent(this, Login.class));

                break;
            case R.id.bAddFriend:
                String friendName = etAddFriend.getText().toString();
                User cUser = userLocalStore.getLoggedInUser();
                String currentUser = cUser.username;
                User user = new User(friendName, currentUser, -2);
                authenticate(user);
                break;
            case R.id.bSend:
                User cuUser = userLocalStore.getLoggedInUser();
                String uName = cuUser.username;
                Intent intent = new Intent(MainActivity.this, SendActivity.class);
                intent.putExtra("uName", uName);
                System.out.println("uName" + uName);
                startActivity(intent);
                break;
        }
    }

    private void authenticate(User user){
        ServerRequests serverRequests = new ServerRequests(this);
        serverRequests.addFriendDataInBackground(user, new GetUserCallBack() {
            @Override
            public void done(User returnedUser) {
                if (returnedUser == null) {
                    showErrorMessage();
                } else {
                    System.out.println("name:" + returnedUser.name);
                    System.out.println("name:" + returnedUser.age);
                    System.out.println("added friend successfully!");
                    showSuccessMessage();
                }
            }
        });
    }

    private void showErrorMessage(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setMessage("nonexistent friend");
        dialogBuilder.setPositiveButton("ok", null);
        dialogBuilder.show();
    }

    private void showSuccessMessage(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setMessage("Friend added successfully");
        dialogBuilder.setPositiveButton("ok", null);
        dialogBuilder.show();
    }

}
