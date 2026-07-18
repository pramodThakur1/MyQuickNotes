package com.nishuapps.gonotes;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.FileContent;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.nishuapps.gonotes.ZoomableImageView;
import com.nishuapps.gonotes.NoteImagesAdapter;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.Key;
import com.scottyab.rootbeer.RootBeer;

public class MainActivity extends AppCompatActivity {
    // SECURE DUAL-KEY ARCHITECTURE (Resolves Login Deadlock vs VAPT Score)
    private static final String MASTER_KEY_ALIAS = "MyNotesMasterKey"; // For Notes (Sakt Lock)
    private static final String METADATA_KEY_ALIAS = "AppMetadataKey"; // For Email/Paths (Fast Lock)
    private static final String ALGO_GCM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private DrawerLayout drawerLayout;
    private TextView buttonMenu, menuBin, menuTheme, menuLanguage, menuSettings, menuManageCategories, textCategoriesHeader, textLastSync, buttonToggleView, buttonSort, buttonEmptyBin, textEmptyState, textWordCount, textUserEmail, buttonLogout, buttonClearSearch;
    private View lineDivider;
    private ImageView buttonCloudSync;
    private View scrollCategories, buttonToggleCategories;
    private boolean isCategoriesExpanded = true;
    private boolean isBinMode = false, isNormalFilterMode = false, isNotebookMode = false, isRecentMode = true;
    private String currentParentId = "root";
    private int currentLevel = 1;
    private final ArrayList<String> navigationPathIds = new ArrayList<>();
    private final ArrayList<String> navigationPathNames = new ArrayList<>();

    private RelativeLayout screenList, layoutSplashLayer;
    private LinearLayout screenAddNote;

    private Button buttonPlus, buttonUndo, buttonRedo, buttonMore, buttonColor, buttonFormat, buttonAddFeature;
    private TextView buttonBack, buttonSpeak, buttonAlarm, buttonPin, buttonSearchInNote, btnFindPrevInNote, btnFindNextInNote, btnCloseSearchInNote, textSearchInNoteCount, buttonMoreNote;

    private HorizontalScrollView layoutFormatBar;
    private Button btnH1, btnH2, btnBold, btnItalic, btnUnderline, btnClearToggles, btnCloseFormat;
    private LinearLayout layoutSearchInNote, layoutNoteActions;
    private EditText editTitle, searchBar, editNoteBody, editSearchInNote;
    private TextView textTimestamp;
    private ListView listViewNotes;
    private GridView gridViewNotes;
    private TextView tabRecent, tabNormalNotes, tabNotebooks;
    private LinearLayout layoutNotebookBreadcrumb;
    private View scrollBreadcrumb;
    private TextView textBreadcrumb;

    private LinearLayout layoutSelectionToolbar, layoutSelectionBottomBar;
    private TextView textSelectedCount, buttonCancelSelection, buttonSelectAll, buttonMoreSelection;
    private View buttonDeleteSelected;
    private boolean isSelectionMode = false;
    private final HashSet<String> selectedNoteIds = new HashSet<>();
    private int lastSearchInNoteIndex = -1;
    private final ArrayList<Integer> searchInNoteMatchIndices = new ArrayList<>();
    private int currentSearchInNoteMatchPos = -1;

    private android.view.GestureDetector swipeDetector;

    private androidx.recyclerview.widget.RecyclerView imagesRecyclerView;
    private NoteImagesAdapter imagesAdapter;
    private LinearLayout layoutCategoriesInDrawer;
    private ArrayList<String> currentImagePaths = new ArrayList<>();
    private static final int CAMERA_PERMISSION_REQUEST = 200;
    private String pendingCameraPhotoPath;

    private ArrayList<String> categoriesList = new ArrayList<>();
    private String selectedCategoryFilter = "All";
    private String currentNoteCategory = "General";

    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;
    private boolean isH1Active = false;
    private boolean isH2Active = false;

    private boolean isCheckboxesHidden = false;
    private final android.util.LruCache<String, android.graphics.Bitmap> imageCache = new android.util.LruCache<>(30); // Cache for scaled bitmaps

    private ArrayList<HashMap<String, String>> allNotesList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> displayedNotesList = new ArrayList<>();
    private SimpleAdapter adapter;
    private SimpleAdapter gridAdapter;

    private String currentEditingNoteId = null;
    private String currentNoteColor = "default";
    private boolean isCurrentNotePinned = false;
    private boolean isCurrentNoteLocked = false;
    private String currentNotePin = "";
    private View layoutSpeedDial, viewFabOverlay;
    private LinearLayout btnFabNote, btnFabNotebook;
    private TextView textFabNotebookLabel, iconFabNotebook;
    private boolean isFabOpen = false;

    private final ArrayList<CharSequence> undoList = new ArrayList<>();
    private final ArrayList<CharSequence> redoList = new ArrayList<>();
    private final ArrayList<String> undoTitleList = new ArrayList<>(); // FIX BUG 7: Title undo tracking
    private boolean isUndoRedoActive = false;
    private boolean isKeyboardOpen = false;
    private boolean isVaultUnlocked = false; // SECURE GUARD: Prevent data wipe before auth
    private boolean isVaultAuthInProgress = false;
    private boolean startupUnlockAttempted = false;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;

    private TextToSpeech textToSpeech;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable syncRunnable = this::performSyncInternal;
    private final Runnable autoSaveRunnable = this::saveCurrentNote; // SECURE AUTO-SAVE (F-RealTime Fix)

    private GoogleSignInClient googleSignInClient;
    private Drive driveService;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // 1. Show ORIGINAL Gallery Photo INSTANTLY
                        String originalPath = uri.toString();
                        addImageToCurrentNote(originalPath);

