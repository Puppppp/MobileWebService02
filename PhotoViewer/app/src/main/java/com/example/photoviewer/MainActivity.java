package com.example.photoviewer;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String API_URL =
            "https://neji.pythonanywhere.com/api_root/Post/";
    private static final String SERVER_URL =
            "https://neji.pythonanywhere.com";
    private static final String AUTHOR_ID = "1";
    private static final int PICK_IMAGE = 1001;

    private LinearLayout postsContainer;
    private TextView statusText;
    private EditText titleInput;
    private EditText textInput;
    private EditText searchInput;
    private ImageView previewImage;
    private Button chooseButton;
    private Button uploadButton;
    private Button syncButton;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private JSONArray allPosts = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(35, 75, 125));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(8));
        root.setBackgroundColor(Color.WHITE);

        TextView heading = new TextView(this);
        heading.setText("PhotoViewer");
        heading.setTextSize(28);
        heading.setTextColor(Color.rgb(25, 55, 95));
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(heading);

        statusText = new TextView(this);
        statusText.setText("Ready to sync");
        statusText.setTextSize(14);
        statusText.setTextColor(Color.DKGRAY);
        statusText.setPadding(0, dp(8), 0, dp(8));
        root.addView(statusText);

        titleInput = new EditText(this);
        titleInput.setSingleLine(true);
        titleInput.setHint("Post title");
        root.addView(titleInput);

        textInput = new EditText(this);
        textInput.setHint("Post description");
        textInput.setMinLines(2);
        textInput.setGravity(Gravity.TOP);
        root.addView(textInput);

        chooseButton = new Button(this);
        chooseButton.setText("Choose Image");
        root.addView(chooseButton);

        // Feature 2: Image preview before uploading
        previewImage = new ImageView(this);
        previewImage.setAdjustViewBounds(true);
        previewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        previewImage.setMaxHeight(dp(180));
        previewImage.setVisibility(View.GONE);
        root.addView(previewImage);

        uploadButton = new Button(this);
        uploadButton.setText("Upload Post");
        root.addView(uploadButton);

        syncButton = new Button(this);
        syncButton.setText("Sync Photos");
        root.addView(syncButton);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);

        TextView postsHeading = new TextView(this);
        postsHeading.setText("Posts");
        postsHeading.setTextSize(21);
        postsHeading.setTextColor(Color.BLACK);
        postsHeading.setTypeface(null, android.graphics.Typeface.BOLD);
        postsHeading.setPadding(0, dp(12), 0, dp(6));
        root.addView(postsHeading);

        // feature 1 Search detection history
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint(
                "Search by object name (person, dog, car...)");
        root.addView(searchInput);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);

        postsContainer = new LinearLayout(this);
        postsContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(postsContainer);

        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);

        chooseButton.setOnClickListener(v -> chooseImage());
        uploadButton.setOnClickListener(v -> uploadPost());
        syncButton.setOnClickListener(v -> loadPosts());

        // filter posts whenever the search text changes.
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                displayFilteredPosts(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        loadPosts();
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void setBusy(boolean busy) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        chooseButton.setEnabled(!busy);
        uploadButton.setEnabled(!busy);
        syncButton.setEnabled(!busy);
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(intent, PICK_IMAGE);
        } catch (Exception e) {
            statusText.setText(
                    "Cannot open image picker: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {

            selectedImageUri = data.getData();

            // feature 2 display the selected image before uploading.
            try {
                getContentResolver().takePersistableUriPermission(
                        selectedImageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }

            previewImage.setImageURI(selectedImageUri);
            previewImage.setVisibility(View.VISIBLE);

            statusText.setText(
                    "Selected: " + getFileName(selectedImageUri));
        }
    }

    private String getFileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME);

                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return "upload.jpg";
    }

    // Upload a post to the Django server.
    private void uploadPost() {
        String title = titleInput.getText().toString().trim();
        String description = textInput.getText().toString().trim();

        if (title.isEmpty()) {
            titleInput.setError("Enter a title");
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Choose an image first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final Uri imageToUpload = selectedImageUri;

        setBusy(true);
        statusText.setText("Uploading post...");

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                String boundary =
                        "----PhotoViewer" + System.currentTimeMillis();

                connection = (HttpURLConnection)
                        new URL(API_URL).openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setDoOutput(true);

                connection.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);
                connection.setRequestProperty(
                        "Accept", "application/json");

                try (OutputStream out = connection.getOutputStream()) {

                    writeField(out, boundary, "author", AUTHOR_ID);
                    writeField(out, boundary, "title", title);
                    writeField(out, boundary, "text", description);

                    String fileName = getFileName(imageToUpload)
                            .replace("\"", "");

                    String mime = getContentResolver()
                            .getType(imageToUpload);

                    if (mime == null) {
                        mime = "image/jpeg";
                    }

                    writeText(out, "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"image\"; "
                            + "filename=\"" + fileName + "\"\r\n"
                            + "Content-Type: " + mime + "\r\n\r\n");

                    try (InputStream in = getContentResolver()
                            .openInputStream(imageToUpload)) {

                        if (in == null) {
                            throw new Exception(
                                    "Cannot read selected image");
                        }

                        byte[] buffer = new byte[8192];
                        int count;

                        while ((count = in.read(buffer)) != -1) {
                            out.write(buffer, 0, count);
                        }
                    }

                    writeText(out, "\r\n--" + boundary + "--\r\n");
                }

                int code = connection.getResponseCode();

                InputStream response = code >= 200 && code < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String body = readStream(response);

                if (code == 201 || code == 200) {
                    runOnUiThread(() -> {
                        titleInput.setText("");
                        textInput.setText("");
                        selectedImageUri = null;

                        // Clear the image preview after successful upload.
                        previewImage.setImageDrawable(null);
                        previewImage.setVisibility(View.GONE);

                        statusText.setText("Upload successful!");

                        Toast.makeText(this, "Post uploaded",
                                Toast.LENGTH_SHORT).show();

                        loadPosts();
                    });
                } else {
                    showError(
                            "Upload failed (HTTP " + code + "): " + body);
                }

            } catch (Exception e) {
                showError("Upload failed: " + e.getMessage());

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void writeField(OutputStream out, String boundary,
                            String name, String value) throws Exception {

        writeText(out, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n");
    }

    private void writeText(OutputStream out, String value)
            throws Exception {
        out.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private String readStream(InputStream input) throws Exception {
        if (input == null) {
            return "";
        }

        try (InputStream in = input;
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int count;

            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }

            return bytes.toString("UTF-8");
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            statusText.setText(message);
            setBusy(false);
        });
    }

    // Retrieve posts from PythonAnywhere.
    private void loadPosts() {
        setBusy(true);
        statusText.setText("Loading posts...");
        postsContainer.removeAllViews();

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                android.util.Log.d(
                        "PhotoViewer", "Trying to connect to: " + API_URL);

                connection = (HttpURLConnection)
                        new URL(API_URL).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int code = connection.getResponseCode();

                if (code != HttpURLConnection.HTTP_OK) {
                    throw new Exception(
                            "Server returned HTTP " + code);
                }

                JSONArray posts;

                try (InputStream input = connection.getInputStream()) {
                    posts = new JSONArray(readStream(input));
                }

                android.util.Log.d(
                        "PhotoViewer", "Posts received: " + posts.length());

                runOnUiThread(() -> {
                    allPosts = posts;

                    // Display all posts or apply the current search.
                    displayFilteredPosts(
                            searchInput.getText().toString());

                    statusText.setText(
                            "Sync complete: " + posts.length() + " posts");

                    setBusy(false);
                });

            } catch (Exception e) {
                android.util.Log.e(
                        "PhotoViewer", "Failed to load posts", e);

                runOnUiThread(() -> {
                    statusText.setText(
                            "Connection failed: " + e.getMessage());
                    setBusy(false);
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // Feature 1: Filter posts by title or description.
    private void displayFilteredPosts(String query) {
        postsContainer.removeAllViews();

        String search = query.trim().toLowerCase();
        int displayedCount = 0;

        for (int i = 0; i < allPosts.length(); i++) {
            try {
                JSONObject post = allPosts.getJSONObject(i);

                String title = post.optString("title", "");
                String description = post.optString("text", "");

                String searchableText =
                        (title + " " + description).toLowerCase();

                if (search.isEmpty()
                        || searchableText.contains(search)) {

                    addPost(post);
                    displayedCount++;
                }

            } catch (Exception e) {
                addMessage("Could not display a post.");
            }
        }

        if (displayedCount == 0) {
            addMessage("No matching posts found.");
        }
    }

    // Display one post and its image.
    private void addPost(JSONObject post) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, dp(8), 0, dp(20));

        TextView title = new TextView(this);
        title.setText(post.optString("title", "Untitled"));
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView body = new TextView(this);
        body.setText(post.optString("text", ""));
        body.setTextSize(16);
        body.setTextColor(Color.DKGRAY);
        card.addView(body);

        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(image);

        postsContainer.addView(card);

        String imageUrl = post.optString("image", "");

        if (!imageUrl.isEmpty() && !imageUrl.equals("null")) {

            if (imageUrl.startsWith("/")) {
                imageUrl = SERVER_URL + imageUrl;
            }

            android.util.Log.d(
                    "PhotoViewer", "Loading image: " + imageUrl);

            loadImage(imageUrl, image);
        }
    }

    private void loadImage(String imageUrl, ImageView imageView) {
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection)
                        new URL(imageUrl).openConnection();

                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                try (InputStream input = connection.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);

                    if (bitmap != null) {
                        runOnUiThread(
                                () -> imageView.setImageBitmap(bitmap));
                    }
                }

            } catch (Exception e) {
                android.util.Log.e(
                        "PhotoViewer",
                        "Image load failed: " + imageUrl, e);

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void addMessage(String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(Color.DKGRAY);
        text.setPadding(0, dp(8), 0, dp(8));
        postsContainer.addView(text);
    }
}
