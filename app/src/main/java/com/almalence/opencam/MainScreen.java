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

/* <!-- +++
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam;

//-+- -->

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaPlayer;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.util.Util;

import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.AlmalenceGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;


/***
 * MainScreen - main activity screen with camera functionality
 * 
 * Passes all main events to PluginManager
 ***/

@SuppressWarnings("deprecation")
public class MainScreen extends Activity implements ApplicationInterface, View.OnClickListener, View.OnTouchListener,
		SurfaceHolder.Callback, Handler.Callback, Camera.ShutterCallback
{
	// >>Description
	// section with different global parameters available for everyone
	//
	// Camera parameters and possibly access to camera instance
	//
	// Global defines and others
	//
	// Description<<

	private static final int			MSG_RETURN_CAPTURED				= -1;

	private static final int			MODE_GENERAL					= 0;
	private static final int			MODE_SMART_MULTISHOT_AND_NIGHT	= 1;
	private static final int			MODE_PANORAMA					= 2;
	private static final int			MODE_VIDEO						= 3;

	private static final int			MIN_MPIX_SUPPORTED				= 1280 * 960;
	private static final int			MIN_MPIX_PREVIEW				= 600 * 400;
	private static final long			MPIX_8							= 3504 * 2336;

	public static MainScreen			thiz;
	public Context						mainContext;
	private Handler						messageHandler;

	// Interface to HALv3 camera and Old style camera
	private CameraController			cameraController				= null;

	// HALv3 camera's objects
	private ImageReader					mImageReaderPreviewYUV;
	private ImageReader					mImageReaderYUV;
	private ImageReader					mImageReaderJPEG;
	private ImageReader					mImageReaderRAW;

	private int							captureFormat					= CameraController.JPEG;

	public GUI							guiManager						= null;

	// OpenGL layer. May be used to allow capture plugins to draw overlaying
	// preview, such as night vision or panorama frames.
	private GLLayer						glView;

	private boolean						mPausing						= false;

	private File						forceFilename					= null;
	private Uri							forceFilenameUri;

	private SurfaceHolder				surfaceHolder;
	private SurfaceView					preview;
	private Surface						mCameraSurface					= null;
	private OrientationEventListener	orientListener;
	private boolean						landscapeIsNormal				= false;
	private boolean						surfaceCreated					= false;

	private int							surfaceWidth					= 0;
	private int							surfaceHeight					= 0;

	// shared between activities
	// private int imageWidth, imageHeight;
	private int							previewWidth, previewHeight;

	private CountDownTimer				screenTimer						= null;
	private boolean						isScreenTimerRunning			= false;

	private static boolean				wantLandscapePhoto				= false;
	private int							orientationMain					= 0;
	private int							orientationMainPrevious			= 0;

	private SoundPlayer					shutterPlayer					= null;

	// Common preferences
	private String						imageSizeIdxPreference;
	private String						multishotImageSizeIdxPreference;
	private boolean						shutterPreference				= true;
	private int							shotOnTapPreference				= 0;

	private boolean						showHelp						= false;

	private boolean						keepScreenOn					= false;

	private String						saveToPath;
	private String						saveToPreference;
	private boolean						sortByDataPreference;

	private boolean						captureRAW;

	private List<Surface>				surfaceList;

	private static boolean				maxScreenBrightnessPreference;

	private static boolean				mAFLocked						= false;

	// shows if mode is currently switching
	private boolean						switchingMode					= false;

	// >>Description
	// section with initialize, resume, start, stop procedures, preferences
	// access
	//
	// Initialize, stop etc depends on plugin type.
	//
	// Create main GUI controls and plugin specific controls.
	//
	// Description<<

	private static boolean				isCreating						= false;
	private static boolean				mApplicationStarted				= false;
	private static boolean				mCameraStarted					= false;
	private static boolean				isForceClose					= false;

	// Clicked mode id from widget.
	public static final String			EXTRA_ITEM						= "WidgetModeID";

	public static final String			EXTRA_TORCH						= "WidgetTorchMode";
	public static final String			EXTRA_BARCODE					= "WidgetBarcodeMode";
	public static final String			EXTRA_SHOP						= "WidgetGoShopping";

	private static boolean				launchTorch						= false;
	private static boolean				launchBarcode					= false;
	private static boolean				goShopping						= false;

	private static int					prefFlash						= -1;
	private static boolean				prefBarcode						= false;

	private static final int			VOLUME_FUNC_SHUTTER				= 0;
	private static final int			VOLUME_FUNC_EXPO				= 2;
	private static final int			VOLUME_FUNC_NONE				= 3;

	private static List<Area>			mMeteringAreaMatrix5			= new ArrayList<Area>();
	private static List<Area>			mMeteringAreaMatrix4			= new ArrayList<Area>();
	private static List<Area>			mMeteringAreaMatrix1			= new ArrayList<Area>();
	private static List<Area>			mMeteringAreaCenter				= new ArrayList<Area>();
	private static List<Area>			mMeteringAreaSpot				= new ArrayList<Area>();

	private int							currentMeteringMode				= -1;

	public static String				sTimestampDate;
	public static String				sTimestampAbbreviation;
	public static String				sTimestampTime;
	public static String				sTimestampSeparator;
	public static String				sTimestampCustomText;
	public static String				sTimestampColor;
	public static String				sTimestampFontSize;

	public static String				sEvPref;
	public static String				sSceneModePref;
	public static String				sWBModePref;
	public static String				sFrontFocusModePref;
	public static String				sRearFocusModePref;
	public static String				sFlashModePref;
	public static String				sISOPref;
	public static String				sMeteringModePref;

	public static String				sDelayedCapturePref;
	public static String				sShowDelayedCapturePref;
	public static String				sDelayedSoundPref;
	public static String				sDelayedFlashPref;
	public static String				sDelayedCaptureIntervalPref;

	public static String				sPhotoTimeLapseCaptureIntervalPref;
	public static String				sPhotoTimeLapseCaptureIntervalMeasurmentPref;
	public static String				sPhotoTimeLapseActivePref;
	public static String				sPhotoTimeLapseIsRunningPref;
	public static String				sPhotoTimeLapseCount;

	public static String				sUseFrontCameraPref;
	private static String				sShutterPref;
	private static String				sShotOnTapPref;
	private static String				sVolumeButtonPref;

	public static String				sImageSizeRearPref;
	public static String				sImageSizeFrontPref;

	public static String				sImageSizeMultishotBackPref;
	public static String				sImageSizeMultishotFrontPref;

	public static String				sImageSizePanoramaBackPref;
	public static String				sImageSizePanoramaFrontPref;

	public static String				sImageSizeVideoBackPref;
	public static String				sImageSizeVideoFrontPref;

	public static String				sCaptureRAWPref;

	public static String				sInitModeListPref				= "initModeListPref";

	public static String				sJPEGQualityPref;

	public static String				sDefaultInfoSetPref;
	public static String				sSWCheckedPref;
	public static String				sSavePathPref;
	public static String				sExportNamePref;
	public static String				sExportNamePrefixPref;
	public static String				sExportNamePostfixPref;
	public static String				sSaveToPref;
	public static String				sSortByDataPref;
	public static String				sEnableExifOrientationTagPref;
	public static String				sAdditionalRotationPref;

	public static String				sExpoPreviewModePref;

	public static String				sDefaultModeName;

	public static int					sDefaultValue					= CameraParameters.SCENE_MODE_AUTO;
	public static int					sDefaultFocusValue				= CameraParameters.AF_MODE_CONTINUOUS_PICTURE;
	public static int					sDefaultFlashValue				= CameraParameters.FLASH_MODE_OFF;
	public static int					sDefaultMeteringValue			= CameraParameters.meteringModeAuto;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
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
		goShopping = intent.getBooleanExtra(EXTRA_SHOP, false);

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

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		if (null != mode)
			prefs.edit().putString("defaultModeName", mode).commit();

		if (launchTorch)
		{
			prefFlash = prefs.getInt(sFlashModePref, CameraParameters.FLASH_MODE_AUTO);
			prefs.edit().putInt(sFlashModePref, CameraParameters.FLASH_MODE_TORCH).commit();
		}

		if (launchBarcode)
		{
			prefBarcode = prefs.getBoolean("PrefBarcodescannerVF", false);
			prefs.edit().putBoolean("PrefBarcodescannerVF", true).commit();
		}

		try
		{
			cameraController = CameraController.getInstance();
		} catch (VerifyError exp)
		{
			Log.e("MainScreen", exp.getMessage());
		}
		CameraController.onCreate(MainScreen.thiz, MainScreen.thiz, PluginManager.getInstance());

		keepScreenOn = prefs.getBoolean("keepScreenOn", false);

		// set preview, on click listener and surface buffers
		preview = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		preview.setOnClickListener(this);
		preview.setOnTouchListener(this);
		preview.setKeepScreenOn(keepScreenOn);

		surfaceHolder = preview.getHolder();
		// CameraController.setSurfaceHolderFixedSize(1280, 720);
		// surfaceHolder.setFixedSize(0, 0);
		// Log.e("MainScreen", "onCreate. surfaceHolder.addCallback(this)");
		surfaceHolder.addCallback(this);

		orientListener = new OrientationEventListener(this)
		{
			@Override
			public void onOrientationChanged(int orientation)
			{
				// figure landscape or portrait
				if (MainScreen.thiz.landscapeIsNormal)
				{
					orientation += 90;
				}

				if ((orientation < 45) || (orientation > 315 && orientation < 405)
						|| ((orientation > 135) && (orientation < 225)))
				{
					if (MainScreen.wantLandscapePhoto)
					{
						MainScreen.wantLandscapePhoto = false;
					}
				} else
				{
					if (!MainScreen.wantLandscapePhoto)
					{
						MainScreen.wantLandscapePhoto = true;
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

				if (orientationMain != orientationMainPrevious)
				{
					orientationMainPrevious = orientationMain;
				}
			}
		};

		// prevent power drain
		screenTimer = new CountDownTimer(180000, 180000)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				boolean isVideoRecording = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext())
						.getBoolean("videorecording", false);
				if (isVideoRecording || keepScreenOn)
				{
					// restart timer
					screenTimer.start();
					isScreenTimerRunning = true;
					preview.setKeepScreenOn(true);
					return;
				}
				preview.setKeepScreenOn(keepScreenOn);
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

		if (this.getIntent().getAction() != null)
		{
			if (this.getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE))
			{
				try
				{
					forceFilenameUri = this.getIntent().getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
					MainScreen.setForceFilename(new File(((Uri) forceFilenameUri).getPath()));
					if (MainScreen.getForceFilename().getAbsolutePath().equals("/scrapSpace"))
					{
						MainScreen.setForceFilename(new File(Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/mms/scrapSpace/.temp.jpg"));
						new File(MainScreen.getForceFilename().getParent()).mkdirs();
					}
				} catch (Exception e)
				{
					MainScreen.setForceFilename(null);
				}
			} else
			{
				MainScreen.setForceFilename(null);
			}
		} else
		{
			MainScreen.setForceFilename(null);
		}

		// <!-- -+-
		if (goShopping)
		{
			if (MainScreen.thiz.titleUnlockAll == null || MainScreen.thiz.titleUnlockAll.endsWith("check for sale"))
			{
				Toast.makeText(MainScreen.getMainContext(),
						"Error connecting to Google Play. Check internet connection.", Toast.LENGTH_LONG).show();
				return;
			}
			guiManager.showStore();
		}
		// -+- -->
	}

	/*
	 * Get/Set method for private variables
	 */
	public static MainScreen getInstance()
	{
		return thiz;
	}

	public static Context getMainContext()
	{
		return thiz.mainContext;
	}

	public static Handler getMessageHandler()
	{
		return thiz.messageHandler;
	}

	public static CameraController getCameraController()
	{
		return thiz.cameraController;
	}

	public static GUI getGUIManager()
	{
		return thiz.guiManager;
	}

	@TargetApi(21)
	public static void createImageReaders()
	{
		Log.e("MainScreen", "createImageReaders");
		// ImageReader for preview frames in YUV format
		thiz.mImageReaderPreviewYUV = ImageReader.newInstance(thiz.previewWidth, thiz.previewHeight,
				ImageFormat.YUV_420_888, 2);
		// thiz.mImageReaderPreviewYUV = ImageReader.newInstance(1280, 960,
		// ImageFormat.YUV_420_888, 1);

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		// ImageReader for YUV still images
		thiz.mImageReaderYUV = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(),
				ImageFormat.YUV_420_888, 2);

		// ImageReader for JPEG still images
		if (getCaptureFormat() == CameraController.RAW)
		{
			CameraController.Size imageSizeJPEG = CameraController.getMaxCameraImageSize(CameraController.JPEG);
			thiz.mImageReaderJPEG = ImageReader.newInstance(imageSizeJPEG.getWidth(), imageSizeJPEG.getHeight(),
					ImageFormat.JPEG, 2);
		} else
			thiz.mImageReaderJPEG = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(),
					ImageFormat.JPEG, 2);

		// ImageReader for RAW still images
		thiz.mImageReaderRAW = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(),
				ImageFormat.RAW_SENSOR, 2);
		
		thiz.guiManager.setupViewfinderPreviewSize(new CameraController.Size(thiz.previewWidth, thiz.previewHeight));

	}

	public static ImageReader getPreviewYUVImageReader()
	{
		return thiz.mImageReaderPreviewYUV;
	}

	public static ImageReader getYUVImageReader()
	{
		return thiz.mImageReaderYUV;
	}

	public static ImageReader getJPEGImageReader()
	{
		return thiz.mImageReaderJPEG;
	}

	public static ImageReader getRAWImageReader()
	{
		return thiz.mImageReaderRAW;
	}

	public static int getCaptureFormat()
	{
		return thiz.captureFormat;
	}

	public static void setCaptureFormat(int capture)
	{
		thiz.captureFormat = capture;
	}

	public static File getForceFilename()
	{
		return thiz.forceFilename;
	}

	public static void setForceFilename(File fileName)
	{
		thiz.forceFilename = fileName;
	}

	public static Uri getForceFilenameURI()
	{
		return thiz.forceFilenameUri;
	}

	public static SurfaceHolder getPreviewSurfaceHolder()
	{
		return thiz.surfaceHolder;
	}

	public static SurfaceView getPreviewSurfaceView()
	{
		return thiz.preview;
	}

	public static void setSurfaceHolderSize(int width, int height)
	{
		if (thiz.surfaceHolder != null)
		{
			Log.e("MainScreen", "setSurfaceHolderSize = " + width + "x" + height);
			thiz.surfaceWidth = width;
			thiz.surfaceHeight = height;
			thiz.surfaceHolder.setFixedSize(width, height);
			// thiz.surfaceWidth = 1280;
			// thiz.surfaceHeight = 720;
			// thiz.surfaceHolder.setFixedSize(1280, 720);
		}
	}

	public static int getOrientation()
	{
		return thiz.orientationMain;
	}

	public static String getImageSizeIndex()
	{
		return thiz.imageSizeIdxPreference;
	}

	public static String getMultishotImageSizeIndex()
	{
		return thiz.multishotImageSizeIdxPreference;
	}

	public static boolean isShutterSoundEnabled()
	{
		return thiz.shutterPreference;
	}

	public static int isShotOnTap()
	{
		return thiz.shotOnTapPreference;
	}

	public static boolean isShowHelp()
	{
		return thiz.showHelp;
	}

	public static void setShowHelp(boolean show)
	{
		thiz.showHelp = show;
	}

	public static String getSaveToPath()
	{
		return thiz.saveToPath;
	}

	public static String getSaveTo()
	{
		return thiz.saveToPreference;
	}

	public static boolean isSortByData()
	{
		return thiz.sortByDataPreference;
	}

	public static int getMeteringMode()
	{
		return thiz.currentMeteringMode;
	}

	/*
	 * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ Get/Set method for private variables
	 */

	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		setImageSizeOptions(prefActivity, MODE_GENERAL);
		setImageSizeOptions(prefActivity, MODE_SMART_MULTISHOT_AND_NIGHT);
		setImageSizeOptions(prefActivity, MODE_PANORAMA);
		setImageSizeOptions(prefActivity, MODE_VIDEO);
	}

	private void setImageSizeOptions(PreferenceFragment prefActivity, int mode)
	{
		CharSequence[] entries = null;
		CharSequence[] entryValues = null;

		int idx = 0;
		int currentIdx = -1;
		String opt1 = "";
		String opt2 = "";

		if (mode == MODE_GENERAL)
		{
			opt1 = sImageSizeRearPref;
			opt2 = sImageSizeFrontPref;
			currentIdx = Integer.parseInt(MainScreen.getImageSizeIndex());

			if (currentIdx == -1)
			{
				currentIdx = 0;
			}

			entries = CameraController.getResolutionsNamesList().toArray(
					new CharSequence[CameraController.getResolutionsNamesList().size()]);
			entryValues = CameraController.getResolutionsIdxesList().toArray(
					new CharSequence[CameraController.getResolutionsIdxesList().size()]);
		} else if (mode == MODE_SMART_MULTISHOT_AND_NIGHT)
		{
			opt1 = sImageSizeMultishotBackPref;
			opt2 = sImageSizeMultishotFrontPref;
			currentIdx = Integer.parseInt(CameraController.MultishotResolutionsIdxesList.get(MainScreen
					.selectImageDimensionMultishot()));
			entries = CameraController.MultishotResolutionsNamesList
					.toArray(new CharSequence[CameraController.MultishotResolutionsNamesList.size()]);
			entryValues = CameraController.MultishotResolutionsIdxesList
					.toArray(new CharSequence[CameraController.MultishotResolutionsIdxesList.size()]);
		} else if (mode == MODE_VIDEO)
		{
			opt1 = sImageSizeVideoBackPref;
			opt2 = sImageSizeVideoFrontPref;

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			currentIdx = Integer.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? opt1 : opt2, "2"));

			CharSequence[] entriesTmp = new CharSequence[6];
			CharSequence[] entryValuesTmp = new CharSequence[6];

			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_1080P))
			{
				entriesTmp[idx] = "1080p";
				entryValuesTmp[idx] = "2";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_720P))
			{
				entriesTmp[idx] = "720p";
				entryValuesTmp[idx] = "3";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_480P))
			{
				entriesTmp[idx] = "480p";
				entryValuesTmp[idx] = "4";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_CIF))
			{
				entriesTmp[idx] = "352 x 288";
				entryValuesTmp[idx] = "1";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_QCIF))
			{
				entriesTmp[idx] = "176 x 144";
				entryValuesTmp[idx] = "0";
				idx++;
			}

			entries = new CharSequence[idx];
			entryValues = new CharSequence[idx];

			for (int i = 0; i < idx; i++)
			{
				entries[i] = entriesTmp[i];
				entryValues[i] = entryValuesTmp[i];
			}
		}

		if (CameraController.getResolutionsIdxesList() != null)
		{
			ListPreference lp = (ListPreference) prefActivity.findPreference(opt1);
			ListPreference lp2 = (ListPreference) prefActivity.findPreference(opt2);

			if (CameraController.getCameraIndex() == 0 && lp2 != null)
				prefActivity.getPreferenceScreen().removePreference(lp2);
			else if (lp != null && lp2 != null)
			{
				prefActivity.getPreferenceScreen().removePreference(lp);
				lp = lp2;
			}
			if (lp != null)
			{
				lp.setEntries(entries);
				lp.setEntryValues(entryValues);

				if (currentIdx != -1)
				{
					// set currently selected image size
					for (idx = 0; idx < entryValues.length; ++idx)
					{
						if (Integer.valueOf(entryValues[idx].toString()) == currentIdx)
						{
							lp.setValueIndex(idx);
							break;
						}
					}
				} else
				{
					lp.setValueIndex(0);
				}

				if (mode == MODE_GENERAL)
				{
					lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
					{
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							thiz.imageSizeIdxPreference = newValue.toString();
							CameraController.setCameraImageSizeIndex(Integer.parseInt(newValue.toString()), false);
							return true;
						}
					});
				}

			}
		}

	}

	public void onAdvancePreferenceCreate(PreferenceFragment prefActivity)
	{
		CheckBoxPreference cp = (CheckBoxPreference) prefActivity.findPreference(getResources().getString(
				R.string.Preference_UseHALv3Key));
		final CheckBoxPreference fp = (CheckBoxPreference) prefActivity.findPreference(MainScreen.sCaptureRAWPref);

		if (cp != null)
		{
				cp.setEnabled(false);


			cp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				public boolean onPreferenceChange(Preference preference, Object useCamera2)
				{
					PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
							.putBoolean(MainScreen.sInitModeListPref, true).commit();

					boolean new_value = Boolean.parseBoolean(useCamera2.toString());
					if (new_value)
					{
						if (fp != null && CameraController.isRAWCaptureSupported())
							fp.setEnabled(true);
						else
							fp.setEnabled(false);
					} else if (fp != null)
					{
						PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
								.putBoolean(MainScreen.sCaptureRAWPref, false).commit();
						fp.setEnabled(false);
					}

					return true;
				}
			});
		}

		final PreferenceFragment mPref = prefActivity;

		if (fp != null)
		{
			fp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				public boolean onPreferenceChange(Preference preference, Object captureRAW)
				{
					boolean new_value = Boolean.parseBoolean(captureRAW.toString());
					if (new_value)
					{
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

	public void glSetRenderingMode(final int renderMode)
	{
		if (renderMode != GLSurfaceView.RENDERMODE_WHEN_DIRTY && renderMode != GLSurfaceView.RENDERMODE_CONTINUOUSLY)
		{
			throw new IllegalArgumentException();
		}

		final GLSurfaceView surfaceView = glView;
		if (surfaceView != null)
		{
			surfaceView.setRenderMode(renderMode);
		}
	}

	public void glRequestRender()
	{
		final GLSurfaceView surfaceView = glView;
		if (surfaceView != null)
		{
			surfaceView.requestRender();
		}
	}

	public void queueGLEvent(final Runnable runnable)
	{
		final GLSurfaceView surfaceView = glView;

		if (surfaceView != null && runnable != null)
		{
			surfaceView.queueEvent(runnable);
		}
	}

	public int glGetPreviewTexture()
	{
		return glView.getPreviewTexture();
	}

	public SurfaceTexture glGetSurfaceTexture()
	{
		return glView.getSurfaceTexture();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		CameraController.onStart();
		MainScreen.getGUIManager().onStart();
		PluginManager.getInstance().onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		switchingMode = false;
		mApplicationStarted = false;
		orientationMain = 0;
		orientationMainPrevious = 0;
		MainScreen.getGUIManager().onStop();
		PluginManager.getInstance().onStop();
		CameraController.onStop();


	}

	@TargetApi(21)
	private void stopImageReaders()
	{
		// IamgeReader should be closed
		if (mImageReaderPreviewYUV != null)
		{
			mImageReaderPreviewYUV.close();
			mImageReaderPreviewYUV = null;
		}
		if (mImageReaderYUV != null)
		{
			mImageReaderYUV.close();
			mImageReaderYUV = null;
		}
		if (mImageReaderJPEG != null)
		{
			mImageReaderJPEG.close();
			mImageReaderJPEG = null;
		}
		if (mImageReaderRAW != null)
		{
			mImageReaderRAW.close();
			mImageReaderRAW = null;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		if (launchTorch && prefs.getInt(sFlashModePref, -1) == CameraParameters.FLASH_MODE_TORCH)
		{
			prefs.edit().putInt(sFlashModePref, prefFlash).commit();
		}
		if (launchBarcode && prefs.getBoolean("PrefBarcodescannerVF", false))
		{
			prefs.edit().putBoolean("PrefBarcodescannerVF", prefBarcode).commit();
		}

		prefs.edit().putBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);
		prefs.edit().putBoolean(MainScreen.sPhotoTimeLapseActivePref, false);

		MainScreen.getGUIManager().onDestroy();
		PluginManager.getInstance().onDestroy();
		CameraController.onDestroy();



		this.hideOpenGLLayer();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		isCameraConfiguring = false;

		if (!isCreating)
			new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(MainScreen.getMainContext());

					updatePreferences();

					preview.setKeepScreenOn(keepScreenOn);

					captureFormat = CameraController.JPEG;

					saveToPath = prefs.getString(sSavePathPref, Environment.getExternalStorageDirectory()
							.getAbsolutePath());
					saveToPreference = prefs.getString(MainScreen.sSaveToPref, "0");
					sortByDataPreference = prefs.getBoolean(MainScreen.sSortByDataPref, false);

					maxScreenBrightnessPreference = prefs.getBoolean("maxScreenBrightnessPref", false);
					setScreenBrightness(maxScreenBrightnessPreference);

					captureRAW = prefs.getBoolean(MainScreen.sCaptureRAWPref, false);


					MainScreen.getGUIManager().onResume();
					PluginManager.getInstance().onResume();
					CameraController.onResume();
					MainScreen.thiz.mPausing = false;

					if ((surfaceCreated && (!CameraController.isCameraCreated())) ||
							(surfaceCreated && MainScreen.getInstance().getSwitchingMode()))
					{
						MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						CameraController.setupCamera(surfaceHolder);

						if (glView != null)
						{
							glView.onResume();
							Log.d("GL", "glView onResume");
						}
					}
					orientListener.enable();
				}
			}.start();

		shutterPlayer = new SoundPlayer(this.getBaseContext(), getResources().openRawResourceFd(
				R.raw.plugin_capture_tick));

		if (screenTimer != null)
		{
			if (isScreenTimerRunning)
				screenTimer.cancel();
			screenTimer.start();
			isScreenTimerRunning = true;
		}

		long memoryFree = getAvailableInternalMemory();
		if (memoryFree < 30)
			Toast.makeText(MainScreen.getMainContext(), "Almost no free space left on internal storage.",
					Toast.LENGTH_LONG).show();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
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

	private long getAvailableInternalMemory()
	{
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize / 1048576;
	}

	private void updatePreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		CameraController.setCameraIndex(!prefs.getBoolean(MainScreen.sUseFrontCameraPref, false) ? 0 : 1);
		shutterPreference = prefs.getBoolean(MainScreen.sShutterPref, false);
		shotOnTapPreference = Integer.parseInt(prefs.getString(MainScreen.sShotOnTapPref, "0"));
		imageSizeIdxPreference = prefs.getString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeRearPref
				: MainScreen.sImageSizeFrontPref, "-1");

		multishotImageSizeIdxPreference = prefs.getString(
				CameraController.getCameraIndex() == 0 ? sImageSizeMultishotBackPref : sImageSizeMultishotFrontPref,
				"-1");

		keepScreenOn = prefs.getBoolean("keepScreenOn", false);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mApplicationStarted = false;

		MainScreen.getGUIManager().onPause();
		PluginManager.getInstance().onPause(true);

		orientListener.disable();

		if (shutterPreference)
		{
			AudioManager mgr = (AudioManager) MainScreen.thiz.getSystemService(MainScreen.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
		}

		this.mPausing = true;

		this.hideOpenGLLayer();

		if (screenTimer != null)
		{
			if (isScreenTimerRunning)
				screenTimer.cancel();
			isScreenTimerRunning = false;
		}

		// CameraController.onPause(CameraController.isUseHALv3()? false :
		// switchingMode);
		CameraController.onPause(switchingMode);
		switchingMode = false;

		this.findViewById(R.id.mainLayout2).setVisibility(View.INVISIBLE);

		if (shutterPlayer != null)
		{
			shutterPlayer.release();
			shutterPlayer = null;
		}
	}

	public void pauseMain()
	{
		onPause();
	}

	public void stopMain()
	{
		onStop();
	}

	public void startMain()
	{
		onStart();
	}

	public void resumeMain()
	{
		onResume();
	}

	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height)
	{
		mCameraSurface = holder.getSurface();

		if (isCameraConfiguring)
		{
			PluginManager.getInstance().sendMessage(PluginManager.MSG_SURFACE_CONFIGURED, 0);
			isCameraConfiguring = false;
			// updatePreferences();
			// MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
			// configureHALv3Camera(captureFormat);
			// messageHandler.sendEmptyMessage(PluginManager.MSG_SURFACE_READY);
		} else if (!isCreating)
		{
			new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					updatePreferences();

					if (!MainScreen.thiz.mPausing && surfaceCreated && (!CameraController.isCameraCreated()))
					{
						MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						Log.d("MainScreen", "surfaceChanged: CameraController.setupCamera(null). SurfaceSize = "
								+ width + "x" + height);
						CameraController.setupCamera(holder);
					}
				}
			}.start();
		} else
		{
			updatePreferences();
		}
	}

	public static int selectImageDimensionMultishot()
	{
		long maxMem = Runtime.getRuntime().maxMemory() - Debug.getNativeHeapAllocatedSize();
		long maxMpix = (maxMem - 1000000) / 3; // 2 x Mpix - result, 1/4 x Mpix
												// x 4 - compressed input jpegs,
												// 1Mb - safe reserve

		// if (maxMpix < MIN_MPIX_SUPPORTED)
		// {
		// String msg;
		// msg = "MainScreen.selectImageDimension maxMem = " + maxMem;
		// // Log.d("MultishotCapturePlugin",
		// // "MainScreen.selectImageDimension maxMpix < MIN_MPIX_SUPPORTED");
		// // Log.d("MultishotCapturePlugin", msg);
		// }

		// find index selected in preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
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
		for (int ii = 0; ii < CameraController.MultishotResolutionsSizeList.size(); ++ii)
		{
			CameraController.Size s = CameraController.MultishotResolutionsSizeList.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((mpix >= minMPIX) && (mpix < maxMpix) && (mpix > defaultCaptureMpix))
			{
				defaultCaptureIdx = ii;
				defaultCaptureMpix = mpix;
			}
		}

		for (int ii = 0; ii < CameraController.MultishotResolutionsSizeList.size(); ++ii)
		{
			CameraController.Size s = CameraController.MultishotResolutionsSizeList.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((Integer.valueOf(CameraController.MultishotResolutionsIdxesList.get(ii)) == prefIdx)
					&& (mpix >= minMPIX))
			{
				prefFound = true;
				captureIdx = ii;
				captureMpix = mpix;
				break;
			}

			if (mpix > captureMpix)
			{
				captureIdx = ii;
				captureMpix = mpix;
			}
		}

		if (defaultCaptureMpix > 0 && !prefFound)
		{
			captureIdx = defaultCaptureIdx;
			captureMpix = defaultCaptureMpix;
		}

		return captureIdx;
	}

	public void onSurfaceChangedMain(final SurfaceHolder holder, final int width, final int height)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		CameraController.setCameraIndex(!prefs.getBoolean(sUseFrontCameraPref, false) ? 0 : 1);

		shutterPreference = prefs.getBoolean(sShutterPref, false);
		shotOnTapPreference = Integer.parseInt(prefs.getString(MainScreen.sShotOnTapPref, "0"));
		imageSizeIdxPreference = prefs.getString(CameraController.getCameraIndex() == 0 ? sImageSizeRearPref
				: sImageSizeFrontPref, "-1");

		if (!MainScreen.thiz.mPausing && surfaceCreated && (!CameraController.isCameraCreated()))
		{
			MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
            Log.d("MainScreen", "surfaceChangedMain: CameraController.setupCamera(null)");
			CameraController.setupCamera(holder);

		}
	}

	@Override
	public void addSurfaceCallback()
	{
		thiz.surfaceHolder.addCallback(thiz);
	}

	boolean	isCameraConfiguring	= false;

	@Override
	public void configureCamera()
	{
		Log.d("MainScreen", "configureCamera()");

		CameraController.updateCameraFeatures();

		// ----- Select preview dimensions with ratio correspondent to
		// full-size image
		PluginManager.getInstance().setCameraPreviewSize();
		// prepare list of surfaces to be used in capture requests

			Camera.Size sz = CameraController.getCameraParameters().getPreviewSize();

			Log.e("MainScreen", "Viewfinder preview size: " + sz.width + "x" + sz.height);
			guiManager.setupViewfinderPreviewSize(new CameraController.Size(sz.width, sz.height));
			CameraController.allocatePreviewBuffer(sz.width * sz.height
					* ImageFormat.getBitsPerPixel(CameraController.getCameraParameters().getPreviewFormat()) / 8);

			CameraController.getCamera().setErrorCallback(CameraController.getInstance());

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAMERA_CONFIGURED, 0);

	}

	private void onCameraConfigured()
	{
		PluginManager.getInstance().setupCameraParameters();

		Camera.Parameters cp = CameraController.getCameraParameters();

			try
			{
				// Nexus 5 is giving preview which is too dark without this
				if (Build.MODEL.contains("Nexus 5"))
				{
					cp.setPreviewFpsRange(7000, 30000);
					CameraController.setCameraParameters(cp);
					cp = CameraController.getCameraParameters();
				}
			} catch (RuntimeException e)
			{
				Log.d("MainScreen", "MainScreen.setupCamera unable setParameters " + e.getMessage());
			}

			if (cp != null)
			{
				previewWidth = cp.getPreviewSize().width;
				previewHeight = cp.getPreviewSize().height;
			}

		try
		{
			Util.initialize(mainContext);
			Util.initializeMeteringMatrix();
		} catch (Exception e)
		{
			Log.e("Main setup camera", "Util.initialize failed!");
		}

		prepareMeteringAreas();


			guiManager.onCameraCreate();
			PluginManager.getInstance().onCameraParametersSetup();
			guiManager.onPluginsInitialized();


		// ----- Start preview and setup frame buffer if needed

		// call camera release sequence from onPause somewhere ???
		new CountDownTimer(10, 10)
		{
			@Override
			public void onFinish()
			{

					if (!CameraController.isCameraCreated())
						return;
					// exceptions sometimes happen here when resuming after
					// processing
					try
					{
						CameraController.startCameraPreview();
					} catch (RuntimeException e)
					{
						Toast.makeText(MainScreen.thiz, "Unable to start camera", Toast.LENGTH_LONG).show();
						return;
					}

					CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
					CameraController.getCamera().addCallbackBuffer(CameraController.getPreviewBuffer());


				PluginManager.getInstance().onCameraSetup();
				guiManager.onCameraSetup();
				MainScreen.mApplicationStarted = true;

				if (MainScreen.isForceClose)
					PluginManager.getInstance().sendMessage(PluginManager.MSG_APPLICATION_STOP, 0);
			}

			@Override
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}
		}.start();
	}

	@TargetApi(21)
	public void createCaptureSession()
	{
		mCameraSurface = surfaceHolder.getSurface();
		surfaceList.add(mCameraSurface); // surface for viewfinder preview

		if (captureFormat != CameraController.RAW) // when capture RAW preview
													// frames is not available
			surfaceList.add(mImageReaderPreviewYUV.getSurface()); // surface for
																	// preview
																	// yuv
		// images
		if (captureFormat == CameraController.YUV)
		{
			Log.d("MainScreen",
					"add mImageReaderYUV " + mImageReaderYUV.getWidth() + " x " + mImageReaderYUV.getHeight());
			surfaceList.add(mImageReaderYUV.getSurface()); // surface for yuv
															// image
			// capture
		} else if (captureFormat == CameraController.JPEG)
		{
			Log.d("MainScreen",
					"add mImageReaderJPEG " + mImageReaderJPEG.getWidth() + " x " + mImageReaderJPEG.getHeight());
			surfaceList.add(mImageReaderJPEG.getSurface()); // surface for jpeg
															// image
			// capture
		} else if (captureFormat == CameraController.RAW)
		{
			Log.d("MainScreen", "add mImageReaderRAW + mImageReaderJPEG " + mImageReaderRAW.getWidth() + " x "
					+ mImageReaderRAW.getHeight());
			surfaceList.add(mImageReaderJPEG.getSurface()); // surface for jpeg
															// image
			// capture
			if (CameraController.isRAWCaptureSupported())
				surfaceList.add(mImageReaderRAW.getSurface());
		}

		CameraController.setPreviewSurface(mImageReaderPreviewYUV.getSurface());

	}

	private void prepareMeteringAreas()
	{
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
	public void surfaceCreated(SurfaceHolder holder)
	{
		// ----- Find 'normal' orientation of the device

		Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if ((rotation == Surface.ROTATION_90) || (rotation == Surface.ROTATION_270))
			landscapeIsNormal = true; // false; - if landscape view orientation
										// set for MainScreen
		else
			landscapeIsNormal = false;

		surfaceCreated = true;

		mCameraSurface = surfaceHolder.getSurface();

		Log.d("MainScreen", "SURFACE CREATED");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		surfaceCreated = false;
	}

	// SURFACES (preview, image readers)
	public Surface getCameraSurface()
	{
		return mCameraSurface;
	}

	@TargetApi(21)
	public Surface getPreviewYUVSurface()
	{
		return mImageReaderPreviewYUV.getSurface();
	}

	@TargetApi(14)
	public boolean isFaceDetectionAvailable(Camera.Parameters params)
	{
		return params.getMaxNumDetectedFaces() > 0;
	}

	public CameraController.Size getPreviewSize()
	{
		LayoutParams lp = preview.getLayoutParams();
		if (lp == null)
			return null;

		return new CameraController.Size(lp.width, lp.height);
	}

	/*
	 * CAMERA PARAMETERS SECTION Supplementary methods for those plugins that
	 * need an icons of supported camera parameters (scene, iso, wb, flash,
	 * focus) Methods return id of drawable icon
	 */
	public int getSceneIcon(int sceneMode)
	{
		return guiManager.getSceneIcon(sceneMode);
	}

	public int getWBIcon(int wb)
	{
		return guiManager.getWBIcon(wb);
	}

	public int getFocusIcon(int focusMode)
	{
		return guiManager.getFocusIcon(focusMode);
	}

	public int getFlashIcon(int flashMode)
	{
		return guiManager.getFlashIcon(flashMode);
	}

	public int getISOIcon(int isoMode)
	{
		return guiManager.getISOIcon(isoMode);
	}

	public void setCameraMeteringMode(int mode)
	{
		if (CameraParameters.meteringModeAuto == mode)
			CameraController.setCameraMeteringAreas(null);
		else if (CameraParameters.meteringModeMatrix == mode)
		{
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

	/*
	 * 
	 * CAMERA parameters access function ended
	 */

	// >>Description
	// section with user control procedures and main capture functions
	//
	// all events translated to PluginManager
	// Description<<

	public static void setAutoFocusLock(boolean locked)
	{
		mAFLocked = locked;
	}

	public static boolean getAutoFocusLock()
	{
		return mAFLocked;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (!mApplicationStarted)
			return true;

		// menu button processing
		if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			menuButtonPressed();
			return true;
		}
		// shutter/camera button processing
		if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
		{
			MainScreen.getGUIManager().onHardwareShutterButtonPressed();
			return true;
		}
		// focus/half-press button processing
		if (keyCode == KeyEvent.KEYCODE_FOCUS)
		{
			if (event.getDownTime() == event.getEventTime())
			{
				MainScreen.getGUIManager().onHardwareFocusButtonPressed();
			}
			return true;
		}

		// check if Headset Hook button has some functions except standard
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			boolean headsetFunc = prefs.getBoolean("headsetPrefCommon", false);
			if (headsetFunc)
			{
				MainScreen.getGUIManager().onHardwareFocusButtonPressed();
				MainScreen.getGUIManager().onHardwareShutterButtonPressed();
				return true;
			}
		}

		// check if volume button has some functions except Zoom-ing
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			int buttonFunc = Integer.parseInt(prefs.getString(MainScreen.sVolumeButtonPref, "0"));
			if (buttonFunc == VOLUME_FUNC_SHUTTER)
			{
				MainScreen.getGUIManager().onHardwareFocusButtonPressed();
				MainScreen.getGUIManager().onHardwareShutterButtonPressed();
				return true;
			} else if (buttonFunc == VOLUME_FUNC_EXPO)
			{
				MainScreen.getGUIManager().onVolumeBtnExpo(keyCode);
				return true;
			} else if (buttonFunc == VOLUME_FUNC_NONE)
				return true;
		}

		if (PluginManager.getInstance().onKeyDown(true, keyCode, event))
			return true;
		if (guiManager.onKeyDown(true, keyCode, event))
			return true;

		if (super.onKeyDown(keyCode, event))
			return true;
		return false;
	}

	@Override
	public void onClick(View v)
	{
		if (mApplicationStarted)
			MainScreen.getGUIManager().onClick(v);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event)
	{
		if (mApplicationStarted)
			return MainScreen.getGUIManager().onTouch(view, event);
		return true;
	}

	public boolean onTouchSuper(View view, MotionEvent event)
	{
		return super.onTouchEvent(event);
	}

	public void onButtonClick(View v)
	{
		MainScreen.getGUIManager().onButtonClick(v);
	}

	@Override
	public void onShutter()
	{
		PluginManager.getInstance().onShutter();
	}

	public boolean isForceClose()
	{
		return isForceClose;
	}

	// >>Description
	// message processor
	//
	// processing main events and calling active plugin procedures
	//
	// possible some additional plugin dependent events.
	//
	// Description<<
	@Override
	public boolean handleMessage(Message msg)
	{

		switch (msg.what)
		{
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
		case PluginManager.MSG_CAMERA_READY:
			{
				if (CameraController.isCameraCreated())
				{
					configureCamera();
					PluginManager.getInstance().onGUICreate();
					MainScreen.getGUIManager().onGUICreate();
				}
			}
			break;
		case PluginManager.MSG_CAMERA_OPENED:
			if (mCameraStarted)
				break;
		case PluginManager.MSG_SURFACE_READY:
			{
				// if both surface is created and camera device is opened
				// - ready to set up preview and other things
				// if (surfaceCreated && (HALv3.getCamera2() != null))
				if (surfaceCreated)
				{
					configureCamera();

						PluginManager.getInstance().onGUICreate();
						MainScreen.getGUIManager().onGUICreate();
						// mCameraStarted = true;
					mCameraStarted = true;
				}
			}
			break;
		case PluginManager.MSG_SURFACE_CONFIGURED:
			{
				createCaptureSession();
				PluginManager.getInstance().onGUICreate();
				MainScreen.getGUIManager().onGUICreate();
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

	public void menuButtonPressed()
	{
		PluginManager.getInstance().menuButtonPressed();
	}

	public void disableCameraParameter(GUI.CameraParameter iParam, boolean bDisable, boolean bInitMenu)
	{
		guiManager.disableCameraParameter(iParam, bDisable, bInitMenu);
	}

	public void showOpenGLLayer(final int version)
	{
		if (glView == null)
		{
			glView = new GLLayer(MainScreen.getMainContext(), version);
			LayoutParams params = MainScreen.getPreviewSurfaceView().getLayoutParams();
			glView.setLayoutParams(params);
			((RelativeLayout) this.findViewById(R.id.mainLayout2)).addView(glView, 0);
			preview.bringToFront();
			glView.setZOrderMediaOverlay(true);
			glView.onResume();
		}
	}

	public void hideOpenGLLayer()
	{
		if (glView != null)
		{
			// preview.getHolder().getSurface().lockCanvas(null).drawColor(Color.BLACK);
			glView.onPause();
			glView.destroyDrawingCache();
			((RelativeLayout) this.findViewById(R.id.mainLayout2)).removeView(glView);
			glView = null;
		}
	}

	public void playShutter(int sound)
	{
		if (!MainScreen.isShutterSoundEnabled())
		{
			MediaPlayer mediaPlayer = MediaPlayer.create(MainScreen.thiz, sound);
			mediaPlayer.start();
		}
	}

	public void playShutter()
	{
		if (!MainScreen.isShutterSoundEnabled())
		{
			if (shutterPlayer != null)
				shutterPlayer.play();
		}
	}

	// set TRUE to mute and FALSE to unmute
	public void muteShutter(boolean mute)
	{
		if (MainScreen.isShutterSoundEnabled())
		{
			AudioManager mgr = (AudioManager) MainScreen.thiz.getSystemService(MainScreen.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
		}
	}

	// public static int getImageWidth()
	// {
	// return thiz.imageWidth;
	// }
	//
	// public static void setImageWidth(int setImageWidth)
	// {
	// thiz.imageWidth = setImageWidth;
	// }
	//
	// public static int getImageHeight()
	// {
	// return thiz.imageHeight;
	// }
	//
	// public static void setImageHeight(int setImageHeight)
	// {
	// thiz.imageHeight = setImageHeight;
	// }

	// public static int getSaveImageWidth()
	// {
	// return thiz.saveImageWidth;
	// }
	//
	// public static void setSaveImageWidth(int setSaveImageWidth)
	// {
	// thiz.saveImageWidth = setSaveImageWidth;
	// }
	//
	// public static int getSaveImageHeight()
	// {
	// return thiz.saveImageHeight;
	// }
	//
	// public static void setSaveImageHeight(int setSaveImageHeight)
	// {
	// thiz.saveImageHeight = setSaveImageHeight;
	// }

	public static int getPreviewWidth()
	{
		return thiz.previewWidth;
	}

	public static void setPreviewWidth(int iWidth)
	{
		thiz.previewWidth = iWidth;
	}

	public static int getPreviewHeight()
	{
		return thiz.previewHeight;
	}

	public static void setPreviewHeight(int iHeight)
	{
		thiz.previewHeight = iHeight;
	}

	public static boolean getWantLandscapePhoto()
	{
		return wantLandscapePhoto;
	}

	public static void setWantLandscapePhoto(boolean setWantLandscapePhoto)
	{
		wantLandscapePhoto = setWantLandscapePhoto;
	}

	public void setScreenBrightness(boolean setMax)
	{
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

	public void setKeepScreenOn(boolean keepScreenOn)
	{
		if (keepScreenOn)
		{
			preview.setKeepScreenOn(keepScreenOn);
		} else
		{
			preview.setKeepScreenOn(this.keepScreenOn);
		}
	}

	public static Resources getAppResources()
	{
		return MainScreen.thiz.getResources();
	}

	// controls subscription status request
	private boolean	subscriptionStatusRequest	= false;
	private long	timeLastSubscriptionCheck	= 0;							// should
																				// check
																				// each
																				// 32
																				// days
																				// 32*24*60*60*1000
	private long	days32						= 32 * 24 * 60 * 60 * 1000L;

	public String								titleUnlockAll				= "$6.95";
	public String								titleUnlockAllCoupon		= "$3.95";
	public String								titleUnlockHDR				= "$2.99";
	public String								titleUnlockSuper			= "$2.99";
	public String								titleUnlockPano				= "$2.99";
	public String								titleUnlockMoving			= "$3.99";
	public String								titleUnlockGroup			= "$2.99";
	public String								titleSubscriptionYear		= "$4.99";

	public String								summary_SKU_PROMO			= "";






	private boolean isInstalled(String packageName)
	{
		PackageManager pm = getPackageManager();
		boolean installed = false;
		try
		{
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			installed = true;
		} catch (PackageManager.NameNotFoundException e)
		{
			installed = false;
		}
		return installed;
	}

	private void resetOrSaveSettings()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Editor prefsEditor = prefs.edit();
		boolean isSaving = prefs.getBoolean("SaveConfiguration_Mode", true);
		if (!isSaving)
		{
			prefsEditor.putString("defaultModeName", "single");
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_ImageSize", true);
		if (!isSaving)
		{
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

			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_SceneMode", false);
		if (!isSaving)
		{
			prefsEditor.putInt(sSceneModePref, sDefaultValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_FocusMode", true);
		if (!isSaving)
		{
			prefsEditor.putInt(sRearFocusModePref, sDefaultFocusValue);
			prefsEditor.putInt(sFrontFocusModePref, sDefaultFocusValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_WBMode", false);
		if (!isSaving)
		{
			prefsEditor.putInt(sWBModePref, sDefaultValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_ISOMode", false);
		if (!isSaving)
		{
			prefsEditor.putInt(sISOPref, sDefaultValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_FlashMode", true);
		if (!isSaving)
		{
			prefsEditor.putInt(sFlashModePref, sDefaultValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_FrontRearCamera", true);
		if (!isSaving)
		{
			prefsEditor.putBoolean(sUseFrontCameraPref, false);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_ExpoCompensation", false);
		if (!isSaving)
		{
			prefsEditor.putInt(MainScreen.sEvPref, 0);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_DelayedCapture", false);
		if (!isSaving)
		{
			prefsEditor.putInt(MainScreen.sDelayedCapturePref, 0);
			prefsEditor.putBoolean(MainScreen.sSWCheckedPref, false);
			prefsEditor.putBoolean(MainScreen.sDelayedFlashPref, false);
			prefsEditor.putBoolean(MainScreen.sDelayedSoundPref, false);
			prefsEditor.putInt(MainScreen.sDelayedCaptureIntervalPref, 0);

			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_TimelapseCapture", false);
		if (!isSaving && !prefs.getBoolean(sPhotoTimeLapseIsRunningPref, false))
		{
			prefsEditor.putInt(MainScreen.sPhotoTimeLapseCaptureIntervalPref, 0);
			prefsEditor.putInt(MainScreen.sPhotoTimeLapseCaptureIntervalMeasurmentPref, 0);
			prefsEditor.putBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);
			prefsEditor.putBoolean(MainScreen.sPhotoTimeLapseActivePref, false);

			prefsEditor.commit();
		}
	}

	public void switchingMode(boolean isModeSwitching)
	{
		switchingMode = isModeSwitching;
	}

	public boolean getSwitchingMode()
	{
		return switchingMode;
	}
}
