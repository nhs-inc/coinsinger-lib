package com.nhs.coinsinger_lib_example;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nhs.youtubedl.YoutubeDL;
import com.nhs.youtubedl.YoutubeDLException;
import com.nhs.youtubedl.dto.VideoInfo;

/**
 * Created by Henry on 2020. 11. 10..
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText mUrlEdit;
    private Button mExcuteBtn;
    private TextView mResultText;
    private Button mUpdateBtn;
    private ProgressBar mProgressBar;
    private boolean mIsUpdating = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initListeners();
    }

    private void initViews() {
        mUrlEdit = findViewById(R.id.edit_url);
        mExcuteBtn = findViewById(R.id.btn_execute);
        mResultText = findViewById(R.id.text_result);
        mUpdateBtn = findViewById(R.id.btn_update);
        mProgressBar = findViewById(R.id.progress_bar);
    }

    private void initListeners() {
        mExcuteBtn.setOnClickListener(this);
        mUpdateBtn.setOnClickListener(this);
    }

    private void updateYoutubeDL() {
        if (mIsUpdating) {
            Toast.makeText(MainActivity.this, "update is already in progress", Toast.LENGTH_LONG).show();
            return;
        }

        mIsUpdating = true;
        mProgressBar.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(getApplication());
                } catch (YoutubeDLException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                mIsUpdating = false;
                mProgressBar.setVisibility(View.GONE);
            }
        }.execute();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_execute: {
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... voids) {
                        String result = "";
                        try {
                            VideoInfo videoInfo = YoutubeDL.getInstance().getInfo(mUrlEdit.getText().toString());
                            result = videoInfo.toString();
                        } catch (YoutubeDLException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        return result;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                        mResultText.setText(result);
                    }
                }.execute();
                break;
            }
            case R.id.btn_update: {
                updateYoutubeDL();
                break;
            }
        }
    }
}
