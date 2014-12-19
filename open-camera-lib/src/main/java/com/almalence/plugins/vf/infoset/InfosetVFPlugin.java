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

package com.almalence.plugins.vf.infoset;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginViewfinder;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.CameraScreenActivity;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginViewfinder;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.util.Util;
import com.almalence.ui.RotateImageView;

/***
 * Implements set of viewfinder's info controls Current set is: Battery status,
 * GPS status
 ***/

public class InfosetVFPlugin extends PluginViewfinder
{
	private RotateImageView				batteryInfoImage			= null;

	private RotateImageView				sceneInfoImage				= null;
	private RotateImageView				wbInfoImage					= null;
	private RotateImageView				focusInfoImage				= null;
	private RotateImageView				flashInfoImage				= null;
	private RotateImageView				isoInfoImage				= null;

	private TextView					memoryInfoText				= null;
	private TextView					evInfoText					= null;

	private static int					mDeviceOrientation;
	private OrientationEventListener	orientListener;

	private boolean						useBatteryMonitor;
	private boolean						usePictureCount;
	private boolean						useEVMonitor;
	private boolean						useSceneMonitor;
	private boolean						useWBMonitor;
	private boolean						useFocusMonitor;
	private boolean						useFlashMonitor;
	private boolean						useISOMonitor;

	private boolean						isBatteryMonitorRegistered	= false;

