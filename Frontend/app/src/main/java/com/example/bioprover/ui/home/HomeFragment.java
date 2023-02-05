package com.example.bioprover.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.bioprover.R;
import com.example.bioprover.databinding.FragmentHomeBinding;
import com.google.android.material.textfield.TextInputEditText;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    private FragmentHomeBinding binding;
    private ImageView imgCapture;
    private static final int Image_Capture_Code = 121;
    private final static int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private Uri mImageUri;
    private Bitmap faceBitmap = null;
    private String currentModel;
    private Activity currentActivity = null;


    /**
     * created the view
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        currentActivity = this.getActivity();

        //setting the button that opens the camera
        imgCapture= binding.capturedImage;
        final Button photoButton = binding.Photobutton;
        setCameraButton(photoButton);

        //setting the button that implements the functionality which uploads a file to the server and therefore enrolls a new user
        final Button enrollBtn = binding.Enroll;
        final TextInputEditText username = binding.username;
        setEnrollButton(enrollBtn,username,root);

        //setting the button that implements the functionality which uploads a file to the server, and checks for matches
        final Button UploadButton = binding.UpLoad;
        setUploadButton(UploadButton);

        return root;
    }

    /**
     * Setting the button which opens the camera
     * this method sets the required permissions
     * @param photoButton
     */
    public void setCameraButton(Button photoButton){
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
    }


    /**
     * OnClick functionality for the enroll button.
     * Requires photo to be taken first.
     * Takes the image, transforms it into byte array, puts it into an json object, which is sent via http.
     * The server tries to add the user, and sends a response if the enrollment was sucessfull
     * @param enrollBtn fragmenthome -> id/Enroll
     * @param username fragmenthome -> id/username
     * @param root fragmenthome
     */
    public void setEnrollButton(Button enrollBtn, TextInputEditText username,View root){

        enrollBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check if username is typed in
                if (username.getText().toString().isEmpty()) {
                    Toast.makeText(root.getContext(), "Please type in a username", Toast.LENGTH_SHORT).show();

                // check if picture was taken
                } else if (faceBitmap == null ){
                    Toast.makeText(root.getContext(), "Please take a picture", Toast.LENGTH_SHORT).show();

                // check if picture was taken and username is typed in
                } else if (!username.getText().toString().isEmpty() && faceBitmap != null) {
                    String url = "http://192.168.50.67:5000/register";
                    debug("enrolling user");

                    // send Data to Backend and get Response
                    sendingDataToBackEnd(url);

                }
            }
        });

    }

    /**
     * sends the Data to backend given the corresponding url for either enrolling the user or uploading the picture
     * @param url
     */
    private void sendingDataToBackEnd(String url) {
        try {
            HttpURLConnection connection = null;
            currentModel = binding.autoCompleteTextView.getText().toString() == "PocketNet"? "pocket": "elastic";

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //takes the pictures bytearray
            faceBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream); //enroll

            //compress the bytearray
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            //encode to string so that it can be sent via json
            String encoded = Base64.getEncoder().encodeToString(byteArray);

            //make json object 'data'
            JSONObject jsonObject = new JSONObject();
            //set the image attribute, and the username attribute.
            jsonObject.put("image", encoded);
            //username is retrieved from the @+id/username editText from fragment_home
            jsonObject.put("user", binding.username.getText().toString());
            //model to be executed ist retrieved from dropdownbox
            jsonObject.put("net", currentModel);

            byte[] data = jsonObject.toString().getBytes(StandardCharsets.UTF_8);

            //make http connection
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", Integer.toString(data.length));
            connection.setDoOutput(true);

            // get Output stream
            debug("writing to output stream");
            connection.getOutputStream().write(data);

            InputStream inputStream = null;
            if (url.contains("register")) {
                debug("Trying to get input stream");
                inputStream = connection.getInputStream();
            } else if (url.contains("picture")) {
                debug("getting upload return stream");
                inputStream = connection.getInputStream();
                debug(inputStream.toString());
                debug("read upload input stream");
            }

            // output server response
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String response = reader.readLine();
            System.out.println("Response from server: " + response);
            displayServerFeedback(response);


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * OnClick functionality for the Upload button.
     * Requires picture to be taken first.
     * Takes the image, transforms it into byte array, puts it into an json object, which is sent via http.
     * The server tries to authenticate the user, and sends a response if the authentication was sucessfull
     * @param UploadButton
     */
    public void setUploadButton(Button  UploadButton){
        UploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debug("button pressed");

                if (imgCapture.getDrawable() == null) {
                    Toast.makeText(binding.getRoot().getContext(), "Please take a picture of yourself", Toast.LENGTH_SHORT).show();
                    debug("No Image");
                    return;

                } else if (binding.username.getText().toString().isEmpty()) {
                    Toast.makeText(binding.getRoot().getContext(), "Please type in a username", Toast.LENGTH_SHORT).show();
                    debug("no username");
                    return;
                }
                debug("image exists");
                String url = "http://192.168.50.67:5000/authenticate/type/picture";
//              String url = "http://192.168.233.1:5000/authenticate/type/picture";
                //executePytorch();

                // send Data to Backend and get Response
                sendingDataToBackEnd(url);
            }
        });
    }


    /**
     * Creates a toast which tells the user what the response of the server was. changes color appropriatly, if it was a success or a failure
     * @param msg the response of the sever
     */
    public void displayServerFeedback(String msg){
        //creates toast with long display time
        Toast toast = Toast.makeText(this.getContext(), "Denied", Toast.LENGTH_LONG);
        LayoutInflater inflater = getLayoutInflater();

        //toast layout is given in layout/toast_layout
        View layout = inflater.inflate(R.layout.toast_layout,
                (ViewGroup) getView().findViewById(R.id.toast_root_view));
        TextView text = layout.findViewById(R.id.toast_text_view);

        text.setTextColor(Color.WHITE);

        toast.setView(layout);
        toast.setGravity(Gravity.TOP, 0, (int) (-2 * getResources().getDisplayMetrics().density));

        if(msg.contains("isSimilar")&&msg.contains("True")){
            layout.setBackgroundResource(R.drawable.toast_background_green);
            text.setText("Access Granted");
        }else if(msg.contains("success")){
            layout.setBackgroundResource(R.drawable.toast_background_green);
            text.setText("Success");}else if(msg.contains("notExisting")){
            layout.setBackgroundResource(R.drawable.toast_background_red);
            text.setText("No User");
        }else{
            layout.setBackgroundResource(R.drawable.toast_background_red);
            text.setText("Denied");
        }
        debug("message is"+ msg);
        //sets color and shape depending on the server message. Shapes are in drawable/toast_background*
        toast.show();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     *
     * @param loginStatus the Textview in fragment_home that displays the "accepted" or "denied" text. Is hidden by default.
     * @param accepted Boolean which determines the contents of the text view
     */
    public void displayBanner(TextView loginStatus,Boolean accepted){
        loginStatus.setVisibility(View.VISIBLE);

        if(accepted){
            loginStatus.setText("ACCEPTED");
            loginStatus.setTextColor(Color.GREEN);
        }else{
            loginStatus.setText("DENIED");
            loginStatus.setTextColor(Color.RED);
        }

    }


    /**
     * create and preload a temp file location, to accept the picture taken from the camera
     * @param part the temp file name. 
     * @param ext what kind of data format the temp file should be
     */
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
    
    /**
     * Ask the user for the premissions, if not all of the premission are premitted from the user side, it is recusive untill the user accept all of the permissions
     * After the premission all accepted, call the take picute action to call up the camera.
     * @param c the context of the permission, it is not necessary here, but you want to rewrite it in a helper function, it is necessary. 
     * @param permissions which are needed, it is a list of permissions listed in https://developer.android.com/reference/android/Manifest.permission
     */
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
            cInt.putExtra("android.intent.extras.CAMERA_FACING", CameraCharacteristics.LENS_FACING_FRONT);
            cInt.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            cInt.putExtra("android.intent.extras.LENS_FACING_FRONT", CameraCharacteristics.LENS_FACING_FRONT);

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

    /**
     * Get the picture from the temp file and show the preview on the screen. 
     * @param imageView where to show the picture
     */
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

    /**
     * Output log
     * @param msg
     */
    public void debug(String msg){
        Log.e("picture",msg);
        //Toast.makeText(this.getContext(),msg,Toast.LENGTH_SHORT).show();
    }

    /**
     * requestCode
     * Image_Capture_Code
     * Successful:Call the grabImage to take the picture from the storage
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Image_Capture_Code) {
            if (resultCode == RESULT_OK) {
                //... some code to inflate/create/find appropriate ImageView to place grabbed image
                this.faceBitmap = this.grabImage(imgCapture);
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

    /**
     * process picture on frontend side
     * @return
     */
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

    /**
     * helping method for executing Pytorch
     * @param context
     * @param assetName
     * @return
     * @throws IOException
     */
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

    /**
     * takes care of displaying models on dropdownmenu
     */
    @Override
    public void onResume() {
        super.onResume();

        //different available models
        String[] models = getResources().getStringArray(R.array.models);

        //Creates an Array Adapter with the models
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter(getContext(), R.layout.dropdown_item, models);

        //Gets the view of the drop down menu with the different models
        AutoCompleteTextView dropDownMenu = getView().findViewById(R.id.autoCompleteTextView);

        //Sets the created Array Adapter to the Dropdown Menu
        dropDownMenu.setAdapter(arrayAdapter);
    }

}


