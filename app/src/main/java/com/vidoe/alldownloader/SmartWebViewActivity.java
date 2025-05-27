package com.vidoe.alldownloader;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class SmartWebViewActivity extends AppCompatActivity {
    private List<String> imageUrls = new ArrayList<>();
    private List<String> videoUrls = new ArrayList<>();
    private List<String> audioUrls = new ArrayList<>();
    private boolean imageLoaded = false;
    private boolean videoLoaded = false;
    private boolean audioLoaded = false;
    private LinearLayout mediaSelectionPanel;
    private boolean isMediaPanelVisible = false;
    private WebView webView;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private FloatingActionButton fabDownload;
    TextView tvImageCount,tvAudioCount,tvVideoCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_web_view);

        initializeWebView();
        setupDownloadButton();
        setupMediaSelectionButtons();

    }

    private void initializeWebView() {
        webView = findViewById(R.id.webView);
        fabDownload = findViewById(R.id.fabDownload);
        mediaSelectionPanel = findViewById(R.id.mediaSelectionPanel);
        tvImageCount = findViewById(R.id.tvImageCount);
        tvAudioCount = findViewById(R.id.tvAudioCount);
        tvVideoCount = findViewById(R.id.tvVideoCount);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress > 50 && !imageLoaded) {
                    evaluateMediaCounts();
                }
            }
        });
