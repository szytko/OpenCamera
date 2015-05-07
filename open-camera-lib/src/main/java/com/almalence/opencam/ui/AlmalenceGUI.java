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


package com.almalence.opencam.ui;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.googsharing.Thumbnail;

import com.almalence.opencam.CameraScreenActivity;
import com.almalence.ui.Panel;
import com.almalence.ui.Panel.OnPanelListener;
import com.almalence.ui.RotateImageView;
import com.almalence.util.Util;
//<!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.Mode;
import com.almalence.opencam.Plugin;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginType;
import com.almalence.opencam.Preferences;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;



/***
 * AlmalenceGUI is an instance of GUI class, implements current GUI
 ***/

public class AlmalenceGUI extends GUI implements SeekBar.OnSeekBarChangeListener, /*View.OnLongClickListener,*/
		View.OnClickListener
{
	private SharedPreferences	preferences;

	private static final int	INFO_ALL	= 0;
	private static final int	INFO_NO		= 1;
	private static final int	INFO_GRID	= 2;
	private static final int	INFO_PARAMS	= 3;
	private int					infoSet		= INFO_PARAMS;

	public enum ShutterButton
	{
		DEFAULT, RECORDER_START_WITH_PAUSE, RECORDER_START, RECORDER_STOP_WITH_PAUSE, RECORDER_STOP, RECORDER_RECORDING_WITH_PAUSE, RECORDER_RECORDING, RECORDER_PAUSED, TIMELAPSE_ACTIVE
	}

	public enum SettingsType
	{
		SCENE, WB, FOCUS, FLASH, ISO, METERING, CAMERA, EV, MORE
	}

	private OrientationEventListener			orientListener;

	// certain quick control visible
	private boolean								quickControlsVisible		= false;

	// Quick control customization variables
	private ElementAdapter						quickControlAdapter;
	private List<View>							quickControlChangeres;
	private View								currentQuickView			= null;
	// Current quick control to replace If qc customization layout is showing
	// now
	private boolean								quickControlsChangeVisible	= false;

	// Settings layout
	private ElementAdapter						settingsAdapter;
	private List<View>							settingsViews;
	private boolean								settingsControlsVisible		= false;
	// If quick settings layout is showing now

	// Mode selector layout
	private ElementAdapter						modeAdapter;
	private List<View>							modeViews;
	private ViewGroup							activeMode					= null;
	private boolean								modeSelectorVisible			= false;
	// If quick settings layout is showing now



	// Assoc list for storing association between mode button and mode ID
	private Map<View, String>					buttonModeViewAssoc;

	private Thumbnail							mThumbnail;


	private static final Integer				ICON_EV						= R.drawable.gui_almalence_settings_exposure;
	private static final Integer				ICON_CAM					= R.drawable.gui_almalence_settings_changecamera;
	private static final Integer				ICON_SETTINGS				= R.drawable.gui_almalence_settings_more_settings;

	private static final int					FOCUS_AF_LOCK				= 10;

	// Lists of icons for camera parameters (scene mode, flash mode, focus mode,
	// white balance, iso)
	private static final Map<Integer, Integer>	ICONS_SCENE					= AlmalenceGUIInitializer.ICONS_SCENE;

	private static final Map<Integer, Integer>	ICONS_WB					= AlmalenceGUIInitializer.ICONS_WB;

	private static final Map<Integer, Integer>	ICONS_FOCUS					= AlmalenceGUIInitializer.ICONS_FOCUS;

	private static final Map<Integer, Integer>	ICONS_FLASH					= AlmalenceGUIInitializer.ICONS_FLASH;

	private static final Map<Integer, Integer>	ICONS_ISO					= AlmalenceGUIInitializer.ICONS_ISO;

	private static final Map<String, Integer>	ICONS_DEFAULT_ISO			= AlmalenceGUIInitializer.ICONS_DEFAULT_ISO;

	private static final Map<Integer, Integer>	ICONS_METERING				= AlmalenceGUIInitializer.ICONS_METERING;


	private static final Map<Integer, String>	NAMES_WB					= AlmalenceGUIInitializer.NAMES_WB;

	private static final Map<Integer, String>	NAMES_FLASH					= AlmalenceGUIInitializer.NAMES_FLASH;



	// Defining for top menu buttons (camera parameters settings)
	private static final int					MODE_EV						= R.id.evButton;
	private static final int					MODE_WB						= R.id.wbButton;
	private static final int					MODE_FLASH					= R.id.flashButton;

	private Map<Integer, View>					topMenuButtons;

	// Each plugin may have one top menu (and appropriate quick control) button
	private Map<String, View>					topMenuPluginButtons;

	// Current quick controls
	private View								quickControl1				= null;
	private View								quickControl2				= null;
	private View								quickControl3				= null;
	private View								quickControl4				= null;

	//private ElementAdapter						scenemodeAdapter;
	private ElementAdapter						wbmodeAdapter;
	//private ElementAdapter						focusmodeAdapter;
	private ElementAdapter						flashmodeAdapter;
	//private ElementAdapter						isoAdapter;
	//private ElementAdapter						meteringmodeAdapter;

	//private Map<Integer, View>					sceneModeButtons;
	private Map<Integer, View>					wbModeButtons;
	//private Map<Integer, View>					focusModeButtons;
	private Map<Integer, View>					flashModeButtons;
	//private Map<Integer, View>					isoButtons;
	private Map<Integer, View>					meteringModeButtons;

	// Camera settings values which is exist at current device
	private List<View>							activeScene;
	private List<View>							activeWB;
	private List<View>							activeFocus;
	private List<View>							activeFlash;
	private List<View>							activeISO;
	private List<View>							activeMetering;

	private List<Integer>						activeSceneNames;
	private List<Integer>						activeWBNames;
	private List<Integer>						activeFocusNames;
	private List<Integer>						activeFlashNames;
	private List<Integer>						activeISONames;
	private List<Integer>						activeMeteringNames;

	private boolean								isEVEnabled					= true;
	private boolean								isSceneEnabled				= true;
	private boolean								isWBEnabled					= true;
	private boolean								isFocusEnabled				= true;
	private boolean								isFlashEnabled				= true;
	private boolean								isIsoEnabled				= true;
	private boolean								isCameraChangeEnabled		= true;
	private boolean								isMeteringEnabled			= true;

	// GUI Layout
	private View								guiView;

	// Current camera parameters
	private int									mEV							= 0;
	private int									mSceneMode					= -1;
	private int									mFlashMode					= -1;
	private int									mFocusMode					= -1;
	private int									mWB							= -1;
	private int									mISO						= -1;
	private int									mMeteringMode				= -1;

	private float								fScreenDensity;

	private int									iInfoViewMaxHeight;
	private int									iInfoViewMaxWidth;
	private int									iInfoViewHeight;

	private int									iCenterViewMaxHeight;
	private int									iCenterViewMaxWidth;

	// indicates if it's first launch - to show hint layer.
	private boolean								isFirstLaunch				= true;

	private static int							iScreenType					= CameraScreenActivity.getAppResources().getInteger(
																					R.integer.screen_type);

	public AlmalenceGUI()
	{

		mThumbnail = null;
		topMenuButtons = new HashMap<Integer, View>();
		topMenuPluginButtons = new HashMap<String, View>();

		//scenemodeAdapter = new ElementAdapter();
		wbmodeAdapter = new ElementAdapter(true, CameraScreenActivity.getMainContext());
		//focusmodeAdapter = new ElementAdapter();
		flashmodeAdapter = new ElementAdapter(true, CameraScreenActivity.getMainContext());
		//isoAdapter = new ElementAdapter();
		//meteringmodeAdapter = new ElementAdapter();

		//sceneModeButtons = new HashMap<Integer, View>();
		wbModeButtons = new HashMap<Integer, View>();
		//focusModeButtons = new HashMap<Integer, View>();
		flashModeButtons = new HashMap<Integer, View>();
		//isoButtons = new HashMap<Integer, View>();
		meteringModeButtons = new HashMap<Integer, View>();

		activeScene = new ArrayList<View>();
		activeWB = new ArrayList<View>();
		activeFocus = new ArrayList<View>();
		activeFlash = new ArrayList<View>();
		activeISO = new ArrayList<View>();
		activeMetering = new ArrayList<View>();

		activeSceneNames = new ArrayList<Integer>();
		activeWBNames = new ArrayList<Integer>();
		activeFocusNames = new ArrayList<Integer>();
		activeFlashNames = new ArrayList<Integer>();
		activeISONames = new ArrayList<Integer>();
		activeMeteringNames = new ArrayList<Integer>();

		settingsAdapter = new ElementAdapter();
		settingsViews = new ArrayList<View>();

		quickControlAdapter = new ElementAdapter();
		quickControlChangeres = new ArrayList<View>();

		modeAdapter = new ElementAdapter();
		modeViews = new ArrayList<View>();
		buttonModeViewAssoc = new HashMap<View, String>();

	}

	/*
	 * CAMERA PARAMETERS SECTION Supplementary methods for those plugins that
	 * need an icons of supported camera parameters (scene, iso, wb, flash,
	 * focus) Methods return id of drawable icon
	 */
	public int getSceneIcon(int sceneMode)
	{
		if (ICONS_SCENE.containsKey(sceneMode))
			return ICONS_SCENE.get(sceneMode);
		else
			return -1;
	}

	public int getWBIcon(int wb)
	{
		if (ICONS_WB.containsKey(wb))
			return ICONS_WB.get(wb);
		else
			return -1;
	}

	public int getFocusIcon(int focusMode)
	{
		if (ICONS_FOCUS.containsKey(focusMode))
		{
			try
			{
				return ICONS_FOCUS.get(focusMode);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("getFocusIcon", "icons_focus.get exception: " + e.getMessage());
				return -1;
			}
		} else
			return -1;
	}

	public int getFlashIcon(int flashMode)
	{
		if (ICONS_FLASH.containsKey(flashMode))
			return ICONS_FLASH.get(flashMode);
		else
			return -1;
	}

	public int getISOIcon(int isoMode)
	{
		if (ICONS_ISO.containsKey(isoMode))
			return ICONS_ISO.get(isoMode);
		else if (ICONS_DEFAULT_ISO.containsKey(isoMode))
			return ICONS_DEFAULT_ISO.get(isoMode);
		else
			return -1;
	}

	@Override
	public float getScreenDensity()
	{
		return fScreenDensity;
	}

	@Override
	public void onStart()
	{

		// Calculate right sizes for plugin's controls
		DisplayMetrics metrics = new DisplayMetrics();
		CameraScreenActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		fScreenDensity = metrics.density;

		iInfoViewMaxHeight = (int) (CameraScreenActivity.getMainContext().getResources().getInteger(R.integer.infoControlHeight) * fScreenDensity);
		iInfoViewMaxWidth = (int) (CameraScreenActivity.getMainContext().getResources().getInteger(R.integer.infoControlWidth) * fScreenDensity);

		iCenterViewMaxHeight = (int) (CameraScreenActivity.getMainContext().getResources().getInteger(R.integer.centerViewHeight) * fScreenDensity);
		iCenterViewMaxWidth = (int) (CameraScreenActivity.getMainContext().getResources().getInteger(R.integer.centerViewWidth) * fScreenDensity);

		// Create orientation listener
		initOrientationListener();


	}

	private void initOrientationListener()
	{
		// set orientation listener to rotate controls
		this.orientListener = new OrientationEventListener(CameraScreenActivity.getMainContext())
		{
			@Override
			public void onOrientationChanged(int orientation)
			{
				if (orientation == ORIENTATION_UNKNOWN)
					return;

				final Display display = ((WindowManager) CameraScreenActivity.getInstance().getSystemService(
						Context.WINDOW_SERVICE)).getDefaultDisplay();
				final int orientationProc = (display.getWidth() <= display.getHeight()) ? Configuration.ORIENTATION_PORTRAIT
						: Configuration.ORIENTATION_LANDSCAPE;
				final int rotation = display.getRotation();

				boolean remapOrientation = Util.shouldRemapOrientation(orientationProc, rotation);

				if (remapOrientation)
					orientation = (orientation - 90 + 360) % 360;

				AlmalenceGUI.mDeviceOrientation = Util.roundOrientation(orientation, AlmalenceGUI.mDeviceOrientation);

				((RotateImageView) topMenuButtons.get(MODE_EV)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_WB)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_FLASH)).setOrientation(AlmalenceGUI.mDeviceOrientation);

				Set<String> keys = topMenuPluginButtons.keySet();
				Iterator<String> it = keys.iterator();
				while (it.hasNext())
				{
					String key = it.next();
					((RotateImageView) topMenuPluginButtons.get(key)).setOrientation(AlmalenceGUI.mDeviceOrientation);
				}

				((RotateImageView) guiView.findViewById(R.id.buttonShutter))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
						: AlmalenceGUI.mDeviceOrientation % 360 + 360;
				if (AlmalenceGUI.mPreviousDeviceOrientation != AlmalenceGUI.mDeviceOrientation)
					rotateSquareViews(degree, 250);

				(guiView.findViewById(R.id.blockingText)).setRotation(-AlmalenceGUI.mDeviceOrientation);

				AlmalenceGUI.mPreviousDeviceOrientation = AlmalenceGUI.mDeviceOrientation;

				PluginManager.getInstance().onOrientationChanged(getDisplayOrientation());


			}
		};
	}

	private void createMergedSelectModeButton()
	{
		// create merged image for select mode button
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		String defaultModeName = prefs.getString(CameraScreenActivity.sDefaultModeName, "");
		Mode mode = ConfigParser.getInstance().getMode(defaultModeName);
		try
		{
			Bitmap bm = null;
			Bitmap iconBase = BitmapFactory.decodeResource(CameraScreenActivity.getMainContext().getResources(),
					R.drawable.gui_almalence_select_mode);
			Bitmap iconOverlay = BitmapFactory.decodeResource(
					CameraScreenActivity.getMainContext().getResources(),
					CameraScreenActivity
							.getInstance()
							.getResources()
							.getIdentifier(CameraController.isUseSuperMode() ? mode.iconHAL : mode.icon, "drawable",
									CameraScreenActivity.getInstance().getPackageName()));
			iconOverlay = Bitmap.createScaledBitmap(iconOverlay, (int) (iconBase.getWidth() / 1.8),
					(int) (iconBase.getWidth() / 1.8), false);

			bm = mergeImage(iconBase, iconOverlay);
			bm = Bitmap.createScaledBitmap(bm,
					(int) (CameraScreenActivity.getMainContext().getResources().getDimension(R.dimen.paramsLayoutHeight)),
					(int) (CameraScreenActivity.getMainContext().getResources().getDimension(R.dimen.paramsLayoutHeight)), false);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onStop()
	{
		removePluginViews();
		mDeviceOrientation = 0;
		mPreviousDeviceOrientation = 0;
	}

	@Override
	public void onPause()
	{
		if (quickControlsChangeVisible)
			closeQuickControlsSettings();
		orientListener.disable();
		if (modeSelectorVisible)
			hideModeList();

		lockControls = false;
		guiView.findViewById(R.id.buttonShutter).setEnabled(true);
		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
	}

	@Override
	public void onResume()
	{
		CameraScreenActivity.getInstance().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				AlmalenceGUI.this.updateThumbnailButton();
			}
		});
		
		
		setShutterIcon(ShutterButton.DEFAULT);

		lockControls = false;

		orientListener.enable();

		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_EV, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_WB, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FOCUS, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FLASH, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_ISO, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_METERING, false, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_CAMERACHANGE, false, true);

		// if first launch - show layout with hints
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		
		if (prefs.getBoolean(CameraScreenActivity.sPhotoTimeLapseIsRunningPref, false)) {
			setShutterIcon(ShutterButton.TIMELAPSE_ACTIVE);
		}
		
		if (prefs.contains("isFirstLaunch"))
		{
			isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);
		} else
		{
			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("isFirstLaunch", false);
			prefsEditor.commit();
			isFirstLaunch = true;
		}


		createMergedSelectModeButton();
	}

	@Override
	public void onDestroy()
	{
		// Not used
	}

	@Override
	public void createInitialGUI()
	{
		guiView = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.gui_almalence_layout, null);
		// Add GUI Layout to main layout of OpenCamera
		((RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.mainLayout1)).addView(guiView);
	}

	// Create standard OpenCamera's buttons and theirs OnClickListener
	@Override
	public void onCreate()
	{
		// Get application preferences object
		preferences = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());

		guiView.findViewById(R.id.evButton).setOnTouchListener(CameraScreenActivity.getInstance());
		guiView.findViewById(R.id.wbButton).setOnTouchListener(CameraScreenActivity.getInstance());
		guiView.findViewById(R.id.flashButton).setOnTouchListener(CameraScreenActivity.getInstance());
		// Get all top menu buttons
		topMenuButtons.put(MODE_EV, guiView.findViewById(R.id.evButton));
		topMenuButtons.put(MODE_WB, guiView.findViewById(R.id.wbButton));
		topMenuButtons.put(MODE_FLASH, guiView.findViewById(R.id.flashButton));
		wbModeButtons = initCameraParameterModeButtons(ICONS_WB, NAMES_WB, wbModeButtons, MODE_WB);
		flashModeButtons = initCameraParameterModeButtons(ICONS_FLASH, NAMES_FLASH, flashModeButtons, MODE_FLASH);

        createPluginTopMenuButtons();


		((RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.mainLayout1)).setOnTouchListener(CameraScreenActivity
				.getInstance());
		((LinearLayout) CameraScreenActivity.getInstance().findViewById(R.id.evLayout)).setOnTouchListener(CameraScreenActivity
				.getInstance());

	}



	private Map<Integer, View> initCameraParameterModeButtons(Map<Integer, Integer> icons_map,
			Map<Integer, String> names_map, Map<Integer, View> paramMap, final int mode)
	{
		paramMap.clear();
		Set<Integer> keys = icons_map.keySet();
		Iterator<Integer> it = keys.iterator();
		LayoutInflater inflator = CameraScreenActivity.getInstance().getLayoutInflater();
		while (it.hasNext())
		{
			final int system_name = it.next();
			final String value_name = names_map.get(system_name);
			View paramMode = inflator.inflate(R.layout.gui_almalence_quick_control_grid_element, null, false);
			// set some mode icon
			((ImageView) paramMode.findViewById(R.id.imageView)).setImageResource(icons_map.get(system_name));
			((TextView) paramMode.findViewById(R.id.textView)).setText(value_name);

			if ( (mode == MODE_FLASH && system_name == CameraParameters.FLASH_MODE_OFF)
					|| (mode == MODE_WB && system_name == CameraParameters.WB_MODE_AUTO))
				paramMode.setOnTouchListener(new OnTouchListener()
				{

					@Override
					public boolean onTouch(View v, MotionEvent event)
					{
						if (event.getAction() == MotionEvent.ACTION_CANCEL)
						{
							settingsModeClicked(mode, system_name);
							return false;
						}
						return false;
					}
				});

			paramMode.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					settingsModeClicked(mode, system_name);
					quickControlsVisible = false;
				}
			});

			paramMap.put(system_name, paramMode);
		}

		return paramMap;
	}

	private void settingsModeClicked(int mode, int system_name)
	{

        if(mode == MODE_WB){ setWhiteBalance(system_name); return; }
        if(mode == MODE_FLASH){ setFlashMode(system_name); return; }
	}

	@Override
	public void onGUICreate()
	{
		if (CameraScreenActivity.getInstance().findViewById(R.id.infoLayout).getVisibility() == View.VISIBLE)
			iInfoViewHeight = CameraScreenActivity.getInstance().findViewById(R.id.infoLayout).getHeight();
		// Recreate plugin views
		removePluginViews();
		createPluginViews();

		// add self-timer control
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		boolean showDelayedCapturePrefCommon = prefs.getBoolean(CameraScreenActivity.sShowDelayedCapturePref, false);

		LinearLayout infoLayout = (LinearLayout) guiView.findViewById(R.id.infoLayout);
		RelativeLayout.LayoutParams infoParams = (RelativeLayout.LayoutParams) infoLayout.getLayoutParams();
		if (infoParams != null)
		{
			int width = infoParams.width;
			if (infoLayout.getChildCount() == 0)
				infoParams.rightMargin = -width;
			else
				infoParams.rightMargin = 0;
			infoLayout.setLayoutParams(infoParams);
			// infoLayout.requestLayout();
		}

		infoSet = prefs.getInt(CameraScreenActivity.sDefaultInfoSetPref, INFO_PARAMS);
		if (infoSet == INFO_PARAMS && !isAnyViewOnViewfinder())
		{
			infoSet = INFO_ALL;
			prefs.edit().putInt(CameraScreenActivity.sDefaultInfoSetPref, infoSet).commit();
		}
		setInfo(false, 0, 0, false);

		CameraScreenActivity.getInstance().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				AlmalenceGUI.this.updateThumbnailButton();
			}
		});

		final View blockingLayout = guiView.findViewById(R.id.blockingLayout);
		final View postProcessingLayout = guiView.findViewById(R.id.postprocessingLayout);
		final View mainButtons = guiView.findViewById(R.id.mainButtons);
		final View qcLayout = guiView.findViewById(R.id.qcLayout);
		final View buttonsLayout = guiView.findViewById(R.id.buttonsLayout);

		mainButtons.bringToFront();
		qcLayout.bringToFront();
		buttonsLayout.bringToFront();
		blockingLayout.bringToFront();
		postProcessingLayout.bringToFront();

	}

    //TODO - setting proper image size
	@Override
	public void setupViewfinderPreviewSize(CameraController.Size previewSize)
	{
		Log.e("GUI",
				"setupViewfinderPreviewSize. Width = " + previewSize.getWidth() + " Height = "
						+ previewSize.getHeight());
		float cameraAspect = (float) previewSize.getWidth() / previewSize.getHeight();

		RelativeLayout ll = (RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.mainLayout1);


		int previewSurfaceWidth = ll.getWidth();
		int previewSurfaceHeight = ll.getHeight();
		float surfaceAspect = (float) previewSurfaceHeight / previewSurfaceWidth;

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		DisplayMetrics metrics = new DisplayMetrics();
		CameraScreenActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int screen_height = metrics.heightPixels;
        int screen_width = metrics.widthPixels;


        lp.width = previewSize.getHeight();
        lp.height = previewSize.getWidth();
        lp.topMargin =  (screen_height - lp.height)/2;


		Log.e("GUI", "setLayoutParams. width = " + lp.width + " height = " + lp.height+" margin="+lp.topMargin );

		guiView.findViewById(R.id.fullscreenLayout).setLayoutParams(lp);
		guiView.findViewById(R.id.specialPluginsLayout).setLayoutParams(lp);
        CameraScreenActivity.getPreviewSurfaceView().setLayoutParams(lp);

    }

	/*
	 * Each plugin may have only one top menu button Icon id and Title (plugin's
	 * members) is use to make design of button
	 */
	public void createPluginTopMenuButtons()
	{
		topMenuPluginButtons.clear();

		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.ViewFinder));
		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.Capture));
		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.Processing));
		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.Filter));
		createPluginTopMenuButtons(PluginManager.getInstance().getActivePlugins(PluginType.Export));
	}

	public void createPluginTopMenuButtons(List<Plugin> plugins)
	{
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);

				if (plugin == null || plugin.getQuickControlIconID() <= 0)
					continue;

				LayoutInflater inflator = CameraScreenActivity.getInstance().getLayoutInflater();
				ImageView qcView = (ImageView) inflator.inflate(R.layout.gui_almalence_quick_control_button,
						(ViewGroup) guiView.findViewById(R.id.paramsLayout), false);

				qcView.setOnTouchListener(CameraScreenActivity.getInstance());
				qcView.setOnClickListener(this);
				//qcView.setOnLongClickListener(this);

				topMenuPluginButtons.put(plugin.getID(), qcView);
			}
		}
	}

	public void createPluginViews()
	{
		createPluginViews(PluginType.ViewFinder);
		createPluginViews(PluginType.Capture);
		createPluginViews(PluginType.Processing);
		createPluginViews(PluginType.Filter);
		createPluginViews(PluginType.Export);
	}

	private void createPluginViews(PluginType type)
	{
		int iInfoControlsRemainingHeight = iInfoViewHeight;

		List<View> info_views = null;
		Map<View, Plugin.ViewfinderZone> plugin_views = null;

		List<Plugin> plugins = PluginManager.getInstance().getActivePlugins(type);
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);
				if (plugin != null)
				{
					plugin_views = plugin.getPluginViews();
					addPluginViews(plugin_views);

					// Add info controls
					info_views = plugin.getInfoViews();
					for (int j = 0; j < info_views.size(); j++)
					{
						View infoView = info_views.get(j);

						// Calculate appropriate size of added plugin's view
						android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) infoView
								.getLayoutParams();
						viewLayoutParams = this.getTunedLinearLayoutParams(infoView, viewLayoutParams,
								iInfoViewMaxWidth, iInfoViewMaxHeight);

						if (iInfoControlsRemainingHeight >= viewLayoutParams.height)
						{
							iInfoControlsRemainingHeight -= viewLayoutParams.height;
							this.addInfoView(infoView, viewLayoutParams);
						}
					}
				}
			}
		}
	}

	//
	private void initDefaultQuickControls()
	{
		quickControl1 = null;
		quickControl2 = null;
		quickControl3 = null;
		quickControl4 = null;
		initDefaultQuickControls(quickControl1);
		initDefaultQuickControls(quickControl2);
		initDefaultQuickControls(quickControl3);
		initDefaultQuickControls(quickControl4);
	}

	private void initDefaultQuickControls(View quickControl)
	{
		LayoutInflater inflator = CameraScreenActivity.getInstance().getLayoutInflater();
		quickControl = inflator.inflate(R.layout.gui_almalence_invisible_button,
				(ViewGroup) guiView.findViewById(R.id.paramsLayout), false);
		quickControl.setOnClickListener(this);
	}

	// Called when camera object created in MainScreen.
	// After camera creation it is possibly to obtain
	// all camera possibilities such as supported scene mode, flash mode and
	// etc.
	@Override
	public void onCameraCreate()
	{
		String defaultQuickControl1 = "";
		String defaultQuickControl2 = "";
		String defaultQuickControl3 = "";
		String defaultQuickControl4 = "";

		// Remove buttons from previous camera start
		removeAllViews(topMenuButtons);
		removeAllViews(topMenuPluginButtons);

		mEVSupported = false;
		mSceneModeSupported = false;
		mWBSupported = false;
		mFocusModeSupported = false;
		mFlashModeSupported = false;
		mISOSupported = false;
		mMeteringAreasSupported = false;
		mCameraChangeSupported = false;

		mEVLockSupported = false;
		mWBLockSupported = false;

		activeScene.clear();
		activeWB.clear();
		activeFocus.clear();
		activeFlash.clear();
		activeISO.clear();
		activeMetering.clear();

		activeSceneNames.clear();
		activeWBNames.clear();
		activeFocusNames.clear();
		activeFlashNames.clear();
		activeISONames.clear();
		activeMeteringNames.clear();

		removeAllQuickViews();
		initDefaultQuickControls();

		createPluginTopMenuButtons();

		if (CameraController.isExposureLockSupported())
			mEVLockSupported = true;
		if (CameraController.isWhiteBalanceLockSupported())
			mWBLockSupported = true;

		// Create Exposure compensation button and slider with supported values
		if (CameraController.isExposureCompensationSupported())
		{
			mEVSupported = true;
			defaultQuickControl1 = String.valueOf(MODE_EV);

			float ev_step = CameraController.getExposureCompensationStep();

			int minValue = CameraController.getMinExposureCompensation();
			int maxValue = CameraController.getMaxExposureCompensation();

			SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
			if (evBar != null)
			{
				int initValue = preferences.getInt(CameraScreenActivity.sEvPref, 0);
				evBar.setMax(maxValue - minValue);
				evBar.setProgress(initValue + maxValue);

				TextView leftText = (TextView) guiView.findViewById(R.id.seekBarLeftText);
				TextView rightText = (TextView) guiView.findViewById(R.id.seekBarRightText);

				int minValueReal = Math.round(minValue * ev_step);
				int maxValueReal = Math.round(maxValue * ev_step);
				String minString = String.valueOf(minValueReal);
				String maxString = String.valueOf(maxValueReal);

				if (minValueReal > 0)
					minString = "+" + minString;
				if (maxValueReal > 0)
					maxString = "+" + maxString;

				leftText.setText(minString);
				rightText.setText(maxString);

				mEV = initValue;
				CameraController.setCameraExposureCompensation(mEV);

				evBar.setOnSeekBarChangeListener(this);
			}

			RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_EV);
			but.setImageResource(ICON_EV);
		} else
			mEVSupported = false;


			mSceneModeSupported = false;
			mSceneMode = -1;

		// Create White Balance mode button and adding supported white balances
		int[] supported_wb = CameraController.getSupportedWhiteBalance();
		if (supported_wb != null && supported_wb.length > 0 && activeWB != null)
		{
			for (int wb_name : supported_wb)
			{
				if (wbModeButtons.containsKey(wb_name))
				{
					activeWB.add(wbModeButtons.get(Integer.valueOf(wb_name)));
					activeWBNames.add(Integer.valueOf(wb_name));
				}
			}

			if (!activeWBNames.isEmpty())
			{
				mWBSupported = true;

				wbmodeAdapter.Elements = activeWB;
				GridView gridview = (GridView) guiView.findViewById(R.id.wbGrid);
				gridview.setAdapter(wbmodeAdapter);

				int initValue = preferences.getInt(CameraScreenActivity.sWBModePref, CameraScreenActivity.sDefaultValue);
				if (!activeWBNames.contains(initValue))
				{
					if (CameraController.isFrontCamera())
						initValue = activeWBNames.get(0);
					else
						initValue = CameraScreenActivity.sDefaultValue;
				}
				setButtonSelected(wbModeButtons, initValue);
				setCameraParameterValue(MODE_WB, initValue);

				if (ICONS_WB != null && ICONS_WB.containsKey(initValue))
				{
					RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_WB);
					int icon_id = ICONS_WB.get(initValue);
					but.setImageResource(icon_id);
				}

				CameraController.setCameraWhiteBalance(mWB);
			} else
			{
				mWBSupported = false;
				mWB = -1;
			}
		} else
		{
			mWBSupported = false;
			mWB = -1;
		}
			mFocusMode = -1;
			mFocusModeSupported = false;

		// Create Flash mode button and adding supported flash modes

        int[] supported_flash = CameraController.getSupportedFlashModes();
		if (supported_flash != null
				&& supported_flash.length > 0
				&& activeFlash != null
				&& !(supported_flash.length == 1 && CameraController.isModeAvailable(supported_flash,
						CameraParameters.FLASH_MODE_OFF)))
		{
			for (int flash_name : supported_flash)
			{
				if (flashModeButtons.containsKey(flash_name))
				{
					activeFlash.add(flashModeButtons.get(Integer.valueOf(flash_name)));
					activeFlashNames.add(Integer.valueOf(flash_name));
				}
			}

			if (!activeFlashNames.isEmpty())
			{
				mFlashModeSupported = true;
				defaultQuickControl2 = String.valueOf(MODE_FLASH);

				flashmodeAdapter.Elements = activeFlash;
				GridView gridview = (GridView) guiView.findViewById(R.id.flashmodeGrid);
				gridview.setAdapter(flashmodeAdapter);

				int initValue = preferences.getInt(CameraScreenActivity.sFlashModePref, CameraScreenActivity.sDefaultFlashValue);
				if (!activeFlashNames.contains(initValue))
				{
					if (CameraController.isFrontCamera())
						initValue = activeFlashNames.get(0);
					else
						initValue = CameraScreenActivity.sDefaultFlashValue;
				}
				setButtonSelected(flashModeButtons, initValue);
				setCameraParameterValue(MODE_FLASH, initValue);

				if (ICONS_FLASH != null && ICONS_FLASH.containsKey(initValue))
				{
					RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FLASH);
					int icon_id = ICONS_FLASH.get(initValue);
					but.setImageResource(icon_id);
				}

				CameraController.setCameraFlashMode(mFlashMode);
			} else
			{
				mFlashModeSupported = false;
				mFlashMode = -1;
			}
		} else
		{
			mFlashModeSupported = false;
			mFlashMode = -1;
		}

			mISOSupported = false;
			mISO = -1;


		this.mMeteringAreasSupported = false;
			this.mMeteringMode = -1;



			mCameraChangeSupported = false;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		String qc1 = prefs.getString(CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton1),
				defaultQuickControl1);
		String qc2 = prefs.getString(CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton2),
				defaultQuickControl2);
		String qc3 = prefs.getString(CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton3),
				defaultQuickControl3);
		String qc4 = prefs.getString(CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton4),
				defaultQuickControl4);

		quickControl1 = isCameraParameterSupported(qc1) ? getQuickControlButton(qc1, quickControl1)
				: getFreeQuickControlButton(qc1, qc2, qc3, qc4, quickControl1);
		quickControl2 = isCameraParameterSupported(qc2) ? getQuickControlButton(qc2, quickControl2)
				: getFreeQuickControlButton(qc1, qc2, qc3, qc4, quickControl2);
		quickControl3 = isCameraParameterSupported(qc3) ? getQuickControlButton(qc3, quickControl3)
				: getFreeQuickControlButton(qc1, qc2, qc3, qc4, quickControl3);
		quickControl4 = isCameraParameterSupported(qc4) ? getQuickControlButton(qc4, quickControl4)
				: getFreeQuickControlButton(qc1, qc2, qc3, qc4, quickControl4);

		try
		{
			((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl1);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("AlmalenceGUI", "addView exception: " + e.getMessage());
		}

		try
		{
			((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl2);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("AlmalenceGUI", "addView exception: " + e.getMessage());
		}

		try
		{
			((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl3);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("AlmalenceGUI", "addView exception: " + e.getMessage());
		}

		try
		{
			((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl4);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("AlmalenceGUI", "addView exception: " + e.getMessage());
		}

		if (mEVSupported)
		{
			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_EV_CHANGED);
		}

	}

	@Override
	public void onPluginsInitialized()
	{
		// Hide all opened menu
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		// create and fill drawing slider
		initSettingsMenu(true);
		initModeList();

		guiView.findViewById(R.id.blockingLayout).bringToFront();
		guiView.findViewById(R.id.postprocessingLayout).bringToFront();
		if (modeSelectorVisible)
		{
			guiView.findViewById(R.id.modeLayout).bringToFront();
		}
	}

	private boolean isCameraParameterSupported(String param)
	{
		if (!param.equals("") && topMenuPluginButtons.containsKey(param))
			return true;
		else if (!param.equals("") && com.almalence.util.Util.isNumeric(param))
		{
			int cameraParameter = Integer.valueOf(param);
            if(cameraParameter == MODE_EV){ return mEVSupported; }
            if(cameraParameter == MODE_WB){ return mWBSupported; }
            if(cameraParameter == MODE_FLASH){ return mFlashModeSupported; }



		}

		return false;
	}

	// bInitMenu - by default should be true. if called several simultaneously -
	// all should be false and last - true
	public void disableCameraParameter(CameraParameter iParam, boolean bDisable, boolean bInitMenu)
	{
		View topMenuView = null;
		switch (iParam)
		{
		case CAMERA_PARAMETER_EV:
			topMenuView = topMenuButtons.get(MODE_EV);
			isEVEnabled = !bDisable;
			break;

		case CAMERA_PARAMETER_WB:
			topMenuView = topMenuButtons.get(MODE_WB);
			isWBEnabled = !bDisable;
			break;

		case CAMERA_PARAMETER_FLASH:
			topMenuView = topMenuButtons.get(MODE_FLASH);
			isFlashEnabled = !bDisable;
			break;

		default:
			break;
		}

		if (topMenuView != null)
		{
			correctTopMenuButtonBackground(topMenuView, !bDisable);

			if (bInitMenu)
				initSettingsMenu(true);
		}

	}

	private void correctTopMenuButtonBackground(View topMenuView, boolean isEnabled)
	{

		if (topMenuView != null)
		{
			if (!isEnabled)
			{
				((RotateImageView) topMenuView).setColorFilter(0x50FAFAFA, PorterDuff.Mode.DST_IN);
			} else
			{
				((RotateImageView) topMenuView).clearColorFilter();
			}
		}
	}

	private View getQuickControlButton(String qcID, View defaultView)
	{
		if (!qcID.equals("") && topMenuPluginButtons.containsKey(qcID))
		{
			Plugin plugin = PluginManager.getInstance().getPlugin(qcID);
			RotateImageView view = (RotateImageView) topMenuPluginButtons.get(qcID);
			view.setImageResource(plugin.getQuickControlIconID());
			return view;
		} else if (!qcID.equals("") && topMenuButtons.containsKey(Integer.valueOf(qcID)))
			return topMenuButtons.get(Integer.valueOf(qcID));

		return defaultView;
	}

	// Method for finding a button for a top menu which not yet represented in
	// that top menu
	private View getFreeQuickControlButton(String qc1, String qc2, String qc3, String qc4, View emptyView)
	{
		Set<Integer> topMenuButtonsKeys = topMenuButtons.keySet();
		Iterator<Integer> topMenuButtonsIterator = topMenuButtonsKeys.iterator();

		Set<String> topMenuPluginButtonsKeys = topMenuPluginButtons.keySet();
		Iterator<String> topMenuPluginButtonsIterator = topMenuPluginButtonsKeys.iterator();

		// Searching for free button in the top menu buttons list (scene mode,
		// wb, focus, flash, camera switch)
		while (topMenuButtonsIterator.hasNext())
		{
			int id1, id2, id3, id4;
			try
			{
				id1 = Integer.valueOf(qc1);
			} catch (NumberFormatException exp)
			{
				id1 = -1;
			}
			try
			{
				id2 = Integer.valueOf(qc2);
			} catch (NumberFormatException exp)
			{
				id2 = -1;
			}
			try
			{
				id3 = Integer.valueOf(qc3);
			} catch (NumberFormatException exp)
			{
				id3 = -1;
			}
			try
			{
				id4 = Integer.valueOf(qc4);
			} catch (NumberFormatException exp)
			{
				id4 = -1;
			}

			int buttonID = topMenuButtonsIterator.next();
			View topMenuButton = topMenuButtons.get(buttonID);

			if (checkTopMenuButtonIntegerID(topMenuButton, buttonID, id1, id2, id3, id4)
					&& isCameraParameterSupported(String.valueOf(buttonID)))
				return topMenuButton;
		}

		// If top menu buttons dosn't have a free button, search in plugin's
		// buttons list
		while (topMenuPluginButtonsIterator.hasNext())
		{
			String buttonID = topMenuPluginButtonsIterator.next();
			View topMenuButton = topMenuPluginButtons.get(buttonID);

			if (checkTopMenuButtonStringID(topMenuButton, buttonID, qc1, qc2, qc3, qc4))
				return topMenuButton;
		}

		// If no button is found create a empty button
		return emptyView;
	}

	// Util function used to check if topMenuButton already added to top menu
	private boolean checkTopMenuButtonStringID(final View topMenuButton, final String buttonID, final String qc1,
			final String qc2, final String qc3, final String qc4)
	{
		return topMenuButton != quickControl1 && topMenuButton != quickControl2 && topMenuButton != quickControl3
				&& topMenuButton != quickControl4 && buttonID != qc1 && buttonID != qc2 && buttonID != qc3
				&& buttonID != qc4;
	}

	// Util function used to check if topMenuButton already added to top menu
	private boolean checkTopMenuButtonIntegerID(final View topMenuButton, final int buttonID, final int qc1,
			final int qc2, final int qc3, final int qc4)
	{
		return topMenuButton != quickControl1 && topMenuButton != quickControl2 && topMenuButton != quickControl3
				&& topMenuButton != quickControl4 && buttonID != qc1 && buttonID != qc2 && buttonID != qc3
				&& buttonID != qc4;
	}


	public void rotateSquareViews(final int degree, int duration)
	{
		if (AlmalenceGUI.mPreviousDeviceOrientation != AlmalenceGUI.mDeviceOrientation || duration == 0)
		{
			int startDegree = AlmalenceGUI.mPreviousDeviceOrientation == 0 ? 0
					: 360 - AlmalenceGUI.mPreviousDeviceOrientation;
			int endDegree = AlmalenceGUI.mDeviceOrientation == 0 ? 0 : 360 - AlmalenceGUI.mDeviceOrientation;

			int diff = endDegree - startDegree;
			// diff = diff >= 0 ? diff : 360 + diff; // make it in range [0,
			// 359]
			//
			// // Make it in range [-179, 180]. That's the shorted distance
			// between the
			// // two angles
			endDegree = diff > 180 ? endDegree - 360 : diff < -180 && endDegree == 0 ? 360 : endDegree;

			if (modeSelectorVisible)
				rotateViews(modeViews, startDegree, endDegree, duration);

			if (!settingsViews.isEmpty())
			{
                int delay = 0;
                //int delay = ((Panel) guiView.findViewById(R.id.topPanel)).isOpen() ? duration : 0;
				rotateViews(settingsViews, startDegree, endDegree, delay);
			}

			if (!quickControlChangeres.isEmpty() && this.quickControlsChangeVisible)
				rotateViews(quickControlChangeres, startDegree, endDegree, duration);

			rotateViews(activeScene, startDegree, endDegree, duration);
			rotateViews(activeWB, startDegree, endDegree, duration);
			rotateViews(activeFocus, startDegree, endDegree, duration);
			rotateViews(activeFlash, startDegree, endDegree, duration);
			rotateViews(activeISO, startDegree, endDegree, duration);
			rotateViews(activeMetering, startDegree, endDegree, duration);
		}
	}

	private void rotateViews(List<View> views, final float startDegree, final float endDegree, long duration)
	{
		for (int i = 0; i < views.size(); i++)
		{
			float start = startDegree;
			float end = endDegree;
			final View view = views.get(i);

			if (view == null)
				continue;

			duration = 0;
			if (duration == 0)
			{
				view.clearAnimation();
				view.setRotation(endDegree);
			} else
			{
				start = startDegree - view.getRotation();
				end = endDegree - view.getRotation();

				RotateAnimation animation = new RotateAnimation(start, end, view.getWidth() / 2, view.getHeight() / 2);

				animation.setDuration(duration);
				animation.setFillAfter(true);
				animation.setInterpolator(new DecelerateInterpolator());
				view.clearAnimation();
				view.startAnimation(animation);
			}
		}
	}

	/***************************************************************************************
	 * 
	 * addQuickSetting method
	 * 
	 * type: EV, SCENE, WB, FOCUS, FLASH, CAMERA or MORE
	 * 
	 * Common method for tune quick control's and quick setting view's icon and
	 * text
	 * 
	 * Quick controls and Quick settings views has a similar design but
	 * different behavior
	 * 
	 ****************************************************************************************/
	private void addQuickSetting(SettingsType type, boolean isQuickControl)
	{
		int icon_id = -1;
		CharSequence icon_text = "";
		boolean isEnabled = true;

		switch (type)
		{
		case SCENE:
            Log.e("SCENE:", ICONS_SCENE.toString());
			icon_id = ICONS_SCENE.get(mSceneMode);
			icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_scene);
			isEnabled = isSceneEnabled;
			break;
		case WB:
			icon_id = ICONS_WB.get(mWB);
			icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_wb);
			isEnabled = isWBEnabled;
			break;
		case FOCUS:
			try
			{
				if (mFocusMode == FOCUS_AF_LOCK)
					icon_id = R.drawable.gui_almalence_settings_focus_aflock;
				else
					icon_id = ICONS_FOCUS.get(mFocusMode);
				icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_focus);
				isEnabled = isFocusEnabled;
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("addQuickSetting", "icons_focus.get exception: " + e.getMessage());
			}
			break;
		case FLASH:
			icon_id = ICONS_FLASH.get(mFlashMode);
			icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_flash);
			isEnabled = isFlashEnabled;
			break;
		case ISO:
			icon_id = ICONS_ISO.get(mISO);
			icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_iso);
			isEnabled = isIsoEnabled;
			break;
		case METERING:
			icon_id = ICONS_METERING.get(mMeteringMode);
			icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_metering);
			isEnabled = isMeteringEnabled;
			break;
		case CAMERA:
			icon_id = ICON_CAM;
			if (!preferences.getBoolean(CameraScreenActivity.sUseFrontCameraPref, false))
				icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_rear);
			else
				icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_front);

			isEnabled = isCameraChangeEnabled;
			break;
		case EV:
			icon_id = ICON_EV;
			icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_exposure);
			isEnabled = isEVEnabled;
			break;
		case MORE:
			icon_id = ICON_SETTINGS;
			icon_text = CameraScreenActivity.getAppResources().getString(R.string.settings_mode_moresettings);
			break;
		default:
			break;
		}

		// Get required size of button
		LayoutInflater inflator = CameraScreenActivity.getInstance().getLayoutInflater();
		View settingView = inflator.inflate(R.layout.gui_almalence_quick_control_grid_element, null, false);
		ImageView iconView = (ImageView) settingView.findViewById(R.id.imageView);
		iconView.setImageResource(icon_id);
		TextView textView = (TextView) settingView.findViewById(R.id.textView);
		textView.setText(icon_text);

		if (!isEnabled && !isQuickControl)
		{
			iconView.setColorFilter(CameraScreenActivity.getMainContext().getResources().getColor(R.color.buttonDisabled),
					PorterDuff.Mode.DST_IN);
			textView.setTextColor(CameraScreenActivity.getMainContext().getResources().getColor(R.color.textDisabled));
		}

		// Create onClickListener of right type
		switch (type)
		{

		case WB:
			if (isQuickControl)
				createQuickControlWBOnClick(settingView);
			else
				createSettingWBOnClick(settingView);
			break;

		case FLASH:
			if (isQuickControl)
				createQuickControlFlashOnClick(settingView);
			else
				createSettingFlashOnClick(settingView);
			break;

		case EV:
			if (isQuickControl)
				createQuickControlEVOnClick(settingView);
			else
				createSettingEVOnClick(settingView);
			break;
		case MORE:
			if (isQuickControl)
				return;
			else
				createSettingMoreOnClick(settingView);
			break;
		default:
			break;
		}

		if (isQuickControl)
			quickControlChangeres.add(settingView);
		else
			settingsViews.add(settingView);
	}

	private void addPluginQuickSetting(Plugin plugin, boolean isQuickControl)
	{
		int iconID = plugin.getQuickControlIconID();
		String title = plugin.getQuickControlTitle();

		if (iconID <= 0)
			return;

		LayoutInflater inflator = CameraScreenActivity.getInstance().getLayoutInflater();
		View qcView = inflator.inflate(R.layout.gui_almalence_quick_control_grid_element, null, false);
		((ImageView) qcView.findViewById(R.id.imageView)).setImageResource(iconID);
		((TextView) qcView.findViewById(R.id.textView)).setText(title);

		createPluginQuickControlOnClick(plugin, qcView, isQuickControl);

		if (isQuickControl)
			quickControlChangeres.add(qcView);
		else
			settingsViews.add(qcView);

		plugin.setQuickControlView(qcView);
	}

	/***************************************************************************************
	 * 
	 * QUICK CONTROLS CUSTOMIZATION METHODS
	 * 
	 * begin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	 ****************************************************************************************/
	private void initQuickControlsMenu(View currentView)
	{
		quickControlChangeres.clear();
		if (quickControlAdapter.Elements != null)
		{
			quickControlAdapter.Elements.clear();
			quickControlAdapter.notifyDataSetChanged();
		}

		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			Integer id = it.next();

            if(id == R.id.evButton) {
                if (mEVSupported)
                    addQuickSetting(SettingsType.EV, true);
            }

            if(id == R.id.wbButton) {
                if (mWBSupported)
                    addQuickSetting(SettingsType.WB, true);
            }
            if(id == R.id.flashButton) {
                if (mFlashModeSupported)
                    addQuickSetting(SettingsType.FLASH, true);
            }
		}

		// Add quick conrols from plugins
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.ViewFinder));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.Capture));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.Processing));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.Filter));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(PluginType.Export));

		quickControlAdapter.Elements = quickControlChangeres;
		quickControlAdapter.notifyDataSetChanged();
	}

	private void initPluginQuickControls(List<Plugin> plugins)
	{
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);
				if (plugin == null)
					continue;

				addPluginQuickSetting(plugin, true);
			}
		}
	}

	private void initPluginSettingsControls(List<Plugin> plugins)
	{
		if (!plugins.isEmpty())
		{
			for (int i = 0; i < plugins.size(); i++)
			{
				Plugin plugin = plugins.get(i);
				if (plugin == null)
					continue;

				addPluginQuickSetting(plugin, false);
			}
		}
	}

	private void createPluginQuickControlOnClick(final Plugin plugin, View view, boolean isQuickControl)
	{
		if (isQuickControl)
			view.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					RotateImageView pluginButton = (RotateImageView) topMenuPluginButtons.get(plugin.getID());
					pluginButton.setImageResource(plugin.getQuickControlIconID());

					switchViews(currentQuickView, pluginButton, plugin.getID());
					recreateQuickControlsMenu();
					changeCurrentQuickControl(pluginButton);
					initQuickControlsMenu(currentQuickView);
					showQuickControlsSettings();
				}
			});
		else
			view.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					try
					{
						plugin.onQuickControlClick();

						int icon_id = plugin.getQuickControlIconID();
						String title = plugin.getQuickControlTitle();
						Drawable icon = CameraScreenActivity.getMainContext().getResources().getDrawable(icon_id);
						((ImageView) v.findViewById(R.id.imageView)).setImageResource(icon_id);
						((TextView) v.findViewById(R.id.textView)).setText(title);

						RotateImageView pluginButton = (RotateImageView) topMenuPluginButtons.get(plugin.getID());
						pluginButton.setImageDrawable(icon);

						initSettingsMenu(true);
					} catch (Exception e)
					{
						e.printStackTrace();
						Log.e("Almalence GUI", "createPluginQuickControlOnClick exception" + e.getMessage());
					}
				}
			});
	}

	private void createQuickControlEVOnClick(View ev)
	{
		ev.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_EV, CameraScreenActivity.getMainContext().getResources().getDrawable(ICON_EV));
			}

		});
	}

	private void createQuickControlWBOnClick(View wb)
	{
		wb.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_WB, CameraScreenActivity.getMainContext().getResources().getDrawable(ICONS_WB.get(mWB)));
			}
		});
	}

	private void createQuickControlFlashOnClick(View flash)
	{
		flash.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				quickControlOnClick(MODE_FLASH, CameraScreenActivity.getMainContext().getResources().getDrawable(ICONS_FLASH.get(mFlashMode)));
			}

		});
	}

	private void quickControlOnClick(int qc, Drawable icon)
	{
		RotateImageView view = (RotateImageView) topMenuButtons.get(qc);

		view.setImageDrawable(icon);
		
		switchViews(currentQuickView, view, String.valueOf(qc));
		recreateQuickControlsMenu();
		changeCurrentQuickControl(view);
		initQuickControlsMenu(currentQuickView);
		showQuickControlsSettings();
	}

	public void changeCurrentQuickControl(View newCurrent)
	{
		if (currentQuickView != null)
			currentQuickView.setBackgroundResource(R.drawable.transparent_background);

		currentQuickView = newCurrent;
		newCurrent.setBackgroundResource(R.drawable.layout_border_qc_button);
		((RotateImageView) newCurrent).setBackgroundEnabled(true);
	}

	private void showQuickControlsSettings()
	{
		unselectPrimaryTopMenuButtons(-1);
		hideSecondaryMenus();

		GridView gridview = (GridView) guiView.findViewById(R.id.qcGrid);
		gridview.setAdapter(quickControlAdapter);

		(guiView.findViewById(R.id.qcLayout)).setVisibility(View.VISIBLE);

		(guiView.findViewById(R.id.paramsLayout))
				.setBackgroundResource(R.drawable.blacktransparentlayertop);

		Set<Integer> topmenu_keys = topMenuButtons.keySet();
		Iterator<Integer> it = topmenu_keys.iterator();
		while (it.hasNext())
		{
			int key = it.next();
			if (currentQuickView != topMenuButtons.get(key))
			{
				((RotateImageView) topMenuButtons.get(key)).setBackgroundEnabled(true);
				topMenuButtons.get(key).setBackgroundResource(R.drawable.transparent_background);
			}
		}

		quickControlsChangeVisible = true;

		final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
				: AlmalenceGUI.mDeviceOrientation % 360 + 360;
		rotateSquareViews(degree, 0);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_LOCKED);
	}

	private void closeQuickControlsSettings()
	{
		RelativeLayout gridview = (RelativeLayout) guiView.findViewById(R.id.qcLayout);
		gridview.setVisibility(View.INVISIBLE);
		quickControlsChangeVisible = false;

		currentQuickView.setBackgroundResource(R.drawable.transparent_background);
		currentQuickView = null;

		(guiView.findViewById(R.id.paramsLayout))
				.setBackgroundResource(R.drawable.blacktransparentlayertop);

		correctTopMenuButtonBackground(CameraScreenActivity.getInstance().findViewById(MODE_EV), isEVEnabled);
		correctTopMenuButtonBackground(CameraScreenActivity.getInstance().findViewById(MODE_WB), isWBEnabled);
		correctTopMenuButtonBackground(CameraScreenActivity.getInstance().findViewById(MODE_FLASH), isFlashEnabled);
		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
	}

	/***************************************************************************************
	 * 
	 * QUICK CONTROLS CUSTOMIZATION METHODS
	 * 
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< end
	 ***************************************************************************************/

	/***************************************************************************************
	 * 
	 * SETTINGS MENU METHODS
	 * 
	 * begin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	 ****************************************************************************************/
	private void initSettingsMenu(boolean isDelayed)
	{
		if (isDelayed)
		{
			Handler h = new Handler();
			h.postDelayed(new Runnable()
			{
	
				@Override
				public void run()
				{
					initSettingsMenuBody();
				}
			}, 2000);
		}
		else
			initSettingsMenuBody();
	}
	
	private void initSettingsMenuBody()
	{
		// Clear view list to recreate all settings buttons
		settingsViews.clear();
		if (settingsAdapter.Elements != null)
		{
			settingsAdapter.Elements.clear();
			settingsAdapter.notifyDataSetChanged();
		}

		// Obtain all theoretical buttons we know
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			// If such camera feature is supported then add a button to
			// settings
			// menu
			Integer id = it.next();

            if(id == R.id.evButton) {
                if (mEVSupported)
                    addQuickSetting(SettingsType.EV, false);
            }
             if(id == R.id.wbButton) {
                 if (mWBSupported)
                     addQuickSetting(SettingsType.WB, false);
             }
            if(id == R.id.flashButton) {
                if (mFlashModeSupported)
                    addQuickSetting(SettingsType.FLASH, false);
            }
		}

		// Add quick conrols from plugins
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.ViewFinder));
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.Capture));
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.Processing));
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.Filter));
		initPluginSettingsControls(PluginManager.getInstance().getActivePlugins(PluginType.Export));

		settingsAdapter.Elements = settingsViews;

		final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
				: AlmalenceGUI.mDeviceOrientation % 360 + 360;
		rotateSquareViews(degree, 0);

	}


	private void createSettingWBOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isWBEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							CameraScreenActivity.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				int[] supported_wb = CameraController.getSupportedWhiteBalance();
				if (supported_wb == null)
					return;
				if (supported_wb.length > 0)
				{

					if (guiView.findViewById(R.id.wbLayout).getVisibility() != View.VISIBLE)
					{
						hideSecondaryMenus();
						showParams(MODE_WB);
					}

					else
						hideSecondaryMenus();
				}
			}
		});
	}


	private void createSettingFlashOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isFlashEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							CameraScreenActivity.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				int[] supported_flash = CameraController.getSupportedFlashModes();
				if (supported_flash == null)
					return;
				if (supported_flash.length > 0)
				{
					if (guiView.findViewById(R.id.flashmodeLayout).getVisibility() != View.VISIBLE)
					{
						hideSecondaryMenus();
						showParams(MODE_FLASH);
					} else
						hideSecondaryMenus();
				}
			}
		});
	}


	private void createSettingEVOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if (!isEVEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							CameraScreenActivity.getAppResources().getString(R.string.settings_not_available), true, false);
					return;
				}
				if (guiView.findViewById(R.id.evLayout).getVisibility() != View.VISIBLE)
				{
					hideSecondaryMenus();
					showParams(MODE_EV);
				} else
					hideSecondaryMenus();
			}
		});
	}

	private void createSettingMoreOnClick(View settingView)
	{
		settingView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				PluginManager.getInstance().onShowPreferences();
				Intent settingsActivity = new Intent(CameraScreenActivity.getMainContext(), Preferences.class);
				CameraScreenActivity.getInstance().startActivity(settingsActivity);
			}
		});
	}

	/***************************************************************************************
	 * 
	 * SETTINGS MENU METHODS
	 * 
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< end
	 ***************************************************************************************/

	private void cameraSwitched(boolean restart)
	{
		if (PluginManager.getInstance().getProcessingCounter() != 0)
			return;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		boolean isFrontCamera = prefs.getBoolean(
				CameraScreenActivity.getMainContext().getResources().getString(R.string.Preference_UseFrontCameraValue), false);
		prefs.edit()
				.putBoolean(
						CameraScreenActivity.getMainContext().getResources().getString(R.string.Preference_UseFrontCameraValue),
						!isFrontCamera).commit();

		if (restart)
		{
			CameraScreenActivity.getInstance().pauseMain();
			CameraScreenActivity.getInstance().resumeMain();
		}
	}

	private void hideModeList()
	{
		RelativeLayout gridview = (RelativeLayout) guiView.findViewById(R.id.modeLayout);

		Animation gone = AnimationUtils
				.loadAnimation(CameraScreenActivity.getInstance(), R.anim.gui_almalence_modelist_invisible);
		gone.setFillAfter(true);

		gridview.setAnimation(gone);
		(guiView.findViewById(R.id.modeGrid)).setAnimation(gone);

		gridview.setVisibility(View.GONE);
		(guiView.findViewById(R.id.modeGrid)).setVisibility(View.GONE);

		gone.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				guiView.findViewById(R.id.modeLayout).clearAnimation();
				guiView.findViewById(R.id.modeGrid).clearAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
				// Not used
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
				// Not used
			}
		});

		modeSelectorVisible = false;

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
	}

	@Override
	public void onHardwareShutterButtonPressed()
	{
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		PluginManager.getInstance().onShutterClick();
	}

	@Override
	public void onHardwareFocusButtonPressed()
	{
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		PluginManager.getInstance().onFocusButtonClick();
	}

	private void shutterButtonPressed()
	{
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		lockControls = true;
		PluginManager.getInstance().onShutterClick();
	}

	private void infoSlide(boolean toLeft, float xToVisible, float xToInvisible)
	{
		if ((infoSet == INFO_ALL & !toLeft) && isAnyViewOnViewfinder())
			infoSet = INFO_PARAMS;
		else if (infoSet == INFO_ALL && !isAnyViewOnViewfinder())
			infoSet = INFO_NO;
		else if (isAnyViewOnViewfinder())
			infoSet = (infoSet + 1 * (toLeft ? 1 : -1)) % 4;
		else
			infoSet = INFO_ALL;
		setInfo(toLeft, xToVisible, xToInvisible, true);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		prefs.edit().putInt(CameraScreenActivity.sDefaultInfoSetPref, infoSet).commit();
	}

	private void setInfo(boolean toLeft, float xToVisible, float xToInvisible, boolean isAnimate)
	{
		int pluginzoneWidth = guiView.findViewById(R.id.pluginsLayout).getWidth();
		int infozoneWidth = guiView.findViewById(R.id.infoLayout).getWidth();
		int screenWidth = pluginzoneWidth + infozoneWidth;

		AnimationSet rlinvisible = new AnimationSet(true);
		rlinvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet rlvisible = new AnimationSet(true);
		rlvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet lrinvisible = new AnimationSet(true);
		lrinvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet lrvisible = new AnimationSet(true);
		lrvisible.setInterpolator(new DecelerateInterpolator());

		int duration_invisible = 0;
		duration_invisible = isAnimate ? com.almalence.util.Util
				.clamp(Math.abs(Math.round(((toLeft ? xToVisible : (screenWidth - xToVisible)) * 500) / screenWidth)),
						10, 500) : 0;

		int duration_visible = 0;
		duration_visible = isAnimate ? com.almalence.util.Util.clamp(
				Math.abs(Math.round(((toLeft ? xToInvisible : (screenWidth - xToInvisible)) * 500) / screenWidth)), 10,
				500) : 0;

		Animation invisible_alpha = new AlphaAnimation(1, 0);
		invisible_alpha.setDuration(duration_invisible);
		invisible_alpha.setRepeatCount(0);

		Animation visible_alpha = new AlphaAnimation(0, 1);
		visible_alpha.setDuration(duration_visible);
		visible_alpha.setRepeatCount(0);

		Animation rlinvisible_translate = new TranslateAnimation(xToInvisible, -screenWidth, 0, 0);
		rlinvisible_translate.setDuration(duration_invisible);
		rlinvisible_translate.setFillAfter(true);

		Animation lrinvisible_translate = new TranslateAnimation(xToInvisible, screenWidth, 0, 0);
		lrinvisible_translate.setDuration(duration_invisible);
		lrinvisible_translate.setFillAfter(true);

		Animation rlvisible_translate = new TranslateAnimation(xToVisible, 0, 0, 0);
		rlvisible_translate.setDuration(duration_visible);
		rlvisible_translate.setFillAfter(true);

		Animation lrvisible_translate = new TranslateAnimation(xToVisible, 0, 0, 0);
		lrvisible_translate.setDuration(duration_visible);
		lrvisible_translate.setFillAfter(true);

		// Add animations to appropriate set
		rlinvisible.addAnimation(invisible_alpha);
		rlinvisible.addAnimation(rlinvisible_translate);

		rlvisible.addAnimation(rlvisible_translate);

		lrinvisible.addAnimation(invisible_alpha);
		lrinvisible.addAnimation(lrinvisible_translate);

		lrvisible.addAnimation(lrvisible_translate);

		int[] zonesVisibility = new int[] { View.GONE, View.GONE, View.GONE };
		switch (infoSet)
		{
		case INFO_ALL:
			zonesVisibility = initZonesVisibility(View.VISIBLE, View.VISIBLE, View.VISIBLE);
			break;

		case INFO_NO:
			zonesVisibility = initZonesVisibility(View.GONE, View.GONE, View.GONE);
			break;

		case INFO_GRID:
			zonesVisibility = initZonesVisibility(View.GONE, View.GONE, View.VISIBLE);
			break;

		case INFO_PARAMS:
			zonesVisibility = initZonesVisibility(View.VISIBLE, View.GONE, View.GONE);
			break;
		default:
			break;
		}
		applyZonesVisibility(zonesVisibility, isAnimate, toLeft, rlvisible, lrvisible, rlinvisible, lrinvisible);

		rlinvisible.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				clearLayoutsAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
				// Not used
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
				// Not used
			}
		});

		lrinvisible.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				clearLayoutsAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
				// Not used
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
				// Not used
			}
		});

		// checks preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		Editor prefsEditor = prefs.edit();
		prefsEditor.putInt(CameraScreenActivity.sDefaultInfoSetPref, infoSet);
		prefsEditor.commit();
	}

	private int[] initZonesVisibility(final int zoneVisibiltyFirst, final int zoneVisibiltySecond,
			final int zoneVisibilityThird)
	{
		int[] zonesVisibility = new int[3];
		zonesVisibility[0] = zoneVisibiltyFirst;
		zonesVisibility[1] = zoneVisibiltySecond;
		zonesVisibility[2] = zoneVisibilityThird;

		return zonesVisibility;
	}

	private void applyZonesVisibility(int[] zonesVisibility, boolean isAnimate, boolean toLeft, AnimationSet rlvisible,
			AnimationSet lrvisible, AnimationSet rlinvisible, AnimationSet lrinvisible)
	{
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		if (guiView.findViewById(R.id.paramsLayout).getVisibility() != zonesVisibility[0])
		{
			if (isAnimate)
				guiView.findViewById(R.id.paramsLayout).startAnimation(
						toLeft ? zonesVisibility[0] == View.VISIBLE ? rlvisible : rlinvisible
								: zonesVisibility[0] == View.VISIBLE ? lrvisible : lrinvisible);
			guiView.findViewById(R.id.paramsLayout).setVisibility(zonesVisibility[0]);
			//((Panel) guiView.findViewById(R.id.topPanel)).reorder(zonesVisibility[0] == View.GONE, true);
		}

		if (guiView.findViewById(R.id.pluginsLayout).getVisibility() != zonesVisibility[1])
		{
			if (isAnimate)
				guiView.findViewById(R.id.pluginsLayout).startAnimation(
						toLeft ? zonesVisibility[1] == View.VISIBLE ? rlvisible : rlinvisible
								: zonesVisibility[1] == View.VISIBLE ? lrvisible : lrinvisible);
			guiView.findViewById(R.id.pluginsLayout).setVisibility(zonesVisibility[1]);

			if (isAnimate)
				guiView.findViewById(R.id.infoLayout).startAnimation(
						toLeft ? zonesVisibility[1] == View.VISIBLE ? rlvisible : rlinvisible
								: zonesVisibility[1] == View.VISIBLE ? lrvisible : lrinvisible);
			guiView.findViewById(R.id.infoLayout).setVisibility(zonesVisibility[1]);
		}

		if (guiView.findViewById(R.id.fullscreenLayout).getVisibility() != zonesVisibility[2])
		{
			if (isAnimate)
				guiView.findViewById(R.id.fullscreenLayout).startAnimation(
						toLeft ? zonesVisibility[2] == View.VISIBLE ? rlvisible : rlinvisible
								: zonesVisibility[2] == View.VISIBLE ? lrvisible : lrinvisible);
			guiView.findViewById(R.id.fullscreenLayout).setVisibility(zonesVisibility[2]);
		}
	}

	private void clearLayoutsAnimation()
	{
		guiView.findViewById(R.id.paramsLayout).clearAnimation();
		guiView.findViewById(R.id.pluginsLayout).clearAnimation();
		guiView.findViewById(R.id.fullscreenLayout).clearAnimation();
		guiView.findViewById(R.id.infoLayout).clearAnimation();
	}

	// Method used by quick controls customization feature. Swaps current quick
	// control with selected from qc grid.
	private void switchViews(View currentView, View newView, String qcID)
	{
		if (currentView == newView)
			return;

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());

		int currentQCNumber = -1;
		String currentViewID = "";
		if (currentView == quickControl1)
		{
			currentViewID = pref.getString(
					CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton1), "");
			currentQCNumber = 1;
		} else if (currentView == quickControl2)
		{
			currentViewID = pref.getString(
					CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton2), "");
			currentQCNumber = 2;
		} else if (currentView == quickControl3)
		{
			currentViewID = pref.getString(
					CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton3), "");
			currentQCNumber = 3;
		} else if (currentView == quickControl4)
		{
			currentViewID = pref.getString(
					CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton4), "");
			currentQCNumber = 4;
		}

		if (newView == quickControl1)
		{
			quickControl1 = currentView;
			pref.edit()
					.putString(CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton1),
							currentViewID).commit();
		} else if (newView == quickControl2)
		{
			quickControl2 = currentView;
			pref.edit()
					.putString(CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton2),
							currentViewID).commit();
		} else if (newView == quickControl3)
		{
			quickControl3 = currentView;
			pref.edit()
					.putString(CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton3),
							currentViewID).commit();
		} else if (newView == quickControl4)
		{
			quickControl4 = currentView;
			pref.edit()
					.putString(CameraScreenActivity.getAppResources().getString(R.string.Preference_QuickControlButton4),
							currentViewID).commit();
		} else
		{
			if (currentView.getParent() != null)
				((ViewGroup) currentView.getParent()).removeView(currentView);
		}

		switch (currentQCNumber)
		{
		case 1:
			quickControl1 = newView;
			pref.edit().putString("quickControlButton1", qcID).commit();
			break;
		case 2:
			quickControl2 = newView;
			pref.edit().putString("quickControlButton2", qcID).commit();
			break;
		case 3:
			quickControl3 = newView;
			pref.edit().putString("quickControlButton3", qcID).commit();
			break;
		case 4:
			quickControl4 = newView;
			pref.edit().putString("quickControlButton4", qcID).commit();
			break;
		default:
			break;
		}
	}

	private void recreateQuickControlsMenu()
	{
		removeAllViews(topMenuButtons);
		removeAllViews(topMenuPluginButtons);
		removeAllQuickViews();

		((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl1);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl2);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl3);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout)).addView(quickControl4);
	}

	private void removeAllQuickViews()
	{
		if (quickControl1 != null && quickControl1.getParent() != null)
			((ViewGroup) quickControl1.getParent()).removeView(quickControl1);
		if (quickControl2 != null && quickControl2.getParent() != null)
			((ViewGroup) quickControl2.getParent()).removeView(quickControl2);
		if (quickControl3 != null && quickControl3.getParent() != null)
			((ViewGroup) quickControl3.getParent()).removeView(quickControl3);
		if (quickControl4 != null && quickControl4.getParent() != null)
			((ViewGroup) quickControl4.getParent()).removeView(quickControl4);
	}

	private void removeAllViews(Map<?, View> buttons)
	{
		Collection<View> button_set = buttons.values();
		Iterator<View> it = button_set.iterator();
		while (it.hasNext())
		{
			View view = it.next();
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);
		}
	}

	private void removePluginViews()
	{
		List<View> pluginsView = new ArrayList<View>();
		RelativeLayout pluginsLayout = (RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.pluginsLayout);
		for (int i = 0; i < pluginsLayout.getChildCount(); i++)
			pluginsView.add(pluginsLayout.getChildAt(i));

		for (int j = 0; j < pluginsView.size(); j++)
		{
			View view = pluginsView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			pluginsLayout.removeView(view);
		}

		List<View> fullscreenView = new ArrayList<View>();
		RelativeLayout fullscreenLayout = (RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.fullscreenLayout);
		for (int i = 0; i < fullscreenLayout.getChildCount(); i++)
			fullscreenView.add(fullscreenLayout.getChildAt(i));

		for (int j = 0; j < fullscreenView.size(); j++)
		{
			View view = fullscreenView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			fullscreenLayout.removeView(view);
		}

		List<View> infoView = new ArrayList<View>();
		LinearLayout infoLayout = (LinearLayout) CameraScreenActivity.getInstance().findViewById(R.id.infoLayout);
		for (int i = 0; i < infoLayout.getChildCount(); i++)
			infoView.add(infoLayout.getChildAt(i));

		for (int j = 0; j < infoView.size(); j++)
		{
			View view = infoView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			infoLayout.removeView(view);
		}
	}


	@Override
	public void onClick(View v)
	{
		hideSecondaryMenus();
		if (!quickControlsChangeVisible)
		{
			if (topMenuPluginButtons.containsValue(v))
			{
				Set<String> pluginIDset = topMenuPluginButtons.keySet();
				Iterator<String> it = pluginIDset.iterator();
				while (it.hasNext())
				{
					String pluginID = it.next();
					if (v == topMenuPluginButtons.get(pluginID))
					{
						Plugin plugin = PluginManager.getInstance().getPlugin(pluginID);
						plugin.onQuickControlClick();

						int icon_id = plugin.getQuickControlIconID();
						Drawable icon = CameraScreenActivity.getMainContext().getResources().getDrawable(icon_id);
						((RotateImageView) v).setImageDrawable(icon);

						initSettingsMenu(false);
						break;
					}
				}
			}
			return;
		}

		changeCurrentQuickControl(v);

		initQuickControlsMenu(v);
		showQuickControlsSettings();
	}

	@Override
	public void onButtonClick(View button)
	{

		int id = button.getId();
		if (lockControls && R.id.buttonShutter != id)
			return;

		// 1. if quick settings slider visible - lock everything
		// 2. if modes visible - allow only selectmode button
		// 3. if change quick controls visible - allow only OK button

		if (settingsControlsVisible || quickControlsChangeVisible
				|| (modeSelectorVisible /*&& (R.id.buttonSelectMode != id)*/))
		{
			// if change control visible and
			if (quickControlsChangeVisible)
			{
				// quick control button pressed
				if ((button != quickControl1) && (button != quickControl2) && (button != quickControl3)
						&& (button != quickControl4))
				{
					closeQuickControlsSettings();
					return;
				}
			}
			if (modeSelectorVisible)
			{
				hideModeList();
				return;
			}
		}

		if(id == R.id.buttonShutter) {
            if (quickControlsChangeVisible || settingsControlsVisible) {

            }else
            if (quickControlsVisible) {
                hideSecondaryMenus();
                unselectPrimaryTopMenuButtons(-1);
                //guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
                quickControlsVisible = false;
            }else {
                shutterButtonPressed();
            }
        }

        // settings
        if(id == R.id.evButton)
			{
				if (changeQuickControlIfVisible(button)){

                }else
				if (!isEVEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							CameraScreenActivity.getAppResources().getString(R.string.settings_not_available), true, false);
				}else {

                    LinearLayout layout = (LinearLayout) guiView.findViewById(R.id.evLayout);
                    if (layout.getVisibility() == View.GONE) {
                        unselectPrimaryTopMenuButtons(MODE_EV);
                        hideSecondaryMenus();
                        showParams(MODE_EV);
                        quickControlsVisible = true;
                    } else {
                        quickControlsVisible = false;
                        unselectPrimaryTopMenuButtons(-1);
                        hideSecondaryMenus();
                    }
                }
			}

        if(id == R.id.wbButton)
			{
				if (changeQuickControlIfVisible(button)){

                }else
				if (!isWBEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							CameraScreenActivity.getAppResources().getString(R.string.settings_not_available), true, false);

				}else {

                    RelativeLayout layout = (RelativeLayout) guiView.findViewById(R.id.wbLayout);
                    if (layout.getVisibility() == View.GONE) {
                        quickControlsVisible = true;
                        unselectPrimaryTopMenuButtons(MODE_WB);
                        hideSecondaryMenus();
                        showParams(MODE_WB);
                    } else {
                        quickControlsVisible = false;
                        unselectPrimaryTopMenuButtons(-1);
                        hideSecondaryMenus();
                    }
                }
			}

        if(id == R.id.flashButton)
			{
				if (changeQuickControlIfVisible(button)){

                }else
                if (!isFlashEnabled)
				{
					showToast(null, Toast.LENGTH_SHORT, Gravity.CENTER,
							CameraScreenActivity.getAppResources().getString(R.string.settings_not_available), true, false);
				}else {

                    RelativeLayout layout = (RelativeLayout) guiView.findViewById(R.id.flashmodeLayout);
                    if (layout.getVisibility() == View.GONE) {
                        quickControlsVisible = true;
                        unselectPrimaryTopMenuButtons(MODE_FLASH);
                        hideSecondaryMenus();
                        showParams(MODE_FLASH);
                    } else {
                        quickControlsVisible = false;
                        unselectPrimaryTopMenuButtons(-1);
                        hideSecondaryMenus();
                    }
                }
			}

		this.initSettingsMenu(false);
	}

	private boolean changeQuickControlIfVisible(View button)
	{
		if (quickControlsChangeVisible)
		{
			changeCurrentQuickControl(button);
			initQuickControlsMenu(button);
			showQuickControlsSettings();
			return true;
		}

		return false;
	}

	private void setWhiteBalance(int newMode)
	{
		if (newMode != -1)
		{
			CameraController.setCameraWhiteBalance(newMode);
			mWB = newMode;
			setButtonSelected(wbModeButtons, mWB);
			preferences.edit().putInt(CameraScreenActivity.sWBModePref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_WB);
		int icon_id = ICONS_WB.get(mWB);
		but.setImageResource(icon_id);

		initSettingsMenu(false);
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_WB_CHANGED);
	}

	private void setFlashMode(int newMode)
	{
		if (newMode != -1)
		{
			CameraController.setCameraFlashMode(newMode);
			mFlashMode = newMode;
			setButtonSelected(flashModeButtons, mFlashMode);

			preferences.edit().putInt(CameraScreenActivity.sFlashModePref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FLASH);
		int icon_id = ICONS_FLASH.get(mFlashMode);
		but.setImageResource(icon_id);

		initSettingsMenu(false);
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_FLASH_CHANGED);
	}

	// Hide all pop-up layouts
	private void unselectPrimaryTopMenuButtons(int iTopMenuButtonSelected)
	{
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			Integer it_button = it.next();
			(topMenuButtons.get(it_button)).setPressed(false);
			(topMenuButtons.get(it_button)).setSelected(false);
		}

		if ((iTopMenuButtonSelected > -1) && topMenuButtons.containsKey(iTopMenuButtonSelected))
		{
			RotateImageView pressed_button = (RotateImageView) topMenuButtons.get(iTopMenuButtonSelected);
			pressed_button.setPressed(false);
			pressed_button.setSelected(true);

		}

		quickControlsVisible = false;
	}

	private int findTopMenuButtonIndex(View view)
	{
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		Integer pressed_button = -1;
		while (it.hasNext())
		{
			Integer it_button = it.next();
			View v = topMenuButtons.get(it_button);
			if (v == view)
			{
				pressed_button = it_button;
				break;
			}
		}

		return pressed_button;
	}

	private void topMenuButtonPressed(int iTopMenuButtonPressed)
	{
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			Integer it_button = it.next();
			if (isTopMenuButtonEnabled(it_button))
				(topMenuButtons.get(it_button)).setBackgroundDrawable(CameraScreenActivity.getMainContext().getResources()
						.getDrawable(R.drawable.almalence_gui_button_background));
		}
        //transparent_background
		if ((iTopMenuButtonPressed > -1 && isTopMenuButtonEnabled(iTopMenuButtonPressed))
				&& topMenuButtons.containsKey(iTopMenuButtonPressed))
		{
			RotateImageView pressed_button = (RotateImageView) topMenuButtons.get(iTopMenuButtonPressed);
			pressed_button.setBackgroundDrawable(CameraScreenActivity.getMainContext().getResources()
					.getDrawable(R.drawable.almalence_gui_button_background_pressed));

		}
	}

	public boolean isTopMenuButtonEnabled(int iTopMenuButtonKey)
	{
		boolean isEnabled = false;

        if(iTopMenuButtonKey == MODE_WB ) { if (isWBEnabled) isEnabled = true; }
        if(iTopMenuButtonKey == MODE_FLASH ) { if (isFlashEnabled) isEnabled = true; }
        if(iTopMenuButtonKey == MODE_EV ) { if (isEVEnabled) isEnabled = true; }
		return isEnabled;
	}

	// Hide all pop-up layouts
	@Override
	public void hideSecondaryMenus()
	{

        int c = CameraScreenActivity.getMainContext().getResources().getColor(android.R.color.transparent);
        CameraScreenActivity.getInstance().findViewById(R.id.paramsLayout).setBackgroundColor(c);

		if (!isSecondaryMenusVisible())
			return;

		guiView.findViewById(R.id.evLayout).setVisibility(View.GONE);
		//guiView.findViewById(R.id.scenemodeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.wbLayout).setVisibility(View.GONE);
		//guiView.findViewById(R.id.focusmodeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.flashmodeLayout).setVisibility(View.GONE);
		//guiView.findViewById(R.id.isoLayout).setVisibility(View.GONE);
		//guiView.findViewById(R.id.meteringLayout).setVisibility(View.GONE);

		guiView.findViewById(R.id.modeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.vfLayout).setVisibility(View.GONE);

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
	}

	public boolean isSecondaryMenusVisible()
	{
		if (guiView.findViewById(R.id.evLayout).getVisibility() == View.VISIBLE
				//|| guiView.findViewById(R.id.scenemodeLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.wbLayout).getVisibility() == View.VISIBLE
				//|| guiView.findViewById(R.id.focusmodeLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.flashmodeLayout).getVisibility() == View.VISIBLE
				//|| guiView.findViewById(R.id.isoLayout).getVisibility() == View.VISIBLE
				//|| guiView.findViewById(R.id.meteringLayout).getVisibility() == View.VISIBLE
                ) {

            return true;
        }
		return false;
	}

	// Decide what layout to show when some main's parameters button is clicked
	private void showParams(int iButton)
	{

        int c = CameraScreenActivity.getMainContext().getResources().getColor(R.color.bg_oc_blue);
        CameraScreenActivity.getInstance().findViewById(R.id.paramsLayout).setBackgroundColor(c);

		DisplayMetrics metrics = new DisplayMetrics();
		CameraScreenActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int width = metrics.widthPixels;
		int modeHeightByWidth = (int) (width / 3 - 5 * metrics.density);
		int modeHeightByDimen = Math.round(CameraScreenActivity.getAppResources().getDimension(R.dimen.gridImageSize)
				+ CameraScreenActivity.getAppResources().getDimension(R.dimen.gridTextLayoutHeight));

		int modeHeight = modeHeightByDimen > modeHeightByWidth ? modeHeightByWidth : modeHeightByDimen;

		AbsListView.LayoutParams params = new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT, modeHeight);

		List<View> views = null;

        if( iButton == MODE_WB){ views = activeWB; }
        if( iButton == MODE_FLASH){ views = activeFlash; }
        if (views != null)
		{
			for (int i = 0; i < views.size(); i++)
			{
				View param = views.get(i);
				if (param != null)
					param.setLayoutParams(params);
			}
		}


        if(iButton == MODE_EV ){
            guiView.findViewById(R.id.evLayout).setVisibility(View.VISIBLE);
        }

        if(iButton == MODE_WB ){
            guiView.findViewById(R.id.wbLayout).setVisibility(View.VISIBLE);
        }

        if(iButton == MODE_FLASH ){
            guiView.findViewById(R.id.flashmodeLayout).setVisibility(View.VISIBLE);
        }

		quickControlsVisible = true;

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_LOCKED);
	}

	private void setButtonSelected(Map<Integer, View> buttonsList, int mode_id)
	{
		Set<Integer> keys = buttonsList.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext())
		{
			int it_button = it.next();
			(buttonsList.get(it_button)).setPressed(false);
		}

		if (buttonsList.containsKey(mode_id))
		{
			RelativeLayout pressed_button = (RelativeLayout) buttonsList.get(mode_id);
			pressed_button.setPressed(false);
		}
	}

	private void setCameraParameterValue(int iParameter, int sValue)
	{
        if(iParameter == MODE_WB ){ mWB = sValue; }
        if(iParameter == MODE_FLASH ){ mFlashMode = sValue; }
	}



	// Public interface for all plugins
	// Automatically decide where to put view and correct view's size if
	// necessary
	@Override
	protected void addPluginViews(Map<View, Plugin.ViewfinderZone> views_map)
	{
		Set<View> view_set = views_map.keySet();
		Iterator<View> it = view_set.iterator();
		while (it.hasNext())
		{

			try
			{
				View view = it.next();
				Plugin.ViewfinderZone desire_zone = views_map.get(view);

				android.widget.RelativeLayout.LayoutParams viewLayoutParams = (android.widget.RelativeLayout.LayoutParams) view
						.getLayoutParams();
				viewLayoutParams = this.getTunedPluginLayoutParams(view, desire_zone, viewLayoutParams);

				if (viewLayoutParams == null) // No free space on plugin's
												// layout
					return;

				view.setLayoutParams(viewLayoutParams);
				if (view.getParent() != null)
					((ViewGroup) view.getParent()).removeView(view);

				if (desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN
						|| desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER)
					((RelativeLayout) guiView.findViewById(R.id.fullscreenLayout)).addView(view, 0, viewLayoutParams);
				else
					((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).addView(view, viewLayoutParams);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("Almalence GUI", "addPluginViews exception: " + e.getMessage());
			}
		}
	}

	@Override
	public void addViewQuick(View view, Plugin.ViewfinderZone desire_zone)
	{
		android.widget.RelativeLayout.LayoutParams viewLayoutParams = (android.widget.RelativeLayout.LayoutParams) view
				.getLayoutParams();
		viewLayoutParams = this.getTunedPluginLayoutParams(view, desire_zone, viewLayoutParams);

		if (viewLayoutParams == null) // No free space on plugin's layout
			return;

		view.setLayoutParams(viewLayoutParams);
		if (desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN
				|| desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER)
			((RelativeLayout) guiView.findViewById(R.id.fullscreenLayout)).addView(view, 0,viewLayoutParams);
		else
			((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).addView(view, viewLayoutParams);
	}

	@Override
	protected void removePluginViews(Map<View, Plugin.ViewfinderZone> views_map)
	{
		Set<View> view_set = views_map.keySet();
		Iterator<View> it = view_set.iterator();
		while (it.hasNext())
		{
			View view = it.next();
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).removeView(view);
		}
	}

	@Override
	public void removeViewQuick(View view)
	{
		if (view == null)
			return;
		if (view.getParent() != null)
			((ViewGroup) view.getParent()).removeView(view);

		((RelativeLayout) guiView.findViewById(R.id.pluginsLayout)).removeView(view);
	}

	/* Private section for adding plugin's views */

	// INFO VIEW SECTION
	@Override
	protected void addInfoView(View view, android.widget.LinearLayout.LayoutParams viewLayoutParams)
	{
		if (((LinearLayout) guiView.findViewById(R.id.infoLayout)).getChildCount() != 0)
			viewLayoutParams.topMargin = 4;

		((LinearLayout) guiView.findViewById(R.id.infoLayout)).addView(view, viewLayoutParams);
	}

	@Override
	protected void removeInfoView(View view)
	{
		if (view.getParent() != null)
			((ViewGroup) view.getParent()).removeView(view);
		((LinearLayout) guiView.findViewById(R.id.infoLayout)).removeView(view);
	}

	protected boolean isAnyViewOnViewfinder()
	{
		RelativeLayout pluginsLayout = (RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.pluginsLayout);
		LinearLayout infoLayout = (LinearLayout) CameraScreenActivity.getInstance().findViewById(R.id.infoLayout);
		RelativeLayout fullScreenLayout = (RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.fullscreenLayout);

		return pluginsLayout.getChildCount() > 0 || infoLayout.getChildCount() > 0
				|| fullScreenLayout.getChildCount() > 0;
	}

	// controls if info about new mode shown or not. to prevent from double info
	private void initModeList()
	{
		boolean initModeList = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext()).getBoolean(CameraScreenActivity.sInitModeListPref, false);
		if (activeMode != null && !initModeList)
		{
			return;
		}
		
		PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext()).edit().putBoolean(CameraScreenActivity.sInitModeListPref, false).commit();

		modeViews.clear();
		buttonModeViewAssoc.clear();

		List<Mode> hash = ConfigParser.getInstance().getList();
		Iterator<Mode> it = hash.iterator();

		int mode_number = 0;
		while (it.hasNext())
		{
			try
			{
				Mode tmp = it.next();

				LayoutInflater inflator = CameraScreenActivity.getInstance().getLayoutInflater();
				View mode = inflator.inflate(R.layout.gui_almalence_select_mode_grid_element, null, false);
				// set some mode icon
				((ImageView) mode.findViewById(R.id.modeImage)).setImageResource(CameraScreenActivity
						.getInstance()
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? tmp.iconHAL : tmp.icon, "drawable",
								CameraScreenActivity.getInstance().getPackageName()));

				int id = CameraScreenActivity
						.getInstance()
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? tmp.modeNameHAL : tmp.modeName, "string",
								CameraScreenActivity.getInstance().getPackageName());
				String modename = CameraScreenActivity.getAppResources().getString(id);

				((TextView) mode.findViewById(R.id.modeText)).setText(modename);
				if (mode_number == 0)
					mode.setOnTouchListener(new OnTouchListener()
					{
						@Override
						public boolean onTouch(View v, MotionEvent event)
						{
							if (event.getAction() == MotionEvent.ACTION_CANCEL)// &&
																				// isFirstMode)
							{
								return changeMode(v);
							}
							return false;
						}

					});
				mode.setOnClickListener(new OnClickListener()
				{
					public void onClick(View v)
					{
						changeMode(v);
					}
				});
				buttonModeViewAssoc.put(mode, tmp.modeID);
				modeViews.add(mode);

				// select active mode in grid with frame
				if (PluginManager.getInstance().getActiveModeID() == tmp.modeID)
				{
					mode.findViewById(R.id.modeSelectLayout2).setBackgroundResource(
							R.drawable.thumbnail_background_selected_inner);

					activeMode = (ViewGroup) mode;
				}
				mode_number++;
			} catch (Exception e)
			{
				e.printStackTrace();
			} catch (OutOfMemoryError e)
			{
				e.printStackTrace();
			}
		}

		modeAdapter.Elements = modeViews;
	}

	private boolean changeMode(View v)
	{
		activeMode.findViewById(R.id.modeSelectLayout2).setBackgroundResource(R.drawable.underlayer);
		v.findViewById(R.id.modeSelectLayout2).setBackgroundResource(R.drawable.thumbnail_background_selected_inner);
		activeMode = (ViewGroup) v;

		hideModeList();

		// get mode associated with pressed button
		String key = buttonModeViewAssoc.get(v);
		Mode mode = ConfigParser.getInstance().getMode(key);
		// if selected the same mode - do not reinitialize camera
		// and other objects.
		if (PluginManager.getInstance().getActiveModeID() == mode.modeID)
			return false;

		final Mode tmpActiveMode = mode;

		if (mode.modeID.equals("video"))
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
			if (prefs.getBoolean("videoStartStandardPref", false))
			{
				PluginManager.getInstance().onPause(true);
				Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
				CameraScreenActivity.getInstance().startActivity(intent);
				return true;
			}
		}

		new CountDownTimer(100, 100)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				PluginManager.getInstance().switchMode(tmpActiveMode);
			}
		}.start();

		// set modes icon inside mode selection icon
		Bitmap bm = null;
		Bitmap iconBase = BitmapFactory.decodeResource(CameraScreenActivity.getMainContext().getResources(),
				R.drawable.gui_almalence_select_mode);
		Bitmap iconOverlay = BitmapFactory.decodeResource(
				CameraScreenActivity.getMainContext().getResources(),
				CameraScreenActivity
						.getInstance()
						.getResources()
						.getIdentifier(CameraController.isUseSuperMode() ? mode.iconHAL : mode.icon, "drawable",
								CameraScreenActivity.getInstance().getPackageName()));
		iconOverlay = Bitmap.createScaledBitmap(iconOverlay, (int) (iconBase.getWidth() / 1.8),
				(int) (iconBase.getWidth() / 1.8), false);

		bm = mergeImage(iconBase, iconOverlay);
		bm = Bitmap.createScaledBitmap(bm,
				(int) (CameraScreenActivity.getMainContext().getResources().getDimension(R.dimen.mainButtonHeightSelect)),
				(int) (CameraScreenActivity.getMainContext().getResources().getDimension(R.dimen.mainButtonHeightSelect)), false);
		//((RotateImageView) guiView.findViewById(R.id.buttonSelectMode)).setImageBitmap(bm);

		int rid = CameraScreenActivity.getAppResources().getIdentifier(tmpActiveMode.howtoText, "string",
				CameraScreenActivity.getInstance().getPackageName());
		String howto = "";
		if (rid != 0)
			howto = CameraScreenActivity.getAppResources().getString(rid);
		// show toast on mode changed
		showToast(
				v,
				Toast.LENGTH_SHORT,
				Gravity.CENTER,
				((TextView) v.findViewById(R.id.modeText)).getText() + " "
						+ CameraScreenActivity.getAppResources().getString(R.string.almalence_gui_selected)
						+ (tmpActiveMode.howtoText.isEmpty() ? "" : "\n") + howto, false, true);
		return false;
	}

	public void showToast(final View v, final int showLength, final int gravity, final String toastText,
			final boolean withBackground, final boolean startOffset)
	{
		CameraScreenActivity.getInstance().runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				final RelativeLayout modeLayout = (RelativeLayout) guiView.findViewById(R.id.changeModeToast);

				DisplayMetrics metrics = new DisplayMetrics();
				CameraScreenActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(metrics);
				int screen_height = metrics.heightPixels;
				int screen_width = metrics.widthPixels;

				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) modeLayout.getLayoutParams();
				int[] rules = lp.getRules();
				if (gravity == Gravity.CENTER || (mDeviceOrientation != 0 && mDeviceOrientation != 360))
					lp.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
				else
				{
					rules[RelativeLayout.CENTER_IN_PARENT] = 0;
					lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);

					View shutter = guiView.findViewById(R.id.buttonShutter);
					int shutter_height = shutter.getHeight() + shutter.getPaddingBottom();
					lp.setMargins(
							0,
							(int) (screen_height
									- CameraScreenActivity.getAppResources().getDimension(R.dimen.paramsLayoutHeight) - shutter_height),
							0, shutter_height);
				}

				if (withBackground)
					modeLayout.setBackgroundDrawable(CameraScreenActivity.getAppResources().getDrawable(
							R.drawable.almalence_gui_toast_background));
				else
					modeLayout.setBackgroundDrawable(null);
				RotateImageView imgView = (RotateImageView) modeLayout.findViewById(R.id.selectModeIcon);
				TextView text = (TextView) modeLayout.findViewById(R.id.selectModeText);

				if (v != null)
				{
					RelativeLayout.LayoutParams pm = (RelativeLayout.LayoutParams) imgView.getLayoutParams();
					pm.width = (int) CameraScreenActivity.getAppResources().getDimension(R.dimen.mainButtonHeight);
					pm.height = (int) CameraScreenActivity.getAppResources().getDimension(R.dimen.mainButtonHeight);
					imgView.setImageDrawable(((ImageView) v.findViewById(R.id.modeImage)).getDrawable()
							.getConstantState().newDrawable());
				} else
				{
					RelativeLayout.LayoutParams pm = (RelativeLayout.LayoutParams) imgView.getLayoutParams();
					pm.width = 0;
					pm.height = 0;
				}
				text.setText("  " + toastText);
				text.setTextSize(16);
				text.setTextColor(Color.WHITE);

				text.setMaxWidth((int) Math.round(screen_width * 0.7));

				Animation visible_alpha = new AlphaAnimation(0, 1);
				visible_alpha.setStartOffset(startOffset ? 2000 : 0);
				visible_alpha.setDuration(1000);
				visible_alpha.setRepeatCount(0);

				final Animation invisible_alpha = new AlphaAnimation(1, 0);
				invisible_alpha.setStartOffset(showLength == Toast.LENGTH_SHORT ? 1000 : 3000);
				invisible_alpha.setDuration(1000);
				invisible_alpha.setRepeatCount(0);

				visible_alpha.setAnimationListener(new AnimationListener()
				{
					@Override
					public void onAnimationEnd(Animation animation)
					{
						modeLayout.startAnimation(invisible_alpha);
					}

					@Override
					public void onAnimationRepeat(Animation animation)
					{
						// Not used
					}

					@Override
					public void onAnimationStart(Animation animation)
					{
						// Not used
					}
				});

				invisible_alpha.setAnimationListener(new AnimationListener()
				{
					@Override
					public void onAnimationEnd(Animation animation)
					{
						modeLayout.setVisibility(View.GONE);
					}

					@Override
					public void onAnimationRepeat(Animation animation)
					{
						// Not used
					}

					@Override
					public void onAnimationStart(Animation animation)
					{
						// Not used
					}
				});

				modeLayout.setRotation(-mDeviceOrientation);
				modeLayout.setVisibility(View.VISIBLE);
				modeLayout.bringToFront();
				modeLayout.startAnimation(visible_alpha);
			}
		});
	}

	// helper function to draw one bitmap on another
	private Bitmap mergeImage(Bitmap base, Bitmap overlay)
	{
		int adWDelta = (int) (base.getWidth() - overlay.getWidth()) / 2;
		int adHDelta = (int) (base.getHeight() - overlay.getHeight()) / 2;

		Bitmap mBitmap = Bitmap.createBitmap(base.getWidth(), base.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(mBitmap);
		canvas.drawBitmap(base, 0, 0, null);
		canvas.drawBitmap(overlay, adWDelta, adHDelta, null);

		return mBitmap;
	}

	// Supplementary methods to find suitable size for plugin's view
	// For LINEARLAYOUT
	private android.widget.RelativeLayout.LayoutParams getTunedRelativeLayoutParams(View view,
			android.widget.RelativeLayout.LayoutParams currParams, int goodWidth, int goodHeight)
	{
		int viewHeight, viewWidth;

		if (currParams != null)
		{
			viewHeight = currParams.height;
			viewWidth = currParams.width;

			if ((viewHeight > goodHeight || viewHeight <= 0) && goodHeight > 0)
				viewHeight = goodHeight;

			if ((viewWidth > goodWidth || viewWidth <= 0) && goodWidth > 0)
				viewWidth = goodWidth;

			currParams.width = viewWidth;
			currParams.height = viewHeight;

			view.setLayoutParams(currParams);
		} else
		{
			currParams = new android.widget.RelativeLayout.LayoutParams(goodWidth, goodHeight);
			view.setLayoutParams(currParams);
		}

		return currParams;
	}

	// For RELATIVELAYOUT
	private android.widget.LinearLayout.LayoutParams getTunedLinearLayoutParams(View view,
			android.widget.LinearLayout.LayoutParams currParams, int goodWidth, int goodHeight)
	{
		if (currParams != null)
		{
			int viewHeight = currParams.height;
			int viewWidth = currParams.width;

			if ((viewHeight > goodHeight || viewHeight <= 0) && goodHeight > 0)
				viewHeight = goodHeight;

			if ((viewWidth > goodWidth || viewWidth <= 0) && goodWidth > 0)
				viewWidth = goodWidth;

			currParams.width = viewWidth;
			currParams.height = viewHeight;

			view.setLayoutParams(currParams);
		} else
		{
			currParams = new android.widget.LinearLayout.LayoutParams(goodWidth, goodHeight);
			view.setLayoutParams(currParams);
		}

		return currParams;
	}

	/************************************************************************************************
	 * >>>>>>>>>
	 * 
	 * DEFINITION OF PLACE ON LAYOUT FOR PLUGIN'S VIEWS
	 * 
	 * >>>>>>>>> BEGIN
	 ***********************************************************************************************/

	protected Plugin.ViewfinderZone[]	zones	= { Plugin.ViewfinderZone.VIEWFINDER_ZONE_TOP_LEFT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_TOP_RIGHT, Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER_RIGHT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_RIGHT, Plugin.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER_LEFT };

	protected android.widget.RelativeLayout.LayoutParams getTunedPluginLayoutParams(View view,
			Plugin.ViewfinderZone desire_zone, android.widget.RelativeLayout.LayoutParams currParams)
	{
		int left = 0, right = 0, top = 0, bottom = 0;

		RelativeLayout pluginLayout = (RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.pluginsLayout);

		if (currParams == null)
			currParams = new android.widget.RelativeLayout.LayoutParams(getMinPluginViewWidth(),
					getMinPluginViewHeight());

		switch (desire_zone)
		{
		case VIEWFINDER_ZONE_TOP_LEFT:
			{
				left = 0;
				right = currParams.width;
				top = 0;
				bottom = currParams.height;

				currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

			}
			break;
		case VIEWFINDER_ZONE_TOP_RIGHT:
			{
				left = pluginLayout.getWidth() - currParams.width;
				right = pluginLayout.getWidth();
				top = 0;
				bottom = currParams.height;

				currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			}
			break;
		case VIEWFINDER_ZONE_CENTER_LEFT:
			{
				left = 0;
				right = currParams.width;
				top = pluginLayout.getHeight() / 2 - currParams.height / 2;
				bottom = pluginLayout.getHeight() / 2 + currParams.height / 2;

				currParams.addRule(RelativeLayout.CENTER_VERTICAL);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			}
			break;
		case VIEWFINDER_ZONE_CENTER_RIGHT:
			{
				left = pluginLayout.getWidth() - currParams.width;
				right = pluginLayout.getWidth();
				top = pluginLayout.getHeight() / 2 - currParams.height / 2;
				bottom = pluginLayout.getHeight() / 2 + currParams.height / 2;

				currParams.addRule(RelativeLayout.CENTER_VERTICAL);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			}
			break;
		case VIEWFINDER_ZONE_BOTTOM_LEFT:
			{
				left = 0;
				right = currParams.width;
				top = pluginLayout.getHeight() - currParams.height;
				bottom = pluginLayout.getHeight();

				currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			}
			break;
		case VIEWFINDER_ZONE_BOTTOM_RIGHT:
			{
				left = pluginLayout.getWidth() - currParams.width;
				right = pluginLayout.getWidth();
				top = pluginLayout.getHeight() - currParams.height;
				bottom = pluginLayout.getHeight();

				currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			}
			break;
		case VIEWFINDER_ZONE_FULLSCREEN:
			{
				Log.e("GUI", "VIEWFINDER_ZONE_FULLSCREEN");
				currParams.width = CameraScreenActivity.getInstance().getPreviewSize() != null ? CameraScreenActivity.getInstance()
						.getPreviewSize().getWidth() : 0;
				currParams.height = CameraScreenActivity.getInstance().getPreviewSize() != null ? CameraScreenActivity.getInstance()
						.getPreviewSize().getHeight() : 0;
				currParams.addRule(RelativeLayout.CENTER_IN_PARENT);
				return currParams;
			}
		case VIEWFINDER_ZONE_CENTER:
			{
				if (currParams.width > iCenterViewMaxWidth)
					currParams.width = iCenterViewMaxWidth;

				if (currParams.height > iCenterViewMaxHeight)
					currParams.height = iCenterViewMaxHeight;

				currParams.addRule(RelativeLayout.CENTER_IN_PARENT);
				return currParams;
			}
		default:
			break;
		}

		return findFreeSpaceOnLayout(new Rect(left, top, right, bottom), currParams, desire_zone);
	}

	protected android.widget.RelativeLayout.LayoutParams findFreeSpaceOnLayout(Rect currRect,
			android.widget.RelativeLayout.LayoutParams currParams, Plugin.ViewfinderZone currZone)
	{
		boolean isFree = true;
		Rect childRect;

		View pluginLayout = CameraScreenActivity.getInstance().findViewById(R.id.pluginsLayout);

		// Looking in all zones at clockwise direction for free place
		for (int j = Plugin.ViewfinderZone.getInt(currZone), counter = 0; j < zones.length; j++, counter++)
		{
			isFree = true;
			// Check intersections with already added views
			for (int i = 0; i < ((ViewGroup) pluginLayout).getChildCount(); ++i)
			{
				View nextChild = ((ViewGroup) pluginLayout).getChildAt(i);

				currRect = getPluginViewRectInZone(currParams, zones[j]);
				childRect = getPluginViewRect(nextChild);

				if (currRect.intersect(childRect))
				{
					isFree = false;
					break;
				}
			}

			if (isFree) // Free zone has found
			{
				if (currZone == zones[j]) // Current zone is free
					return currParams;
				else
				{
					int[] rules = currParams.getRules();
					for (int i = 0; i < rules.length; i++)
						currParams.addRule(i, 0);

					switch (zones[j])
					{
					case VIEWFINDER_ZONE_TOP_LEFT:
						{
							currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						}
						break;
					case VIEWFINDER_ZONE_TOP_RIGHT:
						{
							currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						}
						break;
					case VIEWFINDER_ZONE_CENTER_LEFT:
						{
							currParams.addRule(RelativeLayout.CENTER_VERTICAL);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						}
						break;
					case VIEWFINDER_ZONE_CENTER_RIGHT:
						{
							currParams.addRule(RelativeLayout.CENTER_VERTICAL);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						}
						break;
					case VIEWFINDER_ZONE_BOTTOM_LEFT:
						{
							currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						}
						break;
					case VIEWFINDER_ZONE_BOTTOM_RIGHT:
						{
							currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
							currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						}
						break;
					default:
						break;
					}

					return currParams;
				}
			} else
			{
				if (counter == zones.length - 1) // Already looked in all zones
													// and they are all full
					return null;

				if (j == zones.length - 1) // If we started not from a first
											// zone (top-left)
					j = -1;
			}
		}

		return null;
	}

	protected Rect getPluginViewRectInZone(RelativeLayout.LayoutParams currParams, Plugin.ViewfinderZone zone)
	{
		int left = -1, right = -1, top = -1, bottom = -1;

		RelativeLayout pluginLayout = (RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.pluginsLayout);

		int viewWidth = currParams.width;
		int viewHeight = currParams.height;

		int layoutWidth = pluginLayout.getWidth();
		int layoutHeight = pluginLayout.getHeight();

		switch (zone)
		{
		case VIEWFINDER_ZONE_TOP_LEFT:
			{
				left = 0;
				right = viewWidth;
				top = 0;
				bottom = viewHeight;
			}
			break;
		case VIEWFINDER_ZONE_TOP_RIGHT:
			{
				left = layoutWidth - viewWidth;
				right = layoutWidth;
				top = 0;
				bottom = viewHeight;
			}
			break;
		case VIEWFINDER_ZONE_CENTER_LEFT:
			{
				left = 0;
				right = viewWidth;
				top = layoutHeight / 2 - viewHeight / 2;
				bottom = layoutHeight / 2 + viewHeight / 2;
			}
			break;
		case VIEWFINDER_ZONE_CENTER_RIGHT:
			{
				left = layoutWidth - viewWidth;
				right = layoutWidth;
				top = layoutHeight / 2 - viewHeight / 2;
				bottom = layoutHeight / 2 + viewHeight / 2;
			}
			break;
		case VIEWFINDER_ZONE_BOTTOM_LEFT:
			{
				left = 0;
				right = viewWidth;
				top = layoutHeight - viewHeight;
				bottom = layoutHeight;
			}
			break;
		case VIEWFINDER_ZONE_BOTTOM_RIGHT:
			{
				left = layoutWidth - viewWidth;
				right = layoutWidth;
				top = layoutHeight - viewHeight;
				bottom = layoutHeight;
			}
			break;
		default:
			break;
		}

		return new Rect(left, top, right, bottom);
	}

	protected Rect getPluginViewRect(View view)
	{
		int left = -1, right = -1, top = -1, bottom = -1;

		RelativeLayout pluginLayout = (RelativeLayout) CameraScreenActivity.getInstance().findViewById(R.id.pluginsLayout);
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
		int[] rules = lp.getRules();

		int viewWidth = lp.width;
		int viewHeight = lp.height;

		int layoutWidth = pluginLayout.getWidth();
		int layoutHeight = pluginLayout.getHeight();

		// Get X coordinates
		if (rules[RelativeLayout.ALIGN_PARENT_LEFT] != 0)
		{
			left = 0;
			right = viewWidth;
		} else if (rules[RelativeLayout.ALIGN_PARENT_RIGHT] != 0)
		{
			left = layoutWidth - viewWidth;
			right = layoutWidth;
		}

		// Get Y coordinates
		if (rules[RelativeLayout.ALIGN_PARENT_TOP] != 0)
		{
			top = 0;
			bottom = viewHeight;
		} else if (rules[RelativeLayout.ALIGN_PARENT_BOTTOM] != 0)
		{
			top = layoutHeight - viewHeight;
			bottom = layoutHeight;
		} else if (rules[RelativeLayout.CENTER_VERTICAL] != 0)
		{
			top = layoutHeight / 2 - viewHeight / 2;
			bottom = layoutHeight / 2 + viewHeight / 2;
		}

		return new Rect(left, top, right, bottom);
	}

	// Supplementary methods for getting appropriate sizes for plugin's views
	@Override
	public int getMaxPluginViewHeight()
	{
		return (guiView.findViewById(R.id.pluginsLayout)).getHeight() / 3;
	}

	@Override
	public int getMaxPluginViewWidth()
	{
		return (guiView.findViewById(R.id.pluginsLayout)).getWidth() / 2;
	}

	@Override
	public int getMinPluginViewHeight()
	{
		return (guiView.findViewById(R.id.pluginsLayout)).getHeight() / 12;
	}

	@Override
	public int getMinPluginViewWidth()
	{
		return (guiView.findViewById(R.id.pluginsLayout)).getWidth() / 4;
	}

	/************************************************************************************************
	 * <<<<<<<<<<
	 * 
	 * DEFINITION OF PLACE ON LAYOUT FOR PLUGIN'S VIEWS
	 * 
	 * <<<<<<<<<< END
	 ***********************************************************************************************/

	private static float	X					= 0;

	private static float	Xprev				= 0;

	private static float	Xoffset				= 0;

	private static float	XtoLeftInvisible	= 0;
	private static float	XtoLeftVisible		= 0;

	private static float	XtoRightInvisible	= 0;
	private static float	XtoRightVisible		= 0;

	private MotionEvent		prevEvent;
	private MotionEvent		downEvent;
	private boolean			scrolling			= false;

	@Override
	public boolean onTouch(View view, MotionEvent event)
	{

		if (view == (LinearLayout) guiView.findViewById(R.id.evLayout)
				|| (lockControls && !PluginManager.getInstance().getActiveModeID().equals("video")))
			return true;


		else if (view.getParent() == CameraScreenActivity.getInstance().findViewById(R.id.paramsLayout)
				&& !quickControlsChangeVisible)
		{

			if (event.getAction() == MotionEvent.ACTION_DOWN)
			{
				downEvent = MotionEvent.obtain(event);
				prevEvent = MotionEvent.obtain(event);
				scrolling = false;

				topMenuButtonPressed(findTopMenuButtonIndex(view));

				return false;
			} else if (event.getAction() == MotionEvent.ACTION_UP)
			{
				topMenuButtonPressed(-1);
				scrolling = false;
				if (prevEvent == null || downEvent == null)
					return false;
				if (prevEvent.getAction() == MotionEvent.ACTION_DOWN)
					return false;
				if (prevEvent.getAction() == MotionEvent.ACTION_MOVE)
				{
					if ((event.getY() - downEvent.getY()) < 50)
						return false;
				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE && !scrolling)
			{
				if (downEvent == null)
					return false;
				if ((event.getY() - downEvent.getY()) < 50)
					return false;
				else
				{
					scrolling = true;
				}
			}
		}

		// to allow quickControl's to process onClick, onLongClick
		if (view.getParent() == CameraScreenActivity.getInstance().findViewById(R.id.paramsLayout))
		{
			return false;
		}

		boolean isMenuOpened = false;
		if (quickControlsChangeVisible || modeSelectorVisible || settingsControlsVisible || isSecondaryMenusVisible()) {
            isMenuOpened = true;
        }

		if (quickControlsChangeVisible
				&& view.getParent() != (View) CameraScreenActivity.getInstance().findViewById(R.id.paramsLayout))
			closeQuickControlsSettings();

		if (modeSelectorVisible)
			hideModeList();

		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		if (settingsControlsVisible)
			return true;
		else if (!isMenuOpened)
			// call onTouch of active vf and capture plugins
			PluginManager.getInstance().onTouch(view, event);

		RelativeLayout pluginLayout = (RelativeLayout) guiView.findViewById(R.id.pluginsLayout);
		RelativeLayout fullscreenLayout = (RelativeLayout) guiView.findViewById(R.id.fullscreenLayout);
		LinearLayout paramsLayout = (LinearLayout) guiView.findViewById(R.id.paramsLayout);
		LinearLayout infoLayout = (LinearLayout) guiView.findViewById(R.id.infoLayout);
		// OnTouch listener to show info and sliding grids
		switch (event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			{
				X = event.getX();
				Xoffset = X;
				Xprev = X;

				pluginLayout.clearAnimation();
				fullscreenLayout.clearAnimation();
				paramsLayout.clearAnimation();
				infoLayout.clearAnimation();

				topMenuButtonPressed(findTopMenuButtonIndex(view));

				return true;
			}
		case MotionEvent.ACTION_UP:
			{
				float difX = event.getX();
				if ((X > difX) && (X - difX > 100))
				{
					sliderLeftEvent();
					return true;
				} else if (X < difX && (difX - X > 100))
				{
					sliderRightEvent();
					return true;
				}

				pluginLayout.clearAnimation();
				fullscreenLayout.clearAnimation();
				paramsLayout.clearAnimation();
				infoLayout.clearAnimation();

				topMenuButtonPressed(-1);

				break;
			}
		case MotionEvent.ACTION_MOVE:
			{
				int pluginzoneWidth = guiView.findViewById(R.id.pluginsLayout).getWidth();
				int infozoneWidth = guiView.findViewById(R.id.infoLayout).getWidth();
				int screenWidth = pluginzoneWidth + infozoneWidth;

				float difX = event.getX();

				Animation in_animation;
				Animation out_animation;
				Animation reverseout_animation;
				boolean toLeft;
				if (difX > Xprev)
				{
					out_animation = new TranslateAnimation(Xprev - Xoffset, difX - Xoffset, 0, 0);
					out_animation.setDuration(10);
					out_animation.setInterpolator(new LinearInterpolator());
					out_animation.setFillAfter(true);

					in_animation = new TranslateAnimation(Xprev - Xoffset - screenWidth, difX - Xoffset - screenWidth,
							0, 0);
					in_animation.setDuration(10);
					in_animation.setInterpolator(new LinearInterpolator());
					in_animation.setFillAfter(true);

					reverseout_animation = new TranslateAnimation(difX + (screenWidth - Xoffset), Xprev
							+ (screenWidth - Xoffset), 0, 0);
					reverseout_animation.setDuration(10);
					reverseout_animation.setInterpolator(new LinearInterpolator());
					reverseout_animation.setFillAfter(true);

					toLeft = false;

					XtoRightInvisible = difX - Xoffset;
					XtoRightVisible = difX - Xoffset - screenWidth;
				} else
				{
					out_animation = new TranslateAnimation(difX - Xoffset, Xprev - Xoffset, 0, 0);
					out_animation.setDuration(10);
					out_animation.setInterpolator(new LinearInterpolator());
					out_animation.setFillAfter(true);

					in_animation = new TranslateAnimation(screenWidth + (Xprev - Xoffset), screenWidth
							+ (difX - Xoffset), 0, 0);
					in_animation.setDuration(10);
					in_animation.setInterpolator(new LinearInterpolator());
					in_animation.setFillAfter(true);

					reverseout_animation = new TranslateAnimation(Xprev - Xoffset - screenWidth, difX - Xoffset
							- screenWidth, 0, 0);
					reverseout_animation.setDuration(10);
					reverseout_animation.setInterpolator(new LinearInterpolator());
					reverseout_animation.setFillAfter(true);

					toLeft = true;

					XtoLeftInvisible = Xprev - Xoffset;
					XtoLeftVisible = screenWidth + (difX - Xoffset);
				}

				switch (infoSet)
				{
				case INFO_ALL:
					{
						pluginLayout.startAnimation(out_animation);
						fullscreenLayout.startAnimation(out_animation);
						infoLayout.startAnimation(out_animation);
						if ((difX < X) || !isAnyViewOnViewfinder())
							paramsLayout.startAnimation(out_animation);
					}
					break;
				case INFO_NO:
					{
						if ((toLeft && difX < X) || (!toLeft && difX > X))
							fullscreenLayout.startAnimation(in_animation);
						else
							paramsLayout.startAnimation(reverseout_animation);
						if (!toLeft && isAnyViewOnViewfinder())
						{
							pluginLayout.startAnimation(in_animation);
							fullscreenLayout.startAnimation(in_animation);
							infoLayout.startAnimation(in_animation);
						} else if (toLeft && difX > X && isAnyViewOnViewfinder())
						{
							pluginLayout.startAnimation(reverseout_animation);
							paramsLayout.startAnimation(reverseout_animation);
							infoLayout.startAnimation(reverseout_animation);
						}
					}
					break;
				case INFO_GRID:
					{
						if (difX > X)// to INFO_NO
							fullscreenLayout.startAnimation(out_animation);
						else
						// to INFO_PARAMS
						{
							fullscreenLayout.startAnimation(out_animation);
							paramsLayout.startAnimation(in_animation);
						}
					}
					break;
				case INFO_PARAMS:
					{
						fullscreenLayout.startAnimation(in_animation);
						if (difX > X)
							paramsLayout.startAnimation(out_animation);
						if (toLeft)
						{
							pluginLayout.startAnimation(in_animation);
							infoLayout.startAnimation(in_animation);
						} else if (difX < X)
						{
							pluginLayout.startAnimation(reverseout_animation);
							infoLayout.startAnimation(reverseout_animation);
						}
					}
					break;
				default:
					break;
				}

				Xprev = Math.round(difX);

			}
			break;
		default:
			break;
		}
		return false;
	}


	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{

		int iEv = progress - CameraController.getMaxExposureCompensation();
		CameraController.setCameraExposureCompensation(iEv);

		preferences.edit().putInt(CameraScreenActivity.sEvPref, iEv).commit();

		mEV = iEv;

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_EV_CHANGED);
	}

	@Override
	public void onVolumeBtnExpo(int keyCode)
	{
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
			expoMinus();
		else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			expoPlus();
	}

	private void expoMinus()
	{
		SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
		if (evBar != null)
		{
			int minValue = CameraController.getMinExposureCompensation();
			int step = 1;
			int currProgress = evBar.getProgress();
			int iEv = currProgress - step;
			if (iEv < 0)
				iEv = 0;

			CameraController.setCameraExposureCompensation(iEv + minValue);

			preferences
					.edit()
					.putInt(CameraScreenActivity.sEvPref,
							Math.round((iEv + minValue) * CameraController.getExposureCompensationStep())).commit();

			evBar.setProgress(iEv);
		}
	}

	private void expoPlus()
	{
		SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
		if (evBar != null)
		{
			int minValue = CameraController.getMinExposureCompensation();
			int maxValue = CameraController.getMaxExposureCompensation();

			int step = 1;

			int currProgress = evBar.getProgress();
			int iEv = currProgress + step;
			if (iEv > maxValue - minValue)
				iEv = maxValue - minValue;

			CameraController.setCameraExposureCompensation(iEv + minValue);

			preferences
					.edit()
					.putInt(CameraScreenActivity.sEvPref,
							Math.round((iEv + minValue) * CameraController.getExposureCompensationStep())).commit();

			evBar.setProgress(iEv);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
	}

	// Slider events handler

	private void sliderLeftEvent()
	{
		infoSlide(true, XtoLeftVisible, XtoLeftInvisible);
	}

	private void sliderRightEvent()
	{
		infoSlide(false, XtoRightVisible, XtoRightInvisible);
	}

	// shutter icons setter
	public void setShutterIcon(ShutterButton id)
	{
		RotateImageView mainButton = (RotateImageView) guiView.findViewById(R.id.buttonShutter);
		LinearLayout buttonShutterContainer = (LinearLayout) guiView.findViewById(R.id.buttonShutterContainer);


		if (id == ShutterButton.TIMELAPSE_ACTIVE)
		{
			mainButton.setImageResource(R.drawable.gui_almalence_shutter_timelapse);
		}

		// 1 button
		if (id == ShutterButton.DEFAULT || id == ShutterButton.RECORDER_START || id == ShutterButton.RECORDER_STOP
				|| id == ShutterButton.RECORDER_RECORDING)
		{

			if (id == ShutterButton.DEFAULT)
			{
				mainButton.setImageResource(R.drawable.button_shutter);
			} else if (id == ShutterButton.RECORDER_START)
			{
				mainButton.setImageResource(R.drawable.gui_almalence_shutter_video_off);
			}

		}

	}

	public boolean onKeyDown(boolean isFromMain, int keyCode, KeyEvent event)
	{
		int res = 0;
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (quickControlsChangeVisible)
			{
				closeQuickControlsSettings();
				res++;
			} else if (settingsControlsVisible)
			{
				res++;
			} else if (modeSelectorVisible)
			{
				hideModeList();
				res++;
			} else if (quickControlsVisible)
			{
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
				res++;
				quickControlsVisible = false;
			}


			/*
			if ((guiView.findViewById(R.id.viewPagerLayoutMain)).getVisibility() == View.VISIBLE)
			{
				hideStore();
				res++;
			}*/
		}

		if (keyCode == KeyEvent.KEYCODE_CAMERA)
		{
			if (settingsControlsVisible || quickControlsChangeVisible || modeSelectorVisible)
			{
				if (quickControlsChangeVisible)
					closeQuickControlsSettings();

				if (modeSelectorVisible)
				{
					hideModeList();
					return false;
				}
			}
			shutterButtonPressed();
			return true;
		}

		// check if back button pressed and processing is in progress
		if (res == 0)
			if (keyCode == KeyEvent.KEYCODE_BACK)
			{
				if (PluginManager.getInstance().getProcessingCounter() != 0)
				{
					// splash screen about processing
					AlertDialog.Builder builder = new AlertDialog.Builder(CameraScreenActivity.getInstance())
							.setTitle("Processing...")
							.setMessage(CameraScreenActivity.getAppResources().getString(R.string.processing_not_finished))
							.setPositiveButton("Ok", new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog, int which)
								{
									// continue with delete
									dialog.cancel();
								}
							});
					AlertDialog alert = builder.create();
					alert.show();
				}
			}

		return res > 0 ? true : false;
	}

	public void openGallery(boolean isOpenExternal)
	{
		if (mThumbnail == null)
			return;

		Uri uri = this.mThumbnail.getUri();

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_STOP_CAPTURE);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		boolean isAllowedExternal = prefs.getBoolean(
				CameraScreenActivity.getAppResources().getString(R.string.Preference_allowExternalGalleries), false);
		if (isAllowedExternal || isOpenExternal)
		{
			openExternalGallery(uri);
		}
	}

	private void openExternalGallery(Uri uri)
	{
		try
		{
			CameraScreenActivity.getInstance().startActivity(new Intent(Intent.ACTION_VIEW, uri));
		} catch (ActivityNotFoundException ex)
		{
			try
			{
				CameraScreenActivity.getInstance().startActivity(new Intent(Intent.ACTION_VIEW, uri));
			} catch (ActivityNotFoundException e)
			{
				Log.e("AlmalenceGUI", "review image fail. uri=" + uri, e);
			}
		}
	}

	@Override
	public void onCaptureFinished()
	{
		// Not used
	}

	// called to set any indication when export plugin work finished.
	@Override
	public void onExportFinished()
	{
		// stop animation
		if (processingAnim != null)
		{
			processingAnim.clearAnimation();
			processingAnim.setVisibility(View.GONE);
		}
		RelativeLayout rl = (RelativeLayout) guiView.findViewById(R.id.blockingLayout);
		if (rl.getVisibility() == View.VISIBLE)
		{
			rl.setVisibility(View.GONE);
		}

		updateThumbnailButton();

		if (0 != PluginManager.getInstance().getProcessingCounter())
		{
			new CountDownTimer(10, 10)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					startProcessingAnimation();
				}
			}.start();
		}
	}

	@Override
	public void onPostProcessingStarted()
	{
		guiView.findViewById(R.id.buttonShutter).setEnabled(false);
		guiView.findViewById(R.id.postprocessingLayout).setVisibility(View.VISIBLE);
		guiView.findViewById(R.id.postprocessingLayout).bringToFront();
		List<Plugin> processingPlugins = PluginManager.getInstance().getActivePlugins(PluginType.Processing);
		if (!processingPlugins.isEmpty())
		{
			View postProcessingView = processingPlugins.get(0).getPostProcessingView();
			if (postProcessingView != null)
				((RelativeLayout) guiView.findViewById(R.id.postprocessingLayout)).addView(postProcessingView);
		}
	}

	@Override
	public void onPostProcessingFinished()
	{
		List<View> postprocessingView = new ArrayList<View>();
		RelativeLayout pluginsLayout = (RelativeLayout) CameraScreenActivity.getInstance()
				.findViewById(R.id.postprocessingLayout);
		for (int i = 0; i < pluginsLayout.getChildCount(); i++)
			postprocessingView.add(pluginsLayout.getChildAt(i));

		for (int j = 0; j < postprocessingView.size(); j++)
		{
			View view = postprocessingView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			pluginsLayout.removeView(view);
		}

		guiView.findViewById(R.id.postprocessingLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.buttonShutter).setEnabled(true);

		updateThumbnailButton();
	}

	private UpdateThumbnailButtonTask	t	= null;

	public void updateThumbnailButton()
	{


		t = new UpdateThumbnailButtonTask(CameraScreenActivity.getInstance());
		t.execute();

		new CountDownTimer(1000, 1000)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				try
				{
					if (t != null && t.getStatus() != AsyncTask.Status.FINISHED)
					{
						t.cancel(true);
					}
				} catch (Exception e)
				{
					Log.e("AlmalenceGUI", "Can't stop thumbnail processing");
				}
			}
		}.start();
	}

	private class UpdateThumbnailButtonTask extends AsyncTask<Void, Void, Void>
	{
		public UpdateThumbnailButtonTask(Context context)
		{
		}

		@Override
		protected void onPreExecute()
		{
			// do nothing.
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			mThumbnail = Thumbnail.getLastThumbnail(CameraScreenActivity.getInstance().getContentResolver());
			return null;
		}

		@Override
		protected void onPostExecute(Void v)
		{
			if (mThumbnail != null)
			{
				final Bitmap bitmap = mThumbnail.getBitmap();

				if (bitmap != null)
				{
					if (bitmap.getHeight() > 0 && bitmap.getWidth() > 0)
					{
						System.gc();

						try
						{
							Bitmap bm = Thumbnail.getRoundedCornerBitmap(bitmap, (int) (CameraScreenActivity.getMainContext()
									.getResources().getDimension(R.dimen.mainButtonHeight) * 1.2), (int) (CameraScreenActivity
									.getMainContext().getResources().getDimension(R.dimen.mainButtonHeight) / 1.1));


						} catch (Exception e)
						{
							Log.v("AlmalenceGUI", "Can't set thumbnail");
						}
					}
				}
			} else
			{
				try
				{
					Bitmap bitmap = Bitmap.createBitmap(96, 96, Config.ARGB_8888);
					Canvas canvas = new Canvas(bitmap);
					canvas.drawColor(Color.BLACK);
					Thumbnail.getRoundedCornerBitmap(bitmap, (int) (CameraScreenActivity.getMainContext()
							.getResources().getDimension(R.dimen.mainButtonHeight) * 1.2), (int) (CameraScreenActivity
							.getMainContext().getResources().getDimension(R.dimen.mainButtonHeight) / 1.1));
				} catch (Exception e)
				{
					Log.v("AlmalenceGUI", "Can't set thumbnail");
				}
			}
		}
	}

	private ImageView	processingAnim;

	public void startProcessingAnimation()
	{
	}

	public void processingBlockUI()
	{
		RelativeLayout rl = (RelativeLayout) guiView.findViewById(R.id.blockingLayout);
		if (rl.getVisibility() == View.GONE)
		{
			rl.setVisibility(View.VISIBLE);
			rl.bringToFront();
			guiView.findViewById(R.id.buttonShutter).setEnabled(false);
		}
	}

	// capture indication - will play shutter icon opened/closed
	private boolean	captureIndication	= true;
	private boolean	isIndicationOn		= false;

	public void startContinuousCaptureIndication()
	{
		captureIndication = true;
		new CountDownTimer(200, 200)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				if (captureIndication)
				{
					if (isIndicationOn)
					{
						((RotateImageView) guiView.findViewById(R.id.buttonShutter))
								.setImageResource(R.drawable.gui_almalence_shutter);
						isIndicationOn = false;
					} else
					{
						((RotateImageView) guiView.findViewById(R.id.buttonShutter))
								.setImageResource(R.drawable.gui_almalence_shutter_pressed);
						isIndicationOn = true;
					}
					startContinuousCaptureIndication();
				}
			}
		}.start();
	}


	public void stopCaptureIndication()
	{

		captureIndication = false;
		if (!PluginManager.getInstance().getActiveModeID().equals("video"))
		{
			((RotateImageView) guiView.findViewById(R.id.buttonShutter)).setImageResource(R.drawable.button_shutter);
		}
	}

	public void showCaptureIndication()
	{
		new CountDownTimer(400, 200)
		{
			public void onTick(long millisUntilFinished)
			{
				((RotateImageView) guiView.findViewById(R.id.buttonShutter))
						.setImageResource(R.drawable.gui_almalence_shutter_pressed);
			}

			public void onFinish()
			{
				if (!PluginManager.getInstance().getActiveModeID().equals("video"))
				{
					((RotateImageView) guiView.findViewById(R.id.buttonShutter))
							.setImageResource(R.drawable.button_shutter);
				}

			}
		}.start();
	}

	@Override
	public void onCameraSetup()
	{
		// Not used
	}

	@Override
	public void menuButtonPressed()
	{
		// Not used
	}

	@Override
	public void addMode(View mode)
	{
		// Not used
	}

	@Override
	public void SetModeSelected(View v)
	{
		// Not used
	}

	@Override
	public void hideModes()
	{
		// Not used
	}

	@Override
	public int getMaxModeViewWidth()
	{
		return -1;
	}

	@Override
	public int getMaxModeViewHeight()
	{
		return -1;
	}

	@Override
	@TargetApi(14)
	public void setFocusParameters()
	{
		// Not used
	}

	public View getMainView()
	{
		return guiView;
	}
}
