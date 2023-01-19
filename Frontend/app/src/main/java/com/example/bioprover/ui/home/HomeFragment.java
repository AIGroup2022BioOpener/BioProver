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
import android.os.StrictMode;
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
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.Bitmap;


public class HomeFragment extends Fragment {

    Bitmap pictureTake=null;
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
                debug("button pressed");
                if (imgCapture.getDrawable() != null){
//                    URL url = null;
                    debug("image exists");
//                    String url = "http://10.0.2.2:5000/authenticate/type/picture";
                    String url = "http://192.168.233.1:5000/authenticate/type/picture";
                    HttpURLConnection connection = null;
                    try {
//                        url = new URL("http://10.0.2.2:5000/authenticate/type/picture");

                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

                        StrictMode.setThreadPolicy(policy);
//                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

//                        String base64 = Base64.getEncoder().encodeToString(array);
                        debug("making data");
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        //takes picture puts it into outputstream
                        pictureTake.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        String encoded = Base64.getEncoder().encodeToString(byteArray);

                        //make json object 'data'
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("image", encoded);
                        byte[] data = jsonObject.toString().getBytes("UTF-8");



                        connection = (HttpURLConnection) new URL(url).openConnection();

                        connection.setRequestMethod("POST");

                        connection.setRequestProperty("Content-Type", "application/json");

                        connection.setRequestProperty("Content-Length", Integer.toString(data.length));
                        connection.setDoOutput(true);
//                        debug(data);
                        connection.getOutputStream().write(data);
                        debug("test after connection4");
                        InputStream inputStream = connection.getInputStream();
                        debug(inputStream.toString());
                        debug("test after connection5");

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }

                }

            }
        });



    }

//    private void httpCall(){
//        ObjectMapper mapper = new ObjectMapper();
//
//        OkHttpClient client = new OkHttpClient();
//
//        Request request = new Request.Builder()
//           .url("https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY")
//           .build(); // defaults to GET
//
//        Response response = client.newCall(request).execute();
//
//        APOD apod = mapper.readValue(response.body().byteStream(), APOD.class);
//
//        System.out.println(apod.title);
//
//    }

//    public class Example {
//        public void sendImage(Bitmap image, String url) throws JSONException {
//
//            HttpURLConnection connection = null;
//
//            try {
//                String base64 = Base64.getEncoder().encodeToString(array);
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//                byte[] byteArray = byteArrayOutputStream .toByteArray();
//                String encoded = Base64.encodeToString(byteArray);
//                byte[] data = jsonObject.toString().getBytes("UTF-8");
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("image", encoded);
//
//                connection = (HttpURLConnection) new URL(url).openConnection();
//                connection.setRequestMethod("POST");
//                connection.setRequestProperty("Content-Type", "application/json");
//                connection.setRequestProperty("Content-Length", Integer.toString(data.length));
//                connection.setDoOutput(true);
//                connection.getOutputStream().write(data);
//                InputStream inputStream = connection.getInputStream();
//                // Do something with the response here
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            } finally {
//                if (connection != null) {
//                    connection.disconnect();
//                }
//            }
//        }
//    }


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


    public Bitmap grabImage(ImageView imageView)
    {
        this.getActivity().getContentResolver().notifyChange(mImageUri, null);
        ContentResolver cr = this.getActivity().getContentResolver();
        Bitmap bitmap=null;
        try
        {
            bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, mImageUri);
            imageView.setImageBitmap(bitmap);
        }
        catch (Exception e)
        {

        }
        return bitmap;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void debug(String msg){
        Log.e("picture",msg);
        Toast.makeText(this.getContext(),msg,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Image_Capture_Code) {
            if (resultCode == RESULT_OK) {
                //... some code to inflate/create/find appropriate ImageView to place grabbed image
                pictureTake=this.grabImage(imgCapture);
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
