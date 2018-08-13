package kh.com.rupp.ckcc.photoblock;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import id.zelory.compressor.Compressor;

public class NewPostActivity extends AppCompatActivity {

    private static final int MAX_LENGTH = 100;
    private Toolbar newPostToolbar;
    private ImageView newPostImage;
    private EditText newPostDesc;
    private Button newPostBtn;
    private Uri postImageUri = null;
    private ProgressBar newPostProgress;

    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth mAuth;

    String current_user_id;

    private Bitmap compressedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        current_user_id = mAuth.getCurrentUser().getUid();

        newPostToolbar = findViewById(R.id.new_post_toolbar);
        setSupportActionBar(newPostToolbar);
        getSupportActionBar().setTitle("Add new post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        newPostImage = findViewById(R.id.new_post_image);
        newPostDesc = findViewById(R.id.new_post_des);
        newPostBtn = findViewById(R.id.new_post_btn);
        newPostProgress = findViewById(R.id.new_post_progress);

        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512,512)
                        .setAspectRatio(1,1)
                        .start(NewPostActivity.this);
            }
        });

        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String desc = newPostDesc.getText().toString();
                if(!TextUtils.isEmpty(desc) && postImageUri != null){
                    newPostProgress.setVisibility(View.VISIBLE);
                    final String randomName = UUID.randomUUID().toString();

                    StorageReference filepath = storageReference.child("post_images").child(randomName + ".jpg");
                    filepath.putFile(postImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                            final String downloadUri = task.getResult().getDownloadUrl().toString();
                            if(task.isSuccessful()){
                                File newImageFile = new File(postImageUri.getPath());
                                try {
                                    compressedImageFile = new Compressor(NewPostActivity.this)
                                            .setMaxWidth(200)
                                            .setMaxHeight(200)
                                            .setQuality(10)
                                            .compressToBitmap(newImageFile);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] thumbData = baos.toByteArray();

                                UploadTask uploadTask = storageReference.child("post_images/thumbs").child(randomName + ".jpg").putBytes(thumbData);

                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                        String downlaodthumbUri = taskSnapshot.getDownloadUrl().toString();

                                        Map<String , Object> postMap = new HashMap<>();
                                        postMap.put("image_url",downloadUri);
                                        postMap.put("thumb",downlaodthumbUri);
                                        postMap.put("Desc", desc);
                                        postMap.put("User_id",current_user_id);
                                        postMap.put("timestamp", FieldValue.serverTimestamp());

                                        firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                if(task.isSuccessful()){

                                                    Toast.makeText(NewPostActivity.this,"Post was added.",Toast.LENGTH_LONG).show();
                                                    Intent mainIntent = new Intent(NewPostActivity.this,MainActivity.class);
                                                    startActivity(mainIntent);
                                                    finish();
                                                }else {
                                                    String error = task.getException().getMessage();
                                                    Toast.makeText(NewPostActivity.this, "(Post Error) : " + error, Toast.LENGTH_LONG).show();
                                                }
                                                newPostProgress.setVisibility(View.INVISIBLE);
                                            }
                                        });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });


                            }else{
                                newPostProgress.setVisibility(View.INVISIBLE);
                                String error = task.getException().getMessage();
                                Toast.makeText(NewPostActivity.this, "(Image Error) : " + error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                postImageUri = result.getUri();
                newPostImage.setImageURI(postImageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    // Generate Name for photo name in post storage
    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(MAX_LENGTH);
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }
}
