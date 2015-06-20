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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.uah.servicioocr.R;

import android.app.ProgressDialog;
import android.os.Handler;
import android.util.Log;

/**
 * This object wraps the download functionality. The OCR engine needs different language-files that are large in
 * size and will not be kept in the package. Any additional language that will be installed by the client will be
 * downloaded from the Internet (in a zip file), unziped in a temporary directory, and finally saved in the language directory.     
 */
public class DownloadManager implements Runnable
{
	private static final String TAG = "MLOG: Download.java: ";
	
	private int m_iLangIndex;			// the lang index to be downloaded
	private boolean m_bProcessing;		// will not allow to start another download thread if one already running
	private boolean m_bCancelDownload;	// variable used to cancel the current download
	private long m_lCurrentDownloadSz;	// bytes downloaded
	private long m_lMaxDownloadSz;		// total download size in bytes
	private Handler m_ParentMessageHandler = null;	// the parent message handler
	private ProgressDialog m_ParentProgressDialog;	// progress dialog of the parent, to display the status
	private final int PROGRESS_STEP = 100;			// the progress step
	private boolean m_bSdcardError = false;
	
	/**
	 * this class holds the info about each language stored on the server
	 */
	public class ServerLang
	{
		// ex: lang=English,eng,26
		String sFullName;	//English
		String sExtName;	//eng
		long lDownloadSz;	//26MB - ignored at this point (because we can get this via the protocol)

		/**
		 * constructor
		 * @param sLanguageInfo a line containing all language info - to be parsed in the constructor
		 */
		ServerLang(String sLanguageInfo)
		{
			String [] s = sLanguageInfo.split(",");
			if (s.length == 3)
			{
				try{
					sExtName = s[0];
					lDownloadSz = Long.parseLong(s[1]);
					sFullName = s[2].replace("[\r\n\0]+", "");
					Log.v(TAG, "created SL :"+" "+sExtName+" "+lDownloadSz+" "+sFullName);
				}
				catch (Exception ex)
				{
					Log.v(TAG, "Exception: " + ex.toString());
				}
			}
		}
	}
	
	public ServerLang [] m_ServerLanguages = null;	// vector holding the languages available on the server
	
	
	
	
	/**
	 * constructor
	 */
	DownloadManager()
	{
		m_ParentMessageHandler = null;
	}
	
	/**
	 * set the parent message handler
	 * @param mh the parent message handler
	 */
	public void SetMessageHandler(Handler mh)
	{
		m_ParentMessageHandler = mh;
	}
	
	/**
	 * set the parent progress dialog
	 * @param pd the parent progress dialog
	 */
	public void SetProgressDialog(ProgressDialog pd)
	{
		m_ParentProgressDialog = pd;
	}
	
	/** 
	 * this will return the languages available online in a string
	 * and will set the local variables accordingly
	 * @param url the server URL
	 * @param filename the brief-language filename 
	 */	
	public boolean DownloadLanguageBrief(String url, String filename)
	{
		Log.v(TAG, "DownloadLanguageBrief (" + url + filename + ")");
		String ret = "";

		// create url connector
	    URL u;
	    byte[] buffer = new byte[1024];
		try {
			u = new URL(url + filename);
		    HttpURLConnection c = (HttpURLConnection) u.openConnection();
		    c.setRequestMethod("GET");
		    c.setDoOutput(true);
		    c.connect();
		    InputStream in = c.getInputStream();
			Log.v(TAG, "DownloadFile opened connection");
		    
		    FileOutputStream f = new FileOutputStream(new File(Mezzofanti.TEMP_PATH, filename));
		
		    int len = 0;
		    int total_len = 0;
		    while ( (len = in.read(buffer, 0, 1024)) > 0 ) 
		    {
		         f.write(buffer, 0, len);
		         total_len += len;
		         if (len > 0)
		         {
		        	 String s = new String(buffer, 0, len);
		        	 ret += s;
		         }
		    }
		    
		    // create serverLanguages vector
		    if (ret.length() > 0)
		    {
		    	String [] lines = ret.split("\n");
		    	m_ServerLanguages = new ServerLang[lines.length];
		    	for (int i=0; i<lines.length; i++)
		    		m_ServerLanguages[i] = new ServerLang(lines[i]);
		    }
		    
		    Log.v(TAG, "total len=" + total_len);
		    f.close();
		    c.disconnect();
		    
		} catch (Exception e) 
		{
			Log.v(TAG, e.toString());
			return false;
		}
	    
		Log.v(TAG, "returning: " + ret);
	    return true;
	}
    
