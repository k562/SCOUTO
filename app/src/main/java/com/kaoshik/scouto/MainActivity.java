package com.kaoshik.scouto;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final int THUMBNAIL_SIZE = 90*112;

    private Spinner makeSpinner, modelSpinner;
    private TextView carImageEditText;
    private Button addCarButton,logout;
    private ListView carListView;

    private ArrayList<Car> carList;
    private CarListAdapter carListAdapter;

    private SharedPreferences sharedPreferences;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_UPLOAD = 2;
    private static final int PERMISSION_REQUEST_CODE = 3;
    private static final String SHARED_PREF_NAME = "carListSharedPref";
    private static final int REQUEST_IMAGE_PICK = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getApplicationContext();
        mAuth = FirebaseAuth.getInstance();


        makeSpinner = findViewById(R.id.car_make_spinner);
        modelSpinner = findViewById(R.id.car_model_spinner);
        carImageEditText = findViewById(R.id.car_image_edittext);
        addCarButton = findViewById(R.id.add_car_button);
        carListView = findViewById(R.id.cars_list_view);





        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);

        // Initialize the car list from local storage
        carList = loadCarList();


        carListAdapter =  new CarListAdapter(this, R.layout.car_list_item, carList, this);
        carListView.setAdapter(carListAdapter);


        loadCarMakes();


        carListAdapter.setOnUploadImageClickListener(new CarListAdapter.OnUploadImageClickListener() {
            @Override
            public void onUploadImageClick(int position) {
                // Open the image gallery to choose an image
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, CarListAdapter.PICK_IMAGE_REQUEST);
            }
        });

//company name selecter
        makeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Load the car models for the selected make from the CARQUERY API
                String make = (String) adapterView.getItemAtPosition(i);
                loadCarModels(make);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });


        addCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    FileOutputStream fos = context.openFileOutput("carList.dat", Context.MODE_PRIVATE);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(carList);
                    oos.close();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String make
                        = (String) makeSpinner.getSelectedItem();
                String model = (String) modelSpinner.getSelectedItem();


                String imageUrl = carImageEditText.getText().toString();


                if (make.isEmpty() || model.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please select a car make and model, and enter a car image URL.", Toast.LENGTH_SHORT).show();
                } else {

                    Car car = new Car(make, model, imageUrl);


                    carList.add(car);


                    carListAdapter.notifyDataSetChanged();


                    saveCarList(carList);


                    carImageEditText.setText("");
                    makeSpinner.setSelection(0);
                    modelSpinner.setSelection(0);
                }
            }
        });

        carListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Car car = (Car) adapterView.getItemAtPosition(i);


                showCarDialog(car);
            }
        });
        logout = findViewById(R.id.LogOutBTN);
        logout.setOnClickListener(v -> signOut());


    }

    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
        finish();
    }

    /**
     * Loading the car makes from the CARQUERY API and populate the make spinner.
     */
    private void loadCarMakes() {
        String url = "https://www.carqueryapi.com/api/0.3/?cmd=getMakes&format=json";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray makesArray = jsonObject.getJSONArray("Makes");

                    ArrayList<String> makeList = new ArrayList<>();
                    makeList.add("Select Car Make");

                    for (int i = 0; i < makesArray.length(); i++) {
                        JSONObject makeObject = makesArray.getJSONObject(i);
                        String make = makeObject.getString("make_display");
                        makeList.add(make);
                    }

                    ArrayAdapter<String> makeAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, makeList);
                    makeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    makeSpinner.setAdapter(makeAdapter);
                } catch (JSONException e) {
                    Log.e("MainActivity", "Error parsing car makes JSON", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("MainActivity", "Error loading car makes", error);
            }
        });

        queue.add(request);
    }

    /**
     * Loading the car models for the specified make from the CARQUERY API and populate the model spinner.
     *
     * @param make The car make to load the models for.
     */
    private void loadCarModels(String make) {
        String url = "https://www.carqueryapi.com/api/0.3/?cmd=getModels&make=" + make.replaceAll(" ", "%20") + "&format=json";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray modelsArray = jsonObject.getJSONArray("Models");

                    ArrayList<String> modelList = new ArrayList<>();
                    modelList.add("Select Car Model");

                    for (int i = 0; i < modelsArray.length(); i++) {
                        JSONObject modelObject = modelsArray.getJSONObject(i);
                        String model = modelObject.getString("model_name");
                        modelList.add(model);
                    }

                    ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, modelList);
                    modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    modelSpinner.setAdapter(modelAdapter);
                } catch (JSONException e) {
                    Log.e("MainActivity", "Error parsing car models JSON", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("MainActivity", "Error loading car models", error);
            }
        });

        queue.add(request);
    }

    /**
     * Showing a dialog with options to view, upload, or delete the specified car.
     *
     * @param car The car to show the dialog for.
     */
    private void showCarDialog(Car car) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(car.getMake() + " " + car.getModel());

        // Set the car image
        ImageView carImageView = new ImageView(MainActivity.this);
        Picasso.get().load(car.getImageUrl()).into(carImageView);
        builder.setView(carImageView);

        // Add buttons to view, upload, or delete the car
        builder.setPositiveButton("View", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Show the car information in a new activity
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.putExtra("car", (CharSequence) car);
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                carList.remove(car);
                carListAdapter.notifyDataSetChanged();


                saveCarList(carList);
            }
        });

        builder.setNeutralButton("Upload", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Upload Car Image");

                final EditText imageUrlEditText = new EditText(MainActivity.this);
                imageUrlEditText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                imageUrlEditText.setText(car.getImageUrl());
                builder.setView(imageUrlEditText);

                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        String imageUrl = imageUrlEditText.getText().toString();
                        car.setImageUrl(imageUrl);
                        carListAdapter.notifyDataSetChanged();


                        saveCarList(carList);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                builder.show();
            }
        });

        builder.show();
    }

    /**
     * Load the car list from local storage.
     *
     * @return The car list.
     */
    private ArrayList<Car> loadCarList() {
        ArrayList<Car> carList = new ArrayList<>();

        try {
            FileInputStream fis = openFileInput("carList.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            carList = (ArrayList<Car>) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            Log.e("MainActivity", "Error loading car list from local storage", e);
        }

        return carList;
    }

    /**
     * Save the car list to local storage.
     *
     * @param carList The car list to save.
     */
    public void saveCarList(ArrayList<Car> carList) {
        try {
            FileOutputStream fos = openFileOutput("carList.dat", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(carList);
            oos.close();
            fos.close();
        } catch (IOException e) {
            Log.e("MainActivity", "Error saving car list to local storage", e);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == CarListAdapter.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri imageUri = data.getData();
            int position = carListAdapter.getSelectedCarPosition();
            Car car = carListAdapter.getItem(position);
            car.setImageUrl(imageUri.toString());


            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
                String thumbnailPath = saveImageToStorage(bitmap);
                car.setThumbnailUrl(thumbnailPath);
            } catch (IOException e) {
                e.printStackTrace();
            }



            carListAdapter.notifyDataSetChanged();
            carListView.smoothScrollToPosition(position);

        }
    }

    private String saveImageToStorage(Bitmap bitmap) {
        String fileName = "thumbnail_" + System.currentTimeMillis() + ".jpg";
        File thumbnailFile = new File(getFilesDir(), fileName);


        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(thumbnailFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return thumbnailFile.getAbsolutePath();
    }
    Bitmap loadImageFromStorage(String filePath) {
        try {
            File file = new File(filePath);
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



}




