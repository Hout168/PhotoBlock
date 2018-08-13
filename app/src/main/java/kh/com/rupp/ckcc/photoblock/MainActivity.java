package kh.com.rupp.ckcc.photoblock;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity implements Serializable{

    private Toolbar mainToolBar;
    private FloatingActionButton addPostBtn;
    private String current_user_id;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;

    private BottomNavigationView mainBottomNav;

    private HomeFragment homeFragment;
    private NotificationFragment notificationFragment;
    private AccountFragment accountFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        mainToolBar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolBar);
        getSupportActionBar().setTitle("Photo Blog");

        firebaseFirestore = FirebaseFirestore.getInstance();
        addPostBtn = findViewById(R.id.add_post_btn);

        addPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newPostIntent = new Intent(MainActivity.this,NewPostActivity.class);
                startActivity(newPostIntent);
            }
        });

        mainBottomNav = findViewById(R.id.mainBottomNav);

        //FRAGMENT
        homeFragment = new HomeFragment();
        notificationFragment = new NotificationFragment();
        accountFragment = new AccountFragment();

        mainBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.bottom_action_home:
                        replaceFragment(homeFragment);
                        return true;
                    case R.id.bottom_action_notification:
                        replaceFragment(notificationFragment);
                        return true;
                    case R.id.bottom_action_account:
                        replaceFragment(accountFragment);
                        return true;

                        default:
                            return false;
                }

            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null){
            sendToLogin();
        }else{
            current_user_id = mAuth.getCurrentUser().getUid();
            firebaseFirestore.collection("Users").document(current_user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){

                        if(!task.getResult().exists()){

                            Intent setupIntent = new Intent(MainActivity.this,SetupActivity.class);
                            startActivity(setupIntent);
                            finish();

                        }

                    }else{
                        String error = task.getException().getMessage();
                        Toast.makeText(MainActivity.this,"Error : " + error,Toast.LENGTH_LONG).show();
                    }
                }
            });


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_logout_btn:

                logOut();
                return true;
            case R.id.action_settings_btn:
                Intent setupIntent = new Intent(MainActivity.this,SetupActivity.class);
                startActivity(setupIntent);
            default:
                return false;
        }
    }

    private void logOut() {

        mAuth.signOut();
        sendToLogin();

    }

    private void sendToLogin() {
        Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    private void replaceFragment(Fragment fragment){

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.mainContainer, fragment);
        fragmentTransaction.commit();
    }
}
