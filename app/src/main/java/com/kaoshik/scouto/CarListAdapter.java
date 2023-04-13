package com.kaoshik.scouto;
import static android.app.Activity.RESULT_OK;
import static com.kaoshik.scouto.MainActivity.REQUEST_CODE_PICK_IMAGE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
public class CarListAdapter extends ArrayAdapter<Car> {
    static final int PICK_IMAGE_REQUEST = 1;
    private int DeleteCarPosition = -1;
    private int selectedCarPosition = -1;
    private OnUploadImageClickListener listener;
    private List<Car> carList;
    private MainActivity mainActivity;




    public CarListAdapter(@NonNull Context context, int car_list_item, @NonNull List<Car> objects, MainActivity mainActivity) {
        super(context, 0, objects);
        this.carList = objects;
        this.mainActivity = mainActivity;

    }



    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.car_list_item, parent, false);
        }
        // get the current car object
        Car car = getItem(position);





        ImageView carImageView = convertView.findViewById(R.id.car_image_view);

        TextView carMakeTextView = convertView.findViewById(R.id.car_make_text_view);
        TextView carModelTextView = convertView.findViewById(R.id.car_model_text_view);
        Button uploadImageButton = convertView.findViewById(R.id.upload_image_button);
        Button deleteButton = convertView.findViewById(R.id.delete_car_button);

        new CountDownTimer(2000, 1000) { // 30 seconds, tick every 1 second
            public void onTick(long millisUntilFinished) {


            }

            public void onFinish() {

                String thumbnailPath = car.getThumbnailUrl();
                Bitmap bitmap = ((MainActivity) getContext()).loadImageFromStorage(thumbnailPath);
                carImageView.setImageBitmap(bitmap);
            }
        }.start();



        if (car.getImageUrl() != null && !car.getImageUrl().isEmpty()) {
            Picasso.get().load(car.getImageUrl()).into(carImageView);
        } else {

            carImageView.setImageResource(R.drawable.ic_action_name);
        }
        carMakeTextView.setText(car.getMake());
        carModelTextView.setText(car.getModel());

            uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                ((MainActivity) getContext()).startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);

                selectedCarPosition = position;


            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteCarPosition = position;
                carList.remove(position);
                notifyDataSetChanged();
                mainActivity.saveCarList((ArrayList<Car>) carList);

            }
        });



        return convertView;
    }
    public void setOnUploadImageClickListener(OnUploadImageClickListener listener) {
        this.listener = listener;
    }
    public interface OnUploadImageClickListener {
        void onUploadImageClick(int position);
    }
    public int getSelectedCarPosition() {
        return selectedCarPosition;
    }
    public void deleteCar(int position) {
        carList.remove(position);
        notifyDataSetChanged();
    }


}


