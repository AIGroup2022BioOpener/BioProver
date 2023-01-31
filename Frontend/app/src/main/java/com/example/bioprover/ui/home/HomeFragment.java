package com.example.bioprover.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.bioprover.R;
import com.example.bioprover.databinding.FragmentHomeBinding;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;


public class HomeFragment extends Fragment {

    Bitmap pictureTake=null;
    private FragmentHomeBinding binding;
    private ImageView imgCapture;
    private static final int Image_Capture_Code = 121;
    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private Uri mImageUri;
    private Bitmap faceBitmap;
    private String currentModel;




    private Activity currentActivity=null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        String[] visibilities = getResources().getStringArray(R.array.models);

        currentActivity = this.getActivity();
        final Button enrollBtn = binding.Enroll;
        final EditText username = binding.username;
        enrollBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (username.getText().toString().isEmpty()) {
                    Toast.makeText(root.getContext(), "Please type in a username", Toast.LENGTH_SHORT).show();
                } else if (faceBitmap == null ){
                    Toast.makeText(root.getContext(), "Please take a picture", Toast.LENGTH_SHORT).show();
                } else if (!username.getText().toString().isEmpty() && faceBitmap != null) {
                    String url = "http://10.0.2.2:5000/register";
                    HttpURLConnection connection = null;
                    debug("enrolling user");
                    try {
                        currentModel = binding.autoCompleteTextView.getText().toString();

                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

                        StrictMode.setThreadPolicy(policy);
                        debug("making data");
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        //takes picture puts it into outputstream
                        faceBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        String encoded = Base64.getEncoder().encodeToString(byteArray);

                        //make json object 'data'
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("image", encoded);
                        jsonObject.put("user", username.getText().toString());
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
                if (imgCapture.getDrawable() == null || binding.username.getText().toString().isEmpty()){
                    debug("No Image || no username");
                    return;
                }

//                    URL url = null;
                    debug("image exists");
                    String url = "http://10.0.2.2:5000/authenticate/type/picture";
//                    String url = "http://192.168.233.1:5000/authenticate/type/picture";
                    HttpURLConnection connection = null;
                    try {
                        currentModel = binding.autoCompleteTextView.getText().toString();
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
                        jsonObject.put("user", binding.username.getText().toString());
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
                    //executePytorch();
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
        this.faceBitmap=bitmap;

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
        //executePytorch(); //TODO:reactivate
    }


    public float[] executePytorch() {
        debug("running model");
        Bitmap bitmap = null;
        Module module = null;
        try {
            // creating bitmap from packaged into app android asset 'image.jpg',
            // app/src/main/assets/image.jpg
            bitmap = faceBitmap;
            // loading serialized torchscript module from packaged into app android asset model.pt,
            // app/src/model/assets/model.pt
            String moduleFilePath = assetFilePath(getContext(),"model.pt");
            debug(moduleFilePath);
            module = LiteModuleLoader.load( moduleFilePath);
        } catch (Exception e) {
            Log.e("PytorchHelloWorld", "Error reading assets", e);
          return null;
        }

        // showing image on UI
//        ImageView imageView = findViewById(R.id.image);
//        imageView.setImageBitmap(bitmap);

        // preparing input tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB, MemoryFormat.CHANNELS_LAST);

        // running the model
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

        // getting tensor content as java array of floats
        final float[] scores = outputTensor.getDataAsFloatArray();
        debug(scores.toString());
        return scores;

        // searching for the index with maximum score
//        float maxScore = -Float.MAX_VALUE;
//        int maxScoreIdx = -1;
//        for (int i = 0; i < scores.length; i++) {
//            if (scores[i] > maxScore) {
//                maxScore = scores[i];
//                maxScoreIdx = i;
//            }
//        }


    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
    @Override
    public void onResume() {
        super.onResume();

        //Visibilies are the different polling types
        String[] visibilities = getResources().getStringArray(R.array.models);

        //Creates an Array Adapter with the visibilities
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter(getContext(), R.layout.dropdown_item, visibilities);

        //Gets the view of the drop down menu with the different visibilities
        AutoCompleteTextView dropDownMenu = getView().findViewById(R.id.autoCompleteTextView);

        //Sets the created Array Adapter to the Dropdown Menu
        dropDownMenu.setAdapter(arrayAdapter);
    }

}


