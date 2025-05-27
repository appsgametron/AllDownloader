package com.vidoe.alldownloader;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


import android.annotation.SuppressLint;
import android.view.View;
import android.webkit.*;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FloatButtonLayout extends AppCompatActivity {

    private WebView webView;
    private FloatingActionButton fabDownload;
    private int videoCount = 0, imageCount = 0, audioCount = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_float_button);

        webView = findViewById(R.id.webView);
        fabDownload = findViewById(R.id.fabDownload);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl("https://m.facebook.com");

        fabDownload.setOnClickListener(v -> {
            // Evaluate JS to count elements
            evaluateMediaCounts();
        });
    }

    private void evaluateMediaCounts() {
        webView.evaluateJavascript(
                "(function() { return document.getElementsByTagName('img').length; })();",
                value -> {
                    imageCount = Integer.parseInt(value.replace("\"", ""));
                    updateBottomSheet();
                });

        webView.evaluateJavascript(
                "(function() { return document.getElementsByTagName('audio').length; })();",
                value -> {
                    audioCount = Integer.parseInt(value.replace("\"", ""));
                    updateBottomSheet();
                });

        webView.evaluateJavascript(
                "(function() { return document.getElementsByTagName('video').length; })();",
                value -> {
                    videoCount = Integer.parseInt(value.replace("\"", ""));
                    updateBottomSheet();
                });
    }

    private void updateBottomSheet() {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_download_item, null);
        sheetDialog.setContentView(sheetView);

        TextView tvImage = sheetView.findViewById(R.id.tvImageCount);
        TextView tvAudio = sheetView.findViewById(R.id.tvAudioCount);
        TextView tvVideo = sheetView.findViewById(R.id.tvVideoCount);

        tvImage.setText(String.valueOf(imageCount));
        tvAudio.setText(String.valueOf(audioCount));
        tvVideo.setText(String.valueOf(videoCount));

        sheetDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}

