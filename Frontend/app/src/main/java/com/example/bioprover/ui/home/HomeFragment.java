package com.example.bioprover.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.bioprover.R;
import com.example.bioprover.databinding.FragmentHomeBinding;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

public class HomeFragment extends Fragment {


    private FragmentHomeBinding binding;
    private ImageView imgCapture;
    private static final int Image_Capture_Code = 121;
    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private Uri mImageUri;

    private Activity currentActivity=null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        currentActivity=this.getActivity();

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        imgCapture= binding.capturedImage;
        final Button photoButton = binding.Photobutton;

        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                requestPermissionsIfNecessary(currentActivity,new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
                });

            }
        });

        final Button UploadButton = binding.UpLoad;

        UploadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (imgCapture.getDrawable() != null){
                    URL url = null;
                    try {
                        url = new URL("http://www.android.com/");
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        try {
                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                            Log.e("Input",in.toString());
                        } finally {
                            urlConnection.disconnect();
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
        });



    }


    private File createTemporaryFile(String part, String ext) throws Exception
    {
        File tempDir= currentActivity.getExternalFilesDir(null);
        Log.e("filepath",tempDir.getAbsolutePath());
        tempDir=new File(tempDir.getAbsolutePath()+"/temp/");
        if(!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        return File.createTempFile(part, ext, tempDir);
    }
    public void requestPermissionsIfNecessary(Activity c, String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(c, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    c,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
        else{
            Intent cInt = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cInt.putExtra("android.intent.extras.CAMERA_FACING", 1);
            File photo=null;

            // place where to store camera taken picture
            try {
                photo = this.createTemporaryFile("picture", ".jpg");
            } catch (Exception e) {
                e.printStackTrace();
            }
            photo.delete();

            mImageUri =  FileProvider.getUriForFile(currentActivity,currentActivity.getApplicationContext().getPackageName() + ".provider",photo);
            cInt.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
            startActivityForResult(cInt,Image_Capture_Code);
        }
    }


    public void grabImage(ImageView imageView)
    {
        this.getActivity().getContentResolver().notifyChange(mImageUri, null);
        ContentResolver cr = this.getActivity().getContentResolver();
        Bitmap bitmap;
        try
        {
            bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, mImageUri);
            imageView.setImageBitmap(bitmap);
        }
        catch (Exception e)
        {

        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Image_Capture_Code) {
            if (resultCode == RESULT_OK) {
                //... some code to inflate/create/find appropriate ImageView to place grabbed image
                this.grabImage(imgCapture);
                Log.e("picture","okay");
            } else if (resultCode == RESULT_CANCELED) {
                Log.e("picture","Failes");
                Toast.makeText(this.getContext(),"canceled",Toast.LENGTH_SHORT).show();
            }
            else {
                Log.e("picture","Bug");

            }
        }
    }
}