// Add this to your WebView setup
        webView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                evaluateMediaCounts();
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mediaSelectionPanel.setVisibility(View.GONE);
                isMediaPanelVisible = false;

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    evaluateMediaCounts();
                }, 8000); // 3 second delay
            }


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("intent://") || url.startsWith("fb://") || url.startsWith("whatsapp://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            PackageManager pm = getPackageManager();
                            if (intent.getPackage() != null) {
                                pm.getPackageInfo(intent.getPackage(), 0);
                                startActivity(intent);
                                return true;
                            } else {
                                Toast.makeText(SmartWebViewActivity.this, "App not installed", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                view.loadUrl(url);
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            startDownload(url, URLUtil.guessFileName(url, contentDisposition, mimeType));
        });

        // Load initial URL
        webView.loadUrl("https://m.facebook.com");
    }

    private void setupDownloadButton() {
        // Add a refresh button to your UI
        fabDownload.setOnLongClickListener(v -> {
            Toast.makeText(this, "Rescanning page for media...", Toast.LENGTH_SHORT).show();
            evaluateMediaCounts();
            return true;
        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                evaluateMediaCounts();
            } else {
                Toast.makeText(this, "Storage permission required for downloads", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void evaluateMediaCounts() {
        String detectMediaJS = "(function() {" +
                "function deepMediaScan() {" +
                "  let results = {images: [], videos: [], audios: []};" +
                "  try {" +
                "    let imgElements = document.querySelectorAll('img, [style*=\"background-image\"]');" +
                "    imgElements.forEach(el => {" +
                "      let src = el.src || el.getAttribute('data-src') || " +
                "               window.getComputedStyle(el).backgroundImage.replace(/^url\\([\"']?/, '').replace(/[\"']?\\)$/, '');" +
                "      if (src && !src.startsWith('data:') && !results.images.includes(src)) results.images.push(src);" +
                "    });" +

                "    let videoElements = document.querySelectorAll('video, [data-video-src], [data-video-url]');" +
                "    videoElements.forEach(video => {" +
                "      let src = video.src || video.getAttribute('data-src') || video.getAttribute('data-video-src') || video.getAttribute('data-video-url');" +
                "      if (src && !results.videos.includes(src)) results.videos.push(src);" +
                "      video.querySelectorAll('source').forEach(source => {" +
                "        let sourceSrc = source.src || source.getAttribute('data-src');" +
                "        if (sourceSrc && !results.videos.includes(sourceSrc)) results.videos.push(sourceSrc);" +
                "      });" +
                "    });" +

                "    let audioElements = document.querySelectorAll('audio, [data-audio-src]');" +
                "    audioElements.forEach(audio => {" +
                "      let src = audio.src || audio.getAttribute('data-src') || audio.getAttribute('data-audio-src');" +
                "      if (src && !results.audios.includes(src)) results.audios.push(src);" +
                "      audio.querySelectorAll('source').forEach(source => {" +
                "        let sourceSrc = source.src || source.getAttribute('data-src');" +
                "        if (sourceSrc && !results.audios.includes(sourceSrc)) results.audios.push(sourceSrc);" +
                "      });" +
                "    });" +
                "  } catch(e) { console.error('Scan error:', e); }" +
                "  return results;" +
                "}" +
                "return JSON.stringify(deepMediaScan());" +
                "})();";

        webView.evaluateJavascript(detectMediaJS, value -> {
            try {
                if (value != null && !value.equals("null")) {
                    JSONObject result = new JSONObject(value);
                    imageUrls.clear();
                    videoUrls.clear();
                    audioUrls.clear();

                    JSONArray images = result.optJSONArray("images");
                    JSONArray videos = result.optJSONArray("videos");
                    JSONArray audios = result.optJSONArray("audios");
                    if (images != null) {
                        for (int i = 0; i < images.length(); i++) {
                            imageUrls.add(images.getString(i));
                        }
                    }

                    if (videos != null) {
                        for (int i = 0; i < videos.length(); i++) {
                            videoUrls.add(videos.getString(i));
                        }
                    }

                    if (audios != null) {
                        for (int i = 0; i < audios.length(); i++) {
                            audioUrls.add(audios.getString(i));
                        }
                    }

                    imageLoaded = true;
                    videoLoaded = true;
                    audioLoaded = true;

                    updateMediaCountUI();      // Update counts
                    updateBottomSheetWithMedia(); // Show UI panel with results
                }





            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateMediaCountUI() {
        runOnUiThread(() -> {
            // Update counts (even if zero)
            int imageCount = imageUrls.size();
            int videoCount = videoUrls.size();
            int audioCount = audioUrls.size();

            Log.d("SmartWebView",
                    String.format("Media counts - Images: %d, Videos: %d, Audios: %d",
                            imageCount, videoCount, audioCount));

            // Show/hide media type buttons based on availability
            findViewById(R.id.tvImageCount).setVisibility(imageCount > 0 ? View.VISIBLE : View.GONE);
            findViewById(R.id.tvVideoCount).setVisibility(videoCount > 0 ? View.VISIBLE : View.GONE);
            findViewById(R.id.tvAudioCount).setVisibility(audioCount > 0 ? View.VISIBLE : View.GONE);

            // If no media found at all, show message
            if (imageCount == 0 && videoCount == 0 && audioCount == 0) {
                Toast.makeText(this, "No media found on this page", Toast.LENGTH_SHORT).show();
                mediaSelectionPanel.setVisibility(View.GONE);
                isMediaPanelVisible = false;
            }
        });
    }

    private void updateBottomSheetWithMedia() {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_download_item, null);
        sheetDialog.setContentView(sheetView);

        LinearLayout container = sheetView.findViewById(R.id.llMediaList);
        container.removeAllViews();

        // Add media sections
        addMediaSection(container, "Images", imageUrls);
        addMediaSection(container, "Videos", videoUrls);
        addMediaSection(container, "Audios", audioUrls);

        sheetDialog.show();
    }

    private void addMediaSection(LinearLayout parent, String title, List<String> urls) {
        if (urls.isEmpty()) return;

        // Add section header
        View header = getLayoutInflater().inflate(R.layout.section_header, parent, false);
        TextView tvTitle = header.findViewById(R.id.tvSectionTitle);
        tvTitle.setText(title + " (" + urls.size() + ")");
        parent.addView(header);

        // Add media items
        for (String url : urls) {
            addMediaItem(parent, title.substring(0, title.length()-1), url); // Remove 's' for type
        }
    }

    private void addMediaItem(LinearLayout container, String type, String url) {
        View item = getLayoutInflater().inflate(R.layout.item_media_preview, null);

        TextView txtType = item.findViewById(R.id.tvType);
        TextView txtSize = item.findViewById(R.id.tvSize);
        ImageView imgPreview = item.findViewById(R.id.ivPreview);
        Button btnDownload = item.findViewById(R.id.btnDownload);

        txtType.setText(type.toUpperCase());
        txtSize.setText("Tap to download");

        if (type.equalsIgnoreCase("Image")) {
            Glide.with(this).load(url).into(imgPreview);
        } else {
            imgPreview.setImageResource(type.equalsIgnoreCase("Video") ?
                    R.drawable.video : R.drawable.audio);
        }

        btnDownload.setOnClickListener(v -> startDownload(url, URLUtil.guessFileName(url, null, null)));

        container.addView(item);
    }

    private void startDownload(String url, String fileName) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setDescription("Downloading file...");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMediaSelectionButtons() {
        fabDownload.setOnClickListener(v -> {
            if (!isMediaPanelVisible) {
                evaluateMediaCounts(); // Refresh counts and show
            } else {
                mediaSelectionPanel.setVisibility(View.GONE);
                isMediaPanelVisible = false;
            }
        });
    }


    private void showMediaBottomSheet(String title, List<String> urls) {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_download_item, null);
        sheetDialog.setContentView(sheetView);

        TextView sheetTitle = sheetView.findViewById(R.id.sheetTitle);
        sheetTitle.setText(title);

        LinearLayout container = sheetView.findViewById(R.id.llMediaList);
        container.removeAllViews();

        for (String url : urls) {
            addMediaItem(container, title.substring(0, title.length()-1), url); // Remove 's' from title
        }

        sheetDialog.show();
        mediaSelectionPanel.setVisibility(View.GONE);
        isMediaPanelVisible = false;
    }



    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}