	private float						currentBatteryLevel			= -1;
	private int							currentBatteryStatus		= -1;

	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context arg0, Intent batteryStatus)
		{
			if (batteryInfoImage == null)
				return;

			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

			float batteryPct = level / (float) scale;

			int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

			if (status != currentBatteryStatus && isCharging)
			{
				batteryInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(R.drawable.battery_charging));
				currentBatteryStatus = status;
			} else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING)
			{
				if (currentBatteryLevel != batteryPct || currentBatteryStatus != status)
				{
					currentBatteryLevel = batteryPct;

					if (currentBatteryLevel > 0.8f)
						batteryInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(R.drawable.battery_full));
					else if (currentBatteryLevel <= 0.8f && currentBatteryLevel > 0.6f)
						batteryInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(R.drawable.battery_75));
					else if (currentBatteryLevel <= 0.6f && currentBatteryLevel > 0.4f)
						batteryInfoImage .setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(R.drawable.battery_50));
					else if (currentBatteryLevel <= 0.4f && currentBatteryLevel > 0.15f)
						batteryInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(R.drawable.battery_25));
					else if (currentBatteryLevel <= 0.15f && currentBatteryLevel > 0.05f)
						batteryInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(R.drawable.battery_10));
					else if (currentBatteryLevel <= 0.05f)
						batteryInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(R.drawable.battery_empty));
				}

				if (currentBatteryStatus != status)
				{
					currentBatteryStatus = status;
				}
			}
		}
	};

	public InfosetVFPlugin()
	{
		super("com.almalence.plugins.infosetvf", R.xml.preferences_vf_infoset, 0, 0, null);
	}

	boolean	isFirstGpsFix	= true;

	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
		useBatteryMonitor = prefs.getBoolean("useBatteryMonitorPrefInfoset", false);
		usePictureCount = prefs.getBoolean("availablePictureCountPrefInfoset", false);
		useEVMonitor = prefs.getBoolean("useEVMonitorPrefInfoset", false);
		useSceneMonitor = prefs.getBoolean("useSceneMonitorPrefInfoset", false);
		useWBMonitor = prefs.getBoolean("useWBMonitorPrefInfoset", false);
		useFocusMonitor = prefs.getBoolean("useFocusMonitorPrefInfoset", false);
		useFlashMonitor = prefs.getBoolean("useFlashMonitorPrefInfoset", false);
		useISOMonitor = prefs.getBoolean("useISOMonitorPrefInfoset", false);
	}

	@Override
	public void onPreferenceCreate(PreferenceFragment preferenceFragment)
	{
		Preference evPref = preferenceFragment.findPreference("useEVMonitorPrefInfoset");
		Preference scenePref = preferenceFragment.findPreference("useSceneMonitorPrefInfoset");
		Preference wbPref = preferenceFragment.findPreference("useWBMonitorPrefInfoset");
		Preference focusPref = preferenceFragment.findPreference("useFocusMonitorPrefInfoset");
		Preference flashPref = preferenceFragment.findPreference("useFlashMonitorPrefInfoset");
		Preference isoPref = preferenceFragment.findPreference("useISOMonitorPrefInfoset");

		if (CameraScreenActivity.getCameraController().isExposureCompensationSupported())
			evPref.setEnabled(true);
		else
			evPref.setEnabled(false);

		if (CameraScreenActivity.getCameraController().isSceneModeSupported())
			scenePref.setEnabled(true);
		else
			scenePref.setEnabled(false);

		if (CameraScreenActivity.getCameraController().isWhiteBalanceSupported())
			wbPref.setEnabled(true);
		else
			wbPref.setEnabled(false);

		if (CameraScreenActivity.getCameraController().isFocusModeSupported())
			focusPref.setEnabled(true);
		else
			focusPref.setEnabled(false);

		if (CameraScreenActivity.getCameraController().isFlashModeSupported())
			flashPref.setEnabled(true);
		else
			flashPref.setEnabled(false);

		if (CameraScreenActivity.getCameraController().isISOSupported())
			isoPref.setEnabled(true);
		else
			isoPref.setEnabled(false);
	}

	@Override
	public void onStart()
	{
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

				boolean remapOrientation = (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0)
						|| (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180)
						|| (orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90)
						|| (orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);

				if (remapOrientation)
					orientation = (orientation - 90 + 360) % 360;

				int newOrientation = Util.roundOrientation(orientation, mDeviceOrientation);
				if (newOrientation == mDeviceOrientation)
					return;
				else
					mDeviceOrientation = newOrientation;

				if (batteryInfoImage != null)
					batteryInfoImage.setOrientation(mDeviceOrientation);
				if (sceneInfoImage != null)
					sceneInfoImage.setOrientation(mDeviceOrientation);
				if (wbInfoImage != null)
					wbInfoImage.setOrientation(mDeviceOrientation);
				if (focusInfoImage != null)
					focusInfoImage.setOrientation(mDeviceOrientation);
				if (flashInfoImage != null)
					flashInfoImage.setOrientation(mDeviceOrientation);
				if (isoInfoImage != null)
					isoInfoImage.setOrientation(mDeviceOrientation);
				if (memoryInfoText != null)
					memoryInfoText.setRotation(-mDeviceOrientation);
				if (evInfoText != null)
					evInfoText.setRotation(-mDeviceOrientation);
			}
		};
	}

	@Override
	public void onGUICreate()
	{
		getPrefs();

		isFirstGpsFix = true;

		mDeviceOrientation = CameraScreenActivity.getGUIManager().getDisplayOrientation();
		mDeviceOrientation = (mDeviceOrientation - 90 + 360) % 360;

		clearInfoViews();

		if (useBatteryMonitor)
		{
			View v = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.plugin_vf_infoset_icon, null);
			batteryInfoImage = (RotateImageView) v.findViewById(R.id.infoImage);
			batteryInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
					.getDrawable(R.drawable.battery_empty));
			batteryInfoImage.setOrientation(mDeviceOrientation);

			addInfoView(batteryInfoImage);
		}

		if (useSceneMonitor)
		{
			View v = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.plugin_vf_infoset_icon, null);
			sceneInfoImage = (RotateImageView) v.findViewById(R.id.infoImage);
			sceneInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
					.getDrawable(CameraScreenActivity.getInstance().getSceneIcon(CameraParameters.SCENE_MODE_AUTO)));
			sceneInfoImage.setOrientation(mDeviceOrientation);

			addInfoView(sceneInfoImage);
		}

		if (useWBMonitor)
		{
			View v = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.plugin_vf_infoset_icon, null);
			wbInfoImage = (RotateImageView) v.findViewById(R.id.infoImage);
			wbInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
					.getDrawable(CameraScreenActivity.getInstance().getWBIcon(CameraParameters.WB_MODE_AUTO)));
			wbInfoImage.setOrientation(mDeviceOrientation);

			addInfoView(wbInfoImage);
		}

		if (useFocusMonitor)
		{
			View v = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.plugin_vf_infoset_icon, null);
			focusInfoImage = (RotateImageView) v.findViewById(R.id.infoImage);
			focusInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
					.getDrawable(CameraScreenActivity.getInstance().getFocusIcon(CameraParameters.AF_MODE_AUTO)));
			focusInfoImage.setOrientation(mDeviceOrientation);

			addInfoView(focusInfoImage);
		}

		if (useFlashMonitor)
		{
			View v = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.plugin_vf_infoset_icon, null);
			flashInfoImage = (RotateImageView) v.findViewById(R.id.infoImage);
			flashInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
					.getDrawable(CameraScreenActivity.getInstance().getFlashIcon(CameraParameters.FLASH_MODE_SINGLE)));
			flashInfoImage.setOrientation(mDeviceOrientation);

			addInfoView(flashInfoImage);
		}

		if (useISOMonitor)
		{
			View v = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.plugin_vf_infoset_icon, null);
			isoInfoImage = (RotateImageView) v.findViewById(R.id.infoImage);
			isoInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
					.getDrawable(CameraScreenActivity.getInstance().getISOIcon(CameraParameters.ISO_AUTO)));
			isoInfoImage.setOrientation(mDeviceOrientation);

			addInfoView(isoInfoImage);
		}

		if (usePictureCount)
		{
			String memoryString = String.valueOf(Util.AvailablePictureCount());
			View v = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.plugin_vf_infoset_text, null);
			memoryInfoText = (TextView) v.findViewById(R.id.infoText);
			memoryInfoText.setText(memoryString);
			memoryInfoText.setRotation(-mDeviceOrientation);

			addInfoView(memoryInfoText);
		}

		if (useEVMonitor)
		{
			View v = LayoutInflater.from(CameraScreenActivity.getMainContext()).inflate(R.layout.plugin_vf_infoset_text, null);
			evInfoText = (TextView) v.findViewById(R.id.infoText);
			evInfoText.setRotation(-mDeviceOrientation);

			addInfoView(evInfoText);
		}

