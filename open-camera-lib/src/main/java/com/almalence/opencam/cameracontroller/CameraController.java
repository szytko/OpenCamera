/*
	CameraController for OpenCamera project - interface to camera device
    Copyright (C) 2014  Almalence Inc.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* <!-- +++
package com.almalence.opencam_plus.cameracontroller;
+++ --> */
// <!-- -+-
package com.almalence.opencam.cameracontroller;


import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.almalence.SwapHeap;
import com.almalence.opencam.CameraScreenActivity;
import com.almalence.opencam.ui.GUI;
import com.almalence.util.ImageConversion;
//<!-- -+-
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginManagerInterface;
import com.almalence.opencam.R;


public class CameraController implements Camera.PictureCallback, Camera.AutoFocusCallback, Camera.ErrorCallback,
        Camera.PreviewCallback, Camera.ShutterCallback, Handler.Callback
{
    private static final String						TAG								= "CameraController";

    // YUV_RAW is the same as YUV (ie NV21) except that
    // noise filtering, edge enhancements and scaler
    // are disabled if possible
    public static final int							RAW							    = 0x20;
    public static final int							YUV_RAW							= 0x22;
    public static final int							YUV								= 0x23;
    public static final int							JPEG							= 0x100;

    protected static final long						MPIX_1080						= 1920 * 1080;

    // Android camera parameters constants
    private static String							sceneAuto;
    private static String							sceneAction;
    private static String							scenePortrait;
    private static String							sceneLandscape;
    private static String							sceneNight;
    private static String							sceneNightPortrait;
    private static String							sceneTheatre;
    private static String							sceneBeach;
    private static String							sceneSnow;
    private static String							sceneSunset;
    private static String							sceneSteadyPhoto;
    private static String							sceneFireworks;
    private static String							sceneSports;
    private static String							sceneParty;
    private static String							sceneCandlelight;
    private static String							sceneBarcode;
    private static String							sceneHDR;
    private static String							sceneAR;

    private static String							wbAuto;
    private static String							wbIncandescent;
    private static String							wbFluorescent;
    private static String							wbWarmFluorescent;
    private static String							wbDaylight;
    private static String							wbCloudyDaylight;
    private static String							wbTwilight;
    private static String							wbShade;

    private static String							focusAuto;
    private static String							focusInfinity;
    private static String							focusNormal;
    private static String							focusMacro;
    private static String							focusFixed;
    private static String							focusEdof;
    private static String							focusContinuousVideo;
    private static String							focusContinuousPicture;
    private static String							focusAfLock;

    private static String							flashAuto;
    private static String							flashOn;
    private static String							flashOff;
    private static String							flashRedEye;
    private static String							flashTorch;

    private static String							isoAuto;
    private static String							iso50;
    private static String							iso100;
    private static String							iso200;
    private static String							iso400;
    private static String							iso800;
    private static String							iso1600;
    private static String							iso3200;

    private static String							isoAuto_2;
    private static String							iso50_2;
    private static String							iso100_2;
    private static String							iso200_2;
    private static String							iso400_2;
    private static String							iso800_2;
    private static String							iso1600_2;
    private static String							iso3200_2;

    private static String							meteringAuto;
    private static String							meteringMatrix;
    private static String							meteringCenter;
    private static String							meteringSpot;

    // List of localized names for camera parameters values
    private static Map<Integer, String>				mode_scene;
    private static Map<String, Integer>				key_scene;

    private static Map<Integer, String>				mode_wb;
    private static Map<String, Integer>				key_wb;

    private static Map<Integer, String>				mode_focus;
    private static Map<String, Integer>				key_focus;

    private static Map<Integer, String>				mode_flash;
    private static Map<String, Integer>				key_flash;

    private static List<Integer>					iso_values;
    private static List<String>						iso_default;
    private static Map<String, String>				iso_default_values;
    private static Map<Integer, String>				mode_iso;
    private static Map<Integer, String>				mode_iso2;
    private static Map<Integer, Integer>			mode_iso_HALv3;
    private static Map<String, Integer>				key_iso;
    private static Map<String, Integer>				key_iso2;

    private static CameraController					cameraController				= null;

    private static PluginManagerInterface			pluginManager					= null;
    private static ApplicationInterface				appInterface					= null;
    protected static Context						mainContext						= null;

    // Old camera interface
    //private static Camera							camera							= null;
    private static boolean                          isCameraSetup                   = false;

    private static Camera.Parameters				cameraParameters				= null;

    private static byte[]							pviewBuffer;

    // Message handler for multishot capturing with pause between shots
    // and different exposure compensations
    private static Handler							messageHandler;
    private static Handler							pauseHandler;

    private static boolean							needRelaunch					= false;
    public static boolean							isVideoModeLaunched				= false;

    protected static boolean						isRAWCaptureSupported			= false;

    public static boolean   isCamera2 = false;
    private static boolean							isUseISO2Keys					= true;


    // Flags to know which camera feature supported at current device
    private static boolean							mEVSupported					= false;
    private static boolean							mSceneModeSupported				= false;
    private static boolean							mWBSupported					= false;
    private static boolean							mFocusModeSupported				= false;
    private static boolean							mFlashModeSupported				= false;
    private static boolean							mISOSupported					= false;

    private static int								minExpoCompensation				= 0;
    private static int								maxExpoCompensation				= 0;
    private static float							expoCompensationStep			= 0;

    protected static boolean						mVideoStabilizationSupported	= false;

    private static int[]							supportedSceneModes;
    private static int[]							supportedWBModes;
    private static int[]							supportedFocusModes;
    private static int[]							supportedFlashModes;
    private static int[]							supportedISOModes;

    private static int								maxRegionsSupported;

    protected static int							CameraIndex						= 0;
    protected static boolean						CameraMirrored					= false;

    // Image size index for capturing
    private static int								CapIdx;

    private static Size								imageSize;

    public static final int							MIN_MPIX_SUPPORTED				= 1080 * 720;

    // Lists of resolutions, their indexes and names (for capturing and preview)
    protected static List<Long>						ResolutionsMPixList;
    protected static List<CameraController.Size>	ResolutionsSizeList;
    protected static List<String>					ResolutionsIdxesList;
    protected static List<String>					ResolutionsNamesList;

    public static List<Long>						MultishotResolutionsMPixList;
    public static List<CameraController.Size>		MultishotResolutionsSizeList;
    public static List<String>						MultishotResolutionsIdxesList;
    public static List<String>						MultishotResolutionsNamesList;

    public static List<Integer>						FastIdxelist;

    protected static List<CameraController.Size>	SupportedPreviewSizesList;
    protected static List<CameraController.Size>	SupportedPictureSizesList;

    protected static final CharSequence[]			RATIO_STRINGS
            = { " ", "4:3", "3:2", "16:9", "1:1" };

    // States of focus and capture
    public static final int							FOCUS_STATE_IDLE				= 0;
    public static final int							FOCUS_STATE_FOCUSED				= 1;
    public static final int							FOCUS_STATE_FAIL				= 3;
    public static final int							FOCUS_STATE_FOCUSING			= 4;

    public static final int							CAPTURE_STATE_IDLE				= 0;
    public static final int							CAPTURE_STATE_CAPTURING			= 1;

    private static int								mFocusState						= FOCUS_STATE_IDLE;
    private static int								mCaptureState					= CAPTURE_STATE_IDLE;

    protected static Surface						mPreviewSurface					= null;

    private static final Object						SYNC_OBJECT						= new Object();

    protected static boolean 						appStarted							= false;

    // Singleton access function
    public static CameraController getInstance()
    {
        if (cameraController == null)
        {
            cameraController = new CameraController();
        }
        return cameraController;
    }

    private CameraController()
    {

    }

    public static void onCreate(Context context, ApplicationInterface app, PluginManagerInterface pluginManagerBase)
    {
        pluginManager = pluginManagerBase;
        appInterface = app;
        mainContext = context;

        messageHandler = new Handler(CameraController.getInstance());
        pauseHandler = new Handler(CameraController.getInstance());

        appStarted = false;

        isVideoModeLaunched = false;

        sceneAuto = mainContext.getResources().getString(R.string.sceneAutoSystem);
        sceneAction = mainContext.getResources().getString(R.string.sceneActionSystem);
        scenePortrait = mainContext.getResources().getString(R.string.scenePortraitSystem);
        sceneLandscape = mainContext.getResources().getString(R.string.sceneLandscapeSystem);
        sceneNight = mainContext.getResources().getString(R.string.sceneNightSystem);
        sceneNightPortrait = mainContext.getResources().getString(R.string.sceneNightPortraitSystem);
        sceneTheatre = mainContext.getResources().getString(R.string.sceneTheatreSystem);
        sceneBeach = mainContext.getResources().getString(R.string.sceneBeachSystem);
        sceneSnow = mainContext.getResources().getString(R.string.sceneSnowSystem);
        sceneSunset = mainContext.getResources().getString(R.string.sceneSunsetSystem);
        sceneSteadyPhoto = mainContext.getResources().getString(R.string.sceneSteadyPhotoSystem);
        sceneFireworks = mainContext.getResources().getString(R.string.sceneFireworksSystem);
        sceneSports = mainContext.getResources().getString(R.string.sceneSportsSystem);
        sceneParty = mainContext.getResources().getString(R.string.scenePartySystem);
        sceneCandlelight = mainContext.getResources().getString(R.string.sceneCandlelightSystem);
        sceneBarcode = mainContext.getResources().getString(R.string.sceneBarcodeSystem);
        sceneHDR = mainContext.getResources().getString(R.string.sceneHDRSystem);
        sceneAR = mainContext.getResources().getString(R.string.sceneARSystem);

        wbAuto = mainContext.getResources().getString(R.string.wbAutoSystem);
        wbIncandescent = mainContext.getResources().getString(R.string.wbIncandescentSystem);
        wbFluorescent = mainContext.getResources().getString(R.string.wbFluorescentSystem);
        wbWarmFluorescent = mainContext.getResources().getString(R.string.wbWarmFluorescentSystem);
        wbDaylight = mainContext.getResources().getString(R.string.wbDaylightSystem);
        wbCloudyDaylight = mainContext.getResources().getString(R.string.wbCloudyDaylightSystem);
        wbTwilight = mainContext.getResources().getString(R.string.wbTwilightSystem);
        wbShade = mainContext.getResources().getString(R.string.wbShadeSystem);

        focusAuto = mainContext.getResources().getString(R.string.focusAutoSystem);
        focusInfinity = mainContext.getResources().getString(R.string.focusInfinitySystem);
        focusNormal = mainContext.getResources().getString(R.string.focusNormalSystem);
        focusMacro = mainContext.getResources().getString(R.string.focusMacroSystem);
        focusFixed = mainContext.getResources().getString(R.string.focusFixedSystem);
        focusEdof = mainContext.getResources().getString(R.string.focusEdofSystem);
        focusContinuousVideo = mainContext.getResources().getString(R.string.focusContinuousVideoSystem);
        focusContinuousPicture = mainContext.getResources().getString(R.string.focusContinuousPictureSystem);
        focusAfLock = mainContext.getResources().getString(R.string.focusAfLockSystem);

        flashAuto = mainContext.getResources().getString(R.string.flashAutoSystem);
        flashOn = mainContext.getResources().getString(R.string.flashOnSystem);
        flashOff = mainContext.getResources().getString(R.string.flashOffSystem);
        flashRedEye = mainContext.getResources().getString(R.string.flashRedEyeSystem);
        flashTorch = mainContext.getResources().getString(R.string.flashTorchSystem);

        isoAuto = mainContext.getResources().getString(R.string.isoAutoSystem);
        iso50 = mainContext.getResources().getString(R.string.iso50System);
        iso100 = mainContext.getResources().getString(R.string.iso100System);
        iso200 = mainContext.getResources().getString(R.string.iso200System);
        iso400 = mainContext.getResources().getString(R.string.iso400System);
        iso800 = mainContext.getResources().getString(R.string.iso800System);
        iso1600 = mainContext.getResources().getString(R.string.iso1600System);
        iso3200 = mainContext.getResources().getString(R.string.iso3200System);

        isoAuto_2 = mainContext.getResources().getString(R.string.isoAutoDefaultSystem);
        iso50_2 = mainContext.getResources().getString(R.string.iso50DefaultSystem);
        iso100_2 = mainContext.getResources().getString(R.string.iso100DefaultSystem);
        iso200_2 = mainContext.getResources().getString(R.string.iso200DefaultSystem);
        iso400_2 = mainContext.getResources().getString(R.string.iso400DefaultSystem);
        iso800_2 = mainContext.getResources().getString(R.string.iso800DefaultSystem);
        iso1600_2 = mainContext.getResources().getString(R.string.iso1600DefaultSystem);
        iso3200_2 = mainContext.getResources().getString(R.string.iso3200DefaultSystem);

        meteringAuto = mainContext.getResources().getString(R.string.meteringAutoSystem);
        meteringMatrix = mainContext.getResources().getString(R.string.meteringMatrixSystem);
        meteringCenter = mainContext.getResources().getString(R.string.meteringCenterSystem);
        meteringSpot = mainContext.getResources().getString(R.string.meteringSpotSystem);

        // List of localized names for camera parameters values
        mode_scene = new HashMap<Integer, String>()
        {
            {
                put(CameraParameters.SCENE_MODE_AUTO, sceneAuto);
                put(CameraParameters.SCENE_MODE_ACTION, sceneAction);
                put(CameraParameters.SCENE_MODE_PORTRAIT, scenePortrait);
                put(CameraParameters.SCENE_MODE_LANDSCAPE, sceneLandscape);
                put(CameraParameters.SCENE_MODE_NIGHT, sceneNight);
                put(CameraParameters.SCENE_MODE_NIGHT_PORTRAIT, sceneNightPortrait);
                put(CameraParameters.SCENE_MODE_THEATRE, sceneTheatre);
                put(CameraParameters.SCENE_MODE_BEACH, sceneBeach);
                put(CameraParameters.SCENE_MODE_SNOW, sceneSnow);
                put(CameraParameters.SCENE_MODE_SUNSET, sceneSunset);
                put(CameraParameters.SCENE_MODE_STEADYPHOTO, sceneSteadyPhoto);
                put(CameraParameters.SCENE_MODE_FIREWORKS, sceneFireworks);
                put(CameraParameters.SCENE_MODE_SPORTS, sceneSports);
                put(CameraParameters.SCENE_MODE_PARTY, sceneParty);
                put(CameraParameters.SCENE_MODE_CANDLELIGHT, sceneCandlelight);
                put(CameraParameters.SCENE_MODE_BARCODE, sceneBarcode);
            }
        };

        key_scene = new HashMap<String, Integer>()
        {
            {
                put(sceneAuto, CameraParameters.SCENE_MODE_AUTO);
                put(sceneAction, CameraParameters.SCENE_MODE_ACTION);
                put(scenePortrait, CameraParameters.SCENE_MODE_PORTRAIT);
                put(sceneLandscape, CameraParameters.SCENE_MODE_LANDSCAPE);
                put(sceneNight, CameraParameters.SCENE_MODE_NIGHT);
                put(sceneNightPortrait, CameraParameters.SCENE_MODE_NIGHT_PORTRAIT);
                put(sceneTheatre, CameraParameters.SCENE_MODE_THEATRE);
                put(sceneBeach, CameraParameters.SCENE_MODE_BEACH);
                put(sceneSnow, CameraParameters.SCENE_MODE_SNOW);
                put(sceneSunset, CameraParameters.SCENE_MODE_SUNSET);
                put(sceneSteadyPhoto, CameraParameters.SCENE_MODE_STEADYPHOTO);
                put(sceneFireworks, CameraParameters.SCENE_MODE_FIREWORKS);
                put(sceneSports, CameraParameters.SCENE_MODE_SPORTS);
                put(sceneParty, CameraParameters.SCENE_MODE_PARTY);
                put(sceneCandlelight, CameraParameters.SCENE_MODE_CANDLELIGHT);
                put(sceneBarcode, CameraParameters.SCENE_MODE_BARCODE);
            }
        };

        mode_wb = new HashMap<Integer, String>()
        {
            {
                put(CameraParameters.WB_MODE_AUTO, wbAuto);
                put(CameraParameters.WB_MODE_INCANDESCENT, wbIncandescent);
                put(CameraParameters.WB_MODE_FLUORESCENT, wbFluorescent);
                put(CameraParameters.WB_MODE_WARM_FLUORESCENT, wbWarmFluorescent);
                put(CameraParameters.WB_MODE_DAYLIGHT, wbDaylight);
                put(CameraParameters.WB_MODE_CLOUDY_DAYLIGHT, wbCloudyDaylight);
                put(CameraParameters.WB_MODE_TWILIGHT, wbTwilight);
                put(CameraParameters.WB_MODE_SHADE, wbShade);
            }
        };

        key_wb = new HashMap<String, Integer>()
        {
            {
                put(wbAuto, CameraParameters.WB_MODE_AUTO);
                put(wbIncandescent, CameraParameters.WB_MODE_INCANDESCENT);
                put(wbFluorescent, CameraParameters.WB_MODE_FLUORESCENT);
                put(wbWarmFluorescent, CameraParameters.WB_MODE_WARM_FLUORESCENT);
                put(wbDaylight, CameraParameters.WB_MODE_DAYLIGHT);
                put(wbCloudyDaylight, CameraParameters.WB_MODE_CLOUDY_DAYLIGHT);
                put(wbTwilight, CameraParameters.WB_MODE_TWILIGHT);
                put(wbShade, CameraParameters.WB_MODE_SHADE);
            }
        };

        mode_focus = new HashMap<Integer, String>()
        {
            {
                put(CameraParameters.AF_MODE_AUTO, focusAuto);
                put(CameraParameters.AF_MODE_INFINITY, focusInfinity);
                put(CameraParameters.AF_MODE_NORMAL, focusNormal);
                put(CameraParameters.AF_MODE_MACRO, focusMacro);
                put(CameraParameters.AF_MODE_FIXED, focusFixed);
                put(CameraParameters.AF_MODE_EDOF, focusEdof);
                put(CameraParameters.AF_MODE_CONTINUOUS_VIDEO, focusContinuousVideo);
                put(CameraParameters.AF_MODE_CONTINUOUS_PICTURE, focusContinuousPicture);
            }
        };

        key_focus = new HashMap<String, Integer>()
        {
            {
                put(focusAuto, CameraParameters.AF_MODE_AUTO);
                put(focusInfinity, CameraParameters.AF_MODE_INFINITY);
                put(focusNormal, CameraParameters.AF_MODE_NORMAL);
                put(focusMacro, CameraParameters.AF_MODE_MACRO);
                put(focusFixed, CameraParameters.AF_MODE_FIXED);
                put(focusEdof, CameraParameters.AF_MODE_EDOF);
                put(focusContinuousVideo, CameraParameters.AF_MODE_CONTINUOUS_VIDEO);
                put(focusContinuousPicture, CameraParameters.AF_MODE_CONTINUOUS_PICTURE);
            }
        };

        mode_flash = new HashMap<Integer, String>()
        {
            {
                put(CameraParameters.FLASH_MODE_OFF, flashOff);
                put(CameraParameters.FLASH_MODE_AUTO, flashAuto);
                put(CameraParameters.FLASH_MODE_SINGLE, flashOn);
                put(CameraParameters.FLASH_MODE_REDEYE, flashRedEye);
                put(CameraParameters.FLASH_MODE_TORCH, flashTorch);
            }
        };

        key_flash = new HashMap<String, Integer>()
        {
            {
                put(flashOff, CameraParameters.FLASH_MODE_OFF);
                put(flashAuto, CameraParameters.FLASH_MODE_AUTO);
                put(flashOn, CameraParameters.FLASH_MODE_SINGLE);
                put(flashRedEye, CameraParameters.FLASH_MODE_REDEYE);
                put(flashTorch, CameraParameters.FLASH_MODE_TORCH);
            }
        };

        iso_values = new ArrayList<Integer>()
        {
            {
                add(CameraParameters.ISO_AUTO);
                add(CameraParameters.ISO_50);
                add(CameraParameters.ISO_100);
                add(CameraParameters.ISO_200);
                add(CameraParameters.ISO_400);
                add(CameraParameters.ISO_800);
                add(CameraParameters.ISO_1600);
                add(CameraParameters.ISO_3200);
            }
        };

        iso_default = new ArrayList<String>()
        {
            {
                add(isoAuto);
                add(iso100);
                add(iso200);
                add(iso400);
                add(iso800);
                add(iso1600);
            }
        };

        iso_default_values = new HashMap<String, String>()
        {
            {
                put(isoAuto, mainContext.getResources().getString(R.string.isoAutoDefaultSystem));
                put(iso100, mainContext.getResources().getString(R.string.iso100DefaultSystem));
                put(iso200, mainContext.getResources().getString(R.string.iso200DefaultSystem));
                put(iso400, mainContext.getResources().getString(R.string.iso400DefaultSystem));
                put(iso800, mainContext.getResources().getString(R.string.iso800DefaultSystem));
                put(iso1600, mainContext.getResources().getString(R.string.iso1600DefaultSystem));
            }
        };

        mode_iso = new HashMap<Integer, String>()
        {
            {
                put(CameraParameters.ISO_AUTO, isoAuto);
                put(CameraParameters.ISO_50, iso50);
                put(CameraParameters.ISO_100, iso100);
                put(CameraParameters.ISO_200, iso200);
                put(CameraParameters.ISO_400, iso400);
                put(CameraParameters.ISO_800, iso800);
                put(CameraParameters.ISO_1600, iso1600);
                put(CameraParameters.ISO_3200, iso3200);
            }
        };

        mode_iso2 = new HashMap<Integer, String>()
        {
            {
                put(CameraParameters.ISO_AUTO, isoAuto_2);
                put(CameraParameters.ISO_50, iso50_2);
                put(CameraParameters.ISO_100, iso100_2);
                put(CameraParameters.ISO_200, iso200_2);
                put(CameraParameters.ISO_400, iso400_2);
                put(CameraParameters.ISO_800, iso800_2);
                put(CameraParameters.ISO_1600, iso1600_2);
                put(CameraParameters.ISO_3200, iso3200_2);
            }
        };

        mode_iso_HALv3 = new HashMap<Integer, Integer>()
        {
            {
                put(CameraParameters.ISO_AUTO, 1);
                put(CameraParameters.ISO_50, 50);
                put(CameraParameters.ISO_100, 100);
                put(CameraParameters.ISO_200, 200);
                put(CameraParameters.ISO_400, 400);
                put(CameraParameters.ISO_800, 800);
                put(CameraParameters.ISO_1600, 1600);
                put(CameraParameters.ISO_3200, 3200);
            }
        };

        key_iso = new HashMap<String, Integer>()
        {
            {
                put(isoAuto, CameraParameters.ISO_AUTO);
                put(iso50, CameraParameters.ISO_50);
                put(iso100, CameraParameters.ISO_100);
                put(iso200, CameraParameters.ISO_200);
                put(iso400, CameraParameters.ISO_400);
                put(iso800, CameraParameters.ISO_800);
                put(iso1600, CameraParameters.ISO_1600);
                put(iso3200, CameraParameters.ISO_3200);
            }
        };

        key_iso2 = new HashMap<String, Integer>()
        {
            {
                put(isoAuto_2, CameraParameters.ISO_AUTO);
                put(iso50_2, CameraParameters.ISO_50);
                put(iso100_2, CameraParameters.ISO_100);
                put(iso200_2, CameraParameters.ISO_200);
                put(iso400_2, CameraParameters.ISO_400);
                put(iso800_2, CameraParameters.ISO_800);
                put(iso1600_2, CameraParameters.ISO_1600);
                put(iso3200_2, CameraParameters.ISO_3200);
            }
        };

        setCameraISO(CameraParameters.ISO_3200);
    }



    public static void onStart()
    {
        // Does nothing yet
    }

    public static void onResume()
    {
        total_frames = 0;
    }

    public static void onPause(boolean isModeSwitching)
    {
        total_frames = 0;

        try
        {
            Camera.Parameters p = cameraController.getCameraParameters();
            if (p != null && cameraController.isFlashModeSupported())
            {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                cameraController.setCameraParameters(p);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if (CameraProvider.getInstance().getCamera() != null)
        {
            CameraProvider.getInstance().getCamera().setPreviewCallback(null);
            if (!isModeSwitching)
            {
                CameraProvider.getInstance().getCamera().stopPreview();
                isCameraSetup = false;
            }
        }

    }

    public static void onStop()
    {
        if(needRelaunch)
        {
            SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext()).edit();
            prefEditor.putBoolean(CameraScreenActivity.getMainContext().getResources().getString(R.string.Preference_UseHALv3Key), true).commit();
        }
    }

    public static void onDestroy(){}

	/* Get different list and maps of camera parameters */

        /* Preview buffer methods */
    public static void allocatePreviewBuffer(int size)
    {
        pviewBuffer = new byte[size];
    }

    public static byte[] getPreviewBuffer()
    {
        return pviewBuffer;
    }


    public static boolean isSuperModePossible()
    {
        boolean SuperModeOk = false;
        return SuperModeOk;
    }

    public static boolean isUseSuperMode()
    {
        return (isSuperModePossible() ) || isVideoModeLaunched;
    }

    public static boolean isRAWCaptureSupported()
    {
        return isRAWCaptureSupported;
    }

    public static void setupCamera(SurfaceHolder holder)
    {
        if (CameraProvider.getInstance().getCamera() == null)
        {
            Toast.makeText(mainContext, "Unable to start camera", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            cameraController.mVideoStabilizationSupported = getVideoStabilizationSupported();


        pluginManager.selectDefaults();

        // screen rotation
        try
        {
            CameraProvider.getInstance().getCamera().setDisplayOrientation(90);
        } catch (RuntimeException e)
        {
            Log.e(TAG, "Unable to set display orientation for camera");
            e.printStackTrace();
        }

        try
        {
            CameraProvider.getInstance().getCamera().setPreviewDisplay(holder);
        } catch (IOException e)
        {
            Log.e(TAG, "Unable to set preview display for camera");
            e.printStackTrace();
        }

        CameraController.fillPreviewSizeList();
        CameraController.fillPictureSizeList();


        populateCameraDimensions();
        populateCameraDimensionsForMultishots();


        pluginManager.selectImageDimension(); // updates SX, SY values

        Message msg = new Message();
        msg.what = PluginManager.MSG_CAMERA_READY;
        CameraScreenActivity.getMessageHandler().sendMessage(msg);
        isCameraSetup = true;

    }

    public static boolean isCameraCreated()
    {
        return CameraProvider.getInstance().getCamera() != null && isCameraSetup == true;


    }

    private static void fillPreviewSizeList()
    {
        CameraController.SupportedPreviewSizesList = new ArrayList<CameraController.Size>();

        if(CameraProvider.getInstance().getCamera() != null && CameraProvider.getInstance().getCamera().getParameters() != null)
        {
            List<Camera.Size> list = CameraProvider.getInstance().getCamera().getParameters().getSupportedPreviewSizes();
            for (Camera.Size sz : list)
                CameraController.SupportedPreviewSizesList.add(new CameraController.Size(sz.width,
                        sz.height));
        }

    }

    private static void fillPictureSizeList()
    {
        CameraController.SupportedPictureSizesList = new ArrayList<CameraController.Size>();
        if(CameraProvider.getInstance().getCamera() != null && CameraProvider.getInstance().getCamera().getParameters() != null)
        {
            List<Camera.Size> list = CameraProvider.getInstance().getCamera().getParameters().getSupportedPictureSizes();
            for (Camera.Size sz : list)
                CameraController.SupportedPictureSizesList.add(new CameraController.Size(sz.width,
                        sz.height));
        }

    }

    public static void populateCameraDimensions()
    {
        CameraController.ResolutionsMPixList = new ArrayList<Long>();
        CameraController.ResolutionsSizeList = new ArrayList<Size>();
        CameraController.ResolutionsIdxesList = new ArrayList<String>();
        CameraController.ResolutionsNamesList = new ArrayList<String>();
        CameraController.FastIdxelist = new ArrayList<Integer>();

        int minMPIX = CameraController.MIN_MPIX_SUPPORTED;
        Camera.Parameters cp = getCameraParameters();
        List<Camera.Size> cs = cp.getSupportedPictureSizes();

        if (cs == null)
            return;

        int iHighestIndex = 0;
        Camera.Size sHighest = cs.get(0);

        for (int ii = 0; ii < cs.size(); ++ii)
        {
            Camera.Size s = cs.get(ii);

            int currSizeWidth = s.width;
            int currSizeHeight = s.height;
            int highestSizeWidth = sHighest.width;
            int highestSizeHeight = sHighest.height;

            if ((long) currSizeWidth * currSizeHeight > (long) highestSizeWidth * highestSizeHeight)
            {
                sHighest = s;
                iHighestIndex = ii;
            }

            if ((long) currSizeWidth * currSizeHeight < minMPIX)
                continue;

            fillResolutionsList(ii, currSizeWidth, currSizeHeight);
        }

        if (CameraController.ResolutionsNamesList.isEmpty())
        {
            Camera.Size s = cs.get(iHighestIndex);

            int currSizeWidth = s.width;
            int currSizeHeight = s.height;

            fillResolutionsList(0, currSizeWidth, currSizeHeight);
        }

        return;
    }

    protected static void fillResolutionsList(int ii, int currSizeWidth, int currSizeHeight)
    {
        boolean needAdd = true;
        boolean isFast = false;

        Long lmpix = (long) currSizeWidth * currSizeHeight;
        float mpix = (float) lmpix / 1000000.f;
        float ratio = (float) currSizeWidth / currSizeHeight;

        // find good location in a list
        int loc;
        for (loc = 0; loc < CameraController.ResolutionsMPixList.size(); ++loc)
            if (CameraController.ResolutionsMPixList.get(loc) < lmpix)
                break;

        int ri = 0;
        if (Math.abs(ratio - 4 / 3.f) < 0.1f)
            ri = 1;
        if (Math.abs(ratio - 3 / 2.f) < 0.12f)
            ri = 2;
        if (Math.abs(ratio - 16 / 9.f) < 0.15f)
            ri = 3;
        if (Math.abs(ratio - 1) == 0)
            ri = 4;

        for (int i = 0; i < CameraController.SupportedPreviewSizesList.size(); i++)
        {
            if (currSizeWidth == CameraController.SupportedPreviewSizesList.get(i).getWidth()
                    && currSizeHeight == CameraController.SupportedPreviewSizesList.get(i).getHeight())
            {
                isFast = true;
            }
        }

        String newName;
        if (isFast)
        {
            newName = String.format("%3.1f Mpix  " + RATIO_STRINGS[ri] + " (fast)", mpix);
        } else
        {
            newName = String.format("%3.1f Mpix  " + RATIO_STRINGS[ri], mpix);
        }

        for (int i = 0; i < CameraController.ResolutionsNamesList.size(); i++)
        {
            if (newName.equals(CameraController.ResolutionsNamesList.get(i)))
            {
                Long lmpixInArray = (long) (CameraController.ResolutionsSizeList.get(i).getWidth() * CameraController.ResolutionsSizeList
                        .get(i).getHeight());
                if (Math.abs(lmpixInArray - lmpix) / lmpix < 0.1)
                {
                    needAdd = false;
                    break;
                }
            }
        }

        if (needAdd)
        {
            if (isFast)
            {
                CameraController.FastIdxelist.add(ii);
            }
            CameraController.ResolutionsNamesList.add(loc, newName);
            CameraController.ResolutionsIdxesList.add(loc, String.format("%d", ii));
            CameraController.ResolutionsMPixList.add(loc, lmpix);
            CameraController.ResolutionsSizeList.add(loc, new CameraController.Size(currSizeWidth,
                    currSizeHeight));
        }
    }

    public static void populateCameraDimensionsForMultishots()
    {
        CameraController.MultishotResolutionsMPixList = new ArrayList<Long>(CameraController.ResolutionsMPixList);
        CameraController.MultishotResolutionsSizeList = new ArrayList<CameraController.Size>(
                CameraController.ResolutionsSizeList);
        CameraController.MultishotResolutionsIdxesList = new ArrayList<String>(CameraController.ResolutionsIdxesList);
        CameraController.MultishotResolutionsNamesList = new ArrayList<String>(CameraController.ResolutionsNamesList);

        if (CameraController.SupportedPreviewSizesList != null && CameraController.SupportedPreviewSizesList.size() > 0)
        {
            fillResolutionsListMultishot(MultishotResolutionsIdxesList.size(),
                    CameraController.SupportedPreviewSizesList.get(0).getWidth(),
                    CameraController.SupportedPreviewSizesList.get(0).getHeight());
        }

        if (CameraController.SupportedPreviewSizesList != null && CameraController.SupportedPreviewSizesList.size() > 1)
        {
            fillResolutionsListMultishot(MultishotResolutionsIdxesList.size(),
                    CameraController.SupportedPreviewSizesList.get(1).getWidth(),
                    CameraController.SupportedPreviewSizesList.get(1).getHeight());
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
        String prefIdx = prefs.getString(CameraScreenActivity.sImageSizeMultishotBackPref, "-1");

        if (prefIdx.equals("-1"))
        {
            int maxFastIdx = -1;
            long maxMpx = 0;
            for (int i = 0; i < CameraController.FastIdxelist.size(); i++)
            {
                for (int j = 0; j < CameraController.MultishotResolutionsMPixList.size(); j++)
                {
                    if (CameraController.FastIdxelist.get(i) == Integer
                            .parseInt(CameraController.MultishotResolutionsIdxesList.get(j))
                            && CameraController.MultishotResolutionsMPixList.get(j) > maxMpx)
                    {
                        maxMpx = CameraController.MultishotResolutionsMPixList.get(j);
                        maxFastIdx = CameraController.FastIdxelist.get(i);
                    }
                }
            }
            if (CameraController.SupportedPreviewSizesList != null
                    && CameraController.SupportedPreviewSizesList.size() > 0 && maxMpx >= MPIX_1080)
            {
                SharedPreferences.Editor prefEditor = prefs.edit();
                prefEditor.putString(CameraScreenActivity.sImageSizeMultishotBackPref, String.valueOf(maxFastIdx));
                prefEditor.commit();
            }
        }

        return;
    }


    public static Map<String, Integer> getIsoKey()
    {
        return key_iso;
    }

    public static List<String> getIsoDefaultList()
    {
        return iso_default;
    }

    protected static void fillResolutionsListMultishot(int ii, int currSizeWidth, int currSizeHeight)
    {
        boolean needAdd = true;
        boolean isFast = true;

        Long lmpix = (long) currSizeWidth * currSizeHeight;
        float mpix = (float) lmpix / 1000000.f;
        float ratio = (float) currSizeWidth / currSizeHeight;

        // find good location in a list
        int loc;
        for (loc = 0; loc < CameraController.MultishotResolutionsMPixList.size(); ++loc)
            if (CameraController.MultishotResolutionsMPixList.get(loc) < lmpix)
                break;

        int ri = 0;
        if (Math.abs(ratio - 4 / 3.f) < 0.12f)
            ri = 1;
        if (Math.abs(ratio - 3 / 2.f) < 0.12f)
            ri = 2;
        if (Math.abs(ratio - 16 / 9.f) < 0.15f)
            ri = 3;
        if (Math.abs(ratio - 1) == 0)
            ri = 4;

        if (mpix < 0.1f) {
            mpix = 0.1f;
        }

        String newName;
        if (isFast)
        {
            newName = String.format("%3.1f Mpix  " + RATIO_STRINGS[ri] + " (fast)", mpix);
        } else
        {
            newName = String.format("%3.1f Mpix  " + RATIO_STRINGS[ri], mpix);
        }

        for (int i = 0; i < CameraController.MultishotResolutionsNamesList.size(); i++)
        {
            if (newName.equals(CameraController.MultishotResolutionsNamesList.get(i)))
            {
                Long lmpixInArray = (long) (CameraController.MultishotResolutionsSizeList.get(i).getWidth() * MultishotResolutionsSizeList
                        .get(i).getHeight());
                if (Math.abs(lmpixInArray - lmpix) / lmpix < 0.1)
                {
                    needAdd = false;
                    break;
                }
            }
        }

        if (needAdd)
        {
            if (isFast)
            {
                CameraController.FastIdxelist.add(ii);
            }
            CameraController.MultishotResolutionsNamesList.add(loc, newName);
            CameraController.MultishotResolutionsIdxesList.add(loc, String.format("%d", ii));
            CameraController.MultishotResolutionsMPixList.add(loc, lmpix);
            CameraController.MultishotResolutionsSizeList.add(loc, new CameraController.Size(
                    currSizeWidth, currSizeHeight));
        }
    }

    public static List<CameraController.Size> getSupportedPreviewSizes()
    {
        List<CameraController.Size> previewSizes = new ArrayList<CameraController.Size>();

        if (CameraController.SupportedPreviewSizesList != null)
        {
            List<CameraController.Size> sizes = SupportedPreviewSizesList;
            for (CameraController.Size sz : sizes)
                previewSizes.add(new CameraController.Size(sz.getWidth(), sz.getHeight()));
        } else
        {
            Log.d(TAG, "SupportedPreviewSizesList == null");
        }

        return previewSizes;


    }

    public static void setCameraPreviewSize(CameraController.Size sz)
    {
        Camera.Parameters params = getCameraParameters();
        if (params != null)
        {
            params.setPreviewSize(sz.mWidth, sz.mHeight);
            setCameraParameters(params);
        }

    }


    public static List<CameraController.Size> getResolutionsSizeList()
    {
        return CameraController.ResolutionsSizeList;
    }

    public static List<String> getResolutionsIdxesList()
    {
        return CameraController.ResolutionsIdxesList;
    }

    public static List<String> getResolutionsNamesList()
    {
        return CameraController.ResolutionsNamesList;
    }

    public static void updateCameraFeatures()
    {
        if (CameraProvider.getInstance().getCamera() != null)
            cameraParameters = CameraProvider.getInstance().getCamera().getParameters();

        mEVSupported = getExposureCompensationSupported();
        mSceneModeSupported = getSceneModeSupported();
        mWBSupported = getWhiteBalanceSupported();
        mFocusModeSupported = getFocusModeSupported();
        mFlashModeSupported = getFlashModeSupported();
        mISOSupported = getISOSupported();

        if (CameraProvider.getInstance().getCamera() != null && cameraParameters != null)
        {
            minExpoCompensation = cameraParameters.getMinExposureCompensation();
            maxExpoCompensation = cameraParameters.getMaxExposureCompensation();
            expoCompensationStep = cameraParameters.getExposureCompensationStep();
        }


        supportedSceneModes = getSupportedSceneModesInternal();
        supportedWBModes = getSupportedWhiteBalanceInternal();
        supportedFocusModes = getSupportedFocusModesInternal();
        supportedFlashModes = getSupportedFlashModesInternal();
        supportedISOModes = getSupportedISOInternal();

        maxRegionsSupported = CameraController.getMaxNumFocusAreas();

        cameraParameters = null;
    }

    @Override
    public void onError(int arg0, Camera arg1){}

    // ------------ CAMERA PARAMETERS AND CAPABILITIES

    public static boolean isFrontCamera()
    {
        return CameraMirrored;
    }


    public static Camera.Parameters getCameraParameters()
    {
        try
        {
            if (CameraProvider.getInstance().getCamera() != null) {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                return params;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean setCameraParameters(Camera.Parameters params)
    {
        if (params != null && CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                CameraProvider.getInstance().getCamera().setParameters(params);
            } catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, "setCameraParameters exception: " + e.getMessage());
                return false;
            }

            return true;
        }

        return false;
    }



    public static void startCameraPreview()
    {
        if (CameraProvider.getInstance().getCamera() != null) {
            CameraProvider.getInstance().getCamera().startPreview();
        }

    }

    @TargetApi(15)
    public static boolean getVideoStabilizationSupported()
    {
        if (CameraProvider.getInstance().getCamera() != null && CameraProvider.getInstance().getCamera().getParameters() != null)
            return CameraProvider.getInstance().getCamera().getParameters().isVideoStabilizationSupported();

        return false;
    }

    public static boolean isExposureLockSupported()
    {

        if (CameraProvider.getInstance().getCamera() == null || ( CameraProvider.getInstance().getCamera() != null && CameraProvider.getInstance().getCamera().getParameters() == null))
            return false;

        return CameraProvider.getInstance().getCamera().getParameters().isAutoExposureLockSupported();

    }

    public static boolean isExposureLock()
    {

        if (CameraProvider.getInstance().getCamera() == null || ( CameraProvider.getInstance().getCamera() != null && CameraProvider.getInstance().getCamera().getParameters() == null))
            return false;

        return CameraProvider.getInstance().getCamera().getParameters().getAutoExposureLock();

    }

    public static boolean isWhiteBalanceLockSupported()
    {

        if (CameraProvider.getInstance().getCamera() == null || ( CameraProvider.getInstance().getCamera() != null && CameraProvider.getInstance().getCamera().getParameters() == null))
            return false;

        return CameraProvider.getInstance().getCamera().getParameters().isAutoWhiteBalanceLockSupported();

    }

    public static boolean isWhiteBalanceLock()
    {

        if (CameraProvider.getInstance().getCamera() == null || ( CameraProvider.getInstance().getCamera() != null && CameraProvider.getInstance().getCamera().getParameters() == null))
            return false;

        return CameraProvider.getInstance().getCamera().getParameters().getAutoWhiteBalanceLock();

    }

    public static boolean isZoomSupported()
    {
        if (null == CameraProvider.getInstance().getCamera() || CameraProvider.getInstance().getCamera().getParameters() == null)
            return false;

        return CameraProvider.getInstance().getCamera().getParameters().isZoomSupported();

    }

    public static int getMaxZoom()
    {

        if (null == CameraProvider.getInstance().getCamera() || CameraProvider.getInstance().getCamera().getParameters() == null)
            return 1;

        return CameraProvider.getInstance().getCamera().getParameters().getMaxZoom();

    }

    public static void setZoom(int value)
    {

        Camera.Parameters cp = getCameraParameters();
        if (cp != null)
        {
            cp.setZoom(value);

            setCameraParameters(cp);
        }

    }

    // Note: getZoom returns zoom in floating point,
    // unlike old android camera API which returns it multiplied by 10
    public static float getZoom()
    {

        Camera.Parameters cp = getCameraParameters();
        if(cp.get("curr_zoom_level")!=null){
            int currZoomLevel = cp.getInt("curr_zoom_level");
            return (currZoomLevel / 10.0f + 1f);
        }

        return (cp.getZoom() / 10.0f + 1f);

    }

    // Used to initialize internal variable
    private static boolean getExposureCompensationSupported()
    {
        if (CameraProvider.getInstance().getCamera() != null)
        {
            if (cameraParameters != null)
            {
                return cameraParameters.getMinExposureCompensation() != 0
                        && cameraParameters.getMaxExposureCompensation() != 0;
            } else
            {
                return CameraProvider.getInstance().getCamera().getParameters().getMinExposureCompensation() != 0
                        && CameraProvider.getInstance().getCamera().getParameters().getMaxExposureCompensation() != 0;
            }

        } else
            return false;

    }

    // Used by CameraController class users.
    public static boolean isExposureCompensationSupported()
    {
        return mEVSupported;
    }

    public static int getMinExposureCompensation()
    {
        return minExpoCompensation;
    }

    public static int getMaxExposureCompensation()
    {
        return maxExpoCompensation;
    }

    public static float getExposureCompensationStep()
    {
        return expoCompensationStep;
    }

    public static float getExposureCompensation()
    {

        if (CameraProvider.getInstance().getCamera() != null && CameraProvider.getInstance().getCamera().getParameters() != null)
        {
            Camera.Parameters cameraParameters = CameraProvider.getInstance().getCamera().getParameters();

            return cameraParameters.getExposureCompensation()
                    * cameraParameters.getExposureCompensationStep();
        }
        else
            return 0;

    }

    private static boolean getSceneModeSupported()
    {
        int[] supported_scene = getSupportedSceneModesInternal();
        return supported_scene != null && supported_scene.length > 0
                && supported_scene[0] != CameraParameters.SCENE_MODE_AUTO;
    }

    public static boolean isSceneModeSupported()
    {
        return mSceneModeSupported;
    }

    private static int[] getSupportedSceneModesInternal()
    {
        List<String> sceneModes = null;
        if (cameraParameters != null)
        {
            sceneModes = cameraParameters.getSupportedSceneModes();
        } else if(CameraProvider.getInstance().getCamera() != null)
        {
            sceneModes = CameraProvider.getInstance().getCamera().getParameters().getSupportedSceneModes();
        }

        if (CameraProvider.getInstance().getCamera() != null && sceneModes != null)
        {
            Set<String> known_scenes = CameraController.key_scene.keySet();
            sceneModes.retainAll(known_scenes);
            int[] scenes = new int[sceneModes.size()];
            for (int i = 0; i < sceneModes.size(); i++)
            {
                String mode = sceneModes.get(i);
                if (CameraController.key_scene.containsKey(mode))
                    scenes[i] = CameraController.key_scene.get(mode).byteValue();
            }

            return scenes;
        }

        return new int[0];

    }


    private static boolean getWhiteBalanceSupported()
    {
        int[] supported_wb = getSupportedWhiteBalanceInternal();
        return supported_wb != null && supported_wb.length > 0;
    }

    public static boolean isWhiteBalanceSupported()
    {
        return mWBSupported;
    }

    private static int[] getSupportedWhiteBalanceInternal()
    {

        List<String> wbModes;
        if (cameraParameters != null)
        {
            wbModes = cameraParameters.getSupportedWhiteBalance();
        } else
        {
            wbModes = CameraProvider.getInstance().getCamera().getParameters().getSupportedWhiteBalance();
        }

        if (CameraProvider.getInstance().getCamera() != null && wbModes != null)
        {
            Set<String> known_wb = CameraController.key_wb.keySet();
            wbModes.retainAll(known_wb);
            int[] wb = new int[wbModes.size()];
            for (int i = 0; i < wbModes.size(); i++)
            {
                String mode = wbModes.get(i);
                if (CameraController.key_wb.containsKey(mode))
                    wb[i] = CameraController.key_wb.get(mode).byteValue();
            }
            return wb;
        }

        return new int[0];

    }

    public static int[] getSupportedWhiteBalance()
    {
        return supportedWBModes;
    }

    private static boolean getFocusModeSupported()
    {
        int[] supported_focus = getSupportedFocusModesInternal();
        return supported_focus != null && supported_focus.length > 0;
    }

    public static boolean isFocusModeSupported()
    {
        return mFocusModeSupported;
    }

    private static int[] getSupportedFocusModesInternal()
    {

        List<String> focusModes;
        if (cameraParameters != null)
        {
            focusModes = cameraParameters.getSupportedFocusModes();
        } else
        {
            focusModes = CameraProvider.getInstance().getCamera().getParameters().getSupportedFocusModes();
        }

        if (CameraProvider.getInstance().getCamera() != null && focusModes != null)
        {
            Set<String> known_focus = CameraController.key_focus.keySet();
            focusModes.retainAll(known_focus);
            int[] focus = new int[focusModes.size()];
            for (int i = 0; i < focusModes.size(); i++)
            {
                String mode = focusModes.get(i);
                if (CameraController.key_focus.containsKey(mode))
                    focus[i] = CameraController.key_focus.get(mode).byteValue();
            }

            return focus;
        }

        return new int[0];

    }

    public static int[] getSupportedFocusModes()
    {
        return supportedFocusModes;
    }

    private static boolean getFlashModeSupported()
    {

        int[] supported_flash = getSupportedFlashModesInternal();
        return supported_flash != null && supported_flash.length > 0;

    }

    public static boolean isFlashModeSupported()
    {
        return mFlashModeSupported;
    }

    private static int[] getSupportedFlashModesInternal()
    {


        List<String> flashModes = null;
        if (cameraParameters != null)
        {
            flashModes = cameraParameters.getSupportedFlashModes();
        } else
        {
            flashModes = CameraProvider.getInstance().getCamera().getParameters().getSupportedFlashModes();
        }

        if (CameraProvider.getInstance().getCamera() != null && flashModes != null)
        {
            Set<String> known_flash = CameraController.key_flash.keySet();
            flashModes.retainAll(known_flash);
            int[] flash = new int[flashModes.size()];
            for (int i = 0; i < flashModes.size(); i++)
            {
                String mode = flashModes.get(i);
                if (CameraController.key_flash.containsKey(mode))
                    flash[i] = CameraController.key_flash.get(flashModes.get(i)).byteValue();
            }

            return flash;
        }

        return new int[0];
    }

    public static int[] getSupportedFlashModes()
    {
        return supportedFlashModes;
    }

    private static boolean getISOSupported()
    {

        int[] supported_iso = getSupportedISOInternal();
        String isoSystem = CameraController.getCameraParameters().get("iso");
        String isoSystem2 = CameraController.getCameraParameters().get("iso-speed");
        return supported_iso.length > 0 || isoSystem != null || isoSystem2 != null;

    }

    public static boolean isISOSupported()
    {
        return mISOSupported;
    }

    public static int[] getSupportedISO()
    {
        return supportedISOModes;
    }

    private static int[] getSupportedISOInternal()
    {
        if (CameraProvider.getInstance().getCamera() != null)
        {
            List<String> isoModes = null;
            Camera.Parameters camParams = CameraController.getCameraParameters();
            String supportedIsoValues = camParams.get("iso-values");
            String supportedIsoValues2 = camParams.get("iso-speed-values");
            String supportedIsoValues3 = camParams.get("iso-mode-values");
            String supportedIsoValues4 = camParams.get("nv-picture-iso-values");

            String delims = "[,]+";
            String[] isoList = null;

            if (supportedIsoValues != null && !supportedIsoValues.equals(""))
                isoList = supportedIsoValues.split(delims);
            else if (supportedIsoValues2 != null && !supportedIsoValues2.equals(""))
                isoList = supportedIsoValues2.split(delims);
            else if (supportedIsoValues3 != null && !supportedIsoValues3.equals(""))
                isoList = supportedIsoValues3.split(delims);
            else if (supportedIsoValues4 != null && !supportedIsoValues4.equals(""))
                isoList = supportedIsoValues4.split(delims);

            if (isoList != null)
            {
                isoModes = new ArrayList<String>();
                for (int i = 0; i < isoList.length; i++)
                    isoModes.add(isoList[i]);
            } else
                return new int[0];

            int supportedISOCount = 0;
            for (int i = 0; i < isoModes.size(); i++)
            {
                String mode = isoModes.get(i);
                if (CameraController.key_iso.containsKey(mode))
                    supportedISOCount++;
                else if (CameraController.key_iso2.containsKey(mode))
                    supportedISOCount++;
            }

            int[] iso = new int[supportedISOCount];
            for (int i = 0, index = 0; i < isoModes.size(); i++)
            {
                String mode = isoModes.get(i);
                if (CameraController.key_iso.containsKey(mode)) {
                    iso[index++] = CameraController.key_iso.get(isoModes.get(i)).byteValue();
                    isUseISO2Keys = false;
                } else if (CameraController.key_iso2.containsKey(mode)) {
                    iso[index++] = CameraController.key_iso2.get(isoModes.get(i)).byteValue();
                    isUseISO2Keys = true;
                }
            }

            return iso;
        }

        return new int[0];

    }

    public static int getMaxNumMeteringAreas()
    {
        if (CameraProvider.getInstance().getCamera() != null)
        {
            Camera.Parameters camParams = CameraProvider.getInstance().getCamera().getParameters();
            return camParams.getMaxNumMeteringAreas();
        }

        return 0;
    }

    private static int getMaxNumFocusAreas()
    {
        if (CameraProvider.getInstance().getCamera() != null)
        {
            Camera.Parameters camParams = CameraProvider.getInstance().getCamera().getParameters();
            return camParams.getMaxNumFocusAreas();
        }

        return 0;
    }

    public static int getMaxAreasSupported()
    {
        return maxRegionsSupported;
    }

    public static int getCameraIndex()
    {
        return CameraIndex;
    }

    public static void setCameraIndex(int index)
    {
        CameraIndex = index;
    }

    public static void setCameraImageSizeIndex(int captureIndex, boolean init)
    {
        CapIdx = captureIndex;
        if(init)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
            prefs.edit().putString(CameraIndex == 0 ? CameraScreenActivity.sImageSizeRearPref
                    : CameraScreenActivity.sImageSizeFrontPref, String.valueOf(captureIndex)).commit();
        }
    }

    public static void setCameraImageSize(Size imgSize)
    {
        imageSize = imgSize;
    }

    public static Size getCameraImageSize()
    {
        return imageSize;
    }



    public static boolean isModeAvailable(int[] modeList, int mode)
    {
        boolean isAvailable = false;
        for (int currMode : modeList)
        {
            if (currMode == mode)
            {
                isAvailable = true;
                break;
            }
        }
        return isAvailable;
    }

    public static int getSceneMode()
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                    return CameraController.key_scene.get(params.getSceneMode());
            } catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, "getSceneMode exception: " + e.getMessage());
            }
        }

        return -1;
    }

    public static int getWBMode()
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                    return CameraController.key_wb.get(params.getWhiteBalance());
            } catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, "getWBMode exception: " + e.getMessage());
            }
        }

        return -1;
    }

    public static int getFocusMode()
    {


        try
        {
            if (CameraProvider.getInstance().getCamera() != null)
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                    return CameraController.key_focus.get(params.getFocusMode());
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            Log.e(TAG, "getFocusMode exception: " + e.getMessage());
        }

        return -1;
    }

    public static int getFlashMode()
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                    return CameraController.key_flash.get(params.getFlashMode());
            } catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, "getFlashMode exception: " + e.getMessage());
            }
        }

        return -1;
    }

    public static int getISOMode()
    {
        if (CameraProvider.getInstance().getCamera() != null)
        {
            Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
            if (params != null)
            {
                String iso = null;
                iso = params.get("iso");
                if (iso == null)
                    iso = params.get("iso-speed");

                if (CameraController.key_iso.containsKey(iso))
                    return CameraController.key_iso.get(iso);
                else if (CameraController.key_iso2.containsKey(iso))
                    return CameraController.key_iso2.get(iso);
            }
        }

        return -1;
    }

    public static void setCameraSceneMode(int mode)
    {
        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                {
                    params.setSceneMode(CameraController.mode_scene.get(mode));
                    setCameraParameters(params);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void setCameraWhiteBalance(int mode)
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                {
                    params.setWhiteBalance(CameraController.mode_wb.get(mode));
                    setCameraParameters(params);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    public static void setCameraFocusMode(int mode)
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                {
                    String focusmode = CameraController.mode_focus.get(mode);
                    params.setFocusMode(focusmode);
                    setCameraParameters(params);
                    CameraScreenActivity.setAutoFocusLock(false);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    public static void setCameraFlashMode(int mode)
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                {
                    String flashmode = CameraController.mode_flash.get(mode);
                    params.setFlashMode(flashmode);
                    setCameraParameters(params);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

    }


    public static void setCameraExposureCompensation(int iEV)
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                {
                    params.setExposureCompensation(iEV);
                    setCameraParameters(params);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    public static void setCameraFocusAreas(List<Area> focusAreas)
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraController.getCameraParameters();
                if (params != null)
                {
                    params.setFocusAreas(focusAreas);
                    cameraController.setCameraParameters(params);
                }
            } catch (RuntimeException e)
            {
                Log.e(TAG, e.getMessage());
            }
        }

    }

    public static void setCameraMeteringAreas(List<Area> meteringAreas)
    {

        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
                if (params != null)
                {
                    if (meteringAreas != null)
                    {
                        params.setMeteringAreas(meteringAreas);
                        cameraController.setCameraParameters(params);
                    }
                }
            } catch (RuntimeException e)
            {
                Log.e(TAG, e.getMessage());
            }
        }

    }

    public static void setFocusState(int state)
    {
        if (state != CameraController.FOCUS_STATE_IDLE && state != CameraController.FOCUS_STATE_FOCUSED
                && state != CameraController.FOCUS_STATE_FAIL)
            return;

        mFocusState = state;

        PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_FOCUS_STATE_CHANGED);
    }

    public static int getFocusState()
    {
        return mFocusState;
    }

    public static boolean isAutoFocusPerform() {
        int focusMode = CameraController.getFocusMode();
        if (focusMode != -1
                && (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE || CameraController
                .getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
                && !(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
                || focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
                || focusMode == CameraParameters.AF_MODE_INFINITY
                || focusMode == CameraParameters.AF_MODE_FIXED || focusMode == CameraParameters.AF_MODE_EDOF)
                && !CameraScreenActivity.getAutoFocusLock())
            return true;
        else
            return false;
    }

    protected static int[]		pauseBetweenShots	= null;
    protected static int[]		evValues			= null;

    protected static int		total_frames;
    protected static int		frame_num;
    protected static int		frameFormat			= CameraController.JPEG;

    protected static boolean	takePreviewFrame	= false;

    protected static boolean	takeYUVFrame		= false;

    protected static boolean	resultInHeap		= false;

    // Note: per-frame 'gain' and 'exposure' parameters are only effective for Camera2 API at the moment
    public static int captureImagesWithParams(int nFrames, int format, int[] pause, int[] evRequested, int[] gain, long[] exposure, boolean resInHeap)
    {
        pauseBetweenShots = pause;
        evValues = evRequested;

        total_frames = nFrames;
        frame_num = 0;
        frameFormat = format;

        resultInHeap = resInHeap;

        previewWorking=false;
        cdt = null;


        takeYUVFrame = (format == CameraController.YUV) || (format == CameraController.YUV_RAW);
        if (evRequested != null && evRequested.length >= total_frames)
            CameraController.sendMessage(MSG_SET_EXPOSURE);
        else
            CameraController.sendMessage(MSG_TAKE_IMAGE);
        return 0;

    }


    public static boolean autoFocus()
    {
        synchronized (SYNC_OBJECT)
        {

            if (CameraProvider.getInstance().getCamera() != null)
            {
                if (CameraController.mCaptureState != CameraController.CAPTURE_STATE_CAPTURING)
                {
                    CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
                    try
                    {
                        CameraProvider.getInstance().getCamera().autoFocus(CameraController.getInstance());
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                        Log.e(TAG, "autoFocus: " + e.getMessage());
                        return false;
                    }
                    return true;
                }
            }


            return false;
        }
    }

    public static void cancelAutoFocus() {
        CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
        if (CameraProvider.getInstance().getCamera() != null)
        {
            try
            {
                CameraProvider.getInstance().getCamera().cancelAutoFocus();
            } catch (RuntimeException exp)
            {
                Log.e(TAG, "cancelAutoFocus failed. Message: " + exp.getMessage());
            }
        }

    }

    // Callback always contains JPEG frame.
    // So, we have to convert JPEG to YUV if capture plugin has requested YUV
    // frame.
    @Override
    public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
    {
        Log.d(TAG, "onPictureTaken");
        CameraProvider.getInstance().getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
        CameraProvider.getInstance().getCamera().addCallbackBuffer(pviewBuffer);

        pluginManager.addToSharedMemExifTags(paramArrayOfByte);
        if (!CameraController.takeYUVFrame) // if JPEG frame requested
        {

            int frame = 0;
            if (resultInHeap)
                frame = SwapHeap.SwapToHeap(paramArrayOfByte);
            pluginManager.onImageTaken(frame, paramArrayOfByte, paramArrayOfByte.length, CameraController.JPEG);
        } else
        {
            int yuvFrame = ImageConversion.JpegConvert(paramArrayOfByte, imageSize.getWidth(),
                    imageSize.getHeight(), false, false, 0);
            int frameLen = imageSize.getWidth() * imageSize.getHeight() + 2
                    * ((imageSize.getWidth() + 1) / 2) * ((imageSize.getHeight() + 1) / 2);

            byte[] frameData = null;
            if (!resultInHeap)
            {
                frameData = SwapHeap.SwapFromHeap(yuvFrame, frameLen);
                yuvFrame = 0;
            }

            pluginManager.onImageTaken(yuvFrame, frameData, frameLen, CameraController.YUV);
        }

        try {
            CameraController.startCameraPreview();
        } catch (RuntimeException e)
        {
            CameraScreenActivity.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED_IOEXCEPTION);
            CameraScreenActivity.getInstance().muteShutter(false);
            CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
            return;
        }
        CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;

        CameraController.sendMessage(MSG_NEXT_FRAME);

        String modeID = PluginManager.getInstance().getActiveModeID();
        if (modeID.equals("hdrmode") || modeID.equals("expobracketing"))
        {
            //if preview not working
            if (previewMode==false)
                return;
            previewWorking = false;

            cdt = new CountDownTimer(5000, 5000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    if (!previewWorking)
                    {
                        Log.d(TAG, "previewMode DISABLED!");
                        previewMode=false;
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CameraScreenActivity.getMainContext());
                        Editor prefsEditor = prefs.edit();
                        prefsEditor.putBoolean(CameraScreenActivity.sExpoPreviewModePref, false);
                        prefsEditor.commit();
                        evLatency=0;
                        CameraController.sendMessage(MSG_TAKE_IMAGE);
                    }
                }
            };
            cdt.start();
        }
    }

    @Override
    public void onAutoFocus(boolean focused, Camera paramCamera)
    {
        pluginManager.onAutoFocus(focused);
        if (focused)
            CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
        else
            CameraController.setFocusState(CameraController.FOCUS_STATE_FAIL);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        pluginManager.onPreviewFrame(data);
        CameraProvider.getInstance().getCamera().addCallbackBuffer(pviewBuffer);

        if (takePreviewFrame)
        {
            takePreviewFrame = false;
            if (CameraController.takeYUVFrame)
            {
                int frame = 0;
                int dataLenght = data.length;
                if (resultInHeap)
                {
                    frame = SwapHeap.SwapToHeap(data);
                    data = null;
                }

                pluginManager.addToSharedMemExifTags(null);
                pluginManager.onImageTaken(frame, data, dataLenght, CameraController.YUV);
            }
            CameraController.sendMessage(MSG_NEXT_FRAME);
            return;
        }

        String modeID = PluginManager.getInstance().getActiveModeID();
        if ((modeID.equals("hdrmode") || modeID.equals("expobracketing")) && evLatency > 0)
        {
            Log.d(TAG, "evLatency = " + evLatency);
            previewWorking = true;

            if (--evLatency == 0)
            {
                if (cdt != null)
                {
                    cdt.cancel();
                    cdt = null;
                }
                CameraController.sendMessage(MSG_TAKE_IMAGE);
            }
            return;
        }
    }


    public static void setCameraISO(int mode)
    {
        if (CameraProvider.getInstance().getCamera() != null)
        {
            //ISO isn't documented by google, so quite all devices has different parameter names and values for ISO
            //There we iterate all known options for ISO
            Camera.Parameters params = CameraProvider.getInstance().getCamera().getParameters();
            if (params != null)
            {
                String iso = isUseISO2Keys ? CameraController.mode_iso2.get(mode) : CameraController.mode_iso
                        .get(mode);
                if (params.get(CameraParameters.isoParam) != null)
                    params.set(CameraParameters.isoParam, iso);
                else if (params.get(CameraParameters.isoParam2) != null)
                    params.set(CameraParameters.isoParam2, iso);
                else if (params.get(CameraParameters.isoParam3) != null)
                    params.set(CameraParameters.isoParam3, iso);
                else
                    params.set(CameraParameters.isoParam, iso);

                Log.d("ISO SET 1", CameraParameters.isoParam);
                Log.d("ISO SET 2", CameraParameters.isoParam2);
                Log.d("ISO SET 3", CameraParameters.isoParam3);

                if (!setCameraParameters(params))
                {
                    iso = isUseISO2Keys ? CameraController.mode_iso.get(mode) : CameraController.mode_iso2
                            .get(mode);
                    if (params.get(CameraParameters.isoParam) != null)
                        params.set(CameraParameters.isoParam, iso);
                    else if (params.get(CameraParameters.isoParam2) != null)
                        params.set(CameraParameters.isoParam2, iso);
                    else if (params.get(CameraParameters.isoParam3) != null)
                        params.set(CameraParameters.isoParam3, iso);
                    else
                        params.set(CameraParameters.isoParam, iso);

                    setCameraParameters(params);
                }
            }
        }

    }

    // ^^^^^^^^^^^^^^^^^^^^^ Image data manipulation ^^^^^^^^^^^^^^^^^^^^^^^^^^^

    public static class Size
    {
        private int	mWidth;
        private int	mHeight;

        public Size(int w, int h)
        {
            mWidth = w;
            mHeight = h;
        }

        public int getWidth()
        {
            return mWidth;
        }

        public int getHeight()
        {
            return mHeight;
        }

        public void setWidth(int width)
        {
            mWidth = width;
        }

        public void setHeight(int height)
        {
            mHeight = height;
        }
    }

    @Override
    public void onShutter()
    {
        // Not used
    }

    // set exposure based on onpreviewframe
    private static int				evLatency;
    private static boolean			previewMode			= true;
    private static boolean			previewWorking		= false;
    private static CountDownTimer	cdt					= null;
    private static long				lastCaptureStarted	= 0;

    public static final int	MSG_SET_EXPOSURE	= 01;
    public static final int	MSG_NEXT_FRAME		= 02;
    public static final int	MSG_TAKE_IMAGE		= 03;

    public static void sendMessage(int what)
    {
        Message message = new Message();
        message.what = what;
        messageHandler.sendMessage(message);
    }

    // Handle messages only for old camera interface logic
    @Override
    public boolean handleMessage(Message msg)
    {

        switch (msg.what)
        {
            case MSG_SET_EXPOSURE:
                try
                {
                    // Note: LumaAdaptation is obsolete and unlikely to be relevant for Android >= 4.0
                    // if (UseLumaAdaptation && LumaAdaptationAvailable)
                    // CameraController.setLumaAdaptation(evValues[frame_num]);
                    // else
                    if (evValues != null && evValues.length > frame_num)
                        CameraController.setCameraExposureCompensation(evValues[frame_num]);
                } catch (RuntimeException e)
                {
                    Log.e(TAG, "setExpo fail in MSG_SET_EXPOSURE");
                }

                String modeID = PluginManager.getInstance().getActiveModeID();
                if ((modeID.equals("hdrmode") || modeID.equals("expobracketing")) && previewMode)
                {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
                    //if true - evLatency will be doubled.
                    boolean isSlow = prefs.getBoolean("PreferenceExpoSlow", false);
                    evLatency = 10*(isSlow?2:1);// the minimum value at which Galaxy Nexus is
                    // changing exposure in a stable way

                } else
                {
                    new CountDownTimer(500, 500)
                    {
                        public void onTick(long millisUntilFinished)
                        {
                        }

                        public void onFinish()
                        {
                            CameraController.sendMessage(MSG_TAKE_IMAGE);
                        }
                    }.start();
                }

                return true;

            case MSG_NEXT_FRAME:
                Log.d(TAG, "MSG_NEXT_FRAME");
                String modeID2 = PluginManager.getInstance().getActiveModeID();
                if (++frame_num < total_frames)
                {
                    if (pauseBetweenShots == null || Array.getLength(pauseBetweenShots) < frame_num)
                    {
                        if (evValues != null && evValues.length >= total_frames)
                            CameraController.sendMessage(MSG_SET_EXPOSURE);
                        else
                            CameraController.sendMessage(MSG_TAKE_IMAGE);
                    } else
                    {
                        pauseHandler.postDelayed(new Runnable()
                                                 {
                                                     public void run()
                                                     {
                                                         if (evValues != null && evValues.length >= total_frames)
                                                             CameraController.sendMessage(MSG_SET_EXPOSURE);
                                                         else
                                                             CameraController.sendMessage(MSG_TAKE_IMAGE);
                                                     }
                                                 },
                                pauseBetweenShots[frame_num] - (SystemClock.uptimeMillis() - lastCaptureStarted));
                    }
                }
                else if (modeID2.equals("hdrmode") || modeID2.equals("expobracketing"))
                {
                    previewWorking = true;
                    if (cdt!=null)
                    {
                        cdt.cancel();
                        cdt = null;
                    }
                }
                break;
            case MSG_TAKE_IMAGE:
                synchronized (SYNC_OBJECT)
                {
                    int imageWidth = imageSize.getWidth();
                    int imageHeight = imageSize.getHeight();
                    int previewWidth = CameraScreenActivity.getPreviewWidth();
                    int previewHeight = CameraScreenActivity.getPreviewHeight();

                    // play tick sound
                    CameraScreenActivity.getGUIManager().showCaptureIndication();
                    CameraScreenActivity.getInstance().playShutter();

                    lastCaptureStarted = SystemClock.uptimeMillis();
                    if (imageWidth == previewWidth && imageHeight == previewHeight &&
                            ((frameFormat == CameraController.YUV) || (frameFormat == CameraController.YUV_RAW)))
                        takePreviewFrame = true; // Temporary make capture by
                        // preview frames only for YUV
                        // requests to avoid slow YUV to
                        // JPEG conversion
                    else if (CameraProvider.getInstance().getCamera() != null && CameraController.getFocusState() != CameraController.FOCUS_STATE_FOCUSING)
                    {
                        try
                        {
                            mCaptureState = CameraController.CAPTURE_STATE_CAPTURING;
                            CameraProvider.getInstance().getCamera().setPreviewCallback(null);
                            CameraProvider.getInstance().getCamera().takePicture(null, null, null, CameraController.getInstance());
                        }
                        catch(Exception exp)
                        {
                            previewWorking = true;
                            if (cdt!=null)
                            {
                                cdt.cancel();
                                cdt = null;
                            }

                            Log.e(TAG, "takePicture exception. Message: " + exp.getMessage());
                            exp.printStackTrace();

                        }

                    }
                }
                break;
            default:
                break;
        }

        return true;
    }
}
