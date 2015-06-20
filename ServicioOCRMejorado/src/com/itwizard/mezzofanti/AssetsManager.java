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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.uah.servicioocr.R;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.widget.ProgressBar;


/**
 *  	This object is responsible for installing all language assets from the package.
 * 		The OCR needs at least one valid language to be installed in order to work, 
 * 		the AssetsManager will check if there is any installed language, and if not will install 
 * 		the default language from the package, by unziping it.
 */
public class AssetsManager implements Runnable 
{
	private static final String TAG = "MLOG: AssetsManager.java: ";
	
	private static final String m_sZipFilename = Mezzofanti.PACKAGE_DATA;	// the zip name, holds the language assets	

	private Handler m_ParentMessageHandler = null;							// message handler of the parent  
	private ProgressBar m_ParentProgressBar = null;							// a progress bar from the parent 
	
	private long m_lCurrentInstallSz;										// bytes Installed
	private long m_lMaxInstallSz;											// total Install size in bytes
	private final int PROGRESS_STEP = 100;									// when to give feedback to the parent progress dialog
    private int kont = 0;													// internal variable for progress
    private boolean m_bModificationsOnDisk = false;
	
    /**
     * constructor
     */
	public AssetsManager(Handler mh, ProgressBar pb)
	{
		m_ParentMessageHandler = mh;
		m_ParentProgressBar = pb;
	}
	
	/**
	 * @return if the language assets are installed or not 
	 */
    @SuppressWarnings("rawtypes")
	public static boolean IsInstalled()
    {
 	    ZipFile zipFile;
    	Log.v(TAG, "isAssetsInstalled():");

    	// check directories
		if ( !DirExists(Mezzofanti.DATA_PATH) || 
			 !DirExists(Mezzofanti.RESULTS_PATH) || 
			 !DirExists(Mezzofanti.TEMP_PATH) )
			return false;

    	
    	// check files
	    try 
	    {
	      zipFile = new ZipFile(m_sZipFilename);

	      Enumeration entries = zipFile.entries();
	      while (entries.hasMoreElements()) 
	      {
	        ZipEntry entry = (ZipEntry)entries.nextElement();

	        String filename = entry.getName();
	        if (filename.contains("assets/") && !filename.contains(".user-words"))
	        {
				File f = new File(Mezzofanti.DATA_PATH + filename.replace("assets/", ""));			
	        	Log.v(TAG, "\tcheck>" + filename + " zifsz=" + entry.getSize() + " disksz=" + f.length());
				
				if (entry.getSize() != f.length())
				{
					Log.v(TAG, "isAssetsInstalled(): asset " + filename + " has different size on disk=" + f.length() + " in zip=" + entry.getSize());
					return false;
				}
	        }
	      }

	      zipFile.close();
	    } 
	    catch (IOException ioe) 
	    {
	      Log.v(TAG, "exception isAssetsInstalled():" + ioe.toString());
	      return false;
	    }
	    
    	Log.v(TAG, "isAssetsInstalled(): Assets are already correctly installed");
    	return true;
    }
    
    /**
     * @return the free space on sdcard in bytes
     */
    public static long GetFreeSpaceB()
    {
        try 
        {
            String storageDirectory = Environment.getExternalStorageDirectory().toString();
            StatFs stat = new StatFs(storageDirectory);
            return (long) stat.getAvailableBlocks() * stat.getBlockSize();
        } 
	    catch (Exception ex) 
	    {
            return -1;
        }
    }
    
    
    /**
     * Start a separate thread to install the asset language
     */
    public void InstallLanguageAssetsJob()
	{
    	// check space on disk
    	long lFreeSpace = GetFreeSpaceB();
    	long lNeededSpace = GetTotalUnzippedSize();
    	if (GetFreeSpaceB() < GetTotalUnzippedSize())
    	{
    		Log.v(TAG, "Warning: not enough space for installation - needed space="+lNeededSpace+ " freespace="+lFreeSpace);
    		Message msg = new Message();
    		msg.setTarget(m_ParentMessageHandler);
    		Bundle bdl = new Bundle();
    		bdl.putLong("needed_space", lNeededSpace);
    		bdl.putLong("free_space", lFreeSpace);
    		msg.setData(bdl);
    		msg.what = R.id.assetsmanager_installingcorrupted;
    		msg.sendToTarget();
    		return;
    	}
    	
    	// create the directory structure
		MKDIR(Mezzofanti.DATA_PATH);
		MKDIR(Mezzofanti.RESULTS_PATH);
		MKDIR(Mezzofanti.TEMP_PATH);
	
		Thread thLanguageInstaller = new Thread(this);
		thLanguageInstaller.start();
	}
    