//		initInfoIndicators();
	}

	@Override
	public void onResume()
	{
		getPrefs();
		this.orientListener.enable();
	}

	@Override
	public void onPause()
	{
		if (useBatteryMonitor && isBatteryMonitorRegistered)
		{
			try
			{
				CameraScreenActivity.getMainContext().unregisterReceiver(this.mBatInfoReceiver);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("InfosetVFPlugin", "onPause unregisterReceiver exception: " + e.getMessage());
			}
			isBatteryMonitorRegistered = false;
		}

		currentBatteryStatus = -1;
		currentBatteryLevel = -1;

		this.orientListener.disable();
	}
	
	@Override
	public void onCameraParametersSetup()
	{
		initInfoIndicators();
	}

	public void initInfoIndicators()
	{
		if (useBatteryMonitor)
		{
			if (isBatteryMonitorRegistered)
				CameraScreenActivity.getMainContext().unregisterReceiver(this.mBatInfoReceiver);

			CameraScreenActivity.getMainContext().registerReceiver(this.mBatInfoReceiver,
					new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

			isBatteryMonitorRegistered = true;
		}

		if (usePictureCount && memoryInfoText != null)
		{
			String memoryString = String.valueOf(Util.AvailablePictureCount());
			memoryInfoText.setText(memoryString);
		}

		if (useEVMonitor && evInfoText != null)
		{
			float iEV = CameraController.getExposureCompensation();
			String evString = (iEV > 0 ? "+" : "") + String.format("%.1f", iEV) + "EV";
			evInfoText.setText(evString);
			if (CameraController.isExposureCompensationSupported())
				evInfoText.setVisibility(View.VISIBLE);
			else
				evInfoText.setVisibility(View.GONE);
		}

		if (useSceneMonitor && sceneInfoImage != null)
		{
			int scene = CameraController.getSceneMode();
			if (scene != -1 && sceneInfoImage != null && CameraController.isSceneModeSupported())
			{
				int scene_id = CameraScreenActivity.getInstance().getSceneIcon(scene);
				if (scene_id != -1)
				{
					sceneInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(scene_id));
					sceneInfoImage.setVisibility(View.VISIBLE);
				} else
					sceneInfoImage.setVisibility(View.GONE);
			} else
				sceneInfoImage.setVisibility(View.GONE);
		}

		if (useWBMonitor && wbInfoImage != null)
		{
			int wb = CameraController.getWBMode();
			if (wb != -1 && wbInfoImage != null && CameraController.isWhiteBalanceSupported())
			{
				int wb_id = CameraScreenActivity.getInstance().getWBIcon(wb);
				if (wb_id != -1)
				{
					wbInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(wb_id));
					wbInfoImage.setVisibility(View.VISIBLE);
				} else
					wbInfoImage.setVisibility(View.GONE);
			} else
				wbInfoImage.setVisibility(View.GONE);
		}

		if (useFocusMonitor && focusInfoImage != null)
		{
			int focus = CameraController.getFocusMode();
			if (focus != -1 && focusInfoImage != null && CameraController.isFocusModeSupported())
			{
				int focus_id = CameraScreenActivity.getInstance().getFocusIcon(focus);
				if (focus_id != -1)
				{
					focusInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(focus_id));
					focusInfoImage.setVisibility(View.VISIBLE);
				} else
					focusInfoImage.setVisibility(View.GONE);
			} else
				focusInfoImage.setVisibility(View.GONE);
		}

		if (useFlashMonitor && flashInfoImage != null)
		{
			int flash = CameraController.getFlashMode();
			if (flash != -1 && flashInfoImage != null && CameraController.isFlashModeSupported())
			{
				int flash_id = CameraScreenActivity.getInstance().getFlashIcon(flash);
				if (flash_id != -1)
				{
					flashInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(flash_id));
					flashInfoImage.setVisibility(View.VISIBLE);
				} else
					flashInfoImage.setVisibility(View.GONE);
			} else
				flashInfoImage.setVisibility(View.GONE);
		}

		if (useISOMonitor && isoInfoImage != null)
		{
			int iso = CameraController.getISOMode();
			if (iso != -1 && isoInfoImage != null && CameraController.isISOSupported())
			{
				int iso_id = CameraScreenActivity.getInstance().getISOIcon(iso);
				if (iso_id != -1)
				{
					isoInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(iso_id));
					isoInfoImage.setVisibility(View.VISIBLE);
				} else
					isoInfoImage.setVisibility(View.GONE);
			} else
				isoInfoImage.setVisibility(View.GONE);
		}
	}

	@Override
	public void onExportFinished()
	{
		if (usePictureCount && memoryInfoText != null)
		{
			String memoryString = String.valueOf(Util.AvailablePictureCount());
			memoryInfoText.setText(memoryString);
		}
	}

	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_EV_CHANGED)
		{
			if (this.useEVMonitor && evInfoText != null)
			{
				try
				{
					float iEV = CameraController.getExposureCompensation();
					String evString = (iEV > 0 ? "+" : "") + String.format("%.1f", iEV) + "EV";
					evInfoText.setText(evString);
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.e("InfosetVFPlugin", "onBroadcast exception: " + e.getMessage());
				}
			}
		} else if (arg1 == PluginManager.MSG_SCENE_CHANGED)
		{
			if (this.useSceneMonitor && sceneInfoImage != null)
			{
				int scene = CameraController.getSceneMode();
				if (scene != -1 && sceneInfoImage != null)
				{
					int scene_id = CameraScreenActivity.getInstance().getSceneIcon(scene);
					if (scene_id != -1)
						sceneInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
								.getDrawable(scene_id));
				}
				sceneInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
						.getDrawable(CameraScreenActivity.getInstance().getSceneIcon(scene)));
			}
		} else if (arg1 == PluginManager.MSG_WB_CHANGED)
		{
			if (this.useWBMonitor && wbInfoImage != null)
			{
				int wb = CameraController.getWBMode();
				if (wb != -1 && wbInfoImage != null)
				{
					int wb_id = CameraScreenActivity.getInstance().getWBIcon(wb);
					if (wb_id != -1)
						wbInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(wb_id));
				}
				wbInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
						.getDrawable(CameraScreenActivity.getInstance().getWBIcon(wb)));
			}
		} else if (arg1 == PluginManager.MSG_FOCUS_CHANGED)
		{
			if (this.useFocusMonitor && focusInfoImage != null)
			{
				int focus = CameraController.getFocusMode();
				if (focus != -1 && focusInfoImage != null)
				{
					int focus_id = CameraScreenActivity.getInstance().getFocusIcon(focus);
					if (focus_id != -1)
						focusInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
								.getDrawable(focus_id));
				}
				focusInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
						.getDrawable(CameraScreenActivity.getInstance().getFocusIcon(focus)));
			}
		} else if (arg1 == PluginManager.MSG_FLASH_CHANGED)
		{
			if (this.useFlashMonitor && flashInfoImage != null)
			{
				int flash = CameraController.getFlashMode();
				if (flash != -1 && flashInfoImage != null)
				{
					int flash_id = CameraScreenActivity.getInstance().getFlashIcon(flash);
					if (flash_id != -1)
						flashInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
								.getDrawable(flash_id));
				}
				flashInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources()
						.getDrawable(CameraScreenActivity.getInstance().getFlashIcon(flash)));
			}
		} else if (arg1 == PluginManager.MSG_ISO_CHANGED)
		{
			if (this.useISOMonitor && isoInfoImage != null)
			{
				int iso = CameraController.getISOMode();
				if (iso != -1 && isoInfoImage != null)
				{
					int iso_id = CameraScreenActivity.getInstance().getISOIcon(iso);
					if (iso_id != -1)
						isoInfoImage.setImageDrawable(CameraScreenActivity.getMainContext().getResources().getDrawable(iso_id));
				}
			}
		}

		return false;
	}
}
