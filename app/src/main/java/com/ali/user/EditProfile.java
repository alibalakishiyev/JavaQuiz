package com.ali.user;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.systemIn.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    public static  final  String TAG = "TAG";
    EditText proName, proEmail, proPhone;
    ImageView proImage;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    ImageButton saveProBtn,backProBtn;
    FirebaseUser user;
    StorageReference storageReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        Intent data = getIntent();
        String mName = data.getStringExtra("name");
        String mEmail = data.getStringExtra("email");
        String mPhone = data.getStringExtra("phone");

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        user = fAuth.getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference();

        proName = findViewById(R.id.editNamePro);
        proEmail = findViewById(R.id.editEmailPro);
        proPhone = findViewById(R.id.editPhonePro);
        proImage = findViewById(R.id.editimgPro);
        saveProBtn = findViewById(R.id.editSaveProf);
        backProBtn = findViewById(R.id.editBackPro);

        StorageReference profileRef = storageReference.child("users/"+fAuth.getCurrentUser().getUid()+"profile.jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(proImage);
            }
        });

        proImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(openGalleryIntent,1000);            }
        });

        saveProBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (proName.getText().toString().isEmpty() || proEmail.getText().toString().isEmpty() || proPhone.getText().toString().isEmpty()){
                    Toast.makeText(EditProfile.this, "Bütün sahələri doldurun", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String newEmail = proEmail.getText().toString();

                // Əgər email dəyişmirsə, sadəcə digər məlumatları yenilə
                if (newEmail.equals(user.getEmail())) {
                    updateProfileWithoutEmail();
                    return;
                }

                // Şifrə tələb edən dialoq göstər
                showPasswordDialog(newEmail);
            }
        });


        backProBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EditProfile.this,ProfileActivity.class);
                startActivity(intent);
            }
        });



        proEmail.setText(mEmail);
        proName.setText(mName);
        proPhone.setText(mPhone);

        Log.d(TAG,"onCreate; "+mName+" "+mEmail+" "+mPhone);

    }
    private void showPasswordDialog(final String newEmail) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Təsdiqləmə");
        builder.setMessage("Email dəyişikliyi üçün hazırkı şifrənizi daxil edin");

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput);

        builder.setPositiveButton("Təsdiqlə", (dialog, which) -> {
            String password = passwordInput.getText().toString().trim();
            if (!password.isEmpty()) {
                reauthenticateAndUpdateEmail(newEmail, password);
            } else {
                Toast.makeText(EditProfile.this, "Şifrə daxil edin", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Ləğv et", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
    private void reauthenticateAndUpdateEmail(final String newEmail, String password) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Email dəyişikliyi
                    user.updateEmail(newEmail)
                            .addOnSuccessListener(aVoid1 -> {
                                // Firestore-da emaili yenilə
                                updateFirestoreEmail(newEmail);
                                Toast.makeText(EditProfile.this, "Email uğurla dəyişdirildi", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(EditProfile.this, "Email dəyişikliyi uğursuz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Email update failed", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfile.this, "Autentifikasiya uğursuz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Reauthentication failed", e);
                });
    }
    private void updateProfileWithoutEmail() {
        DocumentReference docRef = fStore.collection("users").document(user.getUid());
        Map<String, Object> updates = new HashMap<>();
        updates.put("fName", proName.getText().toString());
        updates.put("phone", proPhone.getText().toString());

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfile.this, "Profil uğurla yeniləndi", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfile.this, "Yeniləmə uğursuz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Profile update failed", e);
                });
    }

    private void updateFirestoreEmail(String newEmail) {
        DocumentReference docRef = fStore.collection("users").document(user.getUid());
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", newEmail);
        updates.put("fName", proName.getText().toString());
        updates.put("phone", proPhone.getText().toString());

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfile.this, "Profil uğurla yeniləndi", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfile.this, "Email Firestore-da yenilənmədi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Firestore email update failed", e);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000){
            if (resultCode == Activity.RESULT_OK){
                Uri imageUri = data.getData();
//                profileImage.setImageURI(imageUri);

                uploadImageToFirebase(imageUri);

            }
        }

    }


    private void uploadImageToFirebase(Uri imageUri) {
        //upload image to firebase storge
        StorageReference fileRef = storageReference.child("users/"+fAuth.getCurrentUser().getUid()+"profile.jpg");
        fileRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(EditProfile.this,"Image Uploaded",Toast.LENGTH_SHORT).show();
                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get().load(uri).into(proImage);
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditProfile.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });

    }
}