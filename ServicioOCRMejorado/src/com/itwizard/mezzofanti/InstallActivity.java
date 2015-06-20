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

import com.uah.servicioocr.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

/**
 * 
 * wrapper activity for the AssetsManager 
 *
 */
public class InstallActivity extends Activity 
{
	private static final String TAG = "MLOG: InstallActivity.java: ";
	
	private ProgressBar m_LocalProgressBar = null;			// parent progress bar, to indicate the install status
	private static Handler m_ParentMessageHandler = null;	// parent message handler, for communication
	private AssetsManager m_AssetsManager = null;			// local AssetsManager, to install the language assets from the package

	/**
	 * set the parent message handler, for later communication
	 * @param mh parent message handler
	 */
	public static void SetParentMessageHandler(Handler mh)
	{
		m_ParentMessageHandler = mh;
	}
	
	/**
	 * the onCreate method: allocate all variables
	 */
	public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        try
        {
	        Log.v(TAG, "Starting app ---------------------------");
	        
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
	                WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        
	        // never enter standby when camera open
	        Window window = getWindow();
	        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		    
            Log.v(TAG, "welcome");
            
            m_AssetsManager = new AssetsManager(m_LocalMessageHandler, m_LocalProgressBar);
            m_AssetsManager.InstallLanguageAssetsJob();
        }
        catch (Exception ex)
        {
        	Log.e(TAG, "mexeption: " + ex.toString());
        }
        catch (Throwable t)
        {
        	Log.e(TAG, "mexpcetion: " + t.toString());
        }
    }
	
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) 
	{		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if ( keyCode == KeyEvent.KEYCODE_BACK )
        { 	// if "BACK-KEY" pressed
            if (event.getRepeatCount() == 0)
            {
            	if (AssetsManager.IsInstalled() == false)
            	{
            		// installation stopped by user, before it ended: we do not have valid langs, so we shut down
            		SendKillToParent();
            	}
            }  
        }
        
        return super.onKeyDown(keyCode, event);
    }
	
	/**
	 * the installation finished, thus we hide the status bar and diplay the "start-app" button
	 */
	private void InstallFinishedStartApp()
	{
		m_LocalProgressBar.setVisibility(View.GONE);
	}

	/**
	 * used by the AssetsManager for communication
	 */
	private Handler m_LocalMessageHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what)
			{
				// ---------------------------------------------------------------
				case R.id.assetsmanager_installingdone:
				{
	            	Log.v(TAG, "Installing done.");
					Bundle bdl = msg.getData();
					boolean bModifOnDisk = bdl.getBoolean("modifondisk");
					if (bModifOnDisk)
						OCR.mConfig.LoadFabricDefaults();
					
	    			InstallFinishedStartApp();
					break;
				}	
					// ---------------------------------------------------------------				
				case R.id.assetsmanager_installingcorrupted:
				{
					Bundle bdl = msg.getData();
					long lNeededSpace = bdl.getLong("needed_space");
					long lFreeSpace = bdl.getLong("free_space");
					DisplayProblemsInstallingAlert(lNeededSpace, lFreeSpace);
		    	    break;
				}
				default:
					break;
					
			}
			
			super.handleMessage(msg);
		}
	};
	
	private void DisplayProblemsInstallingAlert(long lNeededSpace, long lFreeSpace)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setIcon(R.drawable.alert32);
	    builder.setTitle(R.string.assetsmanager_title);
	    builder.setMessage(getString(R.string.assetsmanager_msg1) + (lNeededSpace/(1024*1024)) + "[MB] " 
	    		+ getString(R.string.assetsmanager_msg2)+ (lFreeSpace/(1024*1024)) + "[MB]");
	    builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
	    {
			//@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				SendKillToParent();
				finish();
			}
	    	
	    });
	    builder.setCancelable(false);
	    builder.show();
	    
	}

	
	private void SendKillToParent()
	{
		Log.v(TAG, "Send kill to parent");
		m_ParentMessageHandler.sendEmptyMessageDelayed(R.id.installactivity_killApp, 500);
	}

}
