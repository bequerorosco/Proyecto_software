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

/*
 * Class description:
 * 		responsible for camera management: driver initialization, taking pictures etc.
 */

package com.itwizard.mezzofanti;
import java.io.IOException;

import com.uah.servicioocr.R;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 */
final class CameraManager 
{
  private static final String TAG = "MLOG: CameraManager.java: ";
  
  private static byte m_cImgDivisor = 2;		// given the limited memory space, we cannot allocate memory for all the image
  												// thus we lower a bit the resolution by a factor of 2/4
  
  private static CameraManager m_CameraManager;	// the camera manager itself	
  private Camera m_Camera;						// the camera
  private final Context m_Context;				// screen context 
  private Point m_ptScreenResolution;			// the screen resolution 				
  private Rect m_FramingRect;					// the framing rectangle
  private boolean m_bInitialized;				// is the driver initialized
  private boolean m_bPreviewing;				// is camera in preview mode
  private Handler m_ParentMessageHandler;		// the parent's message handler				
  private SurfaceHolder m_ParentSurfaceHolder = null;	// the parent's surface holder
  
  /**
   * called when jpeg-image ready, just send to the parent handler the whole image to be processed   
   */
  Camera.PictureCallback m_PictureCallbackJPG = new Camera.PictureCallback() {
      public void onPictureTaken(byte[] data, Camera c) {
              	  
		Log.i(TAG, "pcjpg - started");
		Mezzofanti.CompareTime(TAG + "just started picture callback");
		if (data == null)
		{
			Log.i(TAG, "data is null");
		}
		else
		{
	        Message message = m_ParentMessageHandler.obtainMessage(R.id.cameramanager_requestpicture, data);
		    message.sendToTarget();
		    m_ParentMessageHandler = null;
			Log.i(TAG, "pcjpg - finish");
			m_Camera.startPreview();
			Mezzofanti.CompareTime(TAG + "just sent picture to handler");
		}
		
      }
  };

  /**
   * save the parent handler, in order to process the image-request 
   */
  public void RequestPicture(Handler handler) 
  {
	    if (m_Camera != null && m_bPreviewing) 
	    {
	    	m_ParentMessageHandler = handler;
	    }
  }


 
  /**
   *  called on autofocus
   */
  private Camera.AutoFocusCallback m_AutoFocusCallback = new Camera.AutoFocusCallback() 
  {
	    public void onAutoFocus(boolean success, Camera camera) 
	    {
	    	if (success)
	    	{
	    		Log.v(TAG," Focus succeded.");
	    		m_ParentMessageHandler.sendEmptyMessage(R.id.cameramanager_focus_succeded);
	    	}
	    	else
	    	{
	    		Log.v(TAG," Focus failed.");
	    		m_ParentMessageHandler.sendEmptyMessage(R.id.cameramanager_focus_failed);
	    	}
	    }
  };

 /**
  * Save the parent message handler. 
  * @param handler parent message handler
  */
  public void RequestCameraFocus(Handler handler) 
  {
	    if (m_Camera != null && m_bPreviewing) 
	      m_ParentMessageHandler = handler;
  }
   
  /**
   * Allocate the camera manager
   */
  public static void Initialize(Context context) 
  {
    if (m_CameraManager == null) 
    {
    	m_CameraManager = new CameraManager(context);
    }
  }
  
  /**
   * set the local image divisor
   */
  public static void SetImgDivisor(int imgDivisor)
  {
	  if (imgDivisor!=1 && imgDivisor!=2 && imgDivisor!=4)
		  m_cImgDivisor = (byte)2;
	  else
		  m_cImgDivisor = (byte)imgDivisor;
  }

  /**
   * Retrieve the private camera manager
   */
  public static CameraManager get() 
  {
    return m_CameraManager;
  }

  /**
   * constructor
   */
  private CameraManager(Context context) 
  {
	  m_Context = context;
	  GetScreenResolution();
	  m_Camera = null;
	  m_bInitialized = false;
	  m_bPreviewing = false;
  }

  /**
   * set the parent surface holder
   */
  public void SetSurfaceHolder(SurfaceHolder holder)
  {
	  m_ParentSurfaceHolder = holder;
  }
  
