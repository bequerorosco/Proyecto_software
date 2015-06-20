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


import com.itwizard.mezzofanti.LanguageDialog.LDActivity;
import com.itwizard.mezzofanti.Languages.Language;
import com.uah.servicioocr.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

/**
 * Wrapper for the Google code, to translate from one language to another.
 *
 */
public class TranslateActivity extends Activity implements LDActivity, Runnable 
{    
	private static final String TAG = "MLOG: TranslateActivity.java: ";
	
	// visual texts
	private TextView m_tvStatus;					// status bat
	private EditText m_etOriginal;					// the original text
	private EditText m_etTranslation;				// the translated text
		
	// buttons
	private CustomImageButton m_btLanguageFrom;		// language to translate from
	private CustomImageButton m_btLanguageTo;		// language to translate-into
    private CustomImageButton m_btEmail;			
    private CustomImageButton m_btSMS;				
	
    private boolean m_bThreadStarted = false;		// thread started or not
	private boolean m_bFrom;						// true=lang-from false=lang-in 
	private boolean m_bEdited = false;				// if original text was edited or not 
	private String m_sResult;						// the translation result
	private String m_sFromShortName = "ro";			// language to translate from (default) - short form 
	private String m_sToShortName = "en";			// language to translate into (default) - short form
	
	// the caller parameters
	private String m_sInput;						// the input text
	private boolean m_bReturnEditText = false;		// if the input is only a part of the original text, we will never return the local edited text 
	private String m_sLanguageFrom = "";			// language to translate from (default) - long form
	private String m_sLanguageTo = "";				// language to translate into (default) - long form

	
     @Override
    public void onResume() 
    {
    	 super.onResume();
    	 GetDataFromMezzofanti();
         m_sFromShortName = Languages.Language.mLongNameToShortName.get(m_sLanguageFrom);
         m_sToShortName = Languages.Language.mLongNameToShortName.get(m_sLanguageTo);    	 
    }

    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        GetDataFromMezzofanti();
        
        m_sFromShortName = Languages.Language.mLongNameToShortName.get(m_sLanguageFrom);
        m_sToShortName = Languages.Language.mLongNameToShortName.get(m_sLanguageTo);
    	Log.v(TAG,"m_sFromShortName " + m_sFromShortName);    	
    	Log.v(TAG,"toShortName " + m_sToShortName);