	// this will download the language, based on the index in the m_ServerLanguages vector
	/**
	 * download a specific language from the server.
	 * @param langIndex the index of the language from the m_ServerLanguages 
	 */
	public void DownloadLanguageJob(int langIndex)
	{	
		if (m_bProcessing)
		{
			// already in a download (to cancel the download call the appropriate method)
			Log.v(TAG, "DownloadLanguageJob: already processing a job, please cancel the prev job before another restart");
			return;
		}
				
		if (m_ServerLanguages == null)
			return;
		if (langIndex > m_ServerLanguages.length)
			return;

		m_bProcessing = true;
		m_bCancelDownload = false;
		m_iLangIndex = langIndex;
		
		// Note: moved these to DownloadFile
		//m_lCurrentDownloadSz = 0;	
		//m_lMaxDownloadSz = m_ServerLanguages[langIndex].lDownloadSz;
		
    	Thread theDownloadThread = new Thread(this);
    	theDownloadThread.start();
	}
    
	/**
	 * smooth cancel of the download
	 */
	public void CancelDownload()
	{
		Log.v(TAG, "Force CANCEL Download [" + GetDownloadStatus() + "%]");
		m_bCancelDownload = true;
	}

	/**
	 * @return the download status [0-100] %
	 */
	public int GetDownloadStatus()
	{
		if (m_bProcessing == false)
			return 0;
		
		long proc_down = 0; // [0-100]
		if (m_lMaxDownloadSz != 0)
			proc_down = m_lCurrentDownloadSz * 100/m_lMaxDownloadSz;
		
		return (int) proc_down;
	}
	
	
	/**
	 * the download job
	 */
	public void run() 
	{
		int downloadsz = 0;
		
		// download the file
		String file_name = m_ServerLanguages[m_iLangIndex].sExtName + ".zip";
		Log.v(TAG, "Downloading file: " + file_name);
		boolean download_ok = DownloadFile(Mezzofanti.DOWNLOAD_URL, file_name);
		int ret = R.id.downloadmanager_downloadFinishedError;
		
		//!! if zip is already downloaded in the temp dir, it should be not downloaded again
		
		m_lCurrentDownloadSz += downloadsz;
		Log.v(TAG, "Download ended: " + file_name + " [" +GetDownloadStatus()+"%]");			

		if (download_ok)
		{
			Log.v(TAG, "Download succesfully finished");
			String zipname = Mezzofanti.TEMP_PATH + file_name;
			
	        	
			m_ParentMessageHandler.sendEmptyMessage(R.id.downloadmanager_unziping);
			if (UnzipLangArchive(zipname))
			{
				Log.v(TAG, "Unzipped ok");
				if (RenameTempFiles())
				{
					Log.v(TAG, "Renamed ok");
					ret = R.id.downloadmanager_downloadFinishedOK;
				}
				else
					Log.v(TAG, "Renamed error");
			}
			else
			{
				Log.v(TAG, "Unzip returned false");				
			}
		}
		else
		{
			Log.v(TAG, "Download UNSUCCESFULLY finished");
		}
		
		m_bProcessing = false;
		if (m_bSdcardError)
			ret = R.id.downloadmanager_downloadFinishedErrorSdcard;
		m_ParentMessageHandler.sendEmptyMessage(ret);
	}	