    /**
     * the thread body
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
		// copy the language assets
    	m_ParentProgressBar.setProgress(0);
    	m_lCurrentInstallSz = 0;
    	m_lMaxInstallSz = GetTotalUnzippedSize();
    	
		String ret = UnzipLanguageAssets();
		
		Log.v(TAG, "InstallLanguageAssets : "+ret);    	
		
		Message msg = new Message();
		msg.setTarget(m_ParentMessageHandler);
		Bundle bdl = new Bundle();
		bdl.putBoolean("modifondisk", m_bModificationsOnDisk);
		msg.setData(bdl);
		msg.what = R.id.assetsmanager_installingdone;
		msg.sendToTarget();
    }
    
    /**
     * @return the install status in procentages [0-100] %
     */
	public int GetInstallStatus()
	{
		long proc_down = 0; // [0-100]
		if (m_lMaxInstallSz != 0)
			proc_down = m_lCurrentInstallSz * 100/m_lMaxInstallSz;
		Log.v(TAG, "GetInstallStatus(): Installed "+m_lCurrentInstallSz +"B ["+proc_down+"%]");
		
		return (int) proc_down;
	}

	/**
	 * @return the total size of the language-assets in the zip file
	 */
    @SuppressWarnings("rawtypes")
	private long GetTotalUnzippedSize()
    {
        ZipFile zipFile;
        long ret = 0;
        
	    try 
	    {
	      zipFile = new ZipFile(m_sZipFilename);
	
	      Enumeration entries = zipFile.entries();
	      while (entries.hasMoreElements()) 
	      {
	    	  ZipEntry entry = (ZipEntry)entries.nextElement();
	          if (entry.getName().contains("assets/"))
	        		ret += entry.getSize();
	      }
	
	      zipFile.close();
	    } 
	    catch (IOException ioe) 
	    {
	      Log.v(TAG, "exception:" + ioe.toString());
	      return 0;
	    }
	    return ret;
    }
    /**
     * Check if directory exists
     * @param dirname
     * @return true/false
     */
    private static boolean DirExists(String dirname)
    {
	       File sddir = new File(dirname);
	       return sddir.exists();
    }
    
    /**
     * create a directory
     */
	private void MKDIR(String dirname)
	{
	       File sddir = new File(dirname);
	       if (sddir.exists())
	       {
	    	   Log.v(TAG, "directory ["+dirname+"] already exists");
	       }
	       else
	       {
	    	  Log.v(TAG, "creating directory ["+dirname+"]");
	          if (!sddir.mkdirs()) { 
	               Log.v(TAG, "error: create dir " + dirname + "on sdcard failed"); 
	               return; 
	          }
	       }		
	}
	

	
	/**
	 * copy from the zip on the disk 
	 */
	private final String CopyInputStream(InputStream in, long in_size, String outname)
		  throws IOException
    {
		    byte[] buffer = new byte[1024];
		    int len;
		    DataOutputStream out = null;
		    
		    try
		    {
				File f = new File(Mezzofanti.DATA_PATH + outname);			
				long fsz = f.length();
				
				if (f!=null && f.isFile() && fsz == in_size)
				{
					Log.v(TAG, "Asset " + outname + " already installed disksz="+fsz +" assetsz="+in_size);
					m_lCurrentInstallSz += fsz;
					return outname + " ALREADY installed ";
				}
				else
				{
					Log.v(TAG, "Asset " + outname + " NOT installed disksz="+fsz +" assetsz="+in_size);
				}
			    
				out = new DataOutputStream(new FileOutputStream(Mezzofanti.DATA_PATH + outname));
			    
			    while((len = in.read(buffer)) >= 0)
			    {
					 kont++;
					 if (kont >= PROGRESS_STEP)
					 {
						 m_ParentProgressBar.setProgress(GetInstallStatus());
						 kont = 0;
					 }
			         out.write(buffer, 0, len);
			         m_bModificationsOnDisk = true;
			         m_lCurrentInstallSz += len;
			    }
	
			    in.close();
			    out.close();
		    }
		    catch (Exception ex)
		    {
		    	Log.v(TAG, "Exception: " + ex.toString());
		    }
		    
			if (out != null)
				Log.v(TAG, "Asset " + outname + " INSTALLED fout=" + out.size());
			return outname + " INSTALLED ";
    }

	
    
    /**
     * unzips all language-assets from the package
     */
	@SuppressWarnings("rawtypes")
	private String UnzipLanguageAssets()
	{
		    ZipFile zipFile;
		    String ret = "";

		    try 
		    {
		      zipFile = new ZipFile(m_sZipFilename);

		      Enumeration entries = zipFile.entries();
		      while (entries.hasMoreElements()) 
		      {
		        ZipEntry entry = (ZipEntry)entries.nextElement();

		        String filename = entry.getName();
		        if (filename.contains("assets/"))
		        {
		        	Log.v(TAG, "Extracting asset file: " + filename);
		        	
		        	ret += CopyInputStream(zipFile.getInputStream(entry), entry.getSize(), (entry.getName().replace("assets/", "")));
		        	m_ParentProgressBar.setProgress(GetInstallStatus());
		        }
		      }

		      zipFile.close();
		    } 
		    catch (IOException ioe) 
		    {
		      Log.v(TAG, "exception:" + ioe.toString());
		      return (ret + ioe.toString());
		    }
		    
		    return ret;
	}	
}