  /**
   * open the camera driver, using the saved parent-surface-holder
   * @return a boolean variable indicating if the open-driver procedure succeeded
   */
  public boolean OpenDriver() 
  {
	if (m_ParentSurfaceHolder == null)
		  return false;
	  
    if (m_Camera == null) 
    {
    	m_Camera = Camera.open();
    	try 
    	{
    		m_Camera.setPreviewDisplay(m_ParentSurfaceHolder);
		} catch (IOException e) 
		{
			Log.v(TAG, e.toString());
			return false;
		}

      if (!m_bInitialized) 
      {
        m_bInitialized = true;
        GetScreenResolution();
      }

      SetCameraParameters();
    }
    return true;
  }

  /**
   * open camera driver using a parameter surface-holder
   * @return a boolean variable indicating if the open-driver procedure succeeded
   */
  public void OpenDriver(SurfaceHolder holder) 
  {
    if (m_Camera == null) {
    	m_Camera = Camera.open();
      try {
    	  m_Camera.setPreviewDisplay(holder);
	} catch (IOException e) {
		Log.v(TAG, e.toString());
	}

      if (!m_bInitialized) {
        m_bInitialized = true;
        GetScreenResolution();
      }

      SetCameraParameters();
            
    }
  }

  /**
   * close the camera driver
   */
  public void CloseDriver() 
  {
    if (m_Camera != null) 
    {
    	m_Camera.release();
    	m_Camera = null;
    	m_ParentSurfaceHolder = null;
    }
  }

  /**
   * start camera preview mode
   */
  public void StartPreview() 
  {
    if (m_Camera != null && !m_bPreviewing) 
    {
    	m_Camera.startPreview();
    	m_bPreviewing = true;
    }
  }

  /**
   * stop camera preview mode
   */
  public void StopPreview() 
  {
    if (m_Camera != null && m_bPreviewing) 
    {
    	m_Camera.setPreviewCallback(null);
    	m_Camera.stopPreview();
    	m_bPreviewing = false;
    }
  }

  /**
   * set the camera auto-focus callback
   */
  public void RequestAutoFocus() 
  {
	    if (m_Camera != null && m_bPreviewing) 
	    {
	    	m_Camera.autoFocus(m_AutoFocusCallback);
	    }
  }  
  
  /**
   * Calculates the framing rect which the UI should draw to show the user where to place the
   * text. The actual captured image should be a bit larger than indicated because they might
   * frame the shot too tightly. This target helps with alignment as well as forces the user to hold
   * the device far enough away to ensure the image will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public Rect GetFramingRect(boolean linemode) 
  {
	int border = 10;
	if (linemode)
		m_FramingRect = new Rect(m_ptScreenResolution.x/4, m_ptScreenResolution.y/2 - 20, 
				m_ptScreenResolution.x * 3/4 , m_ptScreenResolution.y/2 + 20);
	else
		m_FramingRect = new Rect(border, border, m_ptScreenResolution.x - border, m_ptScreenResolution.y - border - 30);

	return m_FramingRect;
  }

    
  /**
   * take a picture, and set the jpg callback
   */
  public void  GetPicture()
  {
	  m_Camera.takePicture(null, null, m_PictureCallbackJPG);
  }
  
  /**
   * Sets the camera up to take preview images which are used for both preview and decoding. 
   */
  public void SetCameraParameters() 
  {
	  if (m_ptScreenResolution == null) 
		  return;
	  Camera.Parameters parameters = m_Camera.getParameters();
	  parameters.setPreviewSize(m_ptScreenResolution.x, m_ptScreenResolution.y);
	  parameters.setPictureSize(2048/m_cImgDivisor, 1536/m_cImgDivisor);
	  m_Camera.setParameters(parameters);
	  Log.v(TAG, parameters.flatten());
  }

  /**
   * @return the screen resolution
   */
  private Point GetScreenResolution() 
  {
	  if (m_ptScreenResolution == null) 
	  {
		  WindowManager manager = (WindowManager) m_Context.getSystemService(Context.WINDOW_SERVICE);
		  Display display = manager.getDefaultDisplay();
		  m_ptScreenResolution = new Point(display.getWidth(), display.getHeight());
	  }
	  return m_ptScreenResolution;
  }

}
