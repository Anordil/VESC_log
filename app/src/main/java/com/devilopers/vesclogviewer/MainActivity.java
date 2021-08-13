package com.devilopers.vesclogviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    Button btnSelectFile;
    EditText etSampling;

    private static final int PICK_FILE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSelectFile.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            startActivityForResult(intent, PICK_FILE);
        });

        etSampling = findViewById(R.id.editTextSampling);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != RESULT_OK) {
            return;
        }

        // Decode the file data
        Uri fileUri = data.getData();

        // Make sure it's a VESC Tools log file -> check that the first line's first token is ms_today
        boolean validFile = false;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(fileUri)));
            String firstLine = br.readLine();
            String[] tokens = firstLine.split(";");
            if (tokens[0].equals("ms_today")) {
                validFile = true;
            }
            br.close();
        } catch (IOException e) {
            Toast.makeText(this, "Whoops! Looks like this isn't a VEST Tools log file.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (validFile) {
            int sampling = 500;
            try {
                sampling = Integer.parseInt(String.valueOf(etSampling.getText()));
            } catch (Exception e) {}

            Bundle bundle = new Bundle();
            bundle.putSerializable("uri", fileUri.toString());
            bundle.putSerializable("sampling", sampling);
            Intent newIntent = new Intent(getApplicationContext(), ViewDataActivity.class);
            newIntent.putExtras(bundle);
            startActivity(newIntent);
        }
    }



}