	/**
	 * get the total unpacked size of the contents of the zip 
	 * @param zipname the zip filename
	 * @return the total unpacked size
	 */
	@SuppressWarnings("rawtypes")
	private long GetZipSize(String zipname)
	{
        ZipFile zipFile;
        long ret = 0;
        
	    try 
	    {
	      zipFile = new ZipFile(zipname);
	
	      Enumeration entries = zipFile.entries();
	      while (entries.hasMoreElements()) 
	      {
	        ZipEntry entry = (ZipEntry)entries.nextElement();
	
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
	 * Unzip an archive
	 * @param zipname the archive name, stored in the temp directory
	 * @return boolean state indicating the unzip success
	 */
    @SuppressWarnings("rawtypes")
	private boolean UnzipLangArchive(String zipname)
    {
        ZipFile zipFile;
        
        // reset the statuses, thus we will be able to get the progress[%] status 
        m_lMaxDownloadSz = GetZipSize(zipname);
        m_lCurrentDownloadSz = 0;

        try 
	    {
	      zipFile = new ZipFile(zipname);
	
	      Enumeration entries = zipFile.entries();
	      while (entries.hasMoreElements()) 
	      {
	        ZipEntry entry = (ZipEntry)entries.nextElement();
	
	        String filename = entry.getName();
	        Log.v(TAG, "Extracting file: " + filename);
	        	
        	if (!CopyInputStream(zipFile.getInputStream(entry), entry.getSize(), entry.getName()))
        		return false;
			 m_ParentProgressDialog.setProgress(GetDownloadStatus());
	      }
	
	      zipFile.close();
	    } 
	    catch (IOException ioe) 
	    {
	      Log.v(TAG, "exception:" + ioe.toString());
	      return false;
	    }
	    
	    return true;
	}	
	
    /**
     * Copy a file from zip on the disk.
     * @param in InputStream of the zip file.
     * @param in_size the size of the input stream
     * @param outname the name of the output file
     * @return a boolean indicating the success of the copy procedure
     * @throws IOException
     */
	private final boolean CopyInputStream(InputStream in, long in_size, String outname)
	  throws IOException
	  {
	    byte[] buffer = new byte[1024];
	    int len;
	    DataOutputStream out = null;
	    int kont = 0;
	    
	    try
	    {
			File f = new File(Mezzofanti.TEMP_PATH + outname);			
			long fsz = f.length();

			//!! consider to remove this for clean reinstall at upload
			if (f!=null && f.isFile() && fsz == in_size)
			{
				Log.v(TAG, "File " + outname + " already installed disksz="+fsz +" zipsz="+in_size);
				return true;
			}
			else
			{
				Log.v(TAG, "File " + outname + " NOT installed disksz="+fsz +" zipsz="+in_size);
			}
		    
			if (in_size > AssetsManager.GetFreeSpaceB())
			{
		    	Log.v(TAG, "Warning: not enough disk space");
		    	m_bSdcardError = true;
		    	return false;
			}
			
			out = new DataOutputStream(new FileOutputStream(Mezzofanti.TEMP_PATH + outname));
		    
		    while((len = in.read(buffer)) >= 0)
		    {
				 if (m_bCancelDownload)
					 return false; 
				 kont++;
				 if (kont >= PROGRESS_STEP)
				 {
					 m_ParentProgressDialog.setProgress(GetDownloadStatus());
					 kont = 0;
				 }
				 out.write(buffer, 0, len);
				 m_lCurrentDownloadSz += len;
		    } 
		    
		    in.close();
		    out.close();
	    }
	    catch (Exception ex)
	    {
	    	m_bSdcardError = true;
	    	Log.v(TAG, "Exception: " + ex.toString());
	    	return false;
	    }
	    
		return true;
	}
    
    
    /**
     * rename/moves the temporary unziped files in the resources directory. 
     * @return status of the procedure
     */
	private boolean RenameTempFiles()
	{
		Log.v(TAG, "RenameTempFiles(): STARTED");
		String lang = m_ServerLanguages[m_iLangIndex].sExtName;
		
		// rename/move the files to the final directory
		for (int i=0; i<Mezzofanti.svLangExtNames.length; i++)
		{
			String filename = lang + Mezzofanti.svLangExtNames[i];
			if (!RenameFileOrDir(Mezzofanti.TEMP_PATH + filename, Mezzofanti.DATA_PATH + filename))
			{
				Log.v(TAG, "FAILED to rename " + Mezzofanti.TEMP_PATH + filename);
				return false;
			}
			else
				Log.v(TAG, "Renamed " + Mezzofanti.TEMP_PATH + filename);
				
		}
		Log.v(TAG, "RenameTempFiles(): Finished");
		return true;
	}
	
	/**
	 * rename a file or a directory
	 * @param initialName input name & path 
	 * @param finalName output name & path
	 * @return state of the process
	 */
	public boolean RenameFileOrDir(String initialName, String finalName)
	{
		boolean ret = false;
		try
		{
		    File file = new File(initialName);
		    File file2 = new File(finalName);
		    
		    ret = file.renameTo(file2);
		}
		catch (Exception ex)
		{
			Log.v(TAG, "Exception: "+ex.toString());
			ret = false;
		}
		
		return ret;
	}
	
	
	/**
	 * Download a file from the server.
	 * @param url the server URL
	 * @param filename the name of the file to be downloaded
	 * @return true on procedure success 
	 */
	public boolean DownloadFile(String url, String filename)
	{
		Log.v(TAG, "DownloadFile (" + url + filename + ")");
	    int len = 0;
	    m_lCurrentDownloadSz = 0;
	    m_lMaxDownloadSz = -1;
	    int kont = 0;

		// create url connector
	    URL u;
	    byte[] buffer = new byte[1024];
		try {
			u = new URL(url + filename);
			
		    HttpURLConnection c = (HttpURLConnection) u.openConnection();
		    c.setRequestMethod("GET");
		    c.setDoOutput(true);
		    c.setUseCaches(true);
		    c.connect();
		    InputStream in = c.getInputStream();
			Log.v(TAG, "DownloadFile opened connection");
		    m_lMaxDownloadSz = c.getContentLength();
		    
		    if (m_lMaxDownloadSz > AssetsManager.GetFreeSpaceB())
		    {
		    	Log.v(TAG, "Warning: not enough disk space");
		    	m_bSdcardError = true;
		    	return false;
		    }
		    
		    File ft = new File(Mezzofanti.TEMP_PATH, filename);
		    if (ft!=null && ft.isFile() && ft.length() == m_lMaxDownloadSz)
		    {
		    	Log.v(TAG, "Download has the same size as the temp file - do not download");
		    	return true;
		    }
		    
		    FileOutputStream f = new FileOutputStream(new File(Mezzofanti.TEMP_PATH, filename)); 
		    Log.v(TAG, "contentlen=" + c.getContentLength());
		    
		    while ( (len = in.read(buffer, 0, 1024)) > 0 ) 
		    {
				if (m_bCancelDownload)
				{
					f.close();
					c.disconnect();
					return false;
				}
				
				 kont++;
				 if (kont >= PROGRESS_STEP)
				 {
					 m_ParentProgressDialog.setProgress(GetDownloadStatus());
					 kont = 0;
				 }
		         f.write(buffer, 0, len);
		         m_lCurrentDownloadSz += len;
		    }
		    Log.v(TAG, "total len=" + m_lCurrentDownloadSz);
		    f.close();
		    c.disconnect();
		    
			if (c.getContentLength() != m_lCurrentDownloadSz)
			{
				Log.v(TAG, "------------ NOT ALL FILE DOWNLOADED -------------------");
			}
		} catch (Exception e) 
		{
			Log.v(TAG, e.toString());
			return false;
		}

	    return (m_lCurrentDownloadSz == m_lMaxDownloadSz);
	}   
}