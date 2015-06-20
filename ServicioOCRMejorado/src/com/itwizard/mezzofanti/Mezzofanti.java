/*
 * Copyright (C) 2011 José Manuel Cernuda
 * http://androidelibre.wordpress.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.itwizard.mezzofanti;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import com.uah.servicioocr.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.view.SurfaceHolder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Window; 
import android.view.WindowManager; 
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.SurfaceView; 
import android.view.View.OnClickListener;
import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;

public class Mezzofanti extends Activity implements SurfaceHolder.Callback, View.OnClickListener, Runnable 
{
	/*
	 * ----------------------------------------------------------------------------------------
	 * the CONSTANTS
	 * ----------------------------------------------------------------------------------------
	 */	
	// language files that are stored in "assets"	
	public static final String svLangExtNames[] = { ".DangAmbigs", ".freq-dawg", ".fst", ".inttemp", ".ngram_triv", 
		".normproto", ".pffmtable", ".unicharset", ".user-words", ".word-dawg"};	

	// global variables used throughout the code
	public static final String PACKAGE_NAME = "com.itwizard.mezzofanti";
	public static final String DATA_PATH = "/sdcard/tessdata/";
	public static final String RESULTS_PATH = DATA_PATH + "out/";
	public static final String TEMP_PATH = DATA_PATH + "temp/";
	public static final String PACKAGE_DATA = "/data/app/" + PACKAGE_NAME + ".apk";	
	public static final String DOWNLOAD_URL = "http://www.itwizard.ro/mezzolang/";
	public static final String CONFIG_FILE = "config.txt";

	// public variables
	public static boolean m_bSkipIntroAtStartup = true;	// skip the introduction at startup (loaded from the xml prefs-file)

	private boolean m_bHorizontalDisplay = true; 		// true=horizontal / false=vertical (this is updated all the time)	
	private Bitmap m_bmOCRBitmap; 						// bitmap we get the image from camera
	private boolean m_bHorizDispAtPicTaken = true;		// this is updated just before the picture taken, it will be sent as a param to the OCR processing
	private boolean m_bScreenRequestPicture = false;


	/* ----------------------------------------------------------------------------------------
	 * local variables
	 * ----------------------------------------------------------------------------------------
	 */	
	private static final String TAG = "MLOG: Mezzofonti.java: ";

	private boolean m_bIntroWasDisplayedAtStartup = false;	// only true when the app first starts (false at resume)	
	private boolean m_bHasSurface = false; 					// used to init the camera
	private boolean m_bSdcardMounted = true;			// checks if the sdcard is mounted or not
	private boolean m_bPreviewReady = false;    			// is the preview ready
	private boolean m_bFocusButtonPressed = false;			// focus button is pressed

	private ProgressDialog m_pdOCRInProgress;				// display this progress dialog while OCR runs
	private OrientationEventListener m_OEListener = null;	// used to get vertical/horizontal camera position
	private OnScreenHint m_oshStorageHint;					// a persistent-toast(message) displayed on the screen 
	private CaptureLayout m_clCapture;						// Capture-mode	

	private boolean m_bDidRegister = false;					// was the broadcast receiver registered or not
	private boolean m_bOCRInProgress = false;				// if OCR is processing or not

	private boolean m_bLineMode = true;					// capture-mode: line/all
	private String m_sOCRResultLineMode = "";				// the ocr string in line mode


	// 'Menu' items
	private Menu m_sPreferencesMenu = null;							
	private static final int PREFERENCES_GROUP_ID = 0;
	private static final int SETTINGS_ID = Menu.FIRST;
	private static final int HELP_ID = Menu.FIRST + 1;
	private static final int FEEDBACK_ID = Menu.FIRST + 2;
	private static final int ABOUT_ID = Menu.FIRST + 3;

	// the buttons on small-capture mode
	private CustomImageButton m_btSwitch;
	private CustomImageButton m_btDelOne;
	private CustomImageButton m_btGotoResults;

	private String rutaImagenTransformar = "";
	private String nombreImagen = "";
	private static final int NOTIF_ALERTA_ID = 1;
	private int contador = 0;

	/*
	 * ----------------------------------------------------------------------------------------
	 * Overrides
	 * ----------------------------------------------------------------------------------------
	 */
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		try
		{
			Log.v(TAG, "Starting app");
			/** Tratamiento de la ruta elegida */
			Bundle recibido = getIntent().getExtras();
			if(recibido !=null)
	        {
	        	rutaImagenTransformar = recibido.getString("Ruta");
	        	int posicionIni = rutaImagenTransformar.lastIndexOf("/");
	        	int posicionFin = rutaImagenTransformar.lastIndexOf(".");
	        	nombreImagen = rutaImagenTransformar.substring(posicionIni + 1, posicionFin);
	        }

			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);

			// never enter standby when camera open
			Window window = getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			CameraManager.Initialize(getApplication());	        

			// set the layout to the xml definition
			setContentView(R.layout.main);
			CustomImageButton bt = (CustomImageButton)findViewById(R.id.mezzofanti_button_camerabutton);
			bt.SetImage(R.drawable.camera_64, 0, 0);
			bt.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					Log.v(TAG, "CameraButton OnClick");
					m_bScreenRequestPicture  = true;
					RequestCameraFocus();
				}
			});
			
			m_clCapture = (CaptureLayout)findViewById(R.id.mezzofanti_capturelayout_view);			

			
			m_bSdcardMounted = CheckSDCardState();

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			OCR.mConfig.GetSettings(prefs);
			
			if (m_bLineMode)
				CameraManager.SetImgDivisor(2);
			else
				CameraManager.SetImgDivisor(OCR.mConfig.GetImgDivisor());

			m_bHasSurface = false;

			// register to the orientation events
			CreateOrientationListener(this);
			OrientationEnable(true);

			RefreshStatusBar("");

			m_bSkipIntroAtStartup = prefs.getBoolean(PreferencesActivity.KEY_SKIP_INTRO_AT_STARTUP, true);
			Log.v(TAG, "skipintro=" + m_bSkipIntroAtStartup + " introwasdisplayed=" + m_bIntroWasDisplayedAtStartup);
			StartInstallActivity(); // note: this should be called only after OCR.mConfig.GetSettings(prefs);

			/**AÑADIDO*/
			Bitmap mBitmap = null;
			try
			{
				mBitmap = BitmapFactory.decodeStream(new FileInputStream((String)rutaImagenTransformar));
				System.gc();
			}
			catch (Throwable th) 
			{
				m_bOCRInProgress = false;
				m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_restartCaptureMode);
			}		
			OCR.Initialize();	
			OCR.get().SetLanguage(OCR.mConfig.GetLanguage());

			DoOCRJob(mBitmap);
			/**FIN AÑADIDO*/
			
			CreateStatusbarButtonsCaptureMode();
		}
		catch (Exception ex)
		{
			Log.e(TAG, "exception: onCreate():" + ex.toString());
		}
		catch (Throwable t)
		{
			Log.e(TAG, "exception: onCreate():" + t.toString());
		}
	}



	@Override
	protected void onPostResume() 
	{
		Log.v(TAG, "onPostResume()");
		super.onPostResume();
	}


	@Override  
	public void onDestroy() 
	{
		StopCamera();
		OrientationEnable(false);
		
		OCR ocr = OCR.get();
		if (ocr != null)
			ocr.Destructor();
 	
		super.onDestroy();
		Log.d(TAG, "onDestroy() ENDING.");          
	}     


	@Override
	protected void onResume() 
	{
		super.onResume();
		Log.v(TAG, "onResume() ----------------------------");
		Log.v(TAG, "mconfig lang=" + OCR.mConfig.GetLanguageMore());

		// remove ocr results
		m_bmOCRBitmap = null;
		System.gc();


		RefreshStatusBar("");
		m_bSdcardMounted = CheckSDCardState();      

		// install an intent filter to receive SD card related events.
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addDataScheme("file");
		registerReceiver(m_brSDcardEvent, intentFilter);
		
		m_bDidRegister = true;
		Log.v(TAG, "onResume() filter to receive SD card related events----------------------------"); 

		
		// read the new settings, maybe the user changed something in results mode 
		// so we store all the settings data in the local variables
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		OCR.mConfig.GetSettings(prefs);
		m_bSkipIntroAtStartup = prefs.getBoolean(PreferencesActivity.KEY_SKIP_INTRO_AT_STARTUP, true);
		
		if (m_bSdcardMounted && AssetsManager.IsInstalled())
		{
			OCR.Initialize();
			OCR.get().SetLanguage(OCR.mConfig.GetLanguage());
		}
		
		if (m_bLineMode)
			CameraManager.SetImgDivisor(2);
		else
			CameraManager.SetImgDivisor(OCR.mConfig.GetImgDivisor());

		m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_startCamera);


		// returned in Capture-mode     	  
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.mezzofanti_preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (m_bHasSurface) 
		{
			// The activity was paused but not stopped, so the surface still exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			InitCamera(surfaceHolder);
		} else 
		{
			// Install the callback and wait for surfaceCreated() to init the camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	}


	@Override
	protected void onPause() 
	{
		// unregister the sdcard broadcast receiver
		if (m_bDidRegister)
		{
			unregisterReceiver(m_brSDcardEvent);
			m_bDidRegister = false;
		}
		
		UpdateStorageHint(false);
		StopCamera();
		
		super.onPause();
	} 


	//@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
	{
	}


	//@Override
	public void surfaceCreated(SurfaceHolder holder) 
	{
		Log.v(TAG, "surfaceCreated.");
		if (!m_bHasSurface) 
		{
			// set the surface holder for the camera's use
			m_bHasSurface = true;
			SetCameraSurfaceHolder(holder);
			m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_startCamera);
			Log.v(TAG, "Call startCamera.");
		} 		
	}

	//@Override
	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		m_bHasSurface = false;		
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		// do nothing when keyboard open
		super.onConfigurationChanged(newConfig);
	}     



	/*
	 * ----------------------------------------------------------------------------------------
	 * The preferences menu
	 * ----------------------------------------------------------------------------------------
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);

		m_sPreferencesMenu = menu;

		menu.add(PREFERENCES_GROUP_ID, SETTINGS_ID, 0, R.string.menu_settings)
		.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(PREFERENCES_GROUP_ID, HELP_ID, 0, R.string.menu_help)
		.setIcon(android.R.drawable.ic_menu_help);
		menu.add(PREFERENCES_GROUP_ID, FEEDBACK_ID, 0, R.string.menu_feedback)
		.setIcon(android.R.drawable.ic_menu_send);
		menu.add(PREFERENCES_GROUP_ID, ABOUT_ID, 0, R.string.menu_about)
		.setIcon(android.R.drawable.ic_menu_info_details);

		if (m_bSdcardMounted == false)
			menu.setGroupVisible(PREFERENCES_GROUP_ID, false);

		return true;
	} 

	/**
	 * Show/Hide the Options menu
	 * @param val true=show, false=hide
	 */
	private void ShowOptionsMenu(boolean val)
	{
		if (m_sPreferencesMenu == null)
			return;
		m_sPreferencesMenu.setGroupVisible(PREFERENCES_GROUP_ID, val);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) {
		case SETTINGS_ID: {
			OCR.ReadAvailableLanguages();
			StopCamera();
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClassName(this, PreferencesActivity.class.getName());
			startActivity(intent);

			break;
		}


		case HELP_ID: 
			AlertDialog.Builder builderH = new AlertDialog.Builder(this);
			builderH.setIcon(R.drawable.wizard_48);
			builderH.setTitle(getString(R.string.preferences_helpTitle));
			builderH.setMessage(getString(R.string.preferences_msg_help_step1) + "\n" + getString(R.string.preferences_itwiz_url));
			builderH.setNegativeButton(R.string.preferences_button_cancel, null);
			builderH.show();
			break;

		case FEEDBACK_ID:
			Intent intent2 = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:office@itwizard.ro"));
			intent2.putExtra("subject", "[MezzofantiFeedback]");
			intent2.putExtra("body", "");
			startActivity(intent2);
			break;

		case ABOUT_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setIcon(R.drawable.wizard_48);
			builder.setTitle(getString(R.string.preferences_aboutTitle));
			builder.setMessage(getString(R.string.preferences_msg_about) + "\n" + getString(R.string.preferences_itwiz_url));
			builder.setPositiveButton(getString(R.string.preferences_button_open_browser), mAboutListener);
			builder.setNegativeButton(getString(R.string.preferences_button_cancel), null);
			builder.show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * User requests to connect to company website.
	 */
	private final DialogInterface.OnClickListener mAboutListener = new DialogInterface.OnClickListener() 
	{
		public void onClick(android.content.DialogInterface dialogInterface, int i) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.preferences_itwiz_url)));
			startActivity(intent);
		}
	};     


	/*
	 * ----------------------------------------------------------------------------------------
	 * Camera
	 * ----------------------------------------------------------------------------------------
	 */

	/**
	 * Set the camera surface holder.
	 */
	private void SetCameraSurfaceHolder(SurfaceHolder surfaceHolder)
	{
		if (surfaceHolder == null || CameraManager.get() == null)
			return;

		CameraManager.get().SetSurfaceHolder(surfaceHolder);
	}

	/**
	 * Initialize the camera, open the driver.
	 * @param surfaceHolder the local surface holder.
	 */
	private void InitCamera(SurfaceHolder surfaceHolder) 
	{
		if (CameraManager.get() == null)
			return;

		m_bPreviewReady = true;
		CameraManager.get().OpenDriver(surfaceHolder);
		CameraManager.get().StartPreview();		
	}

	/**
	 *  Initialize the camera, open the driver (no params), we assume the surface holder was set apriori with SetCameraSurfaceHolder.
	 */
	private void InitCamera() 
	{
		Log.v(TAG, "InitCamera: start");		
		if (CameraManager.get() == null)
			return;

		Log.v(TAG, "InitCamera: OpenDriver");

		m_bPreviewReady = true;
		if (CameraManager.get().OpenDriver())
		{
			Log.v(TAG, "InitCamera: StartPreview");
			CameraManager.get().StartPreview();
		}
		Log.v(TAG, "InitCamera: end");
	}

	/**
	 * Stop the camera preview and driver.
	 */
	private void StopCamera()
	{
		CameraManager.get().StopPreview();
		CameraManager.get().CloseDriver();
		m_bPreviewReady = false;	
	}


	/*
	 * ----------------------------------------------------------------------------------------
	 * Keyboard
	 * ----------------------------------------------------------------------------------------
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		switch (keyCode) 
		{
			case KeyEvent.KEYCODE_FOCUS:
			{
				if (event.getRepeatCount() == 0)
					return RequestCameraFocus();
				return true;
			}
			
			case KeyEvent.KEYCODE_CAMERA:
			{
				if (event.getRepeatCount() == 0)
					return RequestCameraTakePicture();
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Request the camera focus
	 * @return if function call succeeded
	 */
	private boolean RequestCameraFocus()
	{
		String ss[] = OCR.getLanguagesNative();
		if (ss.length == 0)
		{
			ShowLanguageMissingAlertDialog();
			return false;
		}
		
		
		if (!m_bSdcardMounted)
			return false;
		if (m_bOCRInProgress == true)
			return false;

		m_bFocusButtonPressed = true;
		m_clCapture.DrawFocusIcon(true, m_bHorizontalDisplay);  
		CameraManager.get().RequestCameraFocus(m_MezzofantiMessageHandler);
		CameraManager.get().RequestAutoFocus();
		return true;
	}

	/**
	 * Request camera to take the picture
	 * @return if function call succeeded
	 */
	private boolean RequestCameraTakePicture()
	{
		if (!m_bSdcardMounted)
			return false;
		if (m_bOCRInProgress == true)
			return false;

		m_bOCRInProgress = true;
		if (m_bPreviewReady)
		{
			m_bHorizDispAtPicTaken = m_bHorizontalDisplay;

			CompareTime(TAG + "request a picture");
			m_clCapture.DrawFocused(false, false);
			m_clCapture.DrawFocusIcon(false, m_bHorizontalDisplay);
			CameraManager.get().RequestPicture(m_MezzofantiMessageHandler);
			CameraManager.get().GetPicture();
			if (m_bLineMode)
				m_clCapture.ShowWaiting(getString(R.string.mezzofanti_capturelayout_takingpicture));
			
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) 
	{
		switch (keyCode) {
		case KeyEvent.KEYCODE_FOCUS:
			m_bFocusButtonPressed = false;
			m_clCapture.DrawFocusIcon(false, m_bHorizontalDisplay);
			m_clCapture.DrawFocused(false, false);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onClick(View arg0) 
	{
	} 	

	/*
	 * ----------------------------------------------------------------------------------------------------
	 * OCR Thread 
	 * ---------------------------------------------------------------------------------------------------- 
	 */

	/**
	 * Show a progress bar and start the OCR thread.
	 * @param bm the bitmap to be OCR-ized.
	 */
	public void DoOCRJob(Bitmap bm)
	{    	
		m_bmOCRBitmap = bm;
		if (bm == null)
			return;

		if (!m_bLineMode)
		{
			m_pdOCRInProgress = ProgressDialog.show(this, this.getString(R.string.mezzofanti_ocr_processing_title), 
					this.getString(R.string.mezzofanti_ocr_processing_body_begin) +" "+ OCR.mConfig.GetLanguageMore() + " " + this.getString(R.string.mezzofanti_ocr_processing_body_end), 
					true, true);
			m_pdOCRInProgress.setOnCancelListener( new OnCancelListener() {
				public void onCancel(DialogInterface dialog) 
				{
					android.os.Process.killProcess(android.os.Process.myPid());
				}    		    		
			});
		}

		Thread theOCRthread = new Thread(this);
		theOCRthread.start();
	}


	/**
	 * start the OCR thread
	 */
	public void run() 
	{
		CompareTime(TAG + "STARTING ocr processing");
		contador++;
		System.out.println("Contador: "+contador);
		
		// called by the OCR thread
		int iPicWidth  = m_bmOCRBitmap.getWidth();
		int iPicHeight = m_bmOCRBitmap.getHeight();
		int[] iImage = null;
		try
		{
			iImage = new int[iPicWidth * iPicHeight];
			Log.v(TAG, "allocated img buffer: " +iPicWidth + ", "+iPicHeight);
			m_bmOCRBitmap.getPixels(iImage, 0, iPicWidth, 0, 0, iPicWidth, iPicHeight);
			Log.v(TAG, "pix1="+Integer.toHexString(iImage[0]));
		}
		catch (Exception ex)
		{
			Log.v(TAG, "exception: run():" + ex.toString());
			System.out.println("--------->exception: run():" + ex.toString());
			m_bmOCRBitmap = null;
			System.gc();			
		}


		if (iImage != null)
		{
			String m_sOCRResult = OCR.get().ImgOCRAndFilter(iImage, iPicWidth, iPicHeight, m_bHorizDispAtPicTaken, m_bLineMode);			
			Log.v(TAG, "ocr done text= [" + m_sOCRResult +"]");
			// force free the mem
			iImage = null;
			m_bmOCRBitmap = null;
			System.gc();			

			// bad results, get the (internal) image
			OCR.get().SaveMeanConfidence();

			Log.v(TAG, "starting results handler");
		}
		else
		{
			System.gc();
			m_bOCRInProgress = false;
			m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_restartCaptureMode);
			return;
		}

		m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_ocrFinished);
		Log.i(TAG, "pcjpg - finish startPreview()");

		CompareTime(TAG + "finished the ocr processing");
		
		//Vamos a notificar al usuario que se ha realizado la transformación OCR
		//Obtenemos una referencia al servicio de notificaciones
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager notManager = (NotificationManager) getSystemService(ns);
		//Configuramos la notificación
		int icono = com.uah.servicioocr.R.drawable.ocr;
		CharSequence textoEstado = "Traducción OCR finalizada con éxito";
		long hora = System.currentTimeMillis();
		 
		Notification notif =  new Notification(icono, textoEstado, hora);
		Context contexto = getApplicationContext();
		CharSequence titulo = "Traducción OCR finalizada con éxito";
		CharSequence descripcion = "Pulsa para ver el texto generado";
		 		
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.parse("file://" + "/sdcard/ServicioOCR/salida/"+nombreImagen+"_salida.txt"), "text/*");
		 
		PendingIntent contIntent = PendingIntent.getActivity(
		    contexto, 0, intent, 0);
		 
		notif.setLatestEventInfo(
		    contexto, titulo, descripcion, contIntent);
		
		//AutoCancel: cuando se pulsa la notificaión ésta desaparece
		notif.flags |= Notification.FLAG_AUTO_CANCEL;
		 
		//Añadir sonido, vibración y luces
//		notif.defaults |= Notification.DEFAULT_SOUND;
		notif.defaults |= Notification.DEFAULT_VIBRATE;
		notif.defaults |= Notification.DEFAULT_LIGHTS;

		//Enviar notificación
		notManager.notify(NOTIF_ALERTA_ID, notif);	
		finish();
	} 


	/*
	 * ----------------------------------------------------------------------------------------------------
	 * Events Handler 
	 * ---------------------------------------------------------------------------------------------------- 
	 */

	private Handler m_MezzofantiMessageHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what)
			{
			// ---------------------------------------------------------------
			case R.id.installactivity_killApp:
				Log.v(TAG, "Killed by installer.");
				android.os.Process.killProcess(android.os.Process.myPid());
				break;

				// ---------------------------------------------------------------
			case R.id.mezzofanti_restartCaptureMode:
				// we had a problem in the OCR thread, so we get back to capture mode
				// (mainly this is due to OutOfMemory exceptions)        		

				m_bmOCRBitmap = null;
				System.gc();

				if (m_bLineMode)
					m_clCapture.ShowWaiting("");    		
				else
					m_pdOCRInProgress.dismiss();

				break;

				// ---------------------------------------------------------------
			case R.id.mezzofanti_startCamera:
				InitCamera();
				if (!m_bSdcardMounted)
					UpdateStorageHint(true);
				break;

				// ---------------------------------------------------------------
			case R.id.cameramanager_requestpicture:

				Log.v(TAG, "handleMessage() R.id.decode");
				CompareTime(TAG + "in handler, just received the picture");

				// do ocr
				Bitmap mBitmap = null;
				try
				{					
					mBitmap = BitmapFactory.decodeStream(new FileInputStream((String)rutaImagenTransformar));
					
					msg.obj = null;
					System.gc();
					
					Log.v(TAG, "w="+mBitmap.getWidth() + " h="+mBitmap.getHeight());
					if (m_bLineMode) // we crop just the image of interest
						mBitmap = Bitmap.createBitmap(mBitmap, 256, 768/2-30, 512, 60, null, false);
					// otherwise, we use all image
				}
				catch (Throwable th) 
				{
					Log.v(TAG, "exception: handler-cmrequestpic: "+ th.toString());
					m_bOCRInProgress = false;
					if (m_bLineMode)
						m_clCapture.ShowWaiting("");
					else
						m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_restartCaptureMode);
					break;
				}


				CompareTime(TAG + "starting the thread");
								
				OCR.Initialize();				
				OCR.get().SetLanguage(OCR.mConfig.GetLanguage());
				
				DoOCRJob(mBitmap);

				break;

				// ---------------------------------------------------------------	
			case R.id.mezzofanti_ocrFinished:
				m_bOCRInProgress = false;
				if (!m_bLineMode)
				{
					m_pdOCRInProgress.dismiss();
					System.gc();

					StopCamera();
					StartResultsActivity();
				}
				else
				{
					Log.v(TAG, "before processing results");
					String padding = " ";
					if (m_sOCRResultLineMode.length() == 0)
						padding = "";

					String s = OCR.m_ssOCRResult.toString().replaceAll("[\r\n]+", "");
					while (s.length()>0 && s.charAt(0) == ' ')
						s = s.substring(1);
					while (s.length()>0 && s.charAt(s.length()-1) == ' ')
						s = s.substring(0, s.length()-1);

					/**Añadimos para grabar la traducción en un fichero de salida*/
					grabarTxt("/ServicioOCR/salida/",nombreImagen,s); 
					
					m_sOCRResultLineMode = m_sOCRResultLineMode.concat(padding + s);

					m_clCapture.ShowWaiting("");
					m_clCapture.SetText(m_sOCRResultLineMode);
				}
				OCR.get().OCRClean();
				break;


				// ---------------------------------------------------------------
			case R.id.cameramanager_focus_succeded:
				if (!m_bFocusButtonPressed)
					return;				
				m_clCapture.DrawFocused(true, true);
				PlaySoundOnFocus();
				
				if (m_bScreenRequestPicture)
					RequestCameraTakePicture();
				m_bScreenRequestPicture = false;
				break;

				// ---------------------------------------------------------------
			case R.id.cameramanager_focus_failed:
				if (!m_bFocusButtonPressed)
					return;				
				m_clCapture.DrawFocused(true, false);
				Vibrate();
				m_bScreenRequestPicture = false;
				break;		
				
			case R.id.doOcr:

				Log.v(TAG, "handleMessage() R.id.decode");
				CompareTime(TAG + "in handler, just received the picture");

				// do ocr
				Bitmap mBitmap2 = null;
				try
				{
					mBitmap2 = BitmapFactory.decodeStream(new FileInputStream(rutaImagenTransformar));
					
					msg.obj = null;
					System.gc();
					
					Log.v(TAG, "w="+mBitmap2.getWidth() + " h="+mBitmap2.getHeight());
					if (m_bLineMode) // we crop just the image of interest
						mBitmap = Bitmap.createBitmap(mBitmap2, 256, 768/2-30, 512, 60, null, false);
					// otherwise, we use all image
				}
				catch (Throwable th) 
				{
					Log.v(TAG, "exception: handler-cmrequestpic: "+ th.toString());
					m_bOCRInProgress = false;
					if (m_bLineMode)
						m_clCapture.ShowWaiting("");
					else
						m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_restartCaptureMode);
					break;
				}


				CompareTime(TAG + "starting the thread");
								
				OCR.Initialize();				
				OCR.get().SetLanguage(OCR.mConfig.GetLanguage());
				
				DoOCRJob(mBitmap2);

				break;

				// ---------------------------------------------------------------	
	

			default:
				break;

			}

			super.handleMessage(msg);
		}

	};


	private void ShowLanguageMissingAlertDialog()
	{
		AlertDialog.Builder builderH = new AlertDialog.Builder(this);
		builderH.setIcon(R.drawable.alert32);
		builderH.setTitle(getString(R.string.mezzofanti_nolanginstalled_title));
		builderH.setMessage(getString(R.string.mezzofanti_nolanginstalled_body));
		builderH.setPositiveButton(R.string.mezzofanti_nolanginstalled_ok, null);
		builderH.show();					
	}

	/*
	 * ----------------------------------------------------------------------------------------
	 * Starting the activities
	 * ----------------------------------------------------------------------------------------
	 */	
	/**
	 * Starts the results activity
	 */
	private void StartResultsActivity()
	{
		if (m_bSdcardMounted) 
		{
			Intent intent = new Intent(Intent.ACTION_VIEW);
			Bundle bun = new Bundle();
			// if not in line-mode, the activity will get the results directly from the OCR results
			bun.putString("LineModeText", m_sOCRResultLineMode);    		
			bun.putBoolean("bLineMode", m_bLineMode);
			intent.putExtras(bun);
			intent.setClassName(this, ResultsActivity.class.getName());
			startActivity(intent); 
		}		
	}

	/**
	 * Start the InstallActivity if possible and needed.
	 */
	private void StartInstallActivity()
	{
		if (m_bSdcardMounted && 
				( AssetsManager.IsInstalled()==false || 
						(m_bSkipIntroAtStartup==false && !m_bIntroWasDisplayedAtStartup)
				) 
		) 
		{
			// install the languages if needed, create directory structure (one time)
			Intent intent = new Intent(Intent.ACTION_VIEW);
			InstallActivity.SetParentMessageHandler(m_MezzofantiMessageHandler);
			intent.setClassName(this, InstallActivity.class.getName());
			m_bIntroWasDisplayedAtStartup = true; // only "true" at startup
		}		
	}


	/*
	 * ----------------------------------------------------------------------------------------
	 * SDCard functions
	 * ----------------------------------------------------------------------------------------
	 */	

	/**
	 * Displays the sdcard-state message.
	 * @param show true=display, false=remove
	 */
	private void UpdateStorageHint(boolean show) 
	{
		String state = Environment.getExternalStorageState();
		int resID = 0;

		if (state == Environment.MEDIA_CHECKING) 
			resID = R.string.mezzofanti_sdcard_preparing;
		else 
			resID = R.string.mezzofanti_sdcard_unmount;
		
		if (show) 
		{
			if (m_oshStorageHint == null) 
			{
				m_oshStorageHint = OnScreenHint.makeText(this, resID);
			} 
			else 
			{
				m_oshStorageHint.setText(resID);
			}
			m_oshStorageHint.show();
		} 
		else 
			if (m_oshStorageHint != null) 
			{
				m_oshStorageHint.cancel();
				m_oshStorageHint = null;
			}			
	}

	/**
	 * The sdcard event for the broadcast receiver
	 */
	private final BroadcastReceiver m_brSDcardEvent = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();

			Log.v(TAG,"SD card activity received");
			if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) 
			{
				// SD card available
				// activate mezzofanti
				m_bSdcardMounted = true;
				ShowOptionsMenu(true);
				StartInstallActivity();
				
				OCR.Initialize();				
				OCR.get().SetLanguage(OCR.mConfig.GetLanguage());
			} 
			else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
					|| action.equals(Intent.ACTION_MEDIA_CHECKING)) 
			{
				// SD card unavailable
				UpdateStorageHint(true);
				m_bSdcardMounted = false;
				ShowOptionsMenu(false);
			} 
			else 
				if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) 
				{
					UpdateStorageHint(false);
					Toast.makeText(Mezzofanti.this, R.string.mezzofanti_sdcard_wait, 5000);
				} 
				else 
					if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) 
					{
						UpdateStorageHint(false);
					}
		}

	}; 	

	/**
	 * Get the SDCard state from the Environment.
	 */
	public static boolean CheckSDCardState()
	{
		boolean ret = true;
		String sdcardState = Environment.getExternalStorageState();
		if (sdcardState.compareTo(Environment.MEDIA_MOUNTED) != 0)
		{
			Log.v(TAG, "error: sdcard not mounted! [" + sdcardState + "]"); 
			ret = false;
		}
		return ret;
	}




	/*
	 * ----------------------------------------------------------------------------------------
	 * Orientation listner
	 * ----------------------------------------------------------------------------------------
	 */	

	/**
	 * Creates the orientation listner, in order to check for vertical/horizontal picture capture. 
	 */
	private void CreateOrientationListener(Context context)
	{
		m_OEListener = new OrientationEventListener (context, SensorManager.SENSOR_DELAY_UI)    	
		{
			public void onOrientationChanged (int orientation) 
			{
				if (m_bSdcardMounted == false)
					return;

				if ((orientation >= 0 && orientation <= 10 ) || (orientation >= 350 && orientation <= 360 ))
				{
					if (m_bHorizontalDisplay)
					{
						m_bHorizontalDisplay = false;
					}

				}
				else
				{
					if (orientation >= 260 && orientation <= 280 )
					{
						if (!m_bHorizontalDisplay)
						{
							m_bHorizontalDisplay = true;
						}
					}
				}
			} // end public void onOrientationChanged (int orientation) 
		}; // end mListener = new ... 
	}

	/**
	 * Enable/disable orientation listener.
	 * @param enable true/false
	 */
	public void OrientationEnable(boolean enable) 
	{
		if (m_OEListener==null)
			return;

		if (enable)
			m_OEListener.enable();
		else
			m_OEListener.disable();
	}



	/*
	 * ----------------------------------------------------------------------------------------
	 * Aux functions
	 * ----------------------------------------------------------------------------------------
	 */	

	/**
	 * Capture-mode: Refresh the bottom status bar.
	 * @param angle the orientation angle.
	 */
	private void RefreshStatusBar(String angle)
	{
		TextView tbox = (TextView) findViewById(R.id.mezzofanti_status_text_view);
		if (!m_bLineMode)
		{
			tbox.setText("  << " +  
					getString(R.string.mezzofanti_capturemode_statustext_switchtosmall) );        	        	
		}
		else
		{
			tbox.setText("  << " +  
					getString(R.string.mezzofanti_capturemode_statustext_switchtolarge) + "\n" +
					"\t\t\t\t\t\t\t\t"+ getString(R.string.mezzofanti_capturemode_statustext_switchtolarge2) + " >>" + 
			"  ");        	        	        	
		}

	}

	/**
	 * Play a predefined sound, when camera focused.
	 */
	private void PlaySoundOnFocus()
	{
		ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, 100);
		tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
	}

	/**
	 * Vibrate mobile, when camera not-focused.
	 */
	private void Vibrate()
	{
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(200);
	}

	private static long m_lTime = 0;
	public static void CompareTime(String status)
	{
		long prevtime = m_lTime;
		Calendar now = Calendar.getInstance();
		long currenttime=now.getTimeInMillis();

		Log.v(TAG, "---------------------");
		Log.v(TAG, "Status: "+status);
		Log.v(TAG, "Compare prev="+m_lTime + " to current=" + currenttime + " diff=" + (currenttime-prevtime));
		Log.v(TAG, "---------------------");
		m_lTime = currenttime;
	}




	/**
	 * Adds the 3 buttons needed in small-capture-mode
	 */
	private void CreateStatusbarButtonsCaptureMode()
	{
		// create the status bar buttons
		m_btSwitch 		= (CustomImageButton) findViewById(R.id.mezzofanti_button_switch);
		m_btDelOne      = (CustomImageButton) findViewById(R.id.mezzofanti_button_delone);
		m_btGotoResults = (CustomImageButton) findViewById(R.id.mezzofanti_button_gotoresults);

		m_btDelOne.SetImage(R.drawable.delone_32, 0, 0);
		m_btGotoResults.SetImage(R.drawable.gotoresults_32, 0, 0);

		m_btSwitch.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View clickedView) 
			{
				Log.v(TAG, "switch button: onClick line_mode = " + m_bLineMode);
				m_bLineMode = !m_bLineMode;
				m_clCapture.SetLineMode(m_bLineMode);

				RefreshStatusBar("");

				if (m_bLineMode)
				{
					CameraManager.SetImgDivisor(2);
					CameraManager.get().SetCameraParameters();
					ShowLineModeButtons(true);
				}
				else
				{
					CameraManager.SetImgDivisor(OCR.mConfig.GetImgDivisor());
					CameraManager.get().SetCameraParameters();
					ShowLineModeButtons(false);
					m_sOCRResultLineMode = "";
					m_clCapture.SetText("");
				}
			}
		});

		m_btDelOne.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View clickedView) 
			{
				Log.v(TAG, "m_btDelOne: onClick line_mode = " + m_bLineMode);
				int iLastSpace = m_sOCRResultLineMode.lastIndexOf(' ');
				if (iLastSpace <= 0)
					m_sOCRResultLineMode = "";
				else
					m_sOCRResultLineMode = m_sOCRResultLineMode.substring(0, iLastSpace);
				m_clCapture.SetText(m_sOCRResultLineMode);			
			}
		});

		m_btGotoResults.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View clickedView) 
			{
				Log.v(TAG, "m_btGotoResults: onClick line_mode = " + m_bLineMode);
				StartResultsActivity();				
				System.gc();
			}
		});


	}

	/**
	 * Show/hide the small-capture-mode buttons
	 * @param val show/hide
	 */
	private void ShowLineModeButtons(boolean val)
	{
		if (val)
		{
			m_btDelOne.setVisibility(View.VISIBLE);
			m_btGotoResults.setVisibility(View.VISIBLE);
		}
		else
		{
			m_btDelOne.setVisibility(View.GONE);
			m_btGotoResults.setVisibility(View.GONE);
		}  

		m_clCapture.invalidate();
	}
	
	/**
	 * Graba un texto en un fichero
	 * @param nuestroArchivo
	 * @param texto
	 */
	private static void grabarTxt(String ruta,String nombre, String texto )
	{  
		 try  
		 {  
			 File ruta_sd = Environment.getExternalStorageDirectory();
			 File f = new File(ruta_sd.getAbsolutePath()+ruta, nombre+"_salida.txt");
			 
			 OutputStreamWriter fout = new OutputStreamWriter(new FileOutputStream(f));
			 
			    fout.write(texto);
			    fout.close();
		 }  
		 	catch(IOException e)  
		 {  
		 		e.printStackTrace();  
		 }
	}  



} // end class Mezzofanti

