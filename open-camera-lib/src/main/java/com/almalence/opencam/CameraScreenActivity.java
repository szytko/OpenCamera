/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
 */


package com.almalence.opencam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.almalence.opencam.cameracontroller.CameraProvider;
import com.almalence.util.Util;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.AlmalenceGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;
import com.almalence.util.exifreader.lang.annotations.NotNull;


/**
 * MainScreen - main activity screen with camera functionality
 * <p/>
 * Passes all main events to PluginManager
 * *
 */
@SuppressWarnings("deprecation")
public class CameraScreenActivity extends Activity implements ApplicationInterface, View.OnClickListener, View.OnTouchListener,
        SurfaceHolder.Callback, Handler.Callback, Camera.ShutterCallback {


    private static final int MSG_RETURN_CAPTURED = -1;

    private static final int MODE_GENERAL = 0;
    private static final int MODE_SMART_MULTISHOT_AND_NIGHT = 1;
    private static final int MODE_PANORAMA = 2;
    private static final int MODE_VIDEO = 3;

    private static final int MIN_MPIX_PREVIEW = 600 * 400;

    public static CameraScreenActivity thiz;
    public Context mainContext;
    private Handler messageHandler;

    private CameraController cameraController = null;



    public GUI guiManager = null;
    private GLLayer glView;

    private boolean mPausing = false;

    private File forceFilename = null;
    private Uri forceFilenameUri;

    protected SurfaceHolder surfaceHolder;
    private SurfaceView preview;
    private OrientationEventListener orientListener;
    private boolean landscapeIsNormal = false;
    private boolean surfaceCreated = false;
    private int previewWidth, previewHeight;

    private CountDownTimer screenTimer = null;
    private boolean isScreenTimerRunning = false;

    private static boolean wantLandscapePhoto = false;
    private int orientationMain = 0;
    private int orientationMainPrevious = 0;

    private SoundPlayer shutterPlayer = null;

    // Common preferences
    private String imageSizeIdxPreference;
    private boolean shutterPreference = true;
    private int shotOnTapPreference = 0;

    private boolean showHelp = false;

    private boolean keepScreenOn = false;

    private String saveToPath;
    private String saveToPreference;
    private boolean sortByDataPreference;


    private static boolean maxScreenBrightnessPreference;

    private static boolean mAFLocked = false;

    // shows if mode is currently switching
    private boolean switchingMode = false;


    private static boolean isCreating = false;
    private static boolean mApplicationStarted = false;
    private static boolean mCameraStarted = false;
    private static boolean isForceClose = false;

    // Clicked mode id from widget.
    public static final String EXTRA_ITEM = "WidgetModeID";

    public static final String EXTRA_TORCH = "WidgetTorchMode";
    public static final String EXTRA_BARCODE = "WidgetBarcodeMode";

    private static boolean launchTorch = false;
    private static boolean launchBarcode = false;

    private static int prefFlash = -1;
    private static boolean prefBarcode = false;

    private static final int VOLUME_FUNC_SHUTTER = 0;
    private static final int VOLUME_FUNC_EXPO = 2;
    private static final int VOLUME_FUNC_NONE = 3;

    private static List<Area> mMeteringAreaMatrix5 = new ArrayList<>();
    private static List<Area> mMeteringAreaMatrix4 = new ArrayList<>();
    private static List<Area> mMeteringAreaMatrix1 = new ArrayList<>();
    private static List<Area> mMeteringAreaCenter = new ArrayList<>();
    private static List<Area> mMeteringAreaSpot = new ArrayList<>();

    private int currentMeteringMode = -1;

    public static String sTimestampDate;
    public static String sTimestampAbbreviation;
    public static String sTimestampTime;
    public static String sTimestampSeparator;
    public static String sTimestampCustomText;
    public static String sTimestampColor;
    public static String sTimestampFontSize;

    public static String sEvPref;
    public static String sSceneModePref;
    public static String sWBModePref;
    public static String sFrontFocusModePref;
    public static String sRearFocusModePref;
    public static String sFlashModePref;
    public static String sISOPref;
    public static String sMeteringModePref;

    public static String sDelayedCapturePref;
    public static String sShowDelayedCapturePref;
    public static String sDelayedSoundPref;
    public static String sDelayedFlashPref;
    public static String sDelayedCaptureIntervalPref;

    public static String sPhotoTimeLapseCaptureIntervalPref;
    public static String sPhotoTimeLapseCaptureIntervalMeasurmentPref;
    public static String sPhotoTimeLapseActivePref;
    public static String sPhotoTimeLapseIsRunningPref;
    public static String sPhotoTimeLapseCount;

    public static String sUseFrontCameraPref;
    private static String sShutterPref;
    private static String sShotOnTapPref;
    private static String sVolumeButtonPref;

    public static String sImageSizeRearPref;
    public static String sImageSizeFrontPref;

    public static String sImageSizeMultishotBackPref;
    public static String sImageSizeMultishotFrontPref;

    public static String sImageSizePanoramaBackPref;
    public static String sImageSizePanoramaFrontPref;

    public static String sImageSizeVideoBackPref;
    public static String sImageSizeVideoFrontPref;

    public static String sCaptureRAWPref;

    public static String sInitModeListPref = "initModeListPref";

    public static String sJPEGQualityPref;

    public static String sDefaultInfoSetPref;
    public static String sSWCheckedPref;
    public static String sSavePathPref;
    public static String sExportNamePref;
    public static String sExportNamePrefixPref;
    public static String sExportNamePostfixPref;
    public static String sSaveToPref;
    public static String sSortByDataPref;
    public static String sEnableExifOrientationTagPref;
    public static String sAdditionalRotationPref;

    public static String sExpoPreviewModePref;

    public static String sDefaultModeName;

    public static int sDefaultValue = CameraParameters.SCENE_MODE_AUTO;
    public static int sDefaultFocusValue = CameraParameters.AF_MODE_CONTINUOUS_PICTURE;
    public static int sDefaultFlashValue = CameraParameters.FLASH_MODE_OFF;
    public static int sDefaultMeteringValue = CameraParameters.meteringModeAuto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sEvPref = getResources().getString(R.string.Preference_EvCompensationValue);
        sSceneModePref = getResources().getString(R.string.Preference_SceneModeValue);
        sWBModePref = getResources().getString(R.string.Preference_WBModeValue);
        sFrontFocusModePref = getResources().getString(R.string.Preference_FrontFocusModeValue);
        sRearFocusModePref = getResources().getString(R.string.Preference_RearFocusModeValue);
        sFlashModePref = getResources().getString(R.string.Preference_FlashModeValue);
        sISOPref = getResources().getString(R.string.Preference_ISOValue);
        sMeteringModePref = getResources().getString(R.string.Preference_MeteringModeValue);

        sDelayedCapturePref = getResources().getString(R.string.Preference_DelayedCaptureValue);
        sShowDelayedCapturePref = getResources().getString(R.string.Preference_ShowDelayedCaptureValue);
        sDelayedSoundPref = getResources().getString(R.string.Preference_DelayedSoundValue);
        sDelayedFlashPref = getResources().getString(R.string.Preference_DelayedFlashValue);
        sDelayedCaptureIntervalPref = getResources().getString(R.string.Preference_DelayedCaptureIntervalValue);

        sDelayedCapturePref = getResources().getString(R.string.Preference_DelayedCaptureValue);
        sShowDelayedCapturePref = getResources().getString(R.string.Preference_ShowDelayedCaptureValue);
        sDelayedSoundPref = getResources().getString(R.string.Preference_DelayedSoundValue);
        sDelayedFlashPref = getResources().getString(R.string.Preference_DelayedFlashValue);
        sDelayedCaptureIntervalPref = getResources().getString(R.string.Preference_DelayedCaptureIntervalValue);

        sPhotoTimeLapseCaptureIntervalPref = getResources()
                .getString(R.string.Preference_PhotoTimeLapseCaptureInterval);
        sPhotoTimeLapseCaptureIntervalMeasurmentPref = getResources().getString(
                R.string.Preference_PhotoTimeLapseCaptureIntervalMeasurment);
        sPhotoTimeLapseActivePref = getResources().getString(R.string.Preference_PhotoTimeLapseSWChecked);
        sPhotoTimeLapseIsRunningPref = getResources().getString(R.string.Preference_PhotoTimeLapseIsRunning);
        sPhotoTimeLapseCount = getResources().getString(R.string.Preference_PhotoTimeLapseCount);

        sUseFrontCameraPref = getResources().getString(R.string.Preference_UseFrontCameraValue);
        sShutterPref = getResources().getString(R.string.Preference_ShutterCommonValue);
        sShotOnTapPref = getResources().getString(R.string.Preference_ShotOnTapValue);
        sVolumeButtonPref = getResources().getString(R.string.Preference_VolumeButtonValue);

        sImageSizeRearPref = getResources().getString(R.string.Preference_ImageSizeRearValue);
        sImageSizeFrontPref = getResources().getString(R.string.Preference_ImageSizeFrontValue);

        sImageSizeMultishotBackPref = getResources()
                .getString(R.string.Preference_ImageSizePrefSmartMultishotBackValue);
        sImageSizeMultishotFrontPref = getResources().getString(
                R.string.Preference_ImageSizePrefSmartMultishotFrontValue);

        sImageSizePanoramaBackPref = getResources().getString(R.string.Preference_ImageSizePrefPanoramaBackValue);
        sImageSizePanoramaFrontPref = getResources().getString(R.string.Preference_ImageSizePrefPanoramaFrontValue);

        sImageSizeVideoBackPref = getResources().getString(R.string.Preference_ImageSizePrefVideoBackValue);
        sImageSizeVideoFrontPref = getResources().getString(R.string.Preference_ImageSizePrefVideoFrontValue);

        sCaptureRAWPref = getResources().getString(R.string.Preference_CaptureRAWValue);

        sJPEGQualityPref = getResources().getString(R.string.Preference_JPEGQualityCommonValue);

        sDefaultInfoSetPref = getResources().getString(R.string.Preference_DefaultInfoSetValue);
        sSWCheckedPref = getResources().getString(R.string.Preference_SWCheckedValue);
        sSavePathPref = getResources().getString(R.string.Preference_SavePathValue);
        sExportNamePref = getResources().getString(R.string.Preference_ExportNameValue);
        sExportNamePrefixPref = getResources().getString(R.string.Preference_SavePathPrefixValue);
        sExportNamePostfixPref = getResources().getString(R.string.Preference_SavePathPostfixValue);
        sSaveToPref = getResources().getString(R.string.Preference_SaveToValue);
        sSortByDataPref = getResources().getString(R.string.Preference_SortByDataValue);
        sEnableExifOrientationTagPref = getResources().getString(R.string.Preference_EnableExifTagOrientationValue);
        sAdditionalRotationPref = getResources().getString(R.string.Preference_AdditionalRotationValue);

        sTimestampDate = getResources().getString(R.string.Preference_TimestampDateValue);
        sTimestampAbbreviation = getResources().getString(R.string.Preference_TimestampAbbreviationValue);
        sTimestampTime = getResources().getString(R.string.Preference_TimestampTimeValue);
        sTimestampSeparator = getResources().getString(R.string.Preference_TimestampSeparatorValue);
        sTimestampCustomText = getResources().getString(R.string.Preference_TimestampCustomTextValue);
        sTimestampColor = getResources().getString(R.string.Preference_TimestampColorValue);
        sTimestampFontSize = getResources().getString(R.string.Preference_TimestampFontSizeValue);

        sExpoPreviewModePref = getResources().getString(R.string.Preference_ExpoBracketingPreviewModePref);

        sDefaultModeName = getResources().getString(R.string.Preference_DefaultModeName);

        Intent intent = this.getIntent();
        String mode = intent.getStringExtra(EXTRA_ITEM);
        launchTorch = intent.getBooleanExtra(EXTRA_TORCH, false);
        launchBarcode = intent.getBooleanExtra(EXTRA_BARCODE, false);

        mainContext = this.getBaseContext();
        messageHandler = new Handler(this);
        thiz = this;

        mApplicationStarted = false;
        isForceClose = false;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // ensure landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // set to fullscreen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // set some common view here
        setContentView(R.layout.opencamera_main_layout);

        // reset or save settings
        resetOrSaveSettings();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());

        if (null != mode)
            prefs.edit().putString("defaultModeName", mode).apply();

        if (launchTorch) {
            prefFlash = prefs.getInt(sFlashModePref, CameraParameters.FLASH_MODE_AUTO);
            prefs.edit().putInt(sFlashModePref, CameraParameters.FLASH_MODE_TORCH).apply();
        }

        if (launchBarcode) {
            prefBarcode = prefs.getBoolean("PrefBarcodescannerVF", false);
            prefs.edit().putBoolean("PrefBarcodescannerVF", true).apply();
        }

        try {
            cameraController = CameraController.getInstance();
        } catch (VerifyError exp) {
            Log.e("MainScreen", exp.getMessage());
        }
        CameraController.onCreate(CameraScreenActivity.thiz, CameraScreenActivity.thiz, PluginManager.getInstance());

        keepScreenOn = prefs.getBoolean("keepScreenOn", false);

        // set preview, on click listener and surface buffers
        preview = (SurfaceView) this.findViewById(R.id.SurfaceView01);
        preview.setOnClickListener(this);
        preview.setOnTouchListener(this);
        preview.setKeepScreenOn(keepScreenOn);

        surfaceHolder = preview.getHolder();
        surfaceHolder.addCallback(this);

        orientListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                // figure landscape or portrait
                if (CameraScreenActivity.thiz.landscapeIsNormal) {
                    orientation += 90;
                }

                if ((orientation < 45) || (orientation > 315 && orientation < 405)
                        || ((orientation > 135) && (orientation < 225))) {
                    if (CameraScreenActivity.wantLandscapePhoto) {
                        CameraScreenActivity.wantLandscapePhoto = false;
                    }
                } else {
                    if (!CameraScreenActivity.wantLandscapePhoto) {
                        CameraScreenActivity.wantLandscapePhoto = true;
                    }
                }

                // orient properly for video
                if ((orientation > 135) && (orientation < 225))
                    orientationMain = 270;
                else if ((orientation < 45) || (orientation > 315))
                    orientationMain = 90;
                else if ((orientation < 325) && (orientation > 225))
                    orientationMain = 0;
                else if ((orientation < 135) && (orientation > 45))
                    orientationMain = 180;

                if (orientationMain != orientationMainPrevious) {
                    orientationMainPrevious = orientationMain;
                }
            }
        };

        // prevent power drain
        screenTimer = new CountDownTimer(180000, 180000) {
            public void onTick(long millisUntilFinished) {
                // Not used
            }

            public void onFinish() {
                boolean isVideoRecording = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext())
                        .getBoolean("videorecording", false);
                if (isVideoRecording || keepScreenOn) {
                    // restart timer
                    screenTimer.start();
                    isScreenTimerRunning = true;
                    preview.setKeepScreenOn(true);
                    return;
                }
                preview.setKeepScreenOn(false);
                isScreenTimerRunning = false;
            }
        };
        screenTimer.start();
        isScreenTimerRunning = true;

        PluginManager.getInstance().setupDefaultMode();
        // init gui manager
        guiManager = new AlmalenceGUI();
        guiManager.createInitialGUI();
        this.findViewById(R.id.mainLayout1).invalidate();
        this.findViewById(R.id.mainLayout1).requestLayout();
        guiManager.onCreate();

        // init plugin manager
        PluginManager.getInstance().onCreate();

        if (this.getIntent().getAction() != null) {
            if (this.getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE)) {
                try {
                    forceFilenameUri = this.getIntent().getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
                    CameraScreenActivity.setForceFilename(new File(forceFilenameUri.getPath()));
                    if (CameraScreenActivity.getForceFilename().getAbsolutePath().equals("/scrapSpace")) {
                        CameraScreenActivity.setForceFilename(new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/mms/scrapSpace/.temp.jpg"));
                        new File(CameraScreenActivity.getForceFilename().getParent()).mkdirs();
                    }
                } catch (Exception e) {
                    CameraScreenActivity.setForceFilename(null);
                }
            } else {
                CameraScreenActivity.setForceFilename(null);
            }
        } else {
            CameraScreenActivity.setForceFilename(null);
        }
    }

    /*
     * Get/Set method for private variables
     */
    public static CameraScreenActivity getInstance() {
        return thiz;
    }

    public static Context getMainContext() {
        return thiz.mainContext;
    }

    public static Handler getMessageHandler() {
        return thiz.messageHandler;
    }

    public static CameraController getCameraController() {
        return thiz.cameraController;
    }

    public static GUI getGUIManager() {
        return thiz.guiManager;
    }

    public static File getForceFilename() {
        return thiz.forceFilename;
    }

    public static void setForceFilename(File fileName) {
        thiz.forceFilename = fileName;
    }

    public static Uri getForceFilenameURI() {
        return thiz.forceFilenameUri;
    }


    public static SurfaceView getPreviewSurfaceView() {
        return thiz.preview;
    }


    public static int getOrientation() {
        return thiz.orientationMain;
    }

    public static String getImageSizeIndex() {
        return thiz.imageSizeIdxPreference;
    }


    public static boolean isShutterSoundEnabled() {
        return thiz.shutterPreference;
    }

    public static int isShotOnTap() {
        return thiz.shotOnTapPreference;
    }

    public static boolean isShowHelp() {
        return thiz.showHelp;
    }

    public static void setShowHelp(boolean show) {
        thiz.showHelp = show;
    }

    public static String getSaveToPath() {
        return thiz.saveToPath;
    }

    public static String getSaveTo() {
        return thiz.saveToPreference;
    }

    public static boolean isSortByData() {
        return thiz.sortByDataPreference;
    }

    public static int getMeteringMode() {
        return thiz.currentMeteringMode;
    }

	/*
     * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ Get/Set method for private variables
	 */

    public void onPreferenceCreate(PreferenceFragment prefActivity) {
        setImageSizeOptions(prefActivity, MODE_GENERAL);
        setImageSizeOptions(prefActivity, MODE_SMART_MULTISHOT_AND_NIGHT);
        setImageSizeOptions(prefActivity, MODE_PANORAMA);
        setImageSizeOptions(prefActivity, MODE_VIDEO);
    }

    private void setImageSizeOptions(PreferenceFragment prefActivity, int mode) {
        CharSequence[] entries = null;
        CharSequence[] entryValues = null;

        int idx = 0;
        int currentIdx = -1;
        String opt1 = "";
        String opt2 = "";

        if (mode == MODE_GENERAL) {
            opt1 = sImageSizeRearPref;
            opt2 = sImageSizeFrontPref;
            currentIdx = Integer.parseInt(CameraScreenActivity.getImageSizeIndex());

            if (currentIdx == -1) {
                currentIdx = 0;
            }

            entries = CameraController.getResolutionsNamesList().toArray(
                    new CharSequence[CameraController.getResolutionsNamesList().size()]);
            entryValues = CameraController.getResolutionsIdxesList().toArray(
                    new CharSequence[CameraController.getResolutionsIdxesList().size()]);
        } else if (mode == MODE_SMART_MULTISHOT_AND_NIGHT) {
            opt1 = sImageSizeMultishotBackPref;
            opt2 = sImageSizeMultishotFrontPref;
            currentIdx = Integer.parseInt(CameraController.MultishotResolutionsIdxesList.get(CameraScreenActivity
                    .selectImageDimensionMultishot()));
            entries = CameraController.MultishotResolutionsNamesList
                    .toArray(new CharSequence[CameraController.MultishotResolutionsNamesList.size()]);
            entryValues = CameraController.MultishotResolutionsIdxesList
                    .toArray(new CharSequence[CameraController.MultishotResolutionsIdxesList.size()]);
        } else if (mode == MODE_VIDEO) {
            opt1 = sImageSizeVideoBackPref;
            opt2 = sImageSizeVideoFrontPref;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
            currentIdx = Integer.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? opt1 : opt2, "2"));

            CharSequence[] entriesTmp = new CharSequence[6];
            CharSequence[] entryValuesTmp = new CharSequence[6];

            if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_1080P)) {
                entriesTmp[idx] = "1080p";
                entryValuesTmp[idx] = "2";
                idx++;
            }
            if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_720P)) {
                entriesTmp[idx] = "720p";
                entryValuesTmp[idx] = "3";
                idx++;
            }
            if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_480P)) {
                entriesTmp[idx] = "480p";
                entryValuesTmp[idx] = "4";
                idx++;
            }
            if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_CIF)) {
                entriesTmp[idx] = "352 x 288";
                entryValuesTmp[idx] = "1";
                idx++;
            }
            if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_QCIF)) {
                entriesTmp[idx] = "176 x 144";
                entryValuesTmp[idx] = "0";
                idx++;
            }

            entries = new CharSequence[idx];
            entryValues = new CharSequence[idx];

            for (int i = 0; i < idx; i++) {
                entries[i] = entriesTmp[i];
                entryValues[i] = entryValuesTmp[i];
            }
        }

        if (CameraController.getResolutionsIdxesList() != null) {
            ListPreference lp = (ListPreference) prefActivity.findPreference(opt1);
            ListPreference lp2 = (ListPreference) prefActivity.findPreference(opt2);

            if (CameraController.getCameraIndex() == 0 && lp2 != null)
                prefActivity.getPreferenceScreen().removePreference(lp2);
            else if (lp != null && lp2 != null) {
                prefActivity.getPreferenceScreen().removePreference(lp);
                lp = lp2;
            }
            if (lp != null) {
                lp.setEntries(entries);
                lp.setEntryValues(entryValues);

                if (currentIdx != -1) {
                    // set currently selected image size
                    for (idx = 0; idx < entryValues.length; ++idx) {
                        if (Integer.valueOf(entryValues[idx].toString()) == currentIdx) {
                            lp.setValueIndex(idx);
                            break;
                        }
                    }
                } else {
                    lp.setValueIndex(0);
                }

                if (mode == MODE_GENERAL) {
                    lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            thiz.imageSizeIdxPreference = newValue.toString();
                            CameraController.setCameraImageSizeIndex(Integer.parseInt(newValue.toString()), false);
                            return true;
                        }
                    });
                }

            }
        }

    }

    public void onAdvancePreferenceCreate(PreferenceFragment prefActivity) {
        CheckBoxPreference cp = (CheckBoxPreference) prefActivity.findPreference(getResources().getString(
                R.string.Preference_UseHALv3Key));
        final CheckBoxPreference fp = (CheckBoxPreference) prefActivity.findPreference(CameraScreenActivity.sCaptureRAWPref);

        if (cp != null) {
            cp.setEnabled(false);


            cp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object useCamera2) {
                    PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext()).edit()
                            .putBoolean(CameraScreenActivity.sInitModeListPref, true).commit();

                    boolean new_value = Boolean.parseBoolean(useCamera2.toString());
                    if (new_value) {
                        if (fp != null && CameraController.isRAWCaptureSupported())
                            fp.setEnabled(true);
                        else
                            fp.setEnabled(false);
                    } else if (fp != null) {
                        PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext()).edit()
                                .putBoolean(CameraScreenActivity.sCaptureRAWPref, false).commit();
                        fp.setEnabled(false);
                    }

                    return true;
                }
            });
        }

        final PreferenceFragment mPref = prefActivity;

        if (fp != null) {
            fp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object captureRAW) {
                    boolean new_value = Boolean.parseBoolean(captureRAW.toString());
                    if (new_value) {
                        new AlertDialog.Builder(mPref.getActivity())
                                .setIcon(R.drawable.gui_almalence_alert_dialog_icon)
                                .setTitle(R.string.Pref_Common_CaptureRAW_Title)
                                .setMessage(R.string.Pref_Common_CaptureRAW_Description)
                                .setPositiveButton(android.R.string.ok, null).create().show();
                    }
                    return true;
                }
            });


            fp.setEnabled(false);
        }
    }

    public void glSetRenderingMode(final int renderMode) {
        if (renderMode != GLSurfaceView.RENDERMODE_WHEN_DIRTY && renderMode != GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
            throw new IllegalArgumentException();
        }

        final GLSurfaceView surfaceView = glView;
        if (surfaceView != null) {
            surfaceView.setRenderMode(renderMode);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        CameraController.onStart();
        CameraScreenActivity.getGUIManager().onStart();
        PluginManager.getInstance().onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        switchingMode = false;
        mApplicationStarted = false;
        orientationMain = 0;
        orientationMainPrevious = 0;
        CameraScreenActivity.getGUIManager().onStop();
        PluginManager.getInstance().onStop();
        CameraController.onStop();


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
        if (launchTorch && prefs.getInt(sFlashModePref, -1) == CameraParameters.FLASH_MODE_TORCH) {
            prefs.edit().putInt(sFlashModePref, prefFlash).apply();
        }
        if (launchBarcode && prefs.getBoolean("PrefBarcodescannerVF", false)) {
            prefs.edit().putBoolean("PrefBarcodescannerVF", prefBarcode).apply();
        }

        prefs.edit().putBoolean(CameraScreenActivity.sPhotoTimeLapseIsRunningPref, false);
        prefs.edit().putBoolean(CameraScreenActivity.sPhotoTimeLapseActivePref, false);

        CameraScreenActivity.getGUIManager().onDestroy();
        PluginManager.getInstance().onDestroy();
        CameraController.onDestroy();


        this.hideOpenGLLayer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        isCameraConfiguring = false;

        if (!isCreating)
            new CountDownTimer(50, 50) {
                public void onTick(long millisUntilFinished) {
                    // Not used
                }

                public void onFinish() {
                    SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(CameraScreenActivity.getMainContext());

                    updatePreferences();

                    preview.setKeepScreenOn(keepScreenOn);


                    saveToPath = prefs.getString(sSavePathPref, Environment.getExternalStorageDirectory()
                            .getAbsolutePath());
                    saveToPreference = prefs.getString(CameraScreenActivity.sSaveToPref, "0");
                    sortByDataPreference = prefs.getBoolean(CameraScreenActivity.sSortByDataPref, false);

                    maxScreenBrightnessPreference = prefs.getBoolean("maxScreenBrightnessPref", false);
                    setScreenBrightness(maxScreenBrightnessPreference);


                    CameraScreenActivity.getGUIManager().onResume();
                    PluginManager.getInstance().onResume();
                    CameraController.onResume();
                    CameraScreenActivity.thiz.mPausing = false;

                    if ((surfaceCreated && (!CameraController.isCameraCreated())) ||
                            (surfaceCreated && CameraScreenActivity.getInstance().getSwitchingMode())) {
                        CameraScreenActivity.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
                        CameraController.setupCamera(surfaceHolder);

                        if (glView != null) {
                            glView.onResume();
                            Log.d("GL", "glView onResume");
                        }
                    }
                    orientListener.enable();
                }
            }.start();

        shutterPlayer = new SoundPlayer(this.getBaseContext(), getResources().openRawResourceFd(
                R.raw.plugin_capture_tick));

        if (screenTimer != null) {
            if (isScreenTimerRunning)
                screenTimer.cancel();
            screenTimer.start();
            isScreenTimerRunning = true;
        }

        long memoryFree = getAvailableInternalMemory();
        if (memoryFree < 30)
            Toast.makeText(CameraScreenActivity.getMainContext(), "Almost no free space left on internal storage.",
                    Toast.LENGTH_LONG).show();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
        boolean dismissKeyguard = prefs.getBoolean("dismissKeyguard", true);
        if (dismissKeyguard)
            getWindow()
                    .addFlags(
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        else
            getWindow()
                    .clearFlags(
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    private long getAvailableInternalMemory() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize / 1048576;
    }

    private void updatePreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
        CameraController.setCameraIndex(!prefs.getBoolean(CameraScreenActivity.sUseFrontCameraPref, false) ? 0 : 1);
        shutterPreference = prefs.getBoolean(CameraScreenActivity.sShutterPref, false);
        shotOnTapPreference = Integer.parseInt(prefs.getString(CameraScreenActivity.sShotOnTapPref, "0"));
        imageSizeIdxPreference = prefs.getString(CameraController.getCameraIndex() == 0 ? CameraScreenActivity.sImageSizeRearPref
                : CameraScreenActivity.sImageSizeFrontPref, "-1");

        keepScreenOn = prefs.getBoolean("keepScreenOn", false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mApplicationStarted = false;

        CameraScreenActivity.getGUIManager().onPause();
        PluginManager.getInstance().onPause(true);

        orientListener.disable();

        if (shutterPreference) {
            AudioManager mgr = (AudioManager) CameraScreenActivity.thiz.getSystemService(CameraScreenActivity.AUDIO_SERVICE);
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        }

        this.mPausing = true;

        this.hideOpenGLLayer();

        if (screenTimer != null) {
            if (isScreenTimerRunning)
                screenTimer.cancel();
            isScreenTimerRunning = false;
        }

        // CameraController.onPause(CameraController.isUseHALv3()? false :
        // switchingMode);
        CameraController.onPause(switchingMode);
        switchingMode = false;

        this.findViewById(R.id.mainLayout2).setVisibility(View.INVISIBLE);

        if (shutterPlayer != null) {
            shutterPlayer.release();
            shutterPlayer = null;
        }
    }

    public void pauseMain() {
        onPause();
    }


    public void resumeMain() {
        onResume();
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {

        if (isCameraConfiguring) {
            PluginManager.getInstance().sendMessage(PluginManager.MSG_SURFACE_CONFIGURED, 0);
            isCameraConfiguring = false;

        } else if (!isCreating) {
            new CountDownTimer(50, 50) {
                public void onTick(long millisUntilFinished) {
                    // Not used
                }

                public void onFinish() {
                    updatePreferences();

                    if (!CameraScreenActivity.thiz.mPausing && surfaceCreated && (!CameraController.isCameraCreated())) {
                        CameraScreenActivity.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
                        Log.d("MainScreen", "surfaceChanged: CameraController.setupCamera(null). SurfaceSize = "
                                + width + "x" + height);
                        CameraController.setupCamera(holder);
                    }
                }
            }.start();
        } else {
            updatePreferences();
        }
    }

    public static int selectImageDimensionMultishot() {
        long maxMem = Runtime.getRuntime().maxMemory() - Debug.getNativeHeapAllocatedSize();
        long maxMpix = (maxMem - 1000000) / 3;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
        int prefIdx = Integer.parseInt(prefs.getString(
                CameraController.getCameraIndex() == 0 ? sImageSizeMultishotBackPref : sImageSizeMultishotFrontPref,
                "-1"));

        // ----- Find max-resolution capture dimensions
        int minMPIX = MIN_MPIX_PREVIEW;

        int defaultCaptureIdx = -1;
        long defaultCaptureMpix = 0;
        long captureMpix = 0;
        int captureIdx = -1;
        boolean prefFound = false;

        // figure default resolution
        for (int ii = 0; ii < CameraController.MultishotResolutionsSizeList.size(); ++ii) {
            CameraController.Size s = CameraController.MultishotResolutionsSizeList.get(ii);
            long mpix = (long) s.getWidth() * s.getHeight();

            if ((mpix >= minMPIX) && (mpix < maxMpix) && (mpix > defaultCaptureMpix)) {
                defaultCaptureIdx = ii;
                defaultCaptureMpix = mpix;
            }
        }

        for (int ii = 0; ii < CameraController.MultishotResolutionsSizeList.size(); ++ii) {
            CameraController.Size s = CameraController.MultishotResolutionsSizeList.get(ii);
            long mpix = (long) s.getWidth() * s.getHeight();

            if ((Integer.valueOf(CameraController.MultishotResolutionsIdxesList.get(ii)) == prefIdx)
                    && (mpix >= minMPIX)) {
                prefFound = true;
                captureIdx = ii;
                break;
            }

            if (mpix > captureMpix) {
                captureIdx = ii;
                captureMpix = mpix;
            }
        }

        if (defaultCaptureMpix > 0 && !prefFound) {
            captureIdx = defaultCaptureIdx;

        }
        return captureIdx;
    }


    @Override
    public void addSurfaceCallback() {
        thiz.surfaceHolder.addCallback(thiz);
    }

    boolean isCameraConfiguring = false;

    @Override
    public void configureCamera() {
        Log.d("MainScreen", "configureCamera()");

        CameraController.updateCameraFeatures();

        PluginManager.getInstance().setCameraPreviewSize();

        Camera.Size sz = CameraController.getCameraParameters().getPreviewSize();

        Log.e("MainScreen", "Viewfinder preview size: " + sz.width + "x" + sz.height);
        guiManager.setupViewfinderPreviewSize(new CameraController.Size(sz.width, sz.height));
        CameraController.allocatePreviewBuffer(sz.width * sz.height
                * ImageFormat.getBitsPerPixel(CameraController.getCameraParameters().getPreviewFormat()) / 8);

        CameraProvider.getInstance().getCamera().setErrorCallback(CameraController.getInstance());

        PluginManager.getInstance().sendMessage(PluginManager.MSG_CAMERA_CONFIGURED, 0);

    }

    private void onCameraConfigured() {
        PluginManager.getInstance().setupCameraParameters();

        Camera.Parameters cp = CameraController.getCameraParameters();

        if (cp != null) {
            previewWidth = cp.getPreviewSize().width;
            previewHeight = cp.getPreviewSize().height;
        }

        try {
            Util.initialize(mainContext);
            Util.initializeMeteringMatrix();
        } catch (Exception e) {
            Log.e("Main setup camera", "Util.initialize failed!");
        }

        prepareMeteringAreas();


        guiManager.onCameraCreate();
        PluginManager.getInstance().onCameraParametersSetup();
        guiManager.onPluginsInitialized();


        // ----- Start preview and setup frame buffer if needed

        // call camera release sequence from onPause somewhere ???
        new CountDownTimer(10, 10) {
            @Override
            public void onFinish() {

                if (!CameraController.isCameraCreated())
                    return;
                // exceptions sometimes happen here when resuming after
                // processing
                try {
                    CameraController.startCameraPreview();
                } catch (RuntimeException e) {
                    Toast.makeText(CameraScreenActivity.thiz, "Unable to start camera", Toast.LENGTH_LONG).show();
                    return;
                }

                CameraProvider.getInstance().getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
                CameraProvider.getInstance().getCamera().addCallbackBuffer(CameraController.getPreviewBuffer());


                PluginManager.getInstance().onCameraSetup();
                guiManager.onCameraSetup();
                CameraScreenActivity.mApplicationStarted = true;

                if (CameraScreenActivity.isForceClose)
                    PluginManager.getInstance().sendMessage(PluginManager.MSG_APPLICATION_STOP, 0);
            }

            @Override
            public void onTick(long millisUntilFinished) {
                // Not used
            }
        }.start();
    }


    private void prepareMeteringAreas() {
        Rect centerRect = Util.convertToDriverCoordinates(new Rect(previewWidth / 4, previewHeight / 4, previewWidth
                - previewWidth / 4, previewHeight - previewHeight / 4));
        Rect topLeftRect = Util.convertToDriverCoordinates(new Rect(0, 0, previewWidth / 2, previewHeight / 2));
        Rect topRightRect = Util.convertToDriverCoordinates(new Rect(previewWidth / 2, 0, previewWidth,
                previewHeight / 2));
        Rect bottomRightRect = Util.convertToDriverCoordinates(new Rect(previewWidth / 2, previewHeight / 2,
                previewWidth, previewHeight));
        Rect bottomLeftRect = Util.convertToDriverCoordinates(new Rect(0, previewHeight / 2, previewWidth / 2,
                previewHeight));
        Rect spotRect = Util.convertToDriverCoordinates(new Rect(previewWidth / 2 - 10, previewHeight / 2 - 10,
                previewWidth / 2 + 10, previewHeight / 2 + 10));

        mMeteringAreaMatrix5.clear();
        mMeteringAreaMatrix5.add(new Area(centerRect, 600));
        mMeteringAreaMatrix5.add(new Area(topLeftRect, 200));
        mMeteringAreaMatrix5.add(new Area(topRightRect, 200));
        mMeteringAreaMatrix5.add(new Area(bottomRightRect, 200));
        mMeteringAreaMatrix5.add(new Area(bottomLeftRect, 200));

        mMeteringAreaMatrix4.clear();
        mMeteringAreaMatrix4.add(new Area(topLeftRect, 250));
        mMeteringAreaMatrix4.add(new Area(topRightRect, 250));
        mMeteringAreaMatrix4.add(new Area(bottomRightRect, 250));
        mMeteringAreaMatrix4.add(new Area(bottomLeftRect, 250));

        mMeteringAreaMatrix1.clear();
        mMeteringAreaMatrix1.add(new Area(centerRect, 1000));

        mMeteringAreaCenter.clear();
        mMeteringAreaCenter.add(new Area(centerRect, 1000));

        mMeteringAreaSpot.clear();
        mMeteringAreaSpot.add(new Area(spotRect, 1000));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        landscapeIsNormal = (rotation == Surface.ROTATION_90) || (rotation == Surface.ROTATION_270);
        surfaceCreated = true;
        Log.d("MainScreen", "SURFACE CREATED");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
    }



    public CameraController.Size getPreviewSize() {
        LayoutParams lp = preview.getLayoutParams();
        if (lp == null)
            return null;

        return new CameraController.Size(lp.width, lp.height);
    }

    public int getSceneIcon(int sceneMode) {
        return guiManager.getSceneIcon(sceneMode);
    }

    public int getWBIcon(int wb) {
        return guiManager.getWBIcon(wb);
    }

    public int getFocusIcon(int focusMode) {
        return guiManager.getFocusIcon(focusMode);
    }

    public int getFlashIcon(int flashMode) {
        return guiManager.getFlashIcon(flashMode);
    }

    public int getISOIcon(int isoMode) {
        return guiManager.getISOIcon(isoMode);
    }

    public void setCameraMeteringMode(int mode) {
        if (CameraParameters.meteringModeAuto == mode)
            CameraController.setCameraMeteringAreas(null);
        else if (CameraParameters.meteringModeMatrix == mode) {
            int maxAreasCount = CameraController.getMaxNumMeteringAreas();
            if (maxAreasCount > 4)
                CameraController.setCameraMeteringAreas(mMeteringAreaMatrix5);
            else if (maxAreasCount > 3)
                CameraController.setCameraMeteringAreas(mMeteringAreaMatrix4);
            else if (maxAreasCount > 0)
                CameraController.setCameraMeteringAreas(mMeteringAreaMatrix1);
            else
                CameraController.setCameraMeteringAreas(null);
        } else if (CameraParameters.meteringModeCenter == mode)
            CameraController.setCameraMeteringAreas(mMeteringAreaCenter);
        else if (CameraParameters.meteringModeSpot == mode)
            CameraController.setCameraMeteringAreas(mMeteringAreaSpot);

        currentMeteringMode = mode;
    }


    public static void setAutoFocusLock(boolean locked) {
        mAFLocked = locked;
    }

    public static boolean getAutoFocusLock() {
        return mAFLocked;
    }

    @Override
    public boolean onKeyDown(int keyCode, @NotNull KeyEvent event) {
        if (!mApplicationStarted)
            return true;

        // menu button processing
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            menuButtonPressed();
            return true;
        }
        // shutter/camera button processing
        if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            CameraScreenActivity.getGUIManager().onHardwareShutterButtonPressed();
            return true;
        }
        // focus/half-press button processing
        if (keyCode == KeyEvent.KEYCODE_FOCUS) {
            if (event.getDownTime() == event.getEventTime()) {
                CameraScreenActivity.getGUIManager().onHardwareFocusButtonPressed();
            }
            return true;
        }

        // check if Headset Hook button has some functions except standard
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
            boolean headsetFunc = prefs.getBoolean("headsetPrefCommon", false);
            if (headsetFunc) {
                CameraScreenActivity.getGUIManager().onHardwareFocusButtonPressed();
                CameraScreenActivity.getGUIManager().onHardwareShutterButtonPressed();
                return true;
            }
        }

        // check if volume button has some functions except Zoom-ing
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
            int buttonFunc = Integer.parseInt(prefs.getString(CameraScreenActivity.sVolumeButtonPref, "0"));
            if (buttonFunc == VOLUME_FUNC_SHUTTER) {
                CameraScreenActivity.getGUIManager().onHardwareFocusButtonPressed();
                CameraScreenActivity.getGUIManager().onHardwareShutterButtonPressed();
                return true;
            } else if (buttonFunc == VOLUME_FUNC_EXPO) {
                CameraScreenActivity.getGUIManager().onVolumeBtnExpo(keyCode);
                return true;
            } else if (buttonFunc == VOLUME_FUNC_NONE)
                return true;
        }

        return PluginManager.getInstance().onKeyDown(true, keyCode, event) || guiManager.onKeyDown(true, keyCode, event) || super.onKeyDown(keyCode, event);

    }

    @Override
    public void onClick(View v) {
        if (mApplicationStarted)
            CameraScreenActivity.getGUIManager().onClick(v);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        return !mApplicationStarted || CameraScreenActivity.getGUIManager().onTouch(view, event);
    }

    public void onButtonClick(View v) {
        CameraScreenActivity.getGUIManager().onButtonClick(v);
    }

    @Override
    public void onShutter() {
        PluginManager.getInstance().onShutter();
    }


    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case PluginManager.MSG_APPLICATION_STOP:
                this.setResult(RESULT_OK);
                this.finish();
                break;
            case MSG_RETURN_CAPTURED:
                this.setResult(RESULT_OK);
                this.finish();
                break;
            case PluginManager.MSG_CAMERA_CONFIGURED:
                onCameraConfigured();
                break;
            case PluginManager.MSG_CAMERA_READY: {
                if (CameraController.isCameraCreated()) {
                    configureCamera();
                    PluginManager.getInstance().onGUICreate();
                    CameraScreenActivity.getGUIManager().onGUICreate();
                }
            }
            break;
            case PluginManager.MSG_CAMERA_OPENED:
                if (mCameraStarted)
                    break;
            case PluginManager.MSG_SURFACE_READY: {
                if (surfaceCreated) {
                    configureCamera();

                    PluginManager.getInstance().onGUICreate();
                    CameraScreenActivity.getGUIManager().onGUICreate();
                    mCameraStarted = true;
                }
            }
            break;
            case PluginManager.MSG_SURFACE_CONFIGURED: {
                PluginManager.getInstance().onGUICreate();
                CameraScreenActivity.getGUIManager().onGUICreate();
                mCameraStarted = true;
            }
            break;
            case PluginManager.MSG_CAMERA_STOPED:
                mCameraStarted = false;
                break;
            default:
                PluginManager.getInstance().handleMessage(msg);
                break;
        }

        return true;
    }

    public void menuButtonPressed() {
        //PluginManager.getInstance().menuButtonPressed();
    }

    public void showOpenGLLayer(final int version) {
        if (glView == null) {
            glView = new GLLayer(CameraScreenActivity.getMainContext(), version);
            LayoutParams params = CameraScreenActivity.getPreviewSurfaceView().getLayoutParams();
            glView.setLayoutParams(params);
            ((RelativeLayout) this.findViewById(R.id.mainLayout2)).addView(glView, 0);
            preview.bringToFront();
            glView.setZOrderMediaOverlay(true);
            glView.onResume();
        }
    }

    public void hideOpenGLLayer() {
        if (glView != null) {
            // preview.getHolder().getSurface().lockCanvas(null).drawColor(Color.BLACK);
            glView.onPause();
            glView.destroyDrawingCache();
            ((RelativeLayout) this.findViewById(R.id.mainLayout2)).removeView(glView);
            glView = null;
        }
    }

    public void playShutter() {
        if (!CameraScreenActivity.isShutterSoundEnabled()) {
            if (shutterPlayer != null)
                shutterPlayer.play();
        }
    }

    public void muteShutter(boolean mute) {
        if (CameraScreenActivity.isShutterSoundEnabled()) {
            AudioManager mgr = (AudioManager) CameraScreenActivity.thiz.getSystemService(CameraScreenActivity.AUDIO_SERVICE);
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
        }
    }

    public static int getPreviewWidth() {
        return thiz.previewWidth;
    }

    public static void setPreviewWidth(int iWidth) {
        thiz.previewWidth = iWidth;
    }

    public static int getPreviewHeight() {
        return thiz.previewHeight;
    }

    public static void setPreviewHeight(int iHeight) {
        thiz.previewHeight = iHeight;
    }

    public static boolean getWantLandscapePhoto() {
        return wantLandscapePhoto;
    }

    public void setScreenBrightness(boolean setMax) {
        Window window = getWindow();
        WindowManager.LayoutParams layoutpars = window.getAttributes();

        // Set the brightness of this window
        if (setMax)
            layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        else
            layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

        // Apply attribute changes to this window
        window.setAttributes(layoutpars);
    }

    public static Resources getAppResources() {
        return CameraScreenActivity.thiz.getResources();
    }

    private void resetOrSaveSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
        Editor prefsEditor = prefs.edit();
        boolean isSaving = prefs.getBoolean("SaveConfiguration_Mode", true);
        if (!isSaving) {
            prefsEditor.putString("defaultModeName", "single");
            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_ImageSize", true);
        if (!isSaving) {
            // general settings - image size
            prefsEditor.putString(sImageSizeRearPref, "-1");
            prefsEditor.putString(sImageSizeFrontPref, "-1");

            // multishot and night
            prefsEditor.putString(sImageSizeMultishotBackPref, "-1");
            prefsEditor.putString(sImageSizeMultishotFrontPref, "-1");

            // panorama
            prefsEditor.remove(sImageSizePanoramaBackPref);
            prefsEditor.remove(sImageSizePanoramaFrontPref);

            // video
            prefsEditor.putString(sImageSizeVideoBackPref, "-1");
            prefsEditor.putString(sImageSizeVideoFrontPref, "-1");

            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_SceneMode", false);
        if (!isSaving) {
            prefsEditor.putInt(sSceneModePref, sDefaultValue);
            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_FocusMode", true);
        if (!isSaving) {
            prefsEditor.putInt(sRearFocusModePref, sDefaultFocusValue);
            prefsEditor.putInt(sFrontFocusModePref, sDefaultFocusValue);
            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_WBMode", false);
        if (!isSaving) {
            prefsEditor.putInt(sWBModePref, sDefaultValue);
            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_ISOMode", false);
        if (!isSaving) {
            prefsEditor.putInt(sISOPref, sDefaultValue);
            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_FlashMode", true);
        if (!isSaving) {
            prefsEditor.putInt(sFlashModePref, sDefaultValue);
            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_FrontRearCamera", true);
        if (!isSaving) {
            prefsEditor.putBoolean(sUseFrontCameraPref, false);
            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_ExpoCompensation", false);
        if (!isSaving) {
            prefsEditor.putInt(CameraScreenActivity.sEvPref, 0);
            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_DelayedCapture", false);
        if (!isSaving) {
            prefsEditor.putInt(CameraScreenActivity.sDelayedCapturePref, 0);
            prefsEditor.putBoolean(CameraScreenActivity.sSWCheckedPref, false);
            prefsEditor.putBoolean(CameraScreenActivity.sDelayedFlashPref, false);
            prefsEditor.putBoolean(CameraScreenActivity.sDelayedSoundPref, false);
            prefsEditor.putInt(CameraScreenActivity.sDelayedCaptureIntervalPref, 0);

            prefsEditor.apply();
        }

        isSaving = prefs.getBoolean("SaveConfiguration_TimelapseCapture", false);
        if (!isSaving && !prefs.getBoolean(sPhotoTimeLapseIsRunningPref, false)) {
            prefsEditor.putInt(CameraScreenActivity.sPhotoTimeLapseCaptureIntervalPref, 0);
            prefsEditor.putInt(CameraScreenActivity.sPhotoTimeLapseCaptureIntervalMeasurmentPref, 0);
            prefsEditor.putBoolean(CameraScreenActivity.sPhotoTimeLapseIsRunningPref, false);
            prefsEditor.putBoolean(CameraScreenActivity.sPhotoTimeLapseActivePref, false);

            prefsEditor.apply();
        }
    }

    public void switchingMode(boolean isModeSwitching) {
        switchingMode = isModeSwitching;
    }

    public boolean getSwitchingMode() {
        return switchingMode;
    }
}