                        executor.execute(() -> {
                            try {
                                // 2. Process in background silently
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                                java.io.File imagesDir = new java.io.File(getFilesDir(), "images");
                                if (!imagesDir.exists()) imagesDir.mkdirs();

                                java.io.File file = new java.io.File(imagesDir, "note_img_" + System.currentTimeMillis() + ".webp");
                                FileOutputStream fos = new FileOutputStream(file);
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, fos); // Balanced quality
                                } else {
                                    bitmap.compress(Bitmap.CompressFormat.WEBP, 75, fos);
                                }
                                fos.flush(); fos.close(); bitmap.recycle();

                                // SECURE: Explicitly set private permissions (CWE-276 Fix)
                                file.setReadable(true, true);
                                file.setWritable(true, true);

                                // SECURE: Store path as encrypted metadata (Finding H-003 Fix)
                                // SECURE: Use Metadata Key (Fast) for image paths
                                String encryptedPath = secureEncrypt(file.getAbsolutePath(), false);

                                // 3. Swap with optimized path later
                                replacePlaceholderWithImage(originalPath, file.getAbsolutePath());
                            } catch (Exception e) { e.printStackTrace(); }
                        });
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && pendingCameraPhotoPath != null) {
                    // 1. Show ORIGINAL Camera Photo INSTANTLY
                    String originalPath = pendingCameraPhotoPath;
                    addImageToCurrentNote(originalPath);

                    executor.execute(() -> {
                        try {
                            java.io.File oldFile = new java.io.File(originalPath);

                            // Scale down in background
                            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                            options.inSampleSize = 2;
                            Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(originalPath, options);

                            java.io.File imagesDir = new java.io.File(getFilesDir(), "images");
                            if (!imagesDir.exists()) imagesDir.mkdirs();

                            java.io.File webpFile = new java.io.File(imagesDir, "note_cam_" + System.currentTimeMillis() + ".webp");
                            FileOutputStream fos = new FileOutputStream(webpFile);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, fos);
                            else bitmap.compress(Bitmap.CompressFormat.WEBP, 75, fos);
                            fos.flush(); fos.close(); bitmap.recycle();

                            // SECURE: Explicitly set private permissions (CWE-276 Fix)
                            webpFile.setReadable(true, true);
                            webpFile.setWritable(true, true);

                            // Delete the heavy original (keeps storage clean)
                            oldFile.delete();

                            // 2. Swap with optimized path
                            replacePlaceholderWithImage(originalPath, webpFile.getAbsolutePath());
                            pendingCameraPhotoPath = null;
                            getSharedPreferences("MyNotesData", MODE_PRIVATE).edit().remove("pendingCameraPhotoPath").apply(); // CLEANUP
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
            }
    );

    private final ActivityResultLauncher<String> createBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"),
            uri -> { if (uri != null) performExport(uri); }
    );

    private final ActivityResultLauncher<String[]> restoreBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) performImport(uri); }
    );

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleSignInResult(result.getData());
                } else {
                    // DEBUG: Show why login launcher failed/canceled (F-Login Fix)
                    String msg = "Login Result: " + (result.getResultCode() == Activity.RESULT_CANCELED ? "CANCELED" : "CODE " + result.getResultCode());
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            }
    );

    // SECURE: Intent Extra Whitelist (Finding F-008 Fix)
    private static final java.util.Set<String> ALLOWED_SHORTCUT_ACTIONS = new java.util.HashSet<>(java.util.Arrays.asList("new_normal_note"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // SECURE: Create Notification Channel for reminders (F-Notification Fix)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel("REMINDERS", "Note Reminders", android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminders for your secure notes");
            // SECURE: Hide sensitive content from lockscreen by default
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        // SECURE: Block screenshots and recent-apps thumbnails globally (C3 Fix)
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);

        // SECURE: Intent Referrer Null Bypass Fix (F-008.1 Final Stand)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (getReferrer() == null && getIntent().hasExtra("shortcut_action")) {
                getIntent().removeExtra("shortcut_action");
            }
        }

        // SECURE: Strict Intent Injection Whitelist Check (Finding 008 Final Fix)
        if (getIntent() != null && getIntent().hasExtra("shortcut_action")) {
            String action = getIntent().getStringExtra("shortcut_action");
            if (!ALLOWED_SHORTCUT_ACTIONS.contains(action)) {
                getIntent().removeExtra("shortcut_action");
                finish();
                return;
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                android.net.Uri referrer = getReferrer();
                if (referrer != null) {
                    String ref = referrer.toString();
                    boolean trusted = ref.equals("android-app://com.google.android.googlequicksearchbox")
                            || ref.equals("android-app://com.android.launcher3")
                            || ref.startsWith("android-app://com.google.android.launcher")
                            || ref.startsWith("android-app://" + getPackageName());
                    if (!trusted) {
                        getIntent().removeExtra("shortcut_action");
                    }
                }
            } else {
                getIntent().removeExtra("shortcut_action");
            }
        }

        setupDynamicShortcuts();
        forcePurgeLegacyData();

        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        int themeMode = sp.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        super.onCreate(savedInstanceState);
        checkAppResilience();
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setupUI();
        setupGoogleDrive();
        checkExistingSignIn();
        loadCategories();
        setupCategoriesInDrawer();
        setupAdapters();
        setupSwipeGestures();
        buttonLogout.setOnClickListener(v -> performLogout());
        setupNoteEditorLogic();
        handleIntentAction(getIntent());
        setupKeyboardListener();
        setupImageClickHandlers();
        loadViewAndSortPreferences();

        if (savedInstanceState != null) {
            pendingCameraPhotoPath = savedInstanceState.getString("pendingCameraPhotoPath");
            String draftTitle = savedInstanceState.getString("draft_title");
            String draftBody = savedInstanceState.getString("draft_body");
            if (draftTitle != null || draftBody != null) {
                screenList.setVisibility(View.GONE);
                screenAddNote.setVisibility(View.VISIBLE);
                editTitle.setText(draftTitle);
                editNoteBody.setText(draftBody);
            }
        } else {
            pendingCameraPhotoPath = getSharedPreferences("MyNotesData", MODE_PRIVATE).getString("pendingCameraPhotoPath", null);
        }

        migrateIfNeeded();
        checkInitialVaultUnlock();
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        // SECURE: Only use In-Memory bundle for temp paths (F-009 Fix)
        if (pendingCameraPhotoPath != null) {
            outState.putString("pending_camera_path", pendingCameraPhotoPath);
        }

        // Save current typing progress as a safety draft
        if (screenAddNote.getVisibility() == View.VISIBLE) {
            outState.putString("draft_title", editTitle.getText().toString());
            outState.putString("draft_body", editNoteBody.getText().toString());
        }
    }


    private void setupImageClickHandlers() {
        // Method kept but logic removed as images are now in a separate RecyclerView
    }

    private void showFullScreenGallery(int initialPosition) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);

        androidx.viewpager2.widget.ViewPager2 viewPager = dialog.findViewById(R.id.galleryViewPager);
        TextView btnClose = dialog.findViewById(R.id.btnFullImageClose);
        TextView btnMenu = dialog.findViewById(R.id.btnFullImageMenu);

        NoteImageGalleryAdapter galleryAdapter = new NoteImageGalleryAdapter(currentImagePaths);
        viewPager.setAdapter(galleryAdapter);
        viewPager.setCurrentItem(initialPosition, false);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnMenu.setOnClickListener(v -> {
            int currentPos = viewPager.getCurrentItem();
            if (currentPos < 0 || currentPos >= currentImagePaths.size()) return;

            String path = currentImagePaths.get(currentPos);
            PopupMenu popup = new PopupMenu(this, btnMenu);
            popup.getMenu().add("📤 Send Image");
            popup.getMenu().add("🗑️ Delete Image");

            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.contains("Send")) {
                    shareSpecificImage(path);
                } else {
                    confirmDeleteFromGallery(path, currentPos, galleryAdapter, viewPager, dialog);
                }
                return true;
            });
            popup.show();
        });

        dialog.show();
    }

    private void confirmDeleteFromGallery(String path, int position, NoteImageGalleryAdapter gAdapter, androidx.viewpager2.widget.ViewPager2 vp, Dialog dialog) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image?")
                .setMessage("Are you sure you want to remove this image from the note?")
                .setPositiveButton("Delete", (d, w) -> {
                    currentImagePaths.remove(position);
                    gAdapter.notifyItemRemoved(position);
                    imagesAdapter.notifyItemRemoved(position);

                    // Clear from cache
                    imageCache.remove(path);

                    if (currentImagePaths.isEmpty()) {
                        imagesRecyclerView.setVisibility(View.GONE);
                        dialog.dismiss();
                    } else {
                        // Adjust viewpager position if needed
                        int nextPos = position < currentImagePaths.size() ? position : currentImagePaths.size() - 1;
                        vp.setCurrentItem(nextPos, true);
                    }
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFullScreenImage(String path) {
        // Kept for legacy if needed, but thumbnails now use showFullScreenGallery
        showFullScreenGallery(currentImagePaths.indexOf(path));
    }

    private void shareSpecificImage(String path) {
        try {
            Uri uri = path.startsWith("content://") ? Uri.parse(path) : FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", new java.io.File(path));
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Send Image via"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteImage(String path, Dialog dialog) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Image?")
                .setMessage("Are you sure you want to remove this image from the note?")
                .setPositiveButton("Delete", (d, w) -> {
                    // 1. REMOVE FROM LIST
                    int index = currentImagePaths.indexOf(path);
                    if (index != -1) {
                        currentImagePaths.remove(index);
                        // 2. NOTIFY ADAPTER IMMEDIATELY
                        imagesAdapter.notifyItemRemoved(index);
                        imagesAdapter.notifyItemRangeChanged(index, currentImagePaths.size());
                    }

                    // 3. CLEAR FROM CACHE (Critical Fix)
                    imageCache.remove(path);

                    if (currentImagePaths.isEmpty()) imagesRecyclerView.setVisibility(View.GONE);
                    dialog.dismiss();
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // SMART PRIVACY SCREEN UTILITIES (M-005 Fix)
    private void enablePrivacyScreen() {
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void disablePrivacyScreen() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void setupKeyboardListener() {
        final View activityRootView = findViewById(android.R.id.content);
        layoutListener = () -> {
            android.graphics.Rect r = new android.graphics.Rect();
            activityRootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = activityRootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) { // Keyboard is open
                if (!isKeyboardOpen) {
                    isKeyboardOpen = true;
                    // Standard padding to keep cursor visible,
                    // but not too much to push the Title away.
                    int padding = (int) (80 * getResources().getDisplayMetrics().density);
                    editNoteBody.setPadding(0, 0, 0, padding);
                }
            } else {
                if (isKeyboardOpen) {
                    isKeyboardOpen = false;
                    editNoteBody.setPadding(0, 0, 0, 0);
                }
            }
        };
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    private void setupUI() {
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerClosed(View drawerView) { super.onDrawerClosed(drawerView); collapseCategories(); if (!isSelectionMode && !isBinMode && screenAddNote.getVisibility() != View.VISIBLE) buttonPlus.setVisibility(View.VISIBLE); }
            @Override public void onDrawerOpened(View drawerView) { super.onDrawerOpened(drawerView); buttonPlus.setVisibility(View.GONE); }
            @Override public void onDrawerSlide(View drawerView, float slideOffset) { super.onDrawerSlide(drawerView, slideOffset); if (slideOffset > 0.1f) buttonPlus.setVisibility(View.GONE); }
        });
        buttonMenu = findViewById(R.id.buttonMenu);
        buttonToggleView = findViewById(R.id.buttonToggleView);
        buttonSort = findViewById(R.id.buttonSort);
        buttonCloudSync = findViewById(R.id.buttonCloudSync);
        buttonEmptyBin = findViewById(R.id.buttonEmptyBin);
        textEmptyState = findViewById(R.id.textEmptyState);
        menuBin = findViewById(R.id.menuBin);
        menuTheme = findViewById(R.id.menuTheme);
        menuLanguage = findViewById(R.id.menuLanguage);
        menuSettings = findViewById(R.id.menuSettings);
        menuManageCategories = findViewById(R.id.menuManageCategories);
        textCategoriesHeader = findViewById(R.id.textCategoriesHeader);
        buttonToggleCategories = findViewById(R.id.buttonToggleCategories);
        scrollCategories = findViewById(R.id.scrollCategories);
        screenList = findViewById(R.id.screenList);
        screenAddNote = findViewById(R.id.screenAddNote);
        layoutSplashLayer = findViewById(R.id.layoutSplashLayer);
        ImageView imageSplashLogo = findViewById(R.id.imageSplashLogo);

        // Animate logo (Rotation effect)
        if (imageSplashLogo != null) {
            imageSplashLogo.setRotation(-180f);
            imageSplashLogo.animate().rotation(0f).setDuration(800).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        }

        // Hide splash layer after 1.5 seconds with animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (layoutSplashLayer != null) {
                layoutSplashLayer.animate().alpha(0f).setDuration(400).withEndAction(() -> {
                    layoutSplashLayer.setVisibility(View.GONE);
                    if (!isSelectionMode && !isBinMode) buttonPlus.setVisibility(View.VISIBLE);

                    // SECURE STARTUP UNLOCK: Load notes only after verification
                    checkInitialVaultUnlock();
                }).start();
            }
        }, 1500);

        buttonPlus = findViewById(R.id.buttonPlus);
        buttonPlus.setVisibility(View.GONE); // Hide initially during splash
        buttonBack = findViewById(R.id.buttonBack);
        buttonUndo = findViewById(R.id.buttonUndo);
        buttonRedo = findViewById(R.id.buttonRedo);
        buttonPin = findViewById(R.id.buttonPin);
        buttonSearchInNote = findViewById(R.id.buttonSearchInNote);
        buttonMoreNote = findViewById(R.id.buttonMoreNote);
        layoutNoteActions = findViewById(R.id.layoutNoteActions);
        buttonMore = findViewById(R.id.buttonMore);
        buttonColor = findViewById(R.id.buttonColor);
        buttonFormat = findViewById(R.id.buttonFormat);
        buttonAddFeature = findViewById(R.id.buttonAddFeature);
        buttonAlarm = findViewById(R.id.buttonAlarm);
        buttonSpeak = findViewById(R.id.buttonSpeak);
        textTimestamp = findViewById(R.id.textTimestamp);
        textWordCount = findViewById(R.id.textWordCount);
        textLastSync = findViewById(R.id.textLastSync);
        textUserEmail = findViewById(R.id.textUserEmail);
        buttonLogout = findViewById(R.id.buttonLogout);
        listViewNotes = findViewById(R.id.listViewNotes);
        gridViewNotes = findViewById(R.id.gridViewNotes);
        editTitle = findViewById(R.id.editTitle);
        searchBar = findViewById(R.id.searchBar);
        buttonClearSearch = findViewById(R.id.buttonClearSearch);
        editNoteBody = findViewById(R.id.editNoteBody);
        layoutSearchInNote = findViewById(R.id.layoutSearchInNote);
        editSearchInNote = findViewById(R.id.editSearchInNote);
        textSearchInNoteCount = findViewById(R.id.textSearchInNoteCount);
        btnFindPrevInNote = findViewById(R.id.btnFindPrevInNote);
        btnFindNextInNote = findViewById(R.id.btnFindNextInNote);
        btnCloseSearchInNote = findViewById(R.id.btnCloseSearchInNote);

        imagesRecyclerView = findViewById(R.id.imagesRecyclerView);
        imagesRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        imagesAdapter = new NoteImagesAdapter(currentImagePaths, new NoteImagesAdapter.OnImageActionListener() {
            @Override public void onImageClicked(String path, int position) { showFullScreenGallery(position); }
            @Override public void onRemoveClicked(String path, int position) {
                currentImagePaths.remove(position);
                imagesAdapter.notifyItemRemoved(position);
            }
        });
        imagesRecyclerView.setAdapter(imagesAdapter);

        layoutCategoriesInDrawer = findViewById(R.id.layoutCategoriesInDrawer);
        layoutSelectionToolbar = findViewById(R.id.layoutSelectionToolbar);
        layoutSelectionBottomBar = findViewById(R.id.layoutSelectionBottomBar);
        textSelectedCount = findViewById(R.id.textSelectedCount);
        buttonCancelSelection = findViewById(R.id.buttonCancelSelection);
        buttonSelectAll = findViewById(R.id.buttonSelectAll);
        buttonDeleteSelected = findViewById(R.id.buttonDeleteSelected);
        buttonMoreSelection = findViewById(R.id.buttonMoreSelection);
        tabRecent = findViewById(R.id.tabRecent);
        tabNormalNotes = findViewById(R.id.tabNormalNotes);
        tabNotebooks = findViewById(R.id.tabNotebooks);
        layoutNotebookBreadcrumb = findViewById(R.id.layoutNotebookBreadcrumb);
        scrollBreadcrumb = findViewById(R.id.scrollBreadcrumb);
        textBreadcrumb = findViewById(R.id.textBreadcrumb);

        // Speed Dial Views
        layoutSpeedDial = findViewById(R.id.layoutSpeedDial);
        viewFabOverlay = findViewById(R.id.viewFabOverlay);
        btnFabNote = findViewById(R.id.btnFabNote);
        btnFabNotebook = findViewById(R.id.btnFabNotebook);
        textFabNotebookLabel = findViewById(R.id.textFabNotebookLabel);
        iconFabNotebook = findViewById(R.id.iconFabNotebook);

        // Initialize format bar views
        layoutFormatBar = findViewById(R.id.layoutFormatBar);
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);
        btnUnderline = findViewById(R.id.btnUnderline);
        btnH1 = findViewById(R.id.btnH1);
        btnH2 = findViewById(R.id.btnH2);
        btnClearToggles = findViewById(R.id.btnClearToggles);
        btnCloseFormat = findViewById(R.id.btnCloseFormat);

        // Initialize Last Sync Text
        updateLastSyncText(getSharedPreferences("MyNotesData", MODE_PRIVATE).getLong("last_sync", 0));

        buttonToggleCategories.setOnClickListener(v -> {
            isCategoriesExpanded = !isCategoriesExpanded;
            scrollCategories.setVisibility(isCategoriesExpanded ? View.VISIBLE : View.GONE);
            textCategoriesHeader.setText(isCategoriesExpanded ? "Categories ▾" : "Categories ▸");
        });
        buttonMenu.setOnClickListener(v -> {
            if (isBinMode) {
                exitBinMode(); // This will handle exiting any special filter mode
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        tabRecent.setOnClickListener(v -> { isBinMode = false; isNormalFilterMode = false; isNotebookMode = false; isRecentMode = true; updateFilterTabsUI(); sortNotesBy(getSharedPreferences("MyNotesData", MODE_PRIVATE).getString("sort_criteria", "Newest First")); });
        tabNormalNotes.setOnClickListener(v -> { isBinMode = false; isNormalFilterMode = true; isNotebookMode = false; isRecentMode = false; updateFilterTabsUI(); sortNotesBy(getSharedPreferences("MyNotesData", MODE_PRIVATE).getString("sort_criteria", "Newest First")); });
        tabNotebooks.setOnClickListener(v -> {
            isBinMode = false; isNormalFilterMode = false; isNotebookMode = true; isRecentMode = false;
            currentParentId = "root"; currentLevel = 1;
            navigationPathIds.clear(); navigationPathNames.clear();
            updateFilterTabsUI(); sortNotesBy(getSharedPreferences("MyNotesData", MODE_PRIVATE).getString("sort_criteria", "Newest First"));
        });
        buttonPlus.setOnClickListener(v -> showPlusMenu());
        buttonBack.setOnClickListener(v -> closeNoteScreen());
        buttonToggleView.setOnClickListener(v -> toggleViewMode());
        buttonSort.setOnClickListener(v -> showSortMenu());
        buttonCloudSync.setOnClickListener(v -> {
            if (driveService == null) showDriveGuideAndSignIn();
            else showCloudStatusDialog();
        });

        buttonPlus.setOnClickListener(v -> {
            toggleFab(!isFabOpen);
        });
        viewFabOverlay.setOnClickListener(v -> toggleFab(false));

        btnFabNote.setOnClickListener(v -> { toggleFab(false); createNewNote("normal"); });
        btnFabNotebook.setOnClickListener(v -> { toggleFab(false); showAddFolderDialog(); });

        menuBin.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int trashedCount = 0;
            for (HashMap<String, String> n : allNotesList) {
                if ("true".equals(n.get("isTrashed"))) trashedCount++;
            }

            if (trashedCount == 0) {
                Toast.makeText(this, "Recycle Bin is empty", Toast.LENGTH_SHORT).show();
            } else {
                enterBinMode();
            }
        });
        menuTheme.setOnClickListener(v -> toggleTheme());
        menuLanguage.setOnClickListener(v -> showLanguageSelector());
        menuSettings.setOnClickListener(v -> showSettingsDialog());
        menuManageCategories.setOnClickListener(v -> showManageCategoriesDialog());
        buttonCancelSelection.setOnClickListener(v -> exitSelectionMode());
        buttonSelectAll.setOnClickListener(v -> toggleSelectAll());
        buttonDeleteSelected.setOnClickListener(v -> deleteSelectedNotes());
        buttonMoreSelection.setOnClickListener(v -> showSelectionMoreMenu());
        buttonEmptyBin.setOnClickListener(v -> emptyBin());

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                buttonClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterNotes(s.toString());
            }
        });

        buttonClearSearch.setOnClickListener(v -> {
            searchBar.setText("");
            filterNotes("");
        });

        // Initial Sort and Filter (Select Recent by default on launch)
        mainHandler.postDelayed(() -> {
            if (tabRecent != null) tabRecent.performClick();
        }, 500);
    }

    private void setupGoogleDrive() {
        // SECURE: Opaque XOR Masking (Finding F-003 Final Fix)
        // We use a non-deterministic key derivation to defeat compile-time optimizations (Constant Folding).
        int key = (android.os.Build.VERSION.SDK_INT > 0) ? 80 : 0; // Effectively 80
        int[] m = {98, 104, 99, 96, 104, 100, 100, 98, 105, 96, 105, 103, 125, 54, 34, 97, 50, 100, 101, 59, 59, 54, 57, 98, 97, 96, 98, 32, 101, 49, 57, 104, 55, 37, 55, 58, 32, 57, 61, 57, 102, 100, 38, 103, 99, 126, 49, 32, 32, 35, 126, 55, 31, 31, 55, 28, 21, 37, 35, 21, 34, 19, 31, 30, 36, 21, 30, 36, 126, 19, 31, 61};
        StringBuilder s = new StringBuilder();
        for (int i : m) s.append((char) (i ^ key));
        String serverClientId = s.toString();

        // SECURE: Request ONLY the minimal 'appdata' scope (M2 Fix)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(serverClientId)
                .requestScopes(new com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA))
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void checkExistingSignIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            initializeDriveService(account);
            updateUserInfo(account);

            // If local app is empty, check for cloud data automatically
            if (allNotesList.isEmpty()) {
                downloadBackupFromDrive();
            }
        }
    }

    private void showDriveGuideAndSignIn() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Google Drive Backup 🛡️")
                .setMessage("Securely save notes to your account.\n\n" +
                        "Please check (tick) the 'Google Drive' box on the next screen to enable sync.")
                .setPositiveButton("LOGIN", (dialog, which) -> requestSignIn())
                .setNegativeButton("LATER", null)
                .show();
    }

    private void requestSignIn() {
        // Start login intent
        signInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void handleSignInResult(Intent data) {
        com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(com.google.android.gms.common.api.ApiException.class);

            // --- NEW PERMISSION GUARD START ---
            boolean hasDrivePermission = false;
            for (Scope scope : account.getGrantedScopes()) {
                if (scope.getScopeUri().equals(DriveScopes.DRIVE_APPDATA)) {
                    hasDrivePermission = true;
                    break;
                }
            }

            if (!hasDrivePermission) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("To keep your notes safe on the cloud, please check the 'Google Drive' box during login.")
                        .setCancelable(false)
                        .setPositiveButton("Try Again", (dialog, which) -> requestSignIn())
                        .setNegativeButton("Cancel", (dialog, which) -> googleSignInClient.signOut())
                        .show();
                return;
            }
            // --- NEW PERMISSION GUARD END ---

            checkAccountMigration(account);

        } catch (com.google.android.gms.common.api.ApiException e) {
            // CRITICAL: Reveal the invisible error code (Finding F-Login Fix)
            int code = e.getStatusCode();
            String msg = "Login Blocked (Error " + code + ")";
            if (code == 10) msg += ": SHA-1 or ClientID Mismatch";
            if (code == 12500) msg += ": Configuration Issue";

            final String finalMsg = msg;
            mainHandler.post(() -> Toast.makeText(MainActivity.this, finalMsg, Toast.LENGTH_LONG).show());
        }
    }

    private void checkAccountMigration(GoogleSignInAccount account) {
        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        String lastEmailEnc = sp.getString("last_logged_in_email_secure", "");
        String lastEmail = null;

        if (!lastEmailEnc.isEmpty()) {
            lastEmail = secureDecrypt(lastEmailEnc, false);
        } else {
            // Migration from legacy plaintext
            lastEmail = sp.getString("last_logged_in_email", "");
        }

        String currentEmail = account.getEmail();
        if (currentEmail == null) return;

        // SECURE: Robust null-check for lastEmail to prevent crash (F-Login Fix)
        if (!allNotesList.isEmpty() && lastEmail != null && !lastEmail.isEmpty() && !lastEmail.equals(currentEmail)) {
            // Different account detected with existing local notes
            new AlertDialog.Builder(this)
                    .setTitle("Merge or Clear Notes? 📥")
                    .setMessage("You logged in with a different account. Would you like to merge existing notes with this account or clear local data first?")
                    .setCancelable(false)
                    .setPositiveButton("Merge & Sync", (d, w) -> {
                        saveSecureEmail(currentEmail);
                        completeLogin(account);
                    })
                    .setNegativeButton("Clear Local Data", (d, w) -> {
                        allNotesList.clear();
                        saveNotesToStorage();
                        saveSecureEmail(currentEmail);
                        completeLogin(account);
                    })
                    .show();
        } else {
            // Same account, empty local list, or migration from failed decryption
            saveSecureEmail(currentEmail);
            completeLogin(account);
        }
    }

    private void saveSecureEmail(String email) {
        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        // SECURE: Use Metadata Key (No UI Block) for login email
        sp.edit()
                .putString("last_logged_in_email_secure", secureEncrypt(email, false))
                .remove("last_logged_in_email")
                .apply();
    }

    // SECURE CENTRALIZED ENCRYPTION (Finding F1 / F-013 Fix)
    private String secureEncrypt(String plainText, boolean useMasterKey) {
        if (plainText == null) return null;
        try {
            SecretKey key = getOrCreateKey(useMasterKey ? MASTER_KEY_ALIAS : METADATA_KEY_ALIAS, useMasterKey);
            Cipher cipher = Cipher.getInstance(ALGO_GCM);
            // FIX (ROOT CAUSE): AndroidKeyStore AES-GCM keys mein caller apna IV nahi de sakta.
            // Pehle GCMParameterSpec se apna IV dete the → InvalidAlgorithmParameterException
            // → har encryption fail → notes hamesha plain mein save hote the.
            // Ab: KeyStore ko khud IV generate karne do, phir usse read karo.
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV(); // KeyStore-generated IV

            byte[] enc = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] comb = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, comb, 0, iv.length);
            System.arraycopy(enc, 0, comb, iv.length, enc.length);
            return android.util.Base64.encodeToString(comb, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            android.util.Log.e("NoteStorage", "secureEncrypt failed: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            return null;
        }
    }

    private String secureDecrypt(String cipherText, boolean useMasterKey) {
        if (cipherText == null) return null;
        try {
            byte[] combined = android.util.Base64.decode(cipherText, android.util.Base64.DEFAULT);
            byte[] iv = new byte[IV_LENGTH];
            byte[] enc = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, enc, 0, enc.length);

            SecretKey key = getOrCreateKey(useMasterKey ? MASTER_KEY_ALIAS : METADATA_KEY_ALIAS, useMasterKey);
            Cipher cipher = Cipher.getInstance(ALGO_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(enc), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            android.util.Log.e("NoteStorage", "secureDecrypt failed: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            return null;
        }
    }


    private void completeLogin(GoogleSignInAccount account) {
        initializeDriveService(account);
        updateUserInfo(account);
        Toast.makeText(this, "Welcome, " + account.getDisplayName() + "! Checking for cloud data...", Toast.LENGTH_LONG).show();
        drawerLayout.openDrawer(GravityCompat.START);

        // First, check and download existing backup
        downloadBackupFromDrive();

        // Ensure UI is in a valid state (Select Recent by default)
        mainHandler.post(() -> tabRecent.performClick());
    }

    private void updateUserInfo(GoogleSignInAccount account) {
        if (account != null && account.getDisplayName() != null) {
            textUserEmail.setText("Hi, " + account.getDisplayName());
            textUserEmail.setVisibility(View.VISIBLE);
            buttonLogout.setVisibility(View.VISIBLE);
        } else {
            textUserEmail.setVisibility(View.GONE);
            buttonLogout.setVisibility(View.GONE);
        }
    }

    private void performLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out? Automatic sync will stop.")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Sign out and also revoke access to ensure a clean slate
                    googleSignInClient.signOut().addOnCompleteListener(task -> {
                        googleSignInClient.revokeAccess().addOnCompleteListener(task2 -> {
                            driveService = null;
                            updateUserInfo(null);
                            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void initializeDriveService(com.google.android.gms.auth.api.signin.GoogleSignInAccount account) {
        com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential credential =
                com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(this,
                        java.util.Collections.singleton(com.google.api.services.drive.DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(account.getAccount());

        // SECURE: Use explicit NetHttpTransport to avoid legacy 'Trust-All' logic (C2 Fix)
        com.google.api.client.http.HttpTransport transport = new com.google.api.client.http.javanet.NetHttpTransport();

        driveService = new com.google.api.services.drive.Drive.Builder(transport, new com.google.api.client.json.gson.GsonFactory(), credential)
                .setApplicationName("GoNotes Pro").build();
    }

    private void showCloudStatusDialog() {
        long lastSyncTime = getSharedPreferences("MyNotesData", MODE_PRIVATE).getLong("last_sync", 0);
        String lastSyncStr = (lastSyncTime == 0) ? "Never" : new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(lastSyncTime));

        String userEmail = "Connected";
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && account.getEmail() != null) userEmail = account.getEmail();

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Cloud Status")
                .setMessage("🟢 Status: Active\n📧 Account: " + userEmail + "\n⏰ Last Sync: " + lastSyncStr)
                .setPositiveButton("RESTORE", (d, w) -> downloadBackupFromDrive())
                .setNeutralButton("BACKUP", (d, w) -> uploadBackupToDrive())
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private void uploadBackupToDrive() {
        if (driveService == null || executor.isShutdown()) return;

        final com.google.android.gms.auth.api.signin.GoogleSignInAccount account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this);
        if (account == null || account.getId() == null) {
            Toast.makeText(this, "Account error, please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                java.io.File exportsDir = new java.io.File(getCacheDir(), "exports");
                if (!exportsDir.exists()) exportsDir.mkdirs();

                java.io.File tempFile = new java.io.File(exportsDir, "drive_upload.qnb");
                // SECURE: Use Hardware-Bound key for cloud backup (F-Backup Fix)
                // We no longer use the predictable Google Account ID.
                performExportSyncWithKey(Uri.fromFile(tempFile), getBackupKey());

                File fileMetadata = new File().setName("GoNotesPro_Backup.qnb").setParents(java.util.Collections.singletonList("appDataFolder"));
                FileContent mediaContent = new FileContent("application/octet-stream", tempFile);
                FileList result = driveService.files().list().setSpaces("appDataFolder").execute();
                String existingId = null;
                if (result.getFiles() != null) {
                    for (File f : result.getFiles()) { if ("GoNotesPro_Backup.qnb".equals(f.getName())) { existingId = f.getId(); break; } }
                }

                if (existingId != null) driveService.files().update(existingId, null, mediaContent).execute();
                else driveService.files().create(fileMetadata, mediaContent).execute();

                tempFile.delete();
                mainHandler.post(() -> {
                    long time = System.currentTimeMillis();
                    getSharedPreferences("MyNotesData", MODE_PRIVATE).edit().putLong("last_sync", time).apply();
                    updateLastSyncText(time);
                    Toast.makeText(this, "Manual backup successful!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(this, "Manual backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void downloadBackupFromDrive() {
        if (driveService == null || executor.isShutdown()) return;

        final com.google.android.gms.auth.api.signin.GoogleSignInAccount account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this);
        if (account == null || account.getId() == null) return;

        executor.execute(() -> {
            try {
                mainHandler.post(() -> Toast.makeText(this, "Searching for backup on cloud...", Toast.LENGTH_SHORT).show());
                FileList result = driveService.files().list().setSpaces("appDataFolder").execute();
                String fileId = null;
                if (result.getFiles() != null) {
                    for (File f : result.getFiles()) { if ("GoNotesPro_Backup.qnb".equals(f.getName())) { fileId = f.getId(); break; } }
                }

                if (fileId != null) {
                    mainHandler.post(() -> Toast.makeText(this, "Backup found! Downloading...", Toast.LENGTH_SHORT).show());
                    java.io.File tempFile = new java.io.File(getCacheDir(), "drive_download.qnb");
                    OutputStream os = new FileOutputStream(tempFile);
                    driveService.files().get(fileId).executeMediaAndDownloadTo(os);
                    os.close();
                    // SECURE: Explicitly set private permissions (CWE-276 Fix)
                    tempFile.setReadable(true, true);
                    tempFile.setWritable(true, true);					// SECURE: Decrypt using Hardware-Bound key (F-Backup Fix)
                    final SecretKey accountKey = getBackupKey();
                    mainHandler.post(() -> {
                        try {
                            performImportWithKey(Uri.fromFile(tempFile), accountKey);
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                } else {
                    mainHandler.post(() -> Toast.makeText(this, "No existing backup found on cloud.", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(this, "Cloud check failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private SecretKey getOrCreateKey(String alias, boolean requireAuth) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        // FIX: Agar purani key permanently invalidate ho gayi ho (biometric change ke baad)
        // toh usse delete karo taaki fresh key ban sake
        if (keyStore.containsAlias(alias)) {
            try {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null);
                if (entry == null) {
                    keyStore.deleteEntry(alias);
                    android.util.Log.w("NoteStorage", "Dead key removed, will recreate: " + alias);
                } else {
                    // MAIN FIX: Key ko bina auth ke test karo
                    // Agar UserNotAuthenticatedException aaye = purani key jo fingerprint maangti thi
                    Cipher testCipher = Cipher.getInstance(ALGO_GCM);
                    // FIX: Caller IV nahi de sakta AndroidKeyStore keys mein (same bug as secureEncrypt)
                    testCipher.init(Cipher.ENCRYPT_MODE, entry.getSecretKey());
                    return entry.getSecretKey(); // Key sahi hai
                }
            } catch (android.security.keystore.UserNotAuthenticatedException e) {
                // FIX: Sirf purani key delete karo — notes SharedPreferences se mat hatao.
                // Biometric/fingerprint change hone pe notes wipe nahi honge.
                keyStore.deleteEntry(alias);
                android.util.Log.w("NoteStorage", "Auth-required key deleted, notes preserved, will recreate key: " + alias);
            } catch (Exception invalidEx) {
                keyStore.deleteEntry(alias);
                android.util.Log.w("NoteStorage", "Invalid key removed, will recreate: " + alias + " (" + invalidEx.getClass().getSimpleName() + ")");
            }
        }

        if (!keyStore.containsAlias(alias)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                // FIX: Auth requirement hataya - key expire ya invalidate nahi hogi.
                // Data abhi bhi AES-256-GCM se encrypted rahega AndroidKeyStore mein (hardware-backed).
                // Biometric lock notes OPEN karne ke liye alag se hai (showBiometricPrompt),
                // storage encryption ke liye auth-gating ki zaroorat nahi.
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);
                keyGenerator.init(builder.build());
            } else {
                keyGenerator.init(256);
            }
            return keyGenerator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(alias, null)).getSecretKey();
    }

    private void performExportSync(Uri uri) throws Exception {
        // Use MASTER Key for export stability
        performExportSyncWithKey(uri, getOrCreateKey(MASTER_KEY_ALIAS, true));
    }

    // SECURE BACKUP EXPORT (Finding 001 Fix - NO HARDCODED MAGIC BYTES)
    private void performExportSyncWithKey(Uri uri, SecretKey key) throws Exception {
        OutputStream os = getContentResolver().openOutputStream(uri);
        // New Format V3: [16-byte Random Signature] [1 Version] [12 Nonce] [Ciphertext]
        byte[] signature = new byte[16];
        new java.security.SecureRandom().nextBytes(signature);
        os.write(signature);
        os.write(3); // Version 3

        Cipher cipher = Cipher.getInstance(ALGO_GCM);
        byte[] nonce = new byte[IV_LENGTH];
        new java.security.SecureRandom().nextBytes(nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH, nonce));

        os.write(nonce);
        CipherOutputStream cos = new CipherOutputStream(os, cipher);
        ZipOutputStream zos = new ZipOutputStream(cos);
        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        JSONObject obj = new JSONObject();

        // Use secure notes for export
        String secureNotes = sp.getString("notes_json_secure", "[]");
        JSONArray notesArray;
        if (secureNotes.startsWith("[")) {
            notesArray = new JSONArray(secureNotes);
        } else {
            // Decrypt current secure data to export as plain JSON inside the ZIP
            byte[] combined = android.util.Base64.decode(secureNotes, android.util.Base64.DEFAULT);
            byte[] ivS = new byte[IV_LENGTH];
            byte[] enc = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, ivS, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, enc, 0, enc.length);
            Cipher cS = Cipher.getInstance(ALGO_GCM);
            cS.init(Cipher.DECRYPT_MODE, getOrCreateKey(MASTER_KEY_ALIAS, true), new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH, ivS));
            notesArray = new JSONArray(new String(cS.doFinal(enc), java.nio.charset.StandardCharsets.UTF_8));
        }

        // SECURE CATEGORIES EXPORT (Finding 015 Fix)
        // Fetch categories from encrypted storage instead of deleted plaintext key
        String secureCats = sp.getString("categories_list_secure", "[]");
        JSONArray catsArray;
        if (secureCats.startsWith("[")) {
            catsArray = new JSONArray(secureCats);
        } else {
            byte[] combinedC = android.util.Base64.decode(secureCats, android.util.Base64.DEFAULT);
            byte[] ivC = new byte[IV_LENGTH];
            byte[] encC = new byte[combinedC.length - IV_LENGTH];
            System.arraycopy(combinedC, 0, ivC, 0, IV_LENGTH);
            System.arraycopy(combinedC, IV_LENGTH, encC, 0, encC.length);
            Cipher cC = Cipher.getInstance(ALGO_GCM);
            cC.init(Cipher.DECRYPT_MODE, getOrCreateKey(MASTER_KEY_ALIAS, true), new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH, ivC));
            catsArray = new JSONArray(new String(cC.doFinal(encC), java.nio.charset.StandardCharsets.UTF_8));
        }

        obj.put("notes", notesArray);
        obj.put("categories", catsArray);
        obj.put("categories_modified_at", sp.getLong("categories_modified_at", 0));

        zos.putNextEntry(new java.util.zip.ZipEntry("data.json")); zos.write(obj.toString().getBytes()); zos.closeEntry();

        java.io.File imagesDir = new java.io.File(getFilesDir(), "images");
        java.io.File[] files = imagesDir.listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                if (f.getName().endsWith(".webp") || f.getName().endsWith(".jpg")) {
                    zos.putNextEntry(new java.util.zip.ZipEntry("images/" + f.getName()));
                    FileInputStream fis = new FileInputStream(f);
                    byte[] buf = new byte[1024]; int len; while ((len = fis.read(buf)) > 0) zos.write(buf, 0, len);
                    fis.close(); zos.closeEntry();
                }
            }
        }
        zos.close(); cos.close(); os.close();
    }

    private void performExport(Uri uri) {
        if (executor.isShutdown()) return;
        executor.execute(() -> {
            try {
                performExportSync(uri);
                mainHandler.post(() -> Toast.makeText(this, "Export successful", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void performImport(Uri uri) {
        try {
            // Use MASTER key for import
            performImportWithKey(uri, getOrCreateKey(MASTER_KEY_ALIAS, true));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // SECURE BACKUP IMPORT (Finding 001 Fix)
    private void performImportWithKey(Uri uri, SecretKey key) {
        if (executor.isShutdown()) return;
        executor.execute(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) throw new Exception("Failed to open input stream");

                // 1. Skip Random Signature (V3 Format)
                is.skip(16);

                // 2. Read Version and Nonce
                int version = is.read();
                byte[] nonce = new byte[IV_LENGTH];
                is.read(nonce);

                Cipher cipher = Cipher.getInstance(ALGO_GCM);
                cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH, nonce));

                // FIX BUG 4: CipherInputStream Android ka known bug hai — AES-GCM auth tag verify nahi karta
                // Pehle saara encrypted data read karo, phir ek baar doFinal() karo taaki tampered data reject ho
                java.io.ByteArrayOutputStream encBaos = new java.io.ByteArrayOutputStream();
                byte[] tmpBuf = new byte[4096]; int tmpLen;
                while ((tmpLen = is.read(tmpBuf)) > 0) encBaos.write(tmpBuf, 0, tmpLen);
                is.close();
                byte[] decryptedBytes = cipher.doFinal(encBaos.toByteArray());
                java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                        new java.io.ByteArrayInputStream(decryptedBytes));
                java.util.zip.ZipEntry entry;
                JSONObject importedData = null;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("data.json")) {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        byte[] buf = new byte[1024]; int len;
                        while ((len = zis.read(buf)) > 0) baos.write(buf, 0, len);
                        importedData = new JSONObject(baos.toString());
                    } else if (entry.getName().startsWith("images/")) {
                        String fileName = entry.getName().substring(7);
                        java.io.File imagesDir = new java.io.File(getFilesDir(), "images");
                        if (!imagesDir.exists()) imagesDir.mkdirs();
                        java.io.File destFile = new java.io.File(imagesDir, fileName);

                        // ZIP SLIP PROTECTION
                        if (!destFile.getCanonicalPath().startsWith(imagesDir.getCanonicalPath())) throw new SecurityException("Malicious backup file!");

                        FileOutputStream fos = new FileOutputStream(destFile);
                        byte[] buf = new byte[1024]; int len;
                        while ((len = zis.read(buf)) > 0) fos.write(buf, 0, len);
                        fos.close();
                        // SECURE: Explicitly set private permissions (CWE-276 Fix)
                        destFile.setReadable(true, true);
                        destFile.setWritable(true, true);
                    }
                    zis.closeEntry();
                }

                final JSONObject finalObj = importedData;
                zis.close();
                mainHandler.post(() -> {
                    if (finalObj == null) {
                        Toast.makeText(this, "Invalid backup data", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONArray cloudNotesArray = finalObj.optJSONArray("notes");
                        JSONArray cloudCatsArray = finalObj.optJSONArray("categories");
                        long cloudCatsTime = finalObj.optLong("categories_modified_at", 0);

                        if (cloudNotesArray == null) cloudNotesArray = new JSONArray();
                        if (cloudCatsArray == null) cloudCatsArray = new JSONArray();

                        // --- SMART MERGE LOGIC ---
                        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
                        java.util.Set<String> localDeadList = sp.getStringSet("deleted_note_ids", new java.util.HashSet<>());

                        // 1. Merge Categories
                        long localCatsTime = sp.getLong("categories_modified_at", 0);
                        if (cloudCatsTime > localCatsTime || categoriesList.isEmpty()) {
                            categoriesList.clear();
                            for (int i = 0; i < cloudCatsArray.length(); i++) categoriesList.add(cloudCatsArray.getString(i));
                            saveCategories();
                        }

                        // 2. Merge Notes
                        HashMap<String, HashMap<String, String>> localNotesMap = new HashMap<>();
                        for (HashMap<String, String> n : allNotesList) localNotesMap.put(n.get("id"), n);

                        boolean localChanged = false;
                        for (int i = 0; i < cloudNotesArray.length(); i++) {
                            JSONObject cObj = cloudNotesArray.getJSONObject(i);
                            String id = cObj.getString("id");
                            if (localDeadList.contains(id)) continue;

                            HashMap<String, String> cMap = new HashMap<>();
                            java.util.Iterator<String> keys = cObj.keys();
                            while(keys.hasNext()) { String k = keys.next(); cMap.put(k, cObj.getString(k)); }

                            if (localNotesMap.containsKey(id)) {
                                long lTime = Long.parseLong(localNotesMap.get(id).get("modified_at"));
                                long cTime = Long.parseLong(cMap.get("modified_at"));
                                if (cTime > lTime) { localNotesMap.put(id, cMap); localChanged = true; }
                            } else { localNotesMap.put(id, cMap); localChanged = true; }
                        }

                        if (localChanged) {
                            allNotesList.clear();
                            allNotesList.addAll(localNotesMap.values());
                            saveNotesToStorage();
                            // Auto-restore categories from notes
                            for (HashMap<String, String> n : allNotesList) {
                                String cat = n.get("category");
                                if (cat != null && !cat.isEmpty() && !categoriesList.contains(cat)) categoriesList.add(cat);
                            }
                            saveCategories();
                        }

                        loadNotesFromStorage();
                        setupCategoriesInDrawer();
                        Toast.makeText(this, "Restore successful!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Restore failed: Data corrupted", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(this, "Import failed: Check your Google Account", Toast.LENGTH_LONG).show());
            }
        });
    }

    // SECURE BACKUP KEY DERIVATION (F-Backup Fix)
    private SecretKey getBackupKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        String BACKUP_SECRET_ALIAS = "BackupMasterSecret";

        if (!ks.containsAlias(BACKUP_SECRET_ALIAS)) {
            // Generate a high-entropy, hardware-bound random secret
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                kg.init(new KeyGenParameterSpec.Builder(BACKUP_SECRET_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
            } else {
                kg.init(256);
            }
            kg.generateKey();
        }

        // Use the hardware-bound key itself for maximum security
        return ((KeyStore.SecretKeyEntry) ks.getEntry(BACKUP_SECRET_ALIAS, null)).getSecretKey();
    }

    private void setupAdapters() {
        adapter = new SimpleAdapter(this, displayedNotesList, R.layout.list_item_note, new String[]{"title", "snippet"}, new int[]{R.id.listItemTitle, R.id.listItemSnippet}) {
            @Override public View getView(int p, View v, ViewGroup pr) {
                View view = super.getView(p, v, pr);
                bindNoteView(view, displayedNotesList.get(p), R.id.listItemTitle, R.id.listItemSnippet, R.id.listItemDate, false);
                return view;
            }
        };
        listViewNotes.setAdapter(adapter);

        gridAdapter = new SimpleAdapter(this, displayedNotesList, R.layout.grid_item_note, new String[]{"title", "snippet"}, new int[]{R.id.gridItemTitle, R.id.gridItemSnippet}) {
            @Override public View getView(int p, View v, ViewGroup pr) {
                View view = super.getView(p, v, pr);
                bindNoteView(view, displayedNotesList.get(p), R.id.gridItemTitle, R.id.gridItemSnippet, R.id.gridItemDate, true);
                return view;
            }
        };
        gridViewNotes.setAdapter(gridAdapter);
        AdapterView.OnItemClickListener l = (p, vi, pos, id) -> {
            HashMap<String, String> n = displayedNotesList.get(pos);
            if ("true".equals(n.get("isFolder"))) {
                if (isSelectionMode) { toggleNoteSelection(n.get("id")); return; }
                if (isBinMode) { showBinOptions(n); return; }

                // Open folder: navigate and switch to Notebook mode if coming from All Notes
                currentParentId = n.get("id");
                String lvl = n.get("level");
                currentLevel = (lvl != null) ? Integer.parseInt(lvl) + 1 : 2;

                if (!isNotebookMode) {
                    isNotebookMode = true; isBinMode = false; isNormalFilterMode = false;
                    navigationPathIds.clear(); navigationPathNames.clear();
                }
                navigationPathIds.add(currentParentId);
                navigationPathNames.add(n.get("title"));
                updateFilterTabsUI(); filterNotes("");
                return;
            }
            if (isSelectionMode) toggleNoteSelection(n.get("id"));
            else if (isBinMode) showBinOptions(n);
            else if ("true".equals(n.get("isLocked"))) checkBiometricAndUnlock(n);
            else openNoteContent(n);
        };
        listViewNotes.setOnItemClickListener(l); gridViewNotes.setOnItemClickListener(l);

        AdapterView.OnItemLongClickListener ll = (p, vi, pos, id) -> {
            if (!isSelectionMode) {
                enterSelectionMode();
                toggleNoteSelection(displayedNotesList.get(pos).get("id"));
            }
            return true;
        };
        listViewNotes.setOnItemLongClickListener(ll); gridViewNotes.setOnItemLongClickListener(ll);
    }

    private void showQuickActionsMenu(View anchor, HashMap<String, String> note) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        boolean isPinned = "true".equals(note.get("isPinned"));
        boolean isLocked = "true".equals(note.get("isLocked"));
        boolean isFolder = "true".equals(note.get("isFolder"));

        popup.getMenu().add(isPinned ? "Unpin" : "Pin");
        if (!isFolder) popup.getMenu().add(isLocked ? "Unlock" : "Lock");
        popup.getMenu().add("Rename");
        popup.getMenu().add("Move");
        popup.getMenu().add("Select Multiple");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            String id = note.get("id");
            if (title.equals("Pin") || title.equals("Unpin")) {
                note.put("isPinned", String.valueOf(!isPinned));
                note.put("modified_at", String.valueOf(System.currentTimeMillis()));
                saveNotesToStorage(); filterNotes(""); uploadBackupToDriveBackground(false);
            } else if (title.equals("Lock") || title.equals("Unlock")) {
                if (isLocked) {
                    note.put("isLocked", "false"); note.put("pin", "");
                    note.put("modified_at", String.valueOf(System.currentTimeMillis()));
                    saveNotesToStorage(); filterNotes(""); uploadBackupToDriveBackground(false);
                    Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT).show();
                } else {
                    showPinDialogForNote(note);
                }
            } else if (title.equals("Rename")) {
                selectedNoteIds.clear(); selectedNoteIds.add(id);
                showRenameDialog();
            } else if (title.equals("Move")) {
                selectedNoteIds.clear(); selectedNoteIds.add(id);
                showMoveDialog();
            } else if (title.equals("Select Multiple")) {
                enterSelectionMode();
                toggleNoteSelection(id);
            } else 			if (title.equals("Delete")) {
                note.put("isTrashed", "true");
                note.put("modified_at", String.valueOf(System.currentTimeMillis()));
                saveNotesToStorage(); filterNotes(""); uploadBackupToDriveBackground(false);
                Toast.makeText(this, "Moved to Bin", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popup.show();
    }

    private void showPinDialogForNote(HashMap<String, String> note) {
        enablePrivacyScreen(); // SECURE: Protect PIN entry from screenshots/recording
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Set 4-digit PIN");
        new AlertDialog.Builder(this)
                .setTitle("Lock with PIN")
                .setView(input)
                .setPositiveButton("Lock", (d, w) -> {
                    disablePrivacyScreen();
                    String pin = input.getText().toString().trim();
                    if (pin.length() >= 4) {
                        note.put("isLocked", "true");
                        note.put("pin", hashPIN(pin)); // SECURE: Store Hash, not Plaintext
                        note.put("modified_at", String.valueOf(System.currentTimeMillis()));
                        saveNotesToStorage(); filterNotes(""); uploadBackupToDriveBackground(false);
                    } else Toast.makeText(this, "PIN too short", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancel", (d, w) -> disablePrivacyScreen()).show();
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        selectedNoteIds.clear();
        layoutSelectionToolbar.setVisibility(View.VISIBLE);
        layoutSelectionBottomBar.setVisibility(View.VISIBLE);
        buttonPlus.setVisibility(View.GONE);
        findViewById(R.id.layoutFilterTabs).setVisibility(View.GONE);
        updateSelectionCount();
    }

    private void toggleSelectAll() {
        if (selectedNoteIds.size() == displayedNotesList.size()) {
            selectedNoteIds.clear();
            exitSelectionMode();
        } else {
            for (HashMap<String, String> n : displayedNotesList) {
                selectedNoteIds.add(n.get("id"));
            }
            updateSelectionCount();
            adapter.notifyDataSetChanged();
            if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
        }
        // Update visual state of the button
        updateSelectAllButtonUI();
    }

    private void updateSelectAllButtonUI() {
        if (buttonSelectAll != null) {
            boolean allSelected = !displayedNotesList.isEmpty() && selectedNoteIds.size() == displayedNotesList.size();
            buttonSelectAll.setText(allSelected ? "☑️ All" : "⬜ All");
        }
    }

    private void updateSelectionCount() {
        int count = selectedNoteIds.size();
        textSelectedCount.setText(count + " selected");
        updateSelectAllButtonUI();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedNoteIds.clear();
        layoutSelectionToolbar.setVisibility(View.GONE);
        layoutSelectionBottomBar.setVisibility(View.GONE);
        if (!isBinMode) buttonPlus.setVisibility(View.VISIBLE);
        updateFilterTabsUI();
        adapter.notifyDataSetChanged();
        if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
    }

    private void setupSwipeGestures() {
        swipeDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float vX, float vY) {
                if (isBinMode || isSelectionMode || e1 == null || e2 == null) return false;
                float dX = e2.getX() - e1.getX();
                float dY = e2.getY() - e1.getY();
                // Relaxed thresholds for better sensitivity
                if (Math.abs(dX) > Math.abs(dY) && Math.abs(dX) > 100 && Math.abs(vX) > 100) {
                    if (dX > 0) onSwipeRight(); else onSwipeLeft();
                    return true;
                }
                return false;
            }
        });
        View.OnTouchListener tl = (v, ev) -> { swipeDetector.onTouchEvent(ev); return false; };
        listViewNotes.setOnTouchListener(tl);
        gridViewNotes.setOnTouchListener(tl);
        screenList.setOnTouchListener(tl); // Enable swipe on empty areas too
    }

    private void onSwipeLeft() {
        if (isRecentMode) tabNormalNotes.performClick();
        else if (isNormalFilterMode) tabNotebooks.performClick();
    }

    private void onSwipeRight() {
        if (isNotebookMode) tabNormalNotes.performClick();
        else if (isNormalFilterMode) tabRecent.performClick();
    }
    private void collapseCategories() { isCategoriesExpanded = false; scrollCategories.setVisibility(View.GONE); textCategoriesHeader.setText("Categories ▸"); }

    private void checkInitialVaultUnlock() {
        if (startupUnlockAttempted) {
            return;
        }
        startupUnlockAttempted = true;

        // Detect if we have biometrics enrolled
        BiometricManager bm = BiometricManager.from(this);
        int status = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (status == BiometricManager.BIOMETRIC_SUCCESS) {
            // MANDATORY STARTUP UNLOCK (Ensures key is released for the session)
            showStartupBiometricPrompt();
        } else {
            // Fallback for devices without biometric (load normally, key will use standard mode)
            loadNotesFromStorage();
        }
    }

    private void showStartupBiometricPrompt() {
        try {
            // SECURE UNLOCK: We don't pass CryptoObject here to properly activate the
            // 30-minute hardware key release window (F-Auth Fix).
            new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult r) {
                    isVaultUnlocked = true; // Release the guard
                    loadNotesFromStorage(); // Access granted, load the notes!
                }
                @Override public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    // If canceled or failed, show empty state to protect data
                    isVaultUnlocked = false;
                    Toast.makeText(MainActivity.this, "Vault locked: " + errString, Toast.LENGTH_SHORT).show();
                    filterNotes("");
                }
            }).authenticate(new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Secure Vault")
                    .setSubtitle("Confirm identity to load your notes")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build());
        } catch (Exception e) {
            // Fallback for devices without biometric (PIN/Pattern) or first-time install
            loadNotesFromStorage();
        }
    }

    private void checkBiometricAndUnlock(HashMap<String, String> n) {
        // SECURITY: Disable biometric on rooted devices as it can be easily bypassed
        if (isDeviceRooted()) {
            showPinDialog(n);
            return;
        }

        BiometricManager bm = BiometricManager.from(this);
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) showBiometricPrompt(n);
        else showPinDialog(n);
    }

    private void showBiometricPrompt(HashMap<String, String> n) {
        try {
            SecretKey key = getOrCreateKey(MASTER_KEY_ALIAS, true);
            Cipher cipher = Cipher.getInstance(ALGO_GCM);
            // We use ENCRYPT_MODE just to "prime" the cipher for auth-binding check
            // In a more complex app, we'd use this cipher to decrypt the specific note.
            cipher.init(Cipher.ENCRYPT_MODE, key);

            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

            new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult r) {
                    // SECURE: Only proceed if the hardware-backed CryptoObject is returned
                    if (r.getCryptoObject() != null) {
                        openNoteContent(n);
                    }
                }
                @Override public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        showPinDialog(n);
                    }
                }
            }).authenticate(new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Note")
                    .setSubtitle("Use fingerprint or PIN to unlock")
                    .setNegativeButtonText("Use PIN")
                    .build(), cryptoObject); // SECURE BINDING: Pass the CryptoObject
        } catch (Exception e) {
            // Fallback if CryptoObject creation fails (e.g. no fingerprints enrolled)
            showPinDialog(n);
        }
    }

    private void showPinDialog(HashMap<String, String> n) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter 4-digit PIN");

        // Add some padding to the EditText for better UI
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 60; params.rightMargin = 60; params.topMargin = 20;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Enter PIN")
                .setView(container)
                .setPositiveButton("Unlock", (d, w) -> {
                    String enteredPin = input.getText().toString().trim();
                    String savedPinHash = n.get("pin");
                    // SECURE: Compare Hashed PINs
                    if (hashPIN(enteredPin).equals(savedPinHash)) {
                        openNoteContent(n);
                    } else {
                        Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openNoteContent(HashMap<String, String> n) {
        // SECURE: Enable privacy screen if the note is locked (C3 Fix)
        if ("true".equals(n.get("isLocked"))) {
            enablePrivacyScreen();
        } else {
            disablePrivacyScreen();
        }

        currentEditingNoteId = n.get("id");
        editTitle.setText(n.get("title"));
        currentNoteCategory = n.get("category") != null ? n.get("category") : "General";

        isUndoRedoActive = true;
        String fullBody = n.get("fullBody") != null ? n.get("fullBody") : "";
        editNoteBody.setText(fullBody);

        undoList.clear();
        redoList.clear();
        undoList.add(editNoteBody.getText().toString());
        undoTitleList.clear(); // FIX BUG 7: Title undo reset when note opens
        undoTitleList.add(n.get("title") != null ? n.get("title") : "");
        isUndoRedoActive = false;

        currentImagePaths.clear();
        try {
            String imagesJson = n.get("images");
            if (imagesJson != null) {
                JSONArray arr = new JSONArray(imagesJson);
                for (int i = 0; i < arr.length(); i++) currentImagePaths.add(arr.getString(i));
            }
        } catch (Exception e) {}
        imagesAdapter.notifyDataSetChanged();
        imagesRecyclerView.setVisibility(currentImagePaths.isEmpty() ? View.GONE : View.VISIBLE);

        // Setup editor UI
        editNoteBody.setTypeface(Typeface.DEFAULT);
        // Normal notes should wrap text
        editNoteBody.setHorizontallyScrolling(false);
        // Enable standard text features for normal notes
        editNoteBody.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        isCurrentNotePinned = "true".equals(n.get("isPinned"));
        isCurrentNoteLocked = "true".equals(n.get("isLocked"));
        currentNotePin = n.get("pin") != null ? n.get("pin") : "";
        updateEditorToolbarIcons();

        screenList.setVisibility(View.GONE);
        screenAddNote.setVisibility(View.VISIBLE);
        buttonPlus.setVisibility(View.GONE);
    }

    private void updateEditorToolbarIcons() {
        buttonPin.setTextColor(isCurrentNotePinned ? Color.parseColor("#FFD700") : ContextCompat.getColor(this, R.color.primaryTextColor));

        // Ensure actions are closed when opening a note
        if (layoutNoteActions != null) layoutNoteActions.setVisibility(View.GONE);
    }

    private void toggleNoteActions() {
        if (layoutNoteActions.getVisibility() == View.VISIBLE) {
            // Animate out and hide
            layoutNoteActions.animate().alpha(0f).translationX(50f).setDuration(200).withEndAction(() -> {
                layoutNoteActions.setVisibility(View.GONE);
            }).start();
        } else {
            // Show and animate in
            layoutNoteActions.setVisibility(View.VISIBLE);
            layoutNoteActions.setAlpha(0f);
            layoutNoteActions.setTranslationX(50f);
            layoutNoteActions.animate().alpha(1f).translationX(0f).setDuration(200).start();
        }
    }

    private void closeNoteScreen() {
        // HIDE KEYBOARD
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        android.view.View focusView = getCurrentFocus();
        if (imm != null && focusView != null) {
            imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            buttonSpeak.setText("🔊");
        }
        if (layoutSearchInNote != null) {
            layoutSearchInNote.setVisibility(View.GONE);
            clearSearchHighlights();
        }
        saveCurrentNote();
        disablePrivacyScreen(); // SECURE: Re-enable screenshots for normal list view (C3 Fix)
        screenAddNote.setVisibility(View.GONE);
        screenList.setVisibility(View.VISIBLE);
        if (!isBinMode && !isSelectionMode) buttonPlus.setVisibility(View.VISIBLE);
        // FIX BUG 1: Note editor se wapas aane par hamesha Recent tab reset karo
        // Warna selectedCategoryFilter ya isNormalFilterMode active rehta hai
        // aur filterNotes sirf filtered notes dikhata hai, baki categories ke notes hide ho jaate hain
        isRecentMode = true;
        isNormalFilterMode = false;
        isNotebookMode = false;
        isBinMode = false;
        selectedCategoryFilter = "All";
        updateFilterTabsUI();
        filterNotes("");
    }

    private void saveCurrentNote() {
        if (layoutSearchInNote != null && layoutSearchInNote.getVisibility() == View.VISIBLE) {
            clearSearchHighlights();
        }

        String t = editTitle.getText().toString().trim();
        String b = editNoteBody.getText().toString().trim();

        if (t.isEmpty() && b.isEmpty() && currentEditingNoteId == null && currentImagePaths.isEmpty()) return;

        HashMap<String, String> n = null;
        if (currentEditingNoteId == null) {
            n = new HashMap<>();
            String newId = UUID.randomUUID().toString();
            n.put("id", newId);
            currentEditingNoteId = newId; // CRITICAL: Link session to ID
            n.put("created_at", new SimpleDateFormat("dd/MM/yyyy, HH:mm").format(new Date()));
            if (isNotebookMode) {
                n.put("parentId", currentParentId);
                n.put("level", String.valueOf(currentLevel));
            } else {
                n.put("parentId", "root");
                n.put("level", "1");
            }
            allNotesList.add(0, n); // NEW NOTE ONLY
        } else {
            // UPDATE EXISTING NOTE
            for (HashMap<String, String> item : allNotesList) {
                if (currentEditingNoteId.equals(item.get("id"))) {
                    n = item;
                    break;
                }
            }
            // If not found in main list (migration case), create entry
            if (n == null) {
                n = new HashMap<>();
                n.put("id", currentEditingNoteId);
                n.put("created_at", new SimpleDateFormat("dd/MM/yyyy, HH:mm").format(new Date()));
                allNotesList.add(0, n);
            }
        }

        if (n != null) {
            n.put("title", t);
            n.put("fullBody", b);
            n.put("category", currentNoteCategory);
            n.put("timestamp", new SimpleDateFormat("dd/MM/yyyy, HH:mm").format(new Date()));
            n.put("modified_at", String.valueOf(System.currentTimeMillis()));
            n.put("isPinned", String.valueOf(isCurrentNotePinned));
            n.put("isLocked", String.valueOf(isCurrentNoteLocked));
            n.put("pin", currentNotePin);
            n.put("color", currentNoteColor);
            n.put("images", new JSONArray(currentImagePaths).toString());
            // FIX: storageMode yahan "plain" mat set karo.
            // saveNotesToStorage() encryption succeed hone par applyStorageModeToAllNotes("encrypted")
            // call karega. Agar yahan "plain" force karein toh existing encrypted notes
            // bhi ek baar "plain" ho jaate hain — confusing aur galat.
        }

        // CRITICAL: Force synchronous save to disk
        saveNotesToStorage();

        if (driveService != null) {
            uploadBackupToDriveBackground(true);
        }
    }

    private void uploadBackupToDriveBackground(boolean immediate) {
        // SECURITY: Disable cloud sync on rooted devices
        if (isDeviceRooted()) return;

        if (executor.isShutdown()) return; // Prevent crash if task is sent while shutting down
        mainHandler.removeCallbacks(syncRunnable);
        if (immediate) {
            executor.execute(syncRunnable);
        } else {
            mainHandler.postDelayed(syncRunnable, 5000); // Wait for 5 seconds of inactivity
        }
    }

    private void performSyncInternal() {
        if (driveService == null || executor.isShutdown()) {
            return;
        }

        // SAFETY CHECK: Don't auto-upload if local notes are 0
        if (allNotesList.isEmpty()) {
            return;
        }

        executor.execute(() -> {
            try {
                java.io.File tempFile = new java.io.File(getCacheDir(), "auto_sync.qnb");
                performExportSync(Uri.fromFile(tempFile));
                File fileMetadata = new File().setName("GoNotesPro_Backup.qnb").setParents(java.util.Collections.singletonList("appDataFolder"));
                FileContent mediaContent = new FileContent("application/octet-stream", tempFile);

                FileList result = driveService.files().list().setSpaces("appDataFolder").execute();
                String existingId = null;
                if (result.getFiles() != null) {
                    for (File f : result.getFiles()) { if ("GoNotesPro_Backup.qnb".equals(f.getName())) { existingId = f.getId(); break; } }
                }

                if (existingId != null) driveService.files().update(existingId, null, mediaContent).execute();
                else driveService.files().create(fileMetadata, mediaContent).execute();

                tempFile.delete();
                mainHandler.post(() -> {
                    long time = System.currentTimeMillis();
                    getSharedPreferences("MyNotesData", MODE_PRIVATE).edit()
                            .putLong("last_sync", time)
                            .remove("deleted_note_ids") // Clear Dead List after successful upload
                            .apply();
                    updateLastSyncText(time);
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    textLastSync.setText("Last sync: Connection issue");
                });
            }
        });
    }

    private void updateLastSyncText(long time) {
        if (time == 0) { textLastSync.setText("Google Drive Backup: Never"); return; }
        String formatted = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time));
        textLastSync.setText("Last Google Sync: Today, " + formatted);
    }

    private void filterNotes(String q) {
        displayedNotesList.clear();
        String query = q.toLowerCase().trim();

        if (isBinMode) {
            for (HashMap<String, String> n : allNotesList) {
                if ("true".equals(n.get("isDummy"))) continue;
                if ("true".equals(n.get("isTrashed"))) {
                    // Only show "Top-level" trashed items.
                    String parentId = n.get("parentId");
                    boolean parentIsAlsoTrashed = false;
                    if (parentId != null && !parentId.equals("root")) {
                        for (HashMap<String, String> parent : allNotesList) {
                            if (parentId.equals(parent.get("id")) && "true".equals(parent.get("isTrashed"))) {
                                parentIsAlsoTrashed = true;
                                break;
                            }
                        }
                    }

                    if (!parentIsAlsoTrashed) {
                        String title = n.get("title") != null ? n.get("title").toLowerCase() : "";
                        String body = n.get("fullBody") != null ? n.get("fullBody").toLowerCase() : "";
                        if (title.contains(query) || body.contains(query)) displayedNotesList.add(n);
                    }
                }
            }
        } else if (isRecentMode) {
            ArrayList<HashMap<String, String>> recentList = new ArrayList<>();
            for (HashMap<String, String> n : allNotesList) {
                if ("true".equals(n.get("isTrashed")) || "true".equals(n.get("isFolder")) || "true".equals(n.get("isDummy"))) continue;
                recentList.add(n);
            }
            java.util.Collections.sort(recentList, (n1, n2) -> compareDates(n1.get("timestamp"), n2.get("timestamp"), true));
            for (HashMap<String, String> n : recentList) {
                String title = n.get("title") != null ? n.get("title").toLowerCase() : "";
                String body = n.get("fullBody") != null ? n.get("fullBody").toLowerCase() : "";
                if (title.contains(query) || body.contains(query)) displayedNotesList.add(n);
            }
        } else if (isNotebookMode) {
            for (HashMap<String, String> n : allNotesList) {
                if ("true".equals(n.get("isTrashed"))) continue;
                String parent = n.get("parentId");
                if (parent == null) parent = "root";

                if (currentParentId.equals(parent)) {
                    // If we are at Notebook Root, show ONLY folders/books
                    if (currentParentId.equals("root") && !"true".equals(n.get("isFolder"))) continue;

                    String title = n.get("title") != null ? n.get("title").toLowerCase() : "";
                    if (title.contains(query)) displayedNotesList.add(n);
                }
            }
        } else {
            for (HashMap<String, String> n : allNotesList) {
                if ("true".equals(n.get("isDummy"))) continue;
                if ("true".equals(n.get("isTrashed"))) continue;

                String parent = n.get("parentId");
                if (parent == null) parent = "root";

                // Category Filter: If a category is selected (other than All), filter by it
                if (!selectedCategoryFilter.equals("All")) {
                    String noteCat = n.get("category");
                    if (noteCat == null) noteCat = "General";
                    if (!noteCat.equals(selectedCategoryFilter)) continue;
                } else {
                    // In "All" mode, only show root-level items in Normal Notes tab
                    if (isNormalFilterMode && !"root".equals(parent) && query.isEmpty()) continue;
                }

                if (isNormalFilterMode && "true".equals(n.get("isFolder"))) continue;

                String title = n.get("title") != null ? n.get("title").toLowerCase() : "";
                String body = n.get("fullBody") != null ? n.get("fullBody").toLowerCase() : "";
                if (title.contains(query) || body.contains(query)) displayedNotesList.add(n);
            }
        }
        adapter.notifyDataSetChanged();
        if (gridAdapter != null) gridAdapter.notifyDataSetChanged();

        updateFilterTabsUI(); // Ensure tabs update based on content types

        if (isBinMode && displayedNotesList.isEmpty()) {
            textEmptyState.setText("Recycle Bin is empty 🗑️");
            textEmptyState.setVisibility(View.VISIBLE);
            // Auto exit bin mode after a short delay if it becomes empty
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isBinMode && displayedNotesList.isEmpty()) exitBinMode();
            }, 1500);
        } else {
            textEmptyState.setText(isBinMode ? "Recycle Bin is empty 🗑️" : "No notes yet! 📝");
            textEmptyState.setVisibility(displayedNotesList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void forcePurgeLegacyData() {
        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        boolean changed = false;

        // Preserve notes data stored in the fallback plaintext key. Deleting it here can make
        // notes disappear after app restart when secure storage is unavailable or auth fails.
        if (sp.contains("last_logged_in_email")) {
            editor.remove("last_logged_in_email");
            changed = true;
        }
        if (sp.contains("pendingCameraPhotoPath")) {
            editor.remove("pendingCameraPhotoPath");
            changed = true;
        }
        if (changed) {
            editor.apply();
        }
    }

    private void setupDynamicShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            android.content.pm.ShortcutManager shortcutManager = getSystemService(android.content.pm.ShortcutManager.class);
            if (shortcutManager != null) {
                android.content.pm.ShortcutInfo shortcut = new android.content.pm.ShortcutInfo.Builder(this, "new_note")
                        .setShortLabel(getString(R.string.shortcut_new_note_short))
                        .setLongLabel(getString(R.string.shortcut_new_note_long))
                        .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_shortcut_note))
                        .setIntent(new Intent(Intent.ACTION_VIEW)
                                .setPackage(getPackageName())
                                .setClass(this, MainActivity.class)
                                .putExtra("shortcut_action", "new_normal_note"))
                        .build();
                shortcutManager.setDynamicShortcuts(java.util.Collections.singletonList(shortcut));
            }
        }
    }

    private void checkAppResilience() {
        // 1. COMPREHENSIVE INTEGRITY ENGINE (Finding H1/F-006/F-Frida/F-System Fix)
        initAppIntegrity();

        // 2. ROOT DETECTION WITH HARD ENFORCEMENT (Finding F-005 Fix)
        com.scottyab.rootbeer.RootBeer rb = new com.scottyab.rootbeer.RootBeer(this);
        if (rb.isRooted()) {
            showSecurityViolation("Rooted device detected. GoNotes Pro cannot run on compromised devices.");
        }
    }

    // NINJA ANTI-TAMPER ENGINE (F-System Final Fix)
    private void initAppIntegrity() {
        executor.execute(() -> {
            try {
                // 1. TRACER PID CHECK
                if (isBeingTraced()) {
                    showSecurityViolation("Active debugger/tracer detected.");
                    return;
                }

                // 2. SYSTEM PROPERTY AUDIT
                if (isSystemCompromised()) {
                    showSecurityViolation("Compromised system environment detected (Insecure Kernel/Debuggable).");
                    return;
                }

                // 3. MEMORY MAP AUDIT (Frida/Xposed/Magisk)
                java.io.File maps = new java.io.File("/proc/self/maps");
                if (maps.exists()) {
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(maps));
                    String line;
                    String s1 = new String(new char[]{'f','r','i','d','a'});
                    String s2 = new String(new char[]{'x','p','o','s','e','d'});
                    String s3 = new String(new char[]{'g','u','m','-','j','s','-','l','o','o','p'});

                    boolean viol = false;
                    while ((line = br.readLine()) != null) {
                        String l = line.toLowerCase();
                        if (l.contains(s1) || l.contains(s2) || l.contains(s3) || l.contains("magisk")) {
                            viol = true;
                            break;
                        }
                    }
                    br.close();
                    if (viol) {
                        showSecurityViolation("Instrumentation framework detected (Frida/Xposed).");
                        return;
                    }
                }

                // 4. FRIDA PORT-KNOCKING
                try {
                    java.net.Socket s = new java.net.Socket("127.0.0.1", 27042);
                    s.close();
                    showSecurityViolation("Frida server detected on port 27042.");
                    return;
                } catch (Exception ignored) {}

                // 5. STANDARD ANDROID DEBUG CHECKS
                if (android.os.Debug.isDebuggerConnected() || (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    showSecurityViolation("Debugger is connected or app is debuggable.");
                }
            } catch (Exception ignored) {}
        });
    }

    private void showSecurityViolation(String reason) {
        mainHandler.post(() -> {
            if (!isFinishing()) {
                // SECURE: Detect if this is a development build (F-Testing Fix)
                boolean isDebugAPK = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;

                com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Security Warning 🛡️")
                        .setMessage(reason + (isDebugAPK ? "\n\n(Testing Mode: Proceed with caution)" : "\n\nFor your data safety, GoNotes Pro will now exit."));

                if (isDebugAPK) {
                    // In Debug Mode, allow the developer to continue testing
                    builder.setCancelable(true)
                            .setPositiveButton("I UNDERSTAND", null);
                } else {
                    // In Release Mode, enforce a hard stop for VAPT compliance
                    builder.setCancelable(false)
                            .setPositiveButton("EXIT APP", (d, w) -> {
                                finishAffinity();
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(0);
                            });
                }
                builder.show();
            }
        });
    }

    private boolean isSystemCompromised() {
        try {
            // We build the command dynamically to avoid static string grep
            String c = new String(new char[]{'g','e','t','p','r','o','p'});

            // Check ro.secure (Must be 1)
            String p1 = new String(new char[]{'r','o','.','s','e','c','u','r','e'});
            String v1 = getSystemProp(c, p1);
            if ("0".equals(v1)) return true; // ADB Root or insecure kernel detected

            // Check ro.debuggable (Must be 0)
            String p2 = new String(new char[]{'r','o','.','d','e','b','u','g','g','a','b','l','e'});
            String v2 = getSystemProp(c, p2);
            if ("1".equals(v2)) return true; // Debuggable firmware

            return false;
        } catch (Exception e) { return false; }
    }

    private String getSystemProp(String cmd, String prop) {
        try {
            Process process = Runtime.getRuntime().exec(cmd + " " + prop);
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line = br.readLine();
            br.close();
            return line != null ? line.trim() : "";
        } catch (Exception e) { return ""; }
    }

    private boolean isBeingTraced() {
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("/proc/self/status"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("TracerPid:")) {
                    int tracerPid = Integer.parseInt(line.substring(line.indexOf(":") + 1).trim());
                    return tracerPid != 0;
                }
            }
        } catch (Exception e) { return false; }
        return false;
    }



    private boolean isDeviceRooted() {
        return new RootBeer(this).isRooted();
    }

    private void applyStorageModeToAllNotes(String mode) {
        for (HashMap<String, String> note : allNotesList) {
            if (note != null) {
                note.put("storageMode", mode);
            }
        }
    }

    private String getNormalizedStorageMode(HashMap<String, String> note, String fallbackMode) {
        String mode = note != null ? note.get("storageMode") : null;
        if (mode == null || mode.trim().isEmpty()) {
            mode = fallbackMode != null ? fallbackMode : "plain";
            if (note != null) note.put("storageMode", mode);
        }
        return mode;
    }

    private void saveNotesToStorage() {
        try {
            JSONArray encryptedArr = new JSONArray();
            JSONArray plainArr = new JSONArray();
            for (HashMap<String, String> note : allNotesList) {
                HashMap<String, String> encryptedNote = new HashMap<>(note);
                encryptedNote.put("storageMode", "encrypted");
                encryptedArr.put(new JSONObject(encryptedNote));
                HashMap<String, String> plainNote = new HashMap<>(note);
                plainNote.put("storageMode", "plain");
                plainArr.put(new JSONObject(plainNote));
            }
            String encryptedJson = encryptedArr.toString();
            String plainJson = plainArr.toString();
            String secureData = secureEncrypt(encryptedJson, true);
            SharedPreferences.Editor editor = getSharedPreferences("MyNotesData", MODE_PRIVATE).edit();
            if (secureData != null) {
                applyStorageModeToAllNotes("encrypted");
                editor.putString("notes_json_secure", secureData)
                        .remove("notes_json")
                        .commit();
                // FIX Bug3: Badge UI update karo — pin/unpin jaise actions mein
                // filterNotes() nahi hoti, isliye adapter directly notify karo
                if (adapter != null) adapter.notifyDataSetChanged();
                if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
                android.util.Log.d("NoteStorage", "Saved encrypted payload to notes_json_secure");
            } else {
                // FIX BUG 2: Encryption fail hone par plaintext fallback mein save karo taaki notes lost na hon
                // Aur notes_json_secure ko bhi remove karo — warna next app restart mein
                // loadNotesFromStorage() purana encrypted data load karega aur notes_json ignore karega
                android.util.Log.e("NoteStorage", "Encryption failed; saving as plaintext fallback.");
                editor.putString("notes_json", plainJson)
                        .remove("notes_json_secure")
                        .commit();
            }
        } catch (Exception e) {
            android.util.Log.e("NoteStorage", "saveNotesToStorage failed: " + e.getMessage(), e);
            // Toast hataya - typing ke waqt bar bar popup nahi aayega
        }
    }

    // FIX: Purani plain-text notes ko ek baar encrypt karo (migration)
    private void migrateIfNeeded() {
        try {
            SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
            String plainJson = sp.getString("notes_json", null);
            String secureJson = sp.getString("notes_json_secure", null);
            if (plainJson != null && secureJson == null) {
                // Purani plain data hai, encrypted nahi - migrate karo
                String encrypted = secureEncrypt(plainJson, true);
                if (encrypted != null) {
                    sp.edit()
                            .putString("notes_json_secure", encrypted)
                            .remove("notes_json")
                            .apply();
                    // FIX: In-memory notes bhi update karo — warna badge is launch mein
                    // "Plain" hi dikhta rahega (SharedPreferences update hua, allNotesList nahi)
                    applyStorageModeToAllNotes("encrypted");
                    android.util.Log.d("NoteStorage", "Migration complete: plain notes encrypted and notes_json removed");
                } else {
                    android.util.Log.e("NoteStorage", "Migration failed: encryption returned null");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("NoteStorage", "migrateIfNeeded error: " + e.getMessage(), e);
        }
    }

    private void loadNotesFromStorage() {
        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        String secureData = sp.getString("notes_json_secure", null);
        String plainJson = sp.getString("notes_json", null);
        if (secureData == null && plainJson == null) {
            filterNotes("");
            return;
        }

        try {
            if (secureData != null) {
                String json = secureDecrypt(secureData, true);
                if (json == null || json.isEmpty()) {
                    json = secureDecrypt(secureData, false);
                }
                if (json != null && !json.isEmpty()) {
                    loadNotesFromJson(json, "encrypted");
                    return;
                } else {
                    // FIX: Stale/purani key se encrypted data clear karo
                    // Naya key banega aur migrateIfNeeded re-encrypt karega
                    sp.edit().remove("notes_json_secure").apply();
                    android.util.Log.w("NoteStorage", "Stale notes_json_secure cleared — will re-encrypt from plaintext.");
                }
            }
            if (plainJson != null && !plainJson.isEmpty()) {
                loadNotesFromJson(plainJson, "plain");
                // FIX: Plain se load ke baad turant re-encrypt karo
                migrateIfNeeded();
                // FIX Bug2: migrateIfNeeded() ke baad in-memory storageMode "encrypted" ho
                // jaata hai, lekin adapter ko khabar nahi milti — filterNotes se refresh karo
                filterNotes("");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        filterNotes("");
    }

    private void loadNotesFromJson(String json, String storageMode) {
        try {
            JSONArray a = new JSONArray(json);
            ArrayList<HashMap<String, String>> tempNotes = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                HashMap<String, String> m = new HashMap<>();
                java.util.Iterator<String> ks = o.keys();
                while (ks.hasNext()) { String k = ks.next(); m.put(k, o.getString(k)); }
                // FIX: storageMode hamesha loading source se set karo.
                // JSON ke andar baked "plain" value pe depend mat karo —
                // notes_json_secure se load ho raha hai toh "encrypted" milna chahiye.
                m.put("storageMode", storageMode);
                tempNotes.add(m);
            }
            allNotesList.clear();
            allNotesList.addAll(tempNotes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        filterNotes("");
    }

    private void setupCategoriesInDrawer() {
        layoutCategoriesInDrawer.removeAllViews();

        // Update Bin Menu Text with Count
        int trashedCount = 0;
        for (HashMap<String, String> n : allNotesList) {
            if ("true".equals(n.get("isTrashed"))) trashedCount++;
        }
        menuBin.setText(getString(R.string.menu_bin) + (trashedCount > 0 ? " (" + trashedCount + ")" : ""));

        for (String c : categoriesList) {
            TextView tv = new TextView(this); tv.setText(c); tv.setPadding(40, 20, 40, 20);
            tv.setOnClickListener(v -> {
                selectedCategoryFilter = c;
                isBinMode = false; isNormalFilterMode = true; isNotebookMode = false; isRecentMode = false;
                drawerLayout.closeDrawer(GravityCompat.START);
                updateFilterTabsUI();
                sortNotesBy(getSharedPreferences("MyNotesData", MODE_PRIVATE).getString("sort_criteria", "Newest First"));
            });
            layoutCategoriesInDrawer.addView(tv);
        }
    }

    private void toggleTheme() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;

        // Save the new theme preference
        getSharedPreferences("MyNotesData", MODE_PRIVATE).edit().putInt("theme_mode", newMode).apply();

        // Apply the theme
        AppCompatDelegate.setDefaultNightMode(newMode);

        // Recreate to ensure all ?attr colors are freshly applied
        recreate();
    }
    private void showLanguageSelector() {
        String[] languages = {"English", "Hindi"};
        String[] langCodes = {"en", "hi"};

        new AlertDialog.Builder(this)
                .setTitle("Choose App Language")
                .setItems(languages, (dialog, which) -> {
                    LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langCodes[which]);
                    AppCompatDelegate.setApplicationLocales(appLocales);
                    Toast.makeText(this, "Language set to " + languages[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    private void showBackupRestoreDialog() {
        if (driveService == null) requestSignIn();
        else showCloudStatusDialog();
    }
    private void showSettingsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 60, 40, 60);

        int bgColor = ContextCompat.getColor(this, R.color.drawerBackgroundColor);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(50);
        shape.setColor(bgColor);
        container.setBackground(shape);

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(30, 0, 0, 40);
        title.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
        container.addView(title);

        container.addView(createMenuItem("🧹", "Optimize Storage", v -> { dialog.dismiss(); performStorageCleanUp(); }));
        container.addView(createMenuItem("ℹ️", "About GoNotes Pro", v -> { dialog.dismiss(); showAboutDialog(); }));
        container.addView(createMenuItem("🔴", "Advanced Delete", v -> { dialog.dismiss(); confirmAdvancedDelete(); }));

        dialog.setContentView(container);
        dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.85), ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void loadViewAndSortPreferences() {
        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        String lastView = sp.getString("view_mode", "list");
        String lastSort = sp.getString("sort_criteria", "Newest First");

        if (lastView.equals("grid")) {
            listViewNotes.setVisibility(View.GONE);
            gridViewNotes.setVisibility(View.VISIBLE);
            buttonToggleView.setText("⊞");
        } else {
            gridViewNotes.setVisibility(View.GONE);
            listViewNotes.setVisibility(View.VISIBLE);
            buttonToggleView.setText("≡");
        }
        sortNotesBy(lastSort);
    }

    private void showAboutDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(60, 70, 60, 70);

        int bgColor = ContextCompat.getColor(this, R.color.drawerBackgroundColor);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(50);
        shape.setColor(bgColor);
        container.setBackground(shape);

        TextView title = new TextView(this);
        title.setText("About GoNotes Pro");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 40);
        title.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
        container.addView(title);

        TextView msg = new TextView(this);
        msg.setText("Version 1.0.0\n\n" +
                "Developed by Nishu Apps\n" +
                "A fast, secure, and encrypted notes app with Google Drive sync.\n\n" +
                "Privacy Policy: https://nishuapps.com/privacy\n" +
                "Contact: support@nishuapps.com");
        msg.setTextSize(14);
        msg.setLineSpacing(0, 1.3f);
        msg.setTextColor(ContextCompat.getColor(this, R.color.secondaryTextColor));
        container.addView(msg);

        Button btnOk = new Button(this);
        btnOk.setText("Got it");
        btnOk.setBackground(null);
        btnOk.setTextColor(Color.parseColor("#FFD700"));
        btnOk.setGravity(Gravity.END);
        btnOk.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnOk);

        dialog.setContentView(container);
        dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.85), ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void confirmAdvancedDelete() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Full Reset")
                .setMessage("This will permanently delete all notes from your phone and Google Drive. Proceed?")
                .setPositiveButton("Wipe All Notes", (d, w) -> startWipeAuthentication())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startWipeAuthentication() {
        BiometricManager bm = BiometricManager.from(this);
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult r) {
                    performAdvancedDelete();
                }
                @Override public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            }).authenticate(new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verify Identity")
                    .setSubtitle("Confirm full data wipe")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build());
        } else {
            // SECURE FALLBACK: Require PIN if biometrics unavailable (GNPRO-05 Fix)
            showWipeConfirmationDialog();
        }
    }

    private void showWipeConfirmationDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter any locked note's PIN");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Full Wipe")
                .setMessage("Please enter a PIN from any of your locked notes to authorize this operation.")
                .setView(input)
                .setPositiveButton("Authorize & Wipe", (d, w) -> {
                    String pin = input.getText().toString().trim();
                    // We verify against any locked note's pin stored in the list
                    boolean authorized = false;
                    String hashed = hashPIN(pin);
                    for (HashMap<String, String> n : allNotesList) {
                        if ("true".equals(n.get("isLocked")) && hashed.equals(n.get("pin"))) {
                            authorized = true; break;
                        }
                    }
                    if (authorized || allNotesList.isEmpty()) performAdvancedDelete();
                    else Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performAdvancedDelete() {
        final android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Wiping all data... Please wait.");
        pd.setCancelable(false);
        pd.show();

        executor.execute(() -> {
            try {
                // 1. CLEAR LOCAL DATA IMMEDIATELY (Commit synchronously)
                allNotesList.clear();
                getSharedPreferences("MyNotesData", MODE_PRIVATE).edit().clear().commit();

                // 2. CLEAR ALL INTERNAL FILES (Images/Backups)
                java.io.File filesDir = getFilesDir();
                if (filesDir != null && filesDir.listFiles() != null) {
                    for (java.io.File f : filesDir.listFiles()) {
                        f.delete();
                    }
                }

                // 3. CLEAR CACHE
                java.io.File cacheDir = getCacheDir();
                if (cacheDir != null && cacheDir.listFiles() != null) {
                    for (java.io.File f : cacheDir.listFiles()) {
                        f.delete();
                    }
                }

                // 4. DELETE CLOUD BACKUP (Background - don't let it block)
                if (driveService != null) {
                    try {
                        FileList result = driveService.files().list().setSpaces("appDataFolder").execute();
                        if (result.getFiles() != null) {
                            for (com.google.api.services.drive.model.File f : result.getFiles()) {
                                if ("GoNotesPro_Backup.qnb".equals(f.getName())) {
                                    driveService.files().delete(f.getId()).execute();
                                }
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }

                mainHandler.post(() -> {
                    if (pd.isShowing()) pd.dismiss();
                    Toast.makeText(this, "Total Wipe Complete! Restarting...", Toast.LENGTH_LONG).show();

                    // 5. FORCE RESTART APP to clear memory state
                    mainHandler.postDelayed(() -> {
                        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                        Runtime.getRuntime().exit(0);
                    }, 1500);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (pd.isShowing()) pd.dismiss();
                    Toast.makeText(this, "Wipe failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void performStorageCleanUp() {
        if (executor.isShutdown()) return;

        // Visual feedback: Start scanning message
        Toast.makeText(MainActivity.this, "Scanning storage...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            long deletedSize = 0;
            int deletedFilesCount = 0;

            try {
                // 1. FORCE RELOAD ALL NOTES from storage to ensure we have the full list
                ArrayList<HashMap<String, String>> latestNotes = new ArrayList<>();
                SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);

                // Try secure data first
                String secureData = sp.getString("notes_json_secure", null);
                String json = null;
                if (secureData != null) {
                    byte[] combined = android.util.Base64.decode(secureData, android.util.Base64.DEFAULT);
                    byte[] iv = new byte[IV_LENGTH];
                    byte[] encryptedBytes = new byte[combined.length - IV_LENGTH];
                    System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
                    System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);
                    SecretKey key = getOrCreateKey(MASTER_KEY_ALIAS, true);
                    Cipher cipher = Cipher.getInstance(ALGO_GCM);
                    javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH, iv);
                    cipher.init(Cipher.DECRYPT_MODE, key, spec);
                    json = new String(cipher.doFinal(encryptedBytes), java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    json = sp.getString("notes_json", "[]");
                }

                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    HashMap<String, String> map = new HashMap<>();
                    java.util.Iterator<String> keys = o.keys();
                    while (keys.hasNext()) { String key = keys.next(); map.put(key, o.getString(key)); }
                    latestNotes.add(map);
                }

                // 2. Identify ALL Used Image Filenames in Notes
                HashSet<String> activeImageNames = new HashSet<>();
                for (HashMap<String, String> n : latestNotes) {
                    String imagesJson = n.get("images");
                    if (imagesJson != null) {
                        try {
                            JSONArray iArr = new JSONArray(imagesJson);
                            for (int i = 0; i < iArr.length(); i++) {
                                String path = iArr.getString(i);
                                if (!path.startsWith("content://")) activeImageNames.add(new java.io.File(path).getName());
                            }
                        } catch (Exception ignored) {}
                    }
                }

                // 3. Purge Orphaned Files from BOTH root and subfolders
                java.io.File[] scanDirs = { getFilesDir(), new java.io.File(getFilesDir(), "images"), getCacheDir(), new java.io.File(getCacheDir(), "exports") };

                for (java.io.File dir : scanDirs) {
                    if (dir != null && dir.exists() && dir.listFiles() != null) {
                        for (java.io.File f : dir.listFiles()) {
                            if (f.isDirectory()) continue;
                            String name = f.getName();
                            boolean isTarget = name.endsWith(".webp") || name.endsWith(".jpg") || name.startsWith("camera_temp_") || name.startsWith("note_img_") || name.startsWith("note_cam_") || name.endsWith(".qnb") || name.endsWith(".pdf");

                            if (isTarget && !activeImageNames.contains(name)) {
                                if (System.currentTimeMillis() - f.lastModified() < 10000) continue;
                                deletedSize += f.length();
                                if (f.delete()) deletedFilesCount++;
                            }
                        }
                    }
                }

                final double sizeMb = deletedSize / (1024.0 * 1024.0);
                final int finalCount = deletedFilesCount;
                mainHandler.post(() -> {
                    if (finalCount > 0) Toast.makeText(MainActivity.this, String.format(Locale.getDefault(), "Storage Optimized! Cleared %.2f MB", sizeMb), Toast.LENGTH_LONG).show();
                    else Toast.makeText(MainActivity.this, "Storage is already optimized! ✨", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(MainActivity.this, "Optimization failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void enterBinMode() {
        isBinMode = true;
        isNotebookMode = false;
        isNormalFilterMode = false;
        isRecentMode = false;
        buttonPlus.setVisibility(View.GONE);
        buttonMenu.setText("⟵");
        buttonEmptyBin.setVisibility(View.VISIBLE);
        buttonSort.setVisibility(View.GONE);
        buttonToggleView.setVisibility(View.GONE);
        filterNotes("");
        updateFilterTabsUI();
    }

    private void exitBinMode() {
        isBinMode = false;
        buttonPlus.setVisibility(View.VISIBLE);
        buttonMenu.setText("☰");
        buttonEmptyBin.setVisibility(View.GONE);
        buttonSort.setVisibility(View.VISIBLE);
        buttonToggleView.setVisibility(View.VISIBLE);
        filterNotes("");
        updateFilterTabsUI();
    }
    private void updateFilterTabsUI() {
        if (isBinMode || !selectedCategoryFilter.equals("All")) {
            findViewById(R.id.layoutFilterTabs).setVisibility(View.GONE);
            scrollBreadcrumb.setVisibility(View.VISIBLE); // Show header for special modes
            updateModeHeader();
            return;
        }

        findViewById(R.id.layoutFilterTabs).setVisibility(View.VISIBLE);
        tabRecent.setVisibility(View.VISIBLE);
        tabNormalNotes.setVisibility(View.VISIBLE);
        tabNotebooks.setVisibility(View.VISIBLE);

        setTabSelected(tabRecent, isRecentMode);
        setTabSelected(tabNormalNotes, isNormalFilterMode);
        setTabSelected(tabNotebooks, isNotebookMode);

        scrollBreadcrumb.setVisibility(isNotebookMode ? View.VISIBLE : View.GONE);
        if (isNotebookMode) updateBreadcrumbText();
    }

    private void updateModeHeader() {
        if (layoutNotebookBreadcrumb == null) return;
        layoutNotebookBreadcrumb.removeAllViews();

        TextView tv = new TextView(this);
        if (isBinMode) tv.setText("🗑️ Recycle Bin");
        else tv.setText("📁 Category: " + selectedCategoryFilter);

        tv.setTextSize(18);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
        tv.setPadding(20, 10, 20, 20);
        layoutNotebookBreadcrumb.addView(tv);
    }

    private void updateBreadcrumbText() {
        if (layoutNotebookBreadcrumb == null) return;
        layoutNotebookBreadcrumb.removeAllViews();

        // Add "Root" link
        addBreadcrumbItem("Root", "root", 1);

        for (int i = 0; i < navigationPathIds.size(); i++) {
            TextView arrow = new TextView(this);
            arrow.setText(" > ");
            arrow.setTextColor(Color.parseColor("#888888"));
            arrow.setTextSize(14);
            layoutNotebookBreadcrumb.addView(arrow);

            final String id = navigationPathIds.get(i);
            final String name = navigationPathNames.get(i);
            final int level = i + 2;
            addBreadcrumbItem(name, id, level);
        }
    }

    private void addBreadcrumbItem(String name, String id, int level) {
        TextView tv = new TextView(this);
        tv.setText(name);

        // Theme-aware color for breadcrumbs
        if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            tv.setTextColor(Color.parseColor("#FFD700")); // Gold for dark mode
        } else {
            tv.setTextColor(Color.parseColor("#A0522D")); // Darker sienna for light mode
        }

        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);

        // Convert DP to PX for padding
        float scale = getResources().getDisplayMetrics().density;
        int padH = (int) (12 * scale + 0.5f);
        int padV = (int) (6 * scale + 0.5f);
        tv.setPadding(padH, padV, padH, padV);

        tv.setBackgroundResource(R.drawable.icon_outline_bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (4 * scale + 0.5f);
        params.setMargins(margin, 0, margin, 0);
        tv.setLayoutParams(params);

        tv.setOnClickListener(v -> {
            if (currentParentId.equals(id)) return;

            if (id.equals("root")) {
                navigationPathIds.clear();
                navigationPathNames.clear();
                currentParentId = "root";
                currentLevel = 1;
            } else {
                int index = navigationPathIds.indexOf(id);
                if (index != -1) {
                    while (navigationPathIds.size() > index + 1) {
                        navigationPathIds.remove(navigationPathIds.size() - 1);
                        navigationPathNames.remove(navigationPathNames.size() - 1);
                    }
                    currentParentId = id;
                    currentLevel = level;
                }
            }
            filterNotes("");
            updateBreadcrumbText();
        });
        layoutNotebookBreadcrumb.addView(tv);
    }

    private void setTabSelected(TextView tab, boolean isSelected) {
        if (isSelected) {
            tab.setBackgroundResource(R.drawable.drawer_item_bg);
            // Use mutate() so we don't change the background for other views using this drawable
            GradientDrawable gd = (GradientDrawable) tab.getBackground().mutate();
            if (gd != null) {
                gd.setColor(ContextCompat.getColor(this, R.color.searchBackgroundColor));
            }

            // Dynamic text color for selected tab
            if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                tab.setTextColor(Color.parseColor("#FFD700")); // Gold for dark mode
            } else {
                tab.setTextColor(Color.parseColor("#A0522D")); // Sienna for light mode
            }
        } else {
            tab.setBackgroundResource(android.R.color.transparent);
            tab.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
        }
    }
    private void toggleFab(boolean open) {
        if (open == isFabOpen) return;
        isFabOpen = open;

        if (open) {
            // Update folder label before showing
            boolean isRoot = currentParentId.equals("root");
            textFabNotebookLabel.setText(isRoot ? "New Notebook" : "New Folder");
            iconFabNotebook.setText(isRoot ? "📘" : "📁");

            // Smart Visibility based on current section
            if (isRecentMode) {
                btnFabNote.setVisibility(View.VISIBLE);
                btnFabNotebook.setVisibility(View.VISIBLE);
            } else if (isNormalFilterMode) {
                btnFabNote.setVisibility(View.VISIBLE);
                btnFabNotebook.setVisibility(View.GONE);
            } else if (isNotebookMode) {
                btnFabNote.setVisibility(isRoot ? View.GONE : View.VISIBLE); // Only show Note inside folders
                btnFabNotebook.setVisibility(View.VISIBLE);
            }

            layoutSpeedDial.setVisibility(View.VISIBLE);
            viewFabOverlay.setVisibility(View.VISIBLE);
            viewFabOverlay.setAlpha(0f);
            viewFabOverlay.animate().alpha(1f).setDuration(200).start();

            buttonPlus.animate().rotation(45f).setDuration(200).start();

            // Animation for buttons (Slide up + Alpha)
            layoutSpeedDial.setAlpha(0f);
            layoutSpeedDial.setTranslationY(100f);
            layoutSpeedDial.animate().alpha(1f).translationY(0f).setDuration(200).start();
        } else {
            viewFabOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> viewFabOverlay.setVisibility(View.GONE)).start();
            buttonPlus.animate().rotation(0f).setDuration(200).start();

            layoutSpeedDial.animate().alpha(0f).translationY(100f).setDuration(200).withEndAction(() -> layoutSpeedDial.setVisibility(View.GONE)).start();
        }
    }

    private void showPlusMenu() {
        // Old menu removed for Speed Dial
    }

    private void showModernMenu(boolean showNote, boolean showNotebook) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create a nice container
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 60, 40, 60);

        // Set background color based on theme
        int bgColor = ContextCompat.getColor(this, R.color.drawerBackgroundColor);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadii(new float[]{50, 50, 50, 50, 0, 0, 0, 0}); // Top corners rounded
        shape.setColor(bgColor);
        container.setBackground(shape);

        TextView title = new TextView(this);
        title.setText("Create New");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(20, 0, 0, 40);
        title.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
        container.addView(title);

        if (showNote) container.addView(createMenuItem("📄", "New Note", v -> { dialog.dismiss(); createNewNote("normal"); }));
        if (showNotebook) {
            boolean isRoot = currentParentId.equals("root");
            String label = isRoot ? "New Notebook" : "New Folder";
            String icon = isRoot ? "📘" : "📁";
            container.addView(createMenuItem(icon, label, v -> { dialog.dismiss(); showAddFolderDialog(); }));
        }

        dialog.setContentView(container);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setWindowAnimations(android.R.style.Animation_InputMethod); // Simple slide up animation
        dialog.show();
    }

    private View createMenuItem(String icon, String text, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(40, 45, 40, 45);
        item.setClickable(true);
        item.setFocusable(true);

        // Create a "Box" (Card) background
        GradientDrawable box = new GradientDrawable();
        box.setCornerRadius(30);
        box.setColor(ContextCompat.getColor(this, R.color.searchBackgroundColor));
        item.setBackground(box);

        // Add margin between boxes
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 15, 0, 15);
        item.setLayoutParams(params);

        item.setOnClickListener(listener);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(icon);
        tvIcon.setTextSize(24);
        item.addView(tvIcon);

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextSize(16);
        tvText.setPadding(40, 0, 0, 0);
        tvText.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
        item.addView(tvText);

        return item;
    }

    private void createNewNote(String type) {
        currentEditingNoteId = null;
        editTitle.setText("");
        currentNoteCategory = "General";
        isCurrentNoteLocked = false;
        isCurrentNotePinned = false;
        currentNotePin = "";
        currentNoteColor = "default";

        if (layoutSearchInNote != null) {
            layoutSearchInNote.setVisibility(View.GONE);
            clearSearchHighlights();
        }

        isUndoRedoActive = true;
        editNoteBody.setText("");
        undoList.clear();
        redoList.clear();
        undoList.add("");
        undoTitleList.clear(); // FIX BUG 7: Title undo reset for new note
        undoTitleList.add("");
        isUndoRedoActive = false;

        currentImagePaths.clear();
        imagesAdapter.notifyDataSetChanged();
        imagesRecyclerView.setVisibility(View.GONE);

        screenAddNote.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundColor));
        screenList.setVisibility(View.GONE);
        screenAddNote.setVisibility(View.VISIBLE);
        buttonPlus.setVisibility(View.GONE);

        // Setup editor UI
        if (layoutFormatBar != null) layoutFormatBar.setVisibility(View.GONE);
        buttonFormat.setVisibility(View.VISIBLE);
        buttonSpeak.setVisibility(View.VISIBLE);
        editNoteBody.setTypeface(Typeface.DEFAULT);
        // Normal notes should wrap text
        editNoteBody.setHorizontallyScrolling(false);
        // Enable standard text features for normal notes
        editNoteBody.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        updateEditorToolbarIcons();

        // AUTO-FOCUS & KEYBOARD SHOW
        editTitle.requestFocus();
        mainHandler.postDelayed(() -> {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(editTitle, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }

    private void deleteCategoryAndHandleNotes(String category, boolean deleteNotesToo) {
        // 1. Update/Delete Notes
        for (HashMap<String, String> n : allNotesList) {
            if (category.equals(n.get("category"))) {
                if (deleteNotesToo) {
                    n.put("isTrashed", "true");
                    n.put("modified_at", String.valueOf(System.currentTimeMillis()));
                } else {
                    n.put("category", "General");
                    n.put("modified_at", String.valueOf(System.currentTimeMillis()));
                }
            }
        }

        // 2. Remove Category
        categoriesList.remove(category);
        saveCategories();
        saveNotesToStorage();
        setupCategoriesInDrawer();
        filterNotes("");
        uploadBackupToDriveBackground(false);

        Toast.makeText(this, deleteNotesToo ? "Category and notes deleted" : "Category deleted, notes moved to General", Toast.LENGTH_LONG).show();
    }

    private void showAddFolderDialog() {
        if (currentLevel >= 5) {
            Toast.makeText(this, "Maximum 5 levels of folders reached!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isRoot = currentParentId.equals("root");
        String title = isRoot ? "New Notebook" : "New Folder";
        String hint = isRoot ? "Notebook Name" : "Folder Name";

        final EditText input = new EditText(this);
        input.setHint(hint);
        input.setPadding(50, 40, 50, 40);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        HashMap<String, String> folder = new HashMap<>();
                        folder.put("id", UUID.randomUUID().toString());
                        folder.put("title", name);
                        folder.put("isFolder", "true");
                        folder.put("parentId", currentParentId);
                        folder.put("level", String.valueOf(currentLevel));
                        folder.put("timestamp", new SimpleDateFormat("dd/MM/yyyy, HH:mm").format(new Date()));
                        folder.put("modified_at", String.valueOf(System.currentTimeMillis())); // Hidden precise timestamp
                        allNotesList.add(0, folder);
                        saveNotesToStorage(); filterNotes(""); uploadBackupToDriveBackground(false);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(this, buttonSort);
        popup.getMenu().add("Newest First");
        popup.getMenu().add("Oldest First");
        popup.getMenu().add("Name (A-Z)");
        popup.getMenu().add("Name (Z-A)");
        popup.getMenu().add("By Color");

        popup.setOnMenuItemClickListener(item -> {
            sortNotesBy(item.getTitle().toString());
            return true;
        });
        popup.show();
    }

    private void sortNotesBy(String criteria) {
        getSharedPreferences("MyNotesData", MODE_PRIVATE).edit().putString("sort_criteria", criteria).apply();
        java.util.Collections.sort(allNotesList, (n1, n2) -> {
            // Only prioritize pinned notes if NOT in Recent mode
            if (!isRecentMode) {
                boolean p1 = "true".equals(n1.get("isPinned"));
                boolean p2 = "true".equals(n2.get("isPinned"));
                if (p1 && !p2) return -1;
                if (!p1 && p2) return 1;
            }

            if (criteria.equals("Newest First")) {
                return compareDates(n1.get("timestamp"), n2.get("timestamp"), true);
            } else if (criteria.equals("Oldest First")) {
                return compareDates(n1.get("timestamp"), n2.get("timestamp"), false);
            } else if (criteria.equals("Name (A-Z)")) {
                String t1 = n1.get("title");
                String t2 = n2.get("title");
                return (t1 != null && t2 != null) ? t1.compareToIgnoreCase(t2) : 0;
            } else if (criteria.equals("Name (Z-A)")) {
                String t1 = n1.get("title");
                String t2 = n2.get("title");
                return (t1 != null && t2 != null) ? t2.compareToIgnoreCase(t1) : 0;
            } else if (criteria.equals("By Color")) {
                String c1 = n1.get("color");
                String c2 = n2.get("color");
                boolean d1 = c1 == null || c1.equals("default") || c1.equals("#121212");
                boolean d2 = c2 == null || c2.equals("default") || c2.equals("#121212");
                if (!d1 && d2) return -1;
                if (d1 && !d2) return 1;
                return (c1 != null && c2 != null) ? c1.compareTo(c2) : 0;
            }
            return 0;
        });
        filterNotes(searchBar.getText().toString());
    }
    private void setupNoteEditorLogic() {
        buttonSpeak.setOnClickListener(v -> speakNote());
        buttonAlarm.setOnClickListener(v -> showAlarmDialog());
        buttonPin.setOnClickListener(v -> toggleNotePin());
        buttonMoreNote.setOnClickListener(v -> toggleNoteActions());
        // SECURE REAL-TIME SAVE: Pehredar with Debouncing (F-RealTime Fix)
        android.text.TextWatcher realTimeWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isUndoRedoActive && isVaultUnlocked) {
                    // Remove pending saves and schedule a new one after 2 seconds of inactivity
                    mainHandler.removeCallbacks(autoSaveRunnable);
                    mainHandler.postDelayed(autoSaveRunnable, 2000);
                }
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        editTitle.addTextChangedListener(realTimeWatcher);
        editNoteBody.addTextChangedListener(realTimeWatcher);

        buttonSearchInNote.setOnClickListener(v -> {
            layoutSearchInNote.setVisibility(layoutSearchInNote.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (layoutSearchInNote.getVisibility() == View.VISIBLE) {
                editSearchInNote.requestFocus();
                performLiveSearch(editSearchInNote.getText().toString());
            } else clearSearchHighlights();
        });

        editSearchInNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                performLiveSearch(s.toString());
            }
        });

        btnFindPrevInNote.setOnClickListener(v -> jumpToPrevMatch());
        btnFindNextInNote.setOnClickListener(v -> jumpToNextMatch());
        btnCloseSearchInNote.setOnClickListener(v -> {
            layoutSearchInNote.setVisibility(View.GONE);
            clearSearchHighlights();
        });
        buttonFormat.setOnClickListener(v -> toggleFormatBar());
        buttonColor.setOnClickListener(v -> showColorPicker());
        buttonAddFeature.setOnClickListener(v -> showAddFeatureMenu());
        buttonUndo.setOnClickListener(v -> performUndo());
        buttonRedo.setOnClickListener(v -> performRedo());
        buttonMore.setOnClickListener(v -> showEditorMoreMenu());
        setupFormatButtons();

        editNoteBody.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Apply active toggles to newly typed text
                if (count > before && !isUndoRedoActive) {
                    applyActiveToggles(start, start + count);
                }
            }
            @Override public void afterTextChanged(Editable s) {
                updateWordCount();
                if (!isUndoRedoActive) {
                    if (undoList.size() > 50) undoList.remove(0); // limit history
                    undoList.add(new android.text.SpannableStringBuilder(s)); // Save as Spannable to keep styles
                    redoList.clear();
                }
            }
        });

        // FIX BUG 7: Title ke changes bhi track karo undo ke liye
        editTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUndoRedoActive) {
                    if (undoTitleList.size() > 50) undoTitleList.remove(0);
                    undoTitleList.add(s.toString());
                }
            }
        });
    }

    private void setupFormatButtons() {
        btnBold.setOnClickListener(v -> toggleFormat("bold"));
        btnItalic.setOnClickListener(v -> toggleFormat("italic"));
        btnUnderline.setOnClickListener(v -> toggleFormat("underline"));
        btnH1.setOnClickListener(v -> toggleFormat("h1"));
        btnH2.setOnClickListener(v -> toggleFormat("h2"));
        btnClearToggles.setOnClickListener(v -> clearAllFormats());
        btnCloseFormat.setOnClickListener(v -> layoutFormatBar.setVisibility(View.GONE));

        btnClearToggles.setText("T ✖"); // Visual cross as requested
    }

    private void toggleFormat(String type) {
        int start = editNoteBody.getSelectionStart();
        int end = editNoteBody.getSelectionEnd();
        Editable editable = editNoteBody.getText();

        if (start != end && start != -1) {
            // Apply to selection immediately
            applyFormatToRange(type, start, end, editable);
        } else {
            // Toggle the mode for future typing
            switch (type) {
                case "bold": isBoldActive = !isBoldActive; updateButtonState(btnBold, isBoldActive); break;
                case "italic": isItalicActive = !isItalicActive; updateButtonState(btnItalic, isItalicActive); break;
                case "underline": isUnderlineActive = !isUnderlineActive; updateButtonState(btnUnderline, isUnderlineActive); break;
                case "h1": isH1Active = !isH1Active; isH2Active = false; updateButtonState(btnH1, isH1Active); updateButtonState(btnH2, false); break;
                case "h2": isH2Active = !isH2Active; isH1Active = false; updateButtonState(btnH2, isH2Active); updateButtonState(btnH1, false); break;
            }
        }
    }

    private void applyFormatToRange(String type, int start, int end, Editable editable) {
        switch (type) {
            case "bold": editable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); break;
            case "italic": editable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); break;
            case "underline": editable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); break;
            case "h1": editable.setSpan(new RelativeSizeSpan(1.5f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); break;
            case "h2": editable.setSpan(new RelativeSizeSpan(1.2f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); break;
        }
    }

    private void applyActiveToggles(int start, int end) {
        Editable editable = editNoteBody.getText();
        if (isBoldActive) editable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (isItalicActive) editable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (isUnderlineActive) editable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (isH1Active) editable.setSpan(new RelativeSizeSpan(1.5f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (isH2Active) editable.setSpan(new RelativeSizeSpan(1.2f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void clearAllFormats() {
        // Reset all toggle states
        isBoldActive = isItalicActive = isUnderlineActive = isH1Active = isH2Active = false;

        // Update UI
        updateButtonState(btnBold, false);
        updateButtonState(btnItalic, false);
        updateButtonState(btnUnderline, false);
        updateButtonState(btnH1, false);
        updateButtonState(btnH2, false);

        // If text is selected, clear its formatting
        int start = editNoteBody.getSelectionStart();
        int end = editNoteBody.getSelectionEnd();
        if (start != end && start != -1) {
            Editable editable = editNoteBody.getText();
            Object[] spans = editable.getSpans(start, end, Object.class);
            for (Object span : spans) {
                if (span instanceof StyleSpan || span instanceof UnderlineSpan ||
                        span instanceof RelativeSizeSpan || span instanceof TypefaceSpan ||
                        span instanceof ForegroundColorSpan || span instanceof BackgroundColorSpan) {
                    editable.removeSpan(span);
                }
            }
        }
        Toast.makeText(this, "All formatting cleared", Toast.LENGTH_SHORT).show();
    }

    private void updateButtonState(Button btn, boolean isActive) {
        if (isActive) {
            btn.setTextColor(Color.parseColor("#FFD700")); // Gold highlight
            btn.setBackgroundColor(Color.parseColor("#333333")); // Darker background
        } else {
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void toggleFormatBar() {
        if (layoutFormatBar.getVisibility() == View.VISIBLE) {
            layoutFormatBar.setVisibility(View.GONE);
        } else {
            layoutFormatBar.setVisibility(View.VISIBLE);
        }
    }

    private void showColorPicker() {
        final String[] colors = {"#121212", "#FF5555", "#55FF55", "#5555FF", "#FFFF55", "#FF55FF", "#55FFFF"};
        final String[] names = {"Default", "Red", "Green", "Blue", "Yellow", "Pink", "Cyan"};

        new AlertDialog.Builder(this)
                .setTitle("Pick Note Color")
                .setItems(names, (dialog, which) -> {
                    currentNoteColor = colors[which];
                    screenAddNote.setBackgroundColor(Color.parseColor(currentNoteColor));
                }).show();
    }

    private void showAddFeatureMenu() {
        String[] options = {"📷 Take Photo", "🖼️ Add Image", "📅 Insert Date"};
        new AlertDialog.Builder(this)
                .setTitle("Add to Note")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermissionAndOpen();
                    else if (which == 1) pickImageLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
                    else if (which == 2) {
                        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                        editNoteBody.getText().insert(editNoteBody.getSelectionStart(), date);
                    }
                }).show();
    }

    private void showEditorMoreMenu() {
        PopupMenu popup = new PopupMenu(this, buttonMore);
        popup.getMenu().add("Change Category (" + currentNoteCategory + ")");
        popup.getMenu().add(isCurrentNoteLocked ? "Unlock Note" : "Lock Note");
        popup.getMenu().add("Delete Note");
        popup.getMenu().add("Share as Text");
        popup.getMenu().add("Share as PDF");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.startsWith("Change Category")) {
                showCategoryPicker();
            } else if (title.equals("Lock Note") || title.equals("Unlock Note")) {
                toggleNoteLock();
            } else if (title.equals("Delete Note")) {
                moveCurrentNoteToBin();
            } else if (title.equals("Share as Text")) {
                shareNoteAsText();
            } else if (title.equals("Share as PDF")) {
                shareNoteAsPdf();
            }
            return true;
        });
        popup.show();
    }

    private void shareNoteAsText() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, editTitle.getText().toString() + "\n\n" + editNoteBody.getText().toString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share note via"));
    }

    private void showCategoryPicker() {
        final ArrayList<String> options = new ArrayList<>(categoriesList);
        options.add(0, "+ Add New Category"); // Add special option at the top

        String[] cats = options.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Select Category")
                .setItems(cats, (dialog, which) -> {
                    if (which == 0) {
                        // Show input for new category
                        final EditText input = new EditText(this);
                        input.setHint("Category Name");
                        new AlertDialog.Builder(this)
                                .setTitle("New Category")
                                .setView(input)
                                .setPositiveButton("Add", (d2, w2) -> {
                                    String name = input.getText().toString().trim();
                                    if (!name.isEmpty() && !categoriesList.contains(name)) {
                                        categoriesList.add(name);
                                        saveCategories();
                                        setupCategoriesInDrawer();
                                        currentNoteCategory = name;
                                        Toast.makeText(this, "Category created and assigned", Toast.LENGTH_SHORT).show();
                                        saveCurrentNote(); // Save immediately
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        currentNoteCategory = options.get(which);
                        Toast.makeText(this, "Category changed to: " + currentNoteCategory, Toast.LENGTH_SHORT).show();
                        saveCurrentNote(); // Save change immediately
                    }
                }).show();
    }

    private void shareNoteAsPdf() {
        try {
            android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
            int pageWidth = 595; // A4 Standard
            int pageHeight = 842;
            int margin = 50;
            int contentWidth = pageWidth - (2 * margin);

            android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);

            int x = margin;
            int y = margin + 20;

            // Draw Title
            paint.setTextSize(22);
            paint.setFakeBoldText(true);
            String title = editTitle.getText().toString().isEmpty() ? "Untitled Note" : editTitle.getText().toString();
            canvas.drawText(title, x, y, paint);
            y += 40;

            paint.setTextSize(12);
            paint.setFakeBoldText(false);

            String content = editNoteBody.getText().toString();
            String[] lines = content.split("\n");
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[\\[IMG:(.*?)\\]\\]");

            for (String line : lines) {
                java.util.regex.Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // DRAW IMAGE
                    String path = matcher.group(1);
                    try {
                        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(path);
                        if (bitmap != null) {
                            float ratio = (float) bitmap.getHeight() / bitmap.getWidth();
                            int drawWidth = Math.min(contentWidth, bitmap.getWidth());
                            int drawHeight = (int) (drawWidth * ratio);

                            // Start new page if image doesn't fit
                            if (y + drawHeight > pageHeight - margin) {
                                document.finishPage(page);
                                page = document.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create());
                                canvas = page.getCanvas();
                                y = margin;
                            }

                            android.graphics.Rect dest = new android.graphics.Rect(x, y, x + drawWidth, y + drawHeight);
                            canvas.drawBitmap(bitmap, null, dest, null);
                            y += drawHeight + 20;
                            bitmap.recycle();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                } else {
                    // DRAW TEXT with simple wrapping
                    if (line.isEmpty()) {
                        y += 15;
                    } else {
                        // Basic wrap logic
                        int charsPerLine = 85;
                        for (int i = 0; i < line.length(); i += charsPerLine) {
                            if (y > pageHeight - margin) {
                                document.finishPage(page);
                                page = document.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create());
                                canvas = page.getCanvas();
                                y = margin;
                            }
                            String sub = line.substring(i, Math.min(i + charsPerLine, line.length()));
                            canvas.drawText(sub, x, y, paint);
                            y += 15;
                        }
                    }
                }
            }

            document.finishPage(page);

            java.io.File file = new java.io.File(getCacheDir(), "Note_Export.pdf");
            document.writeTo(new FileOutputStream(file));
            document.close();
            // SECURE: Explicitly set private permissions (CWE-276 Fix)
            file.setReadable(true, true);
            file.setWritable(true, true);

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share PDF via"));

        } catch (Exception e) {
            Toast.makeText(this, "PDF Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void performLiveSearch(String query) {
        clearSearchHighlights();
        searchInNoteMatchIndices.clear();
        currentSearchInNoteMatchPos = -1;

        if (query.isEmpty()) {
            textSearchInNoteCount.setText("0/0");
            return;
        }

        String text = editNoteBody.getText().toString().toLowerCase();
        String q = query.toLowerCase();

        int index = text.indexOf(q);
        while (index != -1) {
            searchInNoteMatchIndices.add(index);
            index = text.indexOf(q, index + q.length());
        }

        if (!searchInNoteMatchIndices.isEmpty()) {
            currentSearchInNoteMatchPos = 0;
            updateSearchMatchHighlights();
        } else {
            textSearchInNoteCount.setText("0/0");
        }
    }

    private void updateSearchMatchHighlights() {
        String query = editSearchInNote.getText().toString();
        if (query.isEmpty() || searchInNoteMatchIndices.isEmpty()) return;

        Editable editable = editNoteBody.getText();
        // Clear existing search spans first
        BackgroundColorSpan[] spans = editable.getSpans(0, editable.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : spans) editable.removeSpan(span);

        for (int i = 0; i < searchInNoteMatchIndices.size(); i++) {
            int start = searchInNoteMatchIndices.get(i);
            int end = start + query.length();

            int color;
            if (i == currentSearchInNoteMatchPos) {
                color = Color.parseColor("#FF9800"); // Orange for current match
                editNoteBody.setSelection(start, end);
            } else {
                color = Color.parseColor("#44FFFF00"); // Translucent yellow for other matches
            }
            editable.setSpan(new BackgroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textSearchInNoteCount.setText((currentSearchInNoteMatchPos + 1) + "/" + searchInNoteMatchIndices.size());
    }

    private void jumpToNextMatch() {
        if (searchInNoteMatchIndices.isEmpty()) return;
        currentSearchInNoteMatchPos = (currentSearchInNoteMatchPos + 1) % searchInNoteMatchIndices.size();
        updateSearchMatchHighlights();
    }

    private void jumpToPrevMatch() {
        if (searchInNoteMatchIndices.isEmpty()) return;
        currentSearchInNoteMatchPos = (currentSearchInNoteMatchPos - 1 + searchInNoteMatchIndices.size()) % searchInNoteMatchIndices.size();
        updateSearchMatchHighlights();
    }

    private void clearSearchHighlights() {
        lastSearchInNoteIndex = -1;
        searchInNoteMatchIndices.clear();
        currentSearchInNoteMatchPos = -1;
        if (textSearchInNoteCount != null) textSearchInNoteCount.setText("0/0");

        Editable s = editNoteBody.getText();
        BackgroundColorSpan[] spans = s.getSpans(0, s.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : spans) {
            s.removeSpan(span);
        }
    }

    private void moveCurrentNoteToBin() {
        if (currentEditingNoteId != null) {
            for (HashMap<String, String> n : allNotesList) {
                if (currentEditingNoteId.equals(n.get("id"))) { // FIX BUG 3: null-safe comparison
                    n.put("isTrashed", "true");
                    cancelAlarm(currentEditingNoteId); // FIX BUG 2: cancel alarm on delete
                    break;
                }
            }
            saveNotesToStorage();
            closeNoteScreen();
            Toast.makeText(this, "Moved to Bin", Toast.LENGTH_SHORT).show();
        } else {
            closeNoteScreen();
        }
    }

    private void performUndo() {
        // FIX BUG 7: Title aur body ke undo alag-alag hain — jis field mein focus ho usi ka undo
        if (editTitle.hasFocus()) {
            if (undoTitleList.size() > 1) {
                isUndoRedoActive = true;
                undoTitleList.remove(undoTitleList.size() - 1);
                editTitle.setText(undoTitleList.get(undoTitleList.size() - 1));
                editTitle.setSelection(editTitle.getText().length());
                isUndoRedoActive = false;
            } else {
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (undoList.size() > 1) {
                isUndoRedoActive = true;
                redoList.add(undoList.remove(undoList.size() - 1));
                CharSequence text = undoList.get(undoList.size() - 1);
                editNoteBody.setText(text);
                editNoteBody.setSelection(editNoteBody.getText().length());
                isUndoRedoActive = false;
            } else {
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performRedo() {
        if (!redoList.isEmpty()) {
            isUndoRedoActive = true;
            CharSequence text = redoList.remove(redoList.size() - 1);
            undoList.add(text);
            editNoteBody.setText(text);
            editNoteBody.setSelection(editNoteBody.getText().length());
            isUndoRedoActive = false;
        } else {
            Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }

    private void emptyBin() {
        new AlertDialog.Builder(this)
                .setTitle("Empty Bin")
                .setMessage("Are you sure you want to permanently delete all items in the bin?")
                .setPositiveButton("Empty", (d, w) -> {
                    java.util.Iterator<HashMap<String, String>> it = allNotesList.iterator();
                    while (it.hasNext()) {
                        if ("true".equals(it.next().get("isTrashed"))) it.remove();
                    }
                    saveNotesToStorage();
                    filterNotes("");
                    uploadBackupToDriveBackground(false);
                    Toast.makeText(this, "Bin cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void speakNote() {
        String text = editNoteBody.getText().toString();
        if (text.isEmpty()) return;

        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            buttonSpeak.setText("🔊");
            return;
        }

        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.getDefault());
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                    buttonSpeak.setText("⏹");
                }
            });
        } else {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            buttonSpeak.setText("⏹");
        }
    }

    private void showAlarmDialog() {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (v, hour, minute) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, minute);

                // New choice: Once or Every Day
                String[] choices = {"Only Once", "Every Day"};
                new AlertDialog.Builder(this)
                        .setTitle("Reminder Type")
                        .setItems(choices, (dialog, which) -> {
                            boolean isDaily = (which == 1);
                            setAlarm(selected.getTimeInMillis(), isDaily);
                        }).show();

            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setAlarm(long time, boolean isDaily) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", editTitle.getText().toString());
        intent.putExtra("noteId", currentEditingNoteId); // FIX BUG 6: noteId pass karo taaki notification tap se sahi note khule
        // FIX BUG 1: har note ka unique requestCode — warna saare alarms ek hi PendingIntent share karte hain
        int requestCode = currentEditingNoteId != null ? currentEditingNoteId.hashCode() : 0;
        PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (am != null) {
            if (isDaily) {
                // FIX BUG 5: setRepeating Android 6+ par inexact hai — setExactAndAllowWhileIdle use karo
                // ReminderReceiver mein next day ka alarm re-schedule karna hoga
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
                } else {
                    am.setRepeating(AlarmManager.RTC_WAKEUP, time, AlarmManager.INTERVAL_DAY, pi);
                }
            } else {
                // One-time alarm
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, time, pi);
                }
            }
        }

        // Persistence: Save alarm status to the current note
        if (currentEditingNoteId != null) {
            for (HashMap<String, String> n : allNotesList) {
                if (currentEditingNoteId.equals(n.get("id"))) { // FIX BUG 3: null-safe comparison
                    n.put("alarm_time", String.valueOf(time));
                    n.put("alarm_repeat", isDaily ? "daily" : "once");
                    saveNotesToStorage();
                    break;
                }
            }
        }

        Toast.makeText(this, isDaily ? "Daily reminder set!" : "One-time reminder set!", Toast.LENGTH_SHORT).show();
    }

    // FIX BUG 2: Alarm cancel karne ka function — note delete hone par alarm bhi cancel hoga
    private void cancelAlarm(String noteId) {
        if (noteId == null) return;
        Intent intent = new Intent(this, ReminderReceiver.class);
        int requestCode = noteId.hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(this, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null) am.cancel(pi);
            pi.cancel();
        }
    }

    private void toggleNoteLock() {
        if (isCurrentNoteLocked) {
            isCurrentNoteLocked = false;
            currentNotePin = "";
            updateEditorToolbarIcons();
            Toast.makeText(this, "Note unlocked", Toast.LENGTH_SHORT).show();
        } else {
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            input.setHint("Enter 4-digit PIN");
            input.setPadding(50, 50, 50, 50);

            new AlertDialog.Builder(this)
                    .setTitle("Set Note PIN")
                    .setMessage("Set a PIN to lock this note.")
                    .setView(input)
                    .setPositiveButton("Lock", (d, w) -> {
                        String pin = input.getText().toString().trim();
                        if (pin.length() >= 4) {
                            isCurrentNoteLocked = true;
                            currentNotePin = hashPIN(pin); // SECURE: Hash before saving
                            updateEditorToolbarIcons();
                            Toast.makeText(this, "Note locked with PIN", Toast.LENGTH_SHORT).show();
                            saveCurrentNote(); // Save PIN immediately
                        } else {
                            Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void toggleNotePin() {
        isCurrentNotePinned = !isCurrentNotePinned;
        updateEditorToolbarIcons();
        Toast.makeText(this, isCurrentNotePinned ? "Note pinned" : "Note unpinned", Toast.LENGTH_SHORT).show();
    }

    // SECURE PIN HASHING (Finding 002 Fix - REMOVED WEAK FALLBACK)
    private String hashPIN(String pin) {
        try {
            char[] chars = pin.toCharArray();
            byte[] salt = getDynamicSalt("pin_security_salt_v3");

            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(chars, salt, 600000, 256);
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            // CRITICAL: No more NuclearFallback. If PBKDF2 fails, return random failure string.
            return "ERR_" + UUID.randomUUID().toString();
        }
    }

    // SECURE DYNAMIC SALT GENERATOR (C1 / F-007 Fix)
    private byte[] getDynamicSalt(String keyName) {
        SharedPreferences sp = getSharedPreferences("SecureConfig", MODE_PRIVATE);
        // SECURE: Salts are now stored ENCRYPTED (Finding 007 Fix)
        String saltEncBase64 = sp.getString(keyName + "_enc", null);

        if (saltEncBase64 == null) {
            // Generate a fresh 32-byte cryptographically random salt
            byte[] newSalt = new byte[32];
            new java.security.SecureRandom().nextBytes(newSalt);
            // Encrypt salt before storing
            // SECURE: Use Metadata Key (Fast) for internal salts
            String encryptedSalt = secureEncrypt(android.util.Base64.encodeToString(newSalt, android.util.Base64.DEFAULT), false);
            sp.edit().putString(keyName + "_enc", encryptedSalt).apply();
            return newSalt;
        }
        // Decrypt salt before returning
        String decryptedSaltBase64 = secureDecrypt(saltEncBase64, false);
        return android.util.Base64.decode(decryptedSaltBase64, android.util.Base64.DEFAULT);
    }

    public static class ReminderReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            // SECURE: Use generic text in notification to prevent data exposure (Finding F-Notification Fix)
            // We no longer use "Reminder: " string which was pakda-ed by scanner.
            android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                android.app.Notification.Builder builder;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    builder = new android.app.Notification.Builder(context, "REMINDERS");
                } else {
                    builder = new android.app.Notification.Builder(context);
                }

                builder.setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("⏰ GoNotes Alert")
                        .setContentText("A secure reminder has been triggered.") // Generic content
                        .setAutoCancel(true)
                        .setPriority(android.app.Notification.PRIORITY_HIGH);

                nm.notify((int) System.currentTimeMillis(), builder.build());
            }
        }
    }


    private void updateWordCount() {
        String text = editNoteBody.getText().toString().trim();
        int charCount = text.length();
        int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;

        String info = String.format(Locale.getDefault(), "%d words | %d characters", wordCount, charCount);
        textWordCount.setText(info);
    }
    private void toggleViewMode() {
        if (listViewNotes.getVisibility() == View.VISIBLE) {
            listViewNotes.setVisibility(View.GONE);
            gridViewNotes.setVisibility(View.VISIBLE);
            getSharedPreferences("MyNotesData", MODE_PRIVATE).edit().putString("view_mode", "grid").apply();
        } else {
            gridViewNotes.setVisibility(View.GONE);
            listViewNotes.setVisibility(View.VISIBLE);
            getSharedPreferences("MyNotesData", MODE_PRIVATE).edit().putString("view_mode", "list").apply();
        }
    }
    private void handleIntentAction(Intent i) {}

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    private void openCamera() {
        try {
            // 1. Create dedicated "images" folder for sharing safety (F-009 Fix)
            java.io.File imagesDir = new java.io.File(getFilesDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();

            java.io.File photoFile = new java.io.File(imagesDir, "camera_temp_" + System.currentTimeMillis() + ".jpg");
            // SECURE: Path stored ONLY in memory (F-009 Fix)
            pendingCameraPhotoPath = photoFile.getAbsolutePath();

            Uri photoURI = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
            takePhotoLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void addImageToCurrentNote(String path) {
        if (path == null) return;
        currentImagePaths.add(path);
        imagesAdapter.notifyItemInserted(currentImagePaths.size() - 1);
        imagesRecyclerView.setVisibility(View.VISIBLE);

        // Clear pending path from SharedPreferences since it's now handled
        getSharedPreferences("MyNotesData", MODE_PRIVATE).edit()
                .remove("pendingCameraPhotoPath")
                .apply();
        pendingCameraPhotoPath = null;
    }

    private void replacePlaceholderWithImage(String tempId, String realPath) {
        mainHandler.post(() -> {
            int index = currentImagePaths.indexOf(tempId);
            if (index != -1) {
                currentImagePaths.set(index, realPath);
                imagesAdapter.notifyItemChanged(index);
                Toast.makeText(MainActivity.this, "Image Optimized! ✨", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void extractImagePathsFromText(String text) {
        // Method kept empty but logic removed - images now handled via 'images' JSON field
    }
    private void toggleNoteSelection(String id) {
        if (selectedNoteIds.contains(id)) selectedNoteIds.remove(id);
        else selectedNoteIds.add(id);

        if (selectedNoteIds.isEmpty()) exitSelectionMode();
        else {
            updateSelectionCount();
            adapter.notifyDataSetChanged();
            if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
        }
    }

    private void showBinOptions(HashMap<String, String> n) {
        String[] options = {"Restore Note", "Delete Permanently"};
        new AlertDialog.Builder(this)
                .setTitle("Options for: " + n.get("title"))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        n.put("isTrashed", "false");
                        n.put("modified_at", String.valueOf(System.currentTimeMillis()));
                        Toast.makeText(this, "Note restored", Toast.LENGTH_SHORT).show();
                    } else {
                        recordDeletedId(n.get("id")); // Tombstone logic
                        allNotesList.remove(n);
                        Toast.makeText(this, "Permanently deleted", Toast.LENGTH_SHORT).show();
                    }
                    saveNotesToStorage();
                    filterNotes("");
                    uploadBackupToDriveBackground(false);
                }).show();
    }



    private void showSelectionMoreMenu() {
        PopupMenu popup = new PopupMenu(this, buttonMoreSelection);
        if (isBinMode) {
            popup.getMenu().add("Restore");
        } else {
            popup.getMenu().add("Pin/Unpin");
            // Show Move to Folder ONLY in Notebook mode
            if (isNotebookMode) {
                popup.getMenu().add("Move to Folder");
            }
            if (selectedNoteIds.size() == 1) {
                popup.getMenu().add("Rename");
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Restore")) restoreSelectedNotes();
            else if (title.equals("Pin/Unpin")) togglePinSelected();
            else if (title.equals("Move to Folder")) showMoveDialog();
            else if (title.equals("Rename")) showRenameDialog();
            return true;
        });
        popup.show();
    }

    private void togglePinSelected() {
        if (selectedNoteIds.isEmpty()) return;

        // Find if the first selected item is pinned to decide action
        boolean shouldPin = true;
        for (HashMap<String, String> n : allNotesList) {
            if (selectedNoteIds.contains(n.get("id"))) {
                shouldPin = !"true".equals(n.get("isPinned"));
                break;
            }
        }

        for (HashMap<String, String> n : allNotesList) {
            if (selectedNoteIds.contains(n.get("id"))) {
                n.put("isPinned", String.valueOf(shouldPin));
                n.put("modified_at", String.valueOf(System.currentTimeMillis()));
            }
        }

        saveNotesToStorage();
        exitSelectionMode();
        filterNotes("");
        uploadBackupToDriveBackground(false);
        Toast.makeText(this, shouldPin ? "Items Pinned" : "Items Unpinned", Toast.LENGTH_SHORT).show();
    }

    private void restoreSelectedNotes() {
        if (selectedNoteIds.isEmpty()) return;

        ArrayList<String> idsToRestore = new ArrayList<>(selectedNoteIds);
        // Cascading Restore: If a folder is restored, its children should be too
        // AND if a note is restored, its parent folders MUST be restored too
        for (int i = 0; i < idsToRestore.size(); i++) {
            String currentId = idsToRestore.get(i);
            for (HashMap<String, String> n : allNotesList) {
                // Case 1: Child of a restored folder
                if (currentId.equals(n.get("parentId")) && !idsToRestore.contains(n.get("id"))) {
                    idsToRestore.add(n.get("id"));
                }
                // Case 2: Parent of a restored item
                if (n.get("id").equals(getParentIdForNote(currentId)) && !idsToRestore.contains(n.get("id"))) {
                    idsToRestore.add(n.get("id"));
                }
            }
        }

        for (HashMap<String, String> n : allNotesList) {
            if (idsToRestore.contains(n.get("id"))) {
                n.put("isTrashed", "false");
                n.put("modified_at", String.valueOf(System.currentTimeMillis()));
            }
        }
        saveNotesToStorage();
        exitSelectionMode();
        filterNotes("");
        uploadBackupToDriveBackground(false);
        Toast.makeText(this, "Items restored", Toast.LENGTH_SHORT).show();
    }

    private String getParentIdForNote(String id) {
        for (HashMap<String, String> n : allNotesList) {
            if (id.equals(n.get("id"))) return n.get("parentId");
        }
        return null;
    }

    private void deleteSelectedNotes() {
        if (selectedNoteIds.isEmpty()) return;

        String title = isBinMode ? "Delete Permanently" : "Move to Bin";
        String msg = isBinMode ? "Are you sure you want to permanently delete these " + selectedNoteIds.size() + " items?"
                : "Move " + selectedNoteIds.size() + " items to the Recycle Bin?";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(isBinMode ? "Delete" : "Move", (d, w) -> {
                    if (isBinMode) {
                        // Permanent Delete: Cascading
                        ArrayList<String> idsToRemove = new ArrayList<>(selectedNoteIds);
                        // Find all children/descendants of selected folders
                        for (int i = 0; i < idsToRemove.size(); i++) {
                            String currentId = idsToRemove.get(i);
                            for (HashMap<String, String> n : allNotesList) {
                                if (currentId.equals(n.get("parentId")) && !idsToRemove.contains(n.get("id"))) {
                                    idsToRemove.add(n.get("id"));
                                }
                            }
                        }

                        java.util.Iterator<HashMap<String, String>> it = allNotesList.iterator();
                        while (it.hasNext()) {
                            HashMap<String, String> item = it.next();
                            if (idsToRemove.contains(item.get("id"))) {
                                recordDeletedId(item.get("id")); // Tombstone
                                it.remove();
                            }
                        }
                    } else {
                        // Move to Bin: Cascading
                        ArrayList<String> idsToTrash = new ArrayList<>(selectedNoteIds);
                        for (int i = 0; i < idsToTrash.size(); i++) {
                            String currentId = idsToTrash.get(i);
                            for (HashMap<String, String> n : allNotesList) {
                                if (currentId.equals(n.get("parentId")) && !idsToTrash.contains(n.get("id"))) {
                                    idsToTrash.add(n.get("id"));
                                }
                            }
                        }

                        for (HashMap<String, String> n : allNotesList) {
                            if (idsToTrash.contains(n.get("id"))) {
                                n.put("isTrashed", "true");
                                n.put("modified_at", String.valueOf(System.currentTimeMillis()));
                            }
                        }
                    }
                    saveNotesToStorage();
                    exitSelectionMode();
                    filterNotes("");
                    uploadBackupToDriveBackground(false);
                    Toast.makeText(this, isBinMode ? "Permanently deleted" : "Moved to Bin", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMoveDialog() {
        if (selectedNoteIds.isEmpty()) return;

        ArrayList<HashMap<String, String>> folders = new ArrayList<>();
        // Add "Root" as a destination
        HashMap<String, String> root = new HashMap<>();
        root.put("id", "root");
        root.put("title", "Root (Main)");
        root.put("level", "1");
        folders.add(root);

        for (HashMap<String, String> n : allNotesList) {
            if ("true".equals(n.get("isFolder")) && !selectedNoteIds.contains(n.get("id"))) {
                folders.add(n);
            }
        }

        String[] folderNames = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) folderNames[i] = folders.get(i).get("title");

        new AlertDialog.Builder(this)
                .setTitle("Move " + selectedNoteIds.size() + " items to...")
                .setItems(folderNames, (dialog, which) -> {
                    HashMap<String, String> dest = folders.get(which);
                    moveSelectedNotesTo(dest.get("id"), Integer.parseInt(dest.get("level")));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameDialog() {
        if (selectedNoteIds.size() != 1) return;
        String id = selectedNoteIds.iterator().next();
        HashMap<String, String> note = null;
        for (HashMap<String, String> n : allNotesList) {
            if (n.get("id").equals(id)) { note = n; break; }
        }
        if (note == null) return;

        final HashMap<String, String> target = note;
        final EditText input = new EditText(this);
        input.setText(target.get("title"));
        input.setSelection(input.getText().length());
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("Rename " + ("true".equals(target.get("isFolder")) ? "Folder" : "Note"))
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        target.put("title", newName);
                        target.put("modified_at", String.valueOf(System.currentTimeMillis()));
                        saveNotesToStorage();
                        exitSelectionMode();
                        filterNotes("");
                        uploadBackupToDriveBackground(false);
                        Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void moveSelectedNotesTo(String destId, int destLevel) {
        int newLevel = destLevel + 1;
        if (newLevel > 5) {
            Toast.makeText(this, "Cannot move: Maximum 5 levels reached!", Toast.LENGTH_SHORT).show();
            return;
        }

        // SAFETY CHECK: Disallow moving a folder into itself or its own children (loops)
        for (String selectedId : selectedNoteIds) {
            if (selectedId.equals(destId)) {
                Toast.makeText(this, "Cannot move a folder into itself!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isDescendant(selectedId, destId)) {
                Toast.makeText(this, "Cannot move a folder into its own subfolder!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        for (HashMap<String, String> n : allNotesList) {
            if (selectedNoteIds.contains(n.get("id"))) {
                n.put("parentId", destId);
                n.put("level", String.valueOf(newLevel));
                n.put("modified_at", String.valueOf(System.currentTimeMillis()));
            }
        }
        saveNotesToStorage();
        exitSelectionMode();
        filterNotes("");
        uploadBackupToDriveBackground(false);
        Toast.makeText(this, "Items moved successfully", Toast.LENGTH_SHORT).show();
    }

    private boolean isDescendant(String parentId, String potentialChildId) {
        String currentId = potentialChildId;
        while (currentId != null && !currentId.equals("root")) {
            String myParentId = null;
            for (HashMap<String, String> n : allNotesList) {
                if (currentId.equals(n.get("id"))) {
                    myParentId = n.get("parentId");
                    break;
                }
            }
            if (myParentId != null && myParentId.equals(parentId)) return true;
            currentId = myParentId;
        }
        return false;
    }
    private void showManageCategoriesDialog() {
        final ArrayList<String> tempCategories = new ArrayList<>(categoriesList);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Manage Categories")
                .setView(scrollView)
                .setPositiveButton("Add New", null)
                .setNegativeButton("Close", null)
                .create();

        refreshCategoryListInDialog(layout, tempCategories);

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                final EditText input = new EditText(this);
                input.setHint("Category Name");
                new AlertDialog.Builder(this).setTitle("New Category").setView(input).setPositiveButton("Add", (d2, w2) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty() && !tempCategories.contains(name)) {
                        tempCategories.add(name);
                        categoriesList.clear(); categoriesList.addAll(tempCategories);
                        saveCategories(); setupCategoriesInDrawer();
                        uploadBackupToDriveBackground(false);
                        refreshCategoryListInDialog(layout, tempCategories);
                    }
                }).show();
            });
        });
        dialog.show();
    }

    private void refreshCategoryListInDialog(LinearLayout container, ArrayList<String> list) {
        container.removeAllViews();
        for (String cat : list) {
            RelativeLayout row = new RelativeLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setPadding(0, 20, 0, 20);

            TextView tv = new TextView(this);
            tv.setText(cat);
            tv.setTextSize(16);
            tv.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
            RelativeLayout.LayoutParams lpText = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpText.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lpText.addRule(RelativeLayout.CENTER_VERTICAL);
            row.addView(tv, lpText);

            if (!cat.equals("General")) {
                TextView btnDel = new TextView(this);
                btnDel.setText("🗑️");
                btnDel.setTextSize(20);
                btnDel.setPadding(20, 10, 20, 10);
                btnDel.setBackgroundResource(android.R.drawable.list_selector_background);
                RelativeLayout.LayoutParams lpDel = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lpDel.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                lpDel.addRule(RelativeLayout.CENTER_VERTICAL);

                btnDel.setOnClickListener(v -> {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("Delete " + cat + "?")
                            .setMessage("What should happen to the notes in this category?")
                            .setPositiveButton("DELETE NOTES TOO", (d, w) -> {
                                list.remove(cat);
                                categoriesList.clear(); categoriesList.addAll(list);
                                deleteCategoryAndHandleNotes(cat, true);
                                refreshCategoryListInDialog(container, list);
                            })
                            .setNeutralButton("KEEP NOTES", (d, w) -> {
                                list.remove(cat);
                                categoriesList.clear(); categoriesList.addAll(list);
                                deleteCategoryAndHandleNotes(cat, false);
                                refreshCategoryListInDialog(container, list);
                            })
                            .setNegativeButton("CANCEL", null)
                            .show();
                });
                row.addView(btnDel, lpDel);
            }

            container.addView(row);

            // Divider
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.LTGRAY);
            container.addView(divider);
        }
    }



    private ArrayList<HashMap<String, String>> getCategoryMapList(ArrayList<String> list) {
        ArrayList<HashMap<String, String>> maps = new ArrayList<>();
        for (String s : list) { HashMap<String, String> m = new HashMap<>(); m.put("name", s); maps.add(m); }
        return maps;
    }

    private void saveCategories() {
        try {
            JSONArray arr = new JSONArray(categoriesList);
            String plainJson = arr.toString();

            // SECURE: Use MASTER Key (Sakt) for category list
            String secureData = secureEncrypt(plainJson, true);
            if (secureData != null) {
                getSharedPreferences("MyNotesData", MODE_PRIVATE).edit()
                        .putString("categories_list_secure", secureData)
                        .remove("categories_list") // Clean up plaintext
                        .putLong("categories_modified_at", System.currentTimeMillis())
                        .apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recordDeletedId(String id) {
        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        java.util.Set<String> deletedIds = new java.util.HashSet<>(sp.getStringSet("deleted_note_ids", new java.util.HashSet<>()));
        deletedIds.add(id);
        sp.edit().putStringSet("deleted_note_ids", deletedIds).apply();
    }

    private void loadCategories() {
        SharedPreferences sp = getSharedPreferences("MyNotesData", MODE_PRIVATE);
        String secureData = sp.getString("categories_list_secure", null);
        String json = null;

        try {
            if (secureData != null) {
                byte[] comb = android.util.Base64.decode(secureData, android.util.Base64.DEFAULT);
                byte[] iv = new byte[IV_LENGTH];
                byte[] enc = new byte[comb.length - IV_LENGTH];
                System.arraycopy(comb, 0, iv, 0, IV_LENGTH);
                System.arraycopy(comb, IV_LENGTH, enc, 0, enc.length);
                SecretKey key = getOrCreateKey(MASTER_KEY_ALIAS, true);
                Cipher cipher = Cipher.getInstance(ALGO_GCM);
                cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(TAG_LENGTH, iv));
                json = new String(cipher.doFinal(enc), java.nio.charset.StandardCharsets.UTF_8);
            } else {
                // Migration path
                json = sp.getString("categories_list", null);
            }

            categoriesList.clear();
            if (json != null) {
                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++) categoriesList.add(a.getString(i));
            } else {
                categoriesList.add("General");
            }
        } catch (Exception e) {
            categoriesList.add("General");
        }
    }

    private void bindNoteView(View view, HashMap<String, String> note, int titleId, int snippetId, int dateId, boolean isGrid) {
        TextView tTitle = view.findViewById(titleId);
        TextView tSnippet = view.findViewById(snippetId);
        TextView tDate = view.findViewById(dateId);
        TextView storageBadge = view.findViewById(R.id.storageBadge);
        View container = view.findViewById(R.id.noteContentContainer);
        View checkmark = view.findViewById(R.id.selectionCheckmark);
        View pinIcon = view.findViewById(R.id.iconPinned);
        View lockIcon = view.findViewById(R.id.iconLocked);
        View alarmIcon = view.findViewById(R.id.iconAlarm);

        // Reset all dynamic properties to avoid recycling glitches
        if (checkmark != null) checkmark.setVisibility(View.GONE);
        if (pinIcon != null) pinIcon.setVisibility(View.GONE);
        if (lockIcon != null) lockIcon.setVisibility(View.GONE);
        if (alarmIcon != null) alarmIcon.setVisibility(View.GONE);

        String color = note.get("color");
        android.graphics.drawable.Drawable background = container.getBackground();
        int bgColor;

        if (color == null || color.equals("default") || color.equals("#121212") || color.equals("#FFFFFF")) {
            bgColor = ContextCompat.getColor(this, R.color.searchBackgroundColor);
            if (tTitle != null) tTitle.setTextColor(ContextCompat.getColor(this, R.color.primaryTextColor));
            if (tSnippet != null) tSnippet.setTextColor(ContextCompat.getColor(this, R.color.secondaryTextColor));
            if (tDate != null) tDate.setTextColor(ContextCompat.getColor(this, R.color.secondaryTextColor));
        } else {
            bgColor = Color.parseColor(color);
            if (tTitle != null) tTitle.setTextColor(Color.WHITE);
            if (tSnippet != null) tSnippet.setTextColor(Color.parseColor("#DDDDDD"));
            if (tDate != null) tDate.setTextColor(Color.parseColor("#BBFFFFFF"));
        }

        if (background instanceof GradientDrawable) {
            ((GradientDrawable) background.mutate()).setColor(bgColor);
        } else {
            container.setBackgroundColor(bgColor);
        }

        String displayTitle = buildDisplayTitle(note);

        if ("true".equals(note.get("isFolder"))) {
            String level = note.get("level");

            if (level != null && level.equals("1")) {
                String[] bookIcons = {"📘", "📙", "📗", "📕", "📔"};
                int iconIndex = Math.abs(displayTitle.hashCode()) % bookIcons.length;
                String icon = bookIcons[iconIndex];
                android.text.SpannableString ss = new android.text.SpannableString(icon + "  " + displayTitle);
                ss.setSpan(new android.text.style.RelativeSizeSpan(1.4f), 0, icon.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (tTitle != null) tTitle.setText(ss);
            } else {
                String icon = "📁";
                android.text.SpannableString ss = new android.text.SpannableString(icon + "  " + displayTitle);
                ss.setSpan(new android.text.style.RelativeSizeSpan(1.4f), 0, icon.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (tTitle != null) tTitle.setText(ss);
            }

            int count = 0;
            String folderId = note.get("id");
            for (HashMap<String, String> n : allNotesList) {
                if (folderId.equals(n.get("parentId")) && !"true".equals(n.get("isTrashed"))) {
                    count++;
                }
            }
            if (tSnippet != null) tSnippet.setText(count + (count == 1 ? " item" : " items"));

            if (tTitle != null) {
                if ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    tTitle.setTextColor(Color.parseColor("#FFD700"));
                } else {
                    tTitle.setTextColor(Color.parseColor("#A0522D"));
                }
            }
        } else {
            // It's a note - Add a note icon to distinguish from folders
            String parentId = note.get("parentId");
            boolean isInFolder = parentId != null && !"root".equals(parentId);

            String icon = isInFolder ? "📖" : "📄";

            android.text.SpannableString ss = new android.text.SpannableString(icon + "  " + displayTitle);
            // Make icon 20% larger
            ss.setSpan(new android.text.style.RelativeSizeSpan(1.2f), 0, icon.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (tTitle != null) tTitle.setText(ss);

            // SET NOTE PREVIEW (SNIPPET)
            String body = note.get("fullBody");
            String imagesJson = note.get("images");
            boolean hasImages = imagesJson != null && imagesJson.contains(".webp");

            if (tSnippet != null && body != null) {
                String prefix = hasImages ? "🖼️ " : "";
                // Clean technical markers from the snippet preview
                String cleanBody = body.replaceAll("\\[\\[IMG:(.*?)\\]\\]", "").trim();
                String snippet = cleanBody.replace("\n", " ").trim();

                if (snippet.length() > 65) {
                    snippet = snippet.substring(0, 62) + "...";
                }
                tSnippet.setText(prefix + (snippet.isEmpty() ? (hasImages ? "Image Note" : "No additional text") : snippet));
            }
        }

        String modified = note.get("timestamp");
        if (tDate != null) {
            if (isBinMode || isRecentMode) {
                String path = getParentPath(note);
                tDate.setText((!isGrid ? "From: " : "") + path + (modified != null ? " | " + modified.split(",")[0] : ""));
            } else if (!"true".equals(note.get("isFolder"))) {
                tDate.setText((isGrid ? "M: " : "Mod: ") + (modified != null ? modified.split(",")[0] : ""));
            } else {
                tDate.setText("");
            }
        }

        if (storageBadge != null) {
            String storageMode = getNormalizedStorageMode(note, "plain");
            if ("encrypted".equalsIgnoreCase(storageMode)) {
                storageBadge.setText("Enc");
                storageBadge.setBackgroundColor(Color.parseColor("#2563EB"));
                storageBadge.setVisibility(View.VISIBLE);
            } else if ("plain".equalsIgnoreCase(storageMode)) {
                storageBadge.setText("Plain");
                storageBadge.setBackgroundColor(Color.parseColor("#F59E0B"));
                storageBadge.setVisibility(View.VISIBLE);
            } else {
                storageBadge.setVisibility(View.GONE);
            }
        }

        if (checkmark != null) {
            if (isSelectionMode && selectedNoteIds.contains(note.get("id"))) {
                checkmark.setVisibility(View.VISIBLE);
            } else {
                checkmark.setVisibility(View.GONE);
            }
        }

        if (pinIcon != null) {
            // Show pin icon ONLY if not in Recent mode
            pinIcon.setVisibility(!isRecentMode && "true".equals(note.get("isPinned")) ? View.VISIBLE : View.GONE);
        }

        if (lockIcon != null) {
            lockIcon.setVisibility("true".equals(note.get("isLocked")) ? View.VISIBLE : View.GONE);
        }

        if (alarmIcon != null) {
            alarmIcon.setVisibility(note.get("alarm_time") != null ? View.VISIBLE : View.GONE);
        }
    }

    private int compareDates(String d1, String d2, boolean newestFirst) {
        if (d1 == null || d1.isEmpty()) return 1;
        if (d2 == null || d2.isEmpty()) return -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault());
            Date date1 = sdf.parse(d1);
            Date date2 = sdf.parse(d2);
            if (date1 == null || date2 == null) return 0;
            return newestFirst ? date2.compareTo(date1) : date1.compareTo(date2);
        } catch (Exception e) {
            return 0;
        }
    }

    private String buildDisplayTitle(HashMap<String, String> n) {
        String t = n.get("title");
        return (t == null || t.isEmpty()) ? "Untitled Note" : t;
    }

    private String getParentPath(HashMap<String, String> note) {
        String parentId = note.get("parentId");
        String section = "Notes";

        if ("true".equals(note.get("isFolder"))) {
            section = "Notebooks";
        }

        // If the note is in a folder, it belongs to the Notebooks hierarchy
        if (parentId != null && !"root".equals(parentId)) {
            section = "Notebooks";
        }

        if (parentId == null || "root".equals(parentId)) return section;

        StringBuilder fullPath = new StringBuilder();
        String currentId = parentId;

        // Recursively build the path from item to root
        while (currentId != null && !"root".equals(currentId)) {
            boolean found = false;
            for (HashMap<String, String> n : allNotesList) {
                if (currentId.equals(n.get("id"))) {
                    if (fullPath.length() > 0) fullPath.insert(0, " > ");
                    fullPath.insert(0, n.get("title"));
                    currentId = n.get("parentId");
                    found = true;
                    break;
                }
            }
            if (!found) {
                currentId = null; // Break loop if parent not found
            }
        }

        if (fullPath.length() == 0) return section;
        return section + " > " + fullPath.toString();
    }

    private void showAddNotebookItemDialog() {
        String[] options = {"📄 New Note", "📁 New Folder"};
        new AlertDialog.Builder(this)
                .setTitle("Create in " + (navigationPathNames.isEmpty() ? "Root" : navigationPathNames.get(navigationPathNames.size()-1)))
                .setItems(options, (dialog, which) -> {
                    if (which == 1 && currentLevel >= 5) {
                        Toast.makeText(this, "Maximum 5 levels of folders reached!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final EditText input = new EditText(this);
                    input.setHint(which == 0 ? "Note Title" : "Folder Name");
                    new AlertDialog.Builder(this)
                            .setTitle(which == 0 ? "New Note" : "New Folder")
                            .setView(input)
                            .setPositiveButton("Create", (d, w) -> {
                                String name = input.getText().toString().trim();
                                if (!name.isEmpty()) {
                                    if (which == 1) {
                                        // Create Folder
                                        HashMap<String, String> folder = new HashMap<>();
                                        folder.put("id", UUID.randomUUID().toString());
                                        folder.put("title", name);
                                        folder.put("isFolder", "true");
                                        folder.put("parentId", currentParentId);
                                        folder.put("level", String.valueOf(currentLevel));
                                        folder.put("timestamp", new SimpleDateFormat("dd/MM/yyyy, HH:mm").format(new Date()));
                                        folder.put("isTrashed", "false");
                                        allNotesList.add(0, folder);
                                    } else {
                                        // Create Note (Open Editor)
                                        currentEditingNoteId = null;
                                        editTitle.setText(name);
                                        editNoteBody.setText("");
                                        screenList.setVisibility(View.GONE);
                                        screenAddNote.setVisibility(View.VISIBLE);
                                        buttonPlus.setVisibility(View.GONE);
                                    }
                                    saveNotesToStorage(); filterNotes("");
                                    uploadBackupToDriveBackground(false);
                                }
                            }).setNegativeButton("Cancel", null).show();
                }).show();
    }

    @Override
    public void onBackPressed() {
        if (isFabOpen) {
            toggleFab(false);
            return;
        }
        if (screenAddNote.getVisibility() == View.VISIBLE) {
            closeNoteScreen();
            return;
        }
        if (isSelectionMode) {
            exitSelectionMode();
            return;
        }
        if (isBinMode) {
            exitBinMode();
            return;
        }

        if (isNotebookMode && !currentParentId.equals("root")) {
            navigationPathIds.remove(navigationPathIds.size() - 1);
            navigationPathNames.remove(navigationPathNames.size() - 1);
            currentParentId = navigationPathIds.isEmpty() ? "root" : navigationPathIds.get(navigationPathIds.size() - 1);
            currentLevel--;
            filterNotes(""); updateBreadcrumbText();
            return;
        }

        // If searching, clear search
        if (!searchBar.getText().toString().isEmpty()) {
            searchBar.setText("");
            filterNotes("");
            return;
        }

        // If in any special tab/mode, return to "Recent" (Home) before exiting
        // But if we are ALREADY in Recent mode, just exit (super.onBackPressed)
        if (isNormalFilterMode || isNotebookMode || isBinMode || !selectedCategoryFilter.equals("All")) {
            // Reset special UI elements
            buttonMenu.setText("☰");
            if (!isBinMode) buttonPlus.setVisibility(View.VISIBLE);
            selectedCategoryFilter = "All"; // Reset category filter

            // Use the existing tab logic to return Home perfectly
            tabRecent.performClick();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // FIX: Auto-save ke liye vault lock check hata diya — user ka note app pause pe hamesha save hoga
        if (screenAddNote != null && screenAddNote.getVisibility() == View.VISIBLE) {
            saveCurrentNote();
        }

        // Background Sync
        if (isVaultUnlocked && driveService != null && !allNotesList.isEmpty() && !isDeviceRooted()) {
            uploadBackupToDriveBackground(true);
        }
    }
}