        // never enter standby
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        handler.sendEmptyMessageDelayed(R.id.translateactivity_createEditTextLayout, 500);
        
        
    }       
 
    /**
     * Get params-data from the caller.
     */
    private void GetDataFromMezzofanti()
    {
        Bundle bun = getIntent().getExtras();
        if (null == bun) 
        {
        	Log.v(TAG,"Bundle is null");
        }
        else
        {
        	 m_sInput = bun.getString("text");
             m_bReturnEditText = bun.getBoolean("return_edit_text");
         	 // set languages for translation
   		     SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
     		 String langfrom = prefs.getString(PreferencesActivity.KEY_TRANSLATE_FROM_SETTINGS, Languages.Language.ENGLISH.getLongName());
     		 m_sLanguageFrom = langfrom;	
     		 String langto = prefs.getString(PreferencesActivity.KEY_TRANSLATE_TO_SETTINGS, Languages.Language.SPANISH.getLongName());
     		 m_sLanguageTo = langto;    		
             Log.v(TAG," text = " + m_sInput + " bool = " + m_bReturnEditText + " m_sLanguageFrom = " + m_sLanguageFrom + " m_sLanguageTo = " + m_sLanguageTo);
        }    	
    }
    
    /**
     * Resize the EditTexts such a way both fill completly the screen.
     */
    private void ResizeEditTextViews()
    {    	
    	Language fromlang = Languages.Language.mShortNameToLanguage.get(m_sFromShortName);
    	Language tolang = Languages.Language.mShortNameToLanguage.get(m_sToShortName);
    	
    	try
    	{
    		int offset = 4;
    		// Language selector from button
	        m_btLanguageFrom = new CustomImageButton(this, fromlang.getFlag(), "Lang From", 48 - offset, 30);
	        m_btLanguageFrom.SetMargins(offset, 0);
	        m_btLanguageFrom.setOnClickListener(new View.OnClickListener() 
	        {
				public void onClick(View v) 
				{
					m_bFrom = true;
		        	showDialog(R.id.translateactivity_dialogid);			
				}
			});
	        m_btLanguageFrom.setLayoutParams(new LinearLayout.LayoutParams(48 , 30));
     
			// Language selector to button
	        m_btLanguageTo = new CustomImageButton(this, tolang.getFlag(), "Lang to", 48 - offset, 30);
	        m_btLanguageTo.SetMargins(offset, 0);
	        m_btLanguageTo.setOnClickListener(new View.OnClickListener() 
	        {
				public void onClick(View v) 
				{
					m_bFrom = false;
		        	showDialog(R.id.translateactivity_dialogid);
				}
			});
	        m_btLanguageTo.setLayoutParams(new LinearLayout.LayoutParams(48 , 30));
	        
			// email button
		    m_btEmail = new CustomImageButton(this, R.drawable.email3_48, "e-mail", 0, 0);
		    m_btEmail.setOnClickListener(new View.OnClickListener() 
		    {
				public void onClick(View v) 
				{
	        	    Intent intent2 = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
	        	    intent2.putExtra("subject", "[Mezzofanti]");
	        	    intent2.putExtra("body", m_etTranslation.getText().toString());
	        	    startActivity(intent2);
				}
			});
		    m_btEmail.setLayoutParams(new LinearLayout.LayoutParams(48 , 48));

			// sms button
		    m_btSMS = new CustomImageButton(this, R.drawable.htc_48, "sms", 0, 0);
		    m_btSMS.setOnClickListener(new View.OnClickListener() 
		    {
				public void onClick(View v) 
				{
	        		Intent sendIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms://"));
	        		sendIntent.putExtra("address", "");
	        		sendIntent.putExtra("sms_body", m_etTranslation.getText().toString());
	        		startActivity(sendIntent);	        		
				}
			});
		    m_btSMS.setLayoutParams(new LinearLayout.LayoutParams(48 , 48));	        
    	}
    	catch(Exception e)
    	{
    		Log.v(TAG,"Exception: " + e.toString());
    	}
    	
        m_etOriginal.setText(m_sInput);
        TranslateThread();
        

        // edit text listener
    	m_etOriginal.addTextChangedListener(new TextWatcher() 
    	{
			
			public void onTextChanged(CharSequence s, int start, int before, int count) 
			{
				Log.v(TAG, "onTextChanged Edited");
				m_sInput = m_etOriginal.getText().toString();
				m_bEdited = true;				
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) 
			{
				Log.v(TAG, "beforeTextChanged Edited ");
			}
			
			public void afterTextChanged(Editable s) 
			{
				Log.v(TAG, "afterTextChanged Edited ");
			}
		});
    	// end edit text listener
    	
    	m_etTranslation.setOnFocusChangeListener(new View.OnFocusChangeListener() 
    	{
			
			public void onFocusChange(View v, boolean hasFocus) 
			{

				if (hasFocus)
				{
					Log.v(TAG, "OnFocusTranslate");
			        TranslateThread();
				}
			}
		});
    	
    }
   
    /**
     * Translate from one language to another.
     * @param input the input text
     * @return the translated text
     */
    public void TranslateThread()
    {
    	m_tvStatus.setText(R.string.translateactivity_inprogress);    	
    	Thread theDownloadThread = new Thread(this);
    	theDownloadThread.start();
    }
    
    /**
     * Reset the images on the buttons according to the language.
     * @param lang the new language to be set
     * @param from true=lang-from false=lang-in
     */
	public void SetNewLanguage(Language lang, boolean from)
	{
		if (from)
		{
        	m_btLanguageFrom.SetImage(lang.getFlag(), 48, 30);
        	m_btLanguageFrom.invalidate();
        	m_sFromShortName = lang.getShortName();
		}
		else
		{
        	m_btLanguageTo.SetImage(lang.getFlag(), 48, 30);
        	m_btLanguageTo.invalidate();
        	m_sToShortName = lang.getShortName();
		}
		m_sInput = m_etOriginal.getText().toString();
        TranslateThread();
	}
	
	/**
	 * Show a dialog on the screen to keep the edited text, before returning to the caller. 
	 */
	private void ShowDialogKeepText()
	{
		  AlertDialog.Builder builder = new AlertDialog.Builder(this);
	      builder.setIcon(R.drawable.alert32);
	      builder.setTitle(R.string.translateactivity_dialog_keepeditedtext_title);
	      builder.setMessage(R.string.translateactivity_dialog_keepeditedtext_body);
	      builder.setPositiveButton(R.string.translateactivity_keep_text_yes, mKeepTextListenerYes);
	      builder.setNegativeButton(R.string.translateactivity_keep_text_no, mKeepTextListenerNo);
	      builder.show();		
	}
	
	/**
	 * Called when the user hits the "keep edited-text" yes-button.
	 */
    private final DialogInterface.OnClickListener mKeepTextListenerYes = new DialogInterface.OnClickListener() 
    {
        public void onClick(android.content.DialogInterface dialogInterface, int i) 
        {
      	  Log.v(TAG, "Keep text");
      	  Bundle conData = new Bundle();
      	  conData.putString("edit_text", m_etOriginal.getText().toString());
      	  Intent intent = new Intent();
      	  intent.putExtras(conData);
      	  setResult(RESULT_OK, intent);
      	  finish();
        }
    };

	/**
	 * Called when the user hits the "keep edited-text" NO-button.
	 */
    private final DialogInterface.OnClickListener mKeepTextListenerNo = new DialogInterface.OnClickListener() 
    {
        public void onClick(android.content.DialogInterface dialogInterface, int i) 
        {
      	  Log.v(TAG, "Do Not Keep text");
      	  Intent intent = new Intent();
      	  setResult(RESULT_CANCELED, intent);
      	  finish();
        }
    };
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
        // do nothing when keyboard open
    	Log.v(TAG, "On configuration changed");
    	super.onConfigurationChanged(newConfig); 	
    }

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if ( keyCode == KeyEvent.KEYCODE_BACK )
        { 	// if "BACK-KEY" pressed
        	Log.v(TAG, "Back KEY pressed");
            if (event.getRepeatCount() == 0)
            {            	
            	if (m_bEdited && m_bReturnEditText)
            	{
            		Log.v(TAG, "Ask me for save text");
            		ShowDialogKeepText();
            		return false;
            	}
            }
        }
        
        return super.onKeyDown(keyCode,event);
	}
	
    @Override
    protected void onPrepareDialog(int id, Dialog d) 
    {
        if (id == R.id.translateactivity_dialogid) 
        {
            boolean from = m_bFrom;
            ((LanguageDialog) d).SetFrom(from);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) 
    {
        if (id == R.id.translateactivity_dialogid) 
        {
            return new LanguageDialog(this);
        }
		return null;
    }
    
    /**
     * Trick to resize the EditTexts on the whole screen, after onCreate.
     */
	private Handler handler = new Handler() 
	{
		
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what)
			{
				case R.id.translateactivity_createEditTextLayout:
					ResizeEditTextViews();
					break;
				case R.id.translateactivity_finishedintern:
			        if (m_sResult != null)        
			        {	
			        	m_etTranslation.setText(m_sResult);
			        	m_tvStatus.setText(R.string.translateactivity_translate_OK);
			        }
			        else
			        {
			        	m_etTranslation.setText("");
			        	m_tvStatus.setText(R.string.translateactivity_translate_FALSE);
			        }
					break;
			}
		}
	};
	



	@Override
	public void run() 
	{
		if (m_bThreadStarted)
			return;
		
		m_bThreadStarted = true;
		
        Log.v(TAG, "Translating from " + m_sFromShortName + " to " + m_sToShortName);
        m_sResult = null;
        try 
        {
			m_sResult = Translate.translate(m_sInput, m_sFromShortName, m_sToShortName);
			Log.v(">", "Translating: " + m_sInput + " = " + m_sResult);
			
			// remove html codes
			// apostrophe 
			m_sResult = m_sResult.replace("&#39;", "'");
			m_sResult = m_sResult.replace("&apos;", "'");
			// quotation mark
			m_sResult = m_sResult.replace("&#34;", "\"");
			m_sResult = m_sResult.replace("&quot;", "\"");
			// ampersand
			m_sResult = m_sResult.replace("&amp;", "&");
			m_sResult = m_sResult.replace("&#38;", "&");
			// less-than
			m_sResult = m_sResult.replace("&lt;", "<");
			m_sResult = m_sResult.replace("&#60;", "<");
			// greater-than
			m_sResult = m_sResult.replace("&gt;", ">");
			m_sResult = m_sResult.replace("&#62;", ">");
		} 
        catch (Exception e) 
		{
			Log.v(TAG,"Exception: " + e.toString());
			m_sResult = null;
		}    	
        
        
        handler.sendEmptyMessage(R.id.translateactivity_finishedintern);
 
        m_bThreadStarted = false;
	}
}