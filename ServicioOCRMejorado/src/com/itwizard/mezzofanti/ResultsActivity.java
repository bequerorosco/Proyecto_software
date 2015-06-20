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
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

public class ResultsActivity  extends Activity
{
	private static final String TAG = "MLOG: ResultsActivity.java: ";
	private String m_sOCRResultLineMode = "";
	private boolean m_bLineMode = false;
	private StyleSpan m_ssOCRResult = new StyleSpan(Typeface.BOLD); // the spanning style for the ocr result	
	
    private SelectedWords[] m_SelectedWords;						// the selected words  
	private String  m_sSelectedText = "";							// the selected text
	
    private boolean m_bEditedOnceTheResults = false;				// the text was edited 
    private boolean m_bEditMode = false;							// in edit-mode
    private boolean m_bSelecting = false;							// in selecting-words mode
    private boolean m_bAllocatedResultsLayoutVariables = false;		// allocate the resources only once 
    private boolean m_bMenuSettings = false;						// used in onRestore to know when we come out from 'Menu'
    
    // Layout for buttons
	private LinearLayout m_llButtonsResultsLayout; 					// buttons layout
    
    // status layout
	private LinearLayout m_llStatusResultsLayout; 					// bottom-left of screen
    private TextView m_tvStatusBar;									// the status bar 
    
    // edit text 1 for selecting 2 for editing
    private LinearLayout m_llEditResultsLayout;						// top-left of screen
    private EditText m_etNonEditableText;							// not editable
    private EditText m_etEditableText;								// the editable text
    private Editable m_edtSpannableOCRResult;						// the OCR result, as a spannable text
    
    /**
     * The words selected from m_etNonEditableText
     */
    private class SelectedWords 
    {
    	public String m_sWord;			// the word body
    	public int m_iStart;			// the start of the word relative to the OCR results text
    	public boolean m_bExist;		// selected or not
    	public StyleSpan m_ssStyle;		// the spanning style for the word
    	
     	public SelectedWords()			
    	{
    		m_sWord = null;
    		m_iStart = -1;
    		m_bExist = false;
    		m_ssStyle = null;
    	}
    };
    
    /**
     *  Used to encapsulate data, when returning from the selecting list
     */
    private class ReturnEncapsulator 
    {
    	public boolean m_bExist;			// word is selected in the list 
    	public int m_iIndex;				// 
    	public String m_sBody = null;		// word body
    	
    	public ReturnEncapsulator()	
    	{
    		m_bExist = false;
    		m_iIndex = -1; 
    	}
    };
    
    
    // sliding drawer
    private SlidingDrawer m_sdPicture;		// the picture sliding drawer (middle - right side)
    
    // Word Layout 
    private LinearLayout m_llWordResultsLayout;	// bottom of screen (buttons + text)
    private TextView m_tvWordInfo;				// info about selected words
    
    // the buttons at the bottom side
    private CustomImageButton m_btWiki;		
    private CustomImageButton m_btTranslate;
    private CustomImageButton m_btGoogle;
    private CustomImageButton m_btDictionary;  	
    private CustomImageButton m_btEmail;
    private CustomImageButton m_btSMS;
    
    // the buttons on the right side
    private CustomImageButton m_btEdit;
    private CustomImageButton m_btSelect;
    private CustomImageButton m_btZoomIn;
    private CustomImageButton m_btZoomOut;

    // 'Menu' items
    Menu m_sPreferencesMenu = null;
	private static final int PREFERENCES_GROUP_ID = 0;
    private static final int SETTINGS_ID = Menu.FIRST;
    private static final int HELP_ID = Menu.FIRST + 1;
    private static final int FEEDBACK_ID = Menu.FIRST + 2;
    private static final int ABOUT_ID = Menu.FIRST + 3;
    
    
    
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
		        
	        
	        // set the layout to the xml definition
	        GetDataFromMezzofanti();
	        AllocateResultsLayoutVariables();
	
			Bitmap m_bmOCRBitmapIntern = BitmapFactory.decodeFile(Mezzofanti.RESULTS_PATH + "img.jpg");

	        EnterResutsMode(m_bmOCRBitmapIntern); 
	        SetClickableButtons(true);   
			m_etNonEditableText.requestFocus();
	        
            Log.v(TAG, "welcome");
        }
        catch (Exception ex)
        {
        	Log.v(TAG, "exception: onCreate(): " + ex.toString());
        }
            
    }

    @Override
    protected void onResume() 
    {
      super.onResume();
      Log.v(TAG, "onResume() ----------------------------");
            
      if (m_bMenuSettings)
      {
    	  // currently in Settings-Menu, and we return to Capture 
    	  // so we store all the settings data in the local variables
    	  m_bMenuSettings = false;
		  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    	  OCR.mConfig.GetSettings(prefs);
    	  OCR.ReadAvailableLanguages();
    	  CameraManager.SetImgDivisor(OCR.mConfig.GetImgDivisor());
      }
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
        	 m_bLineMode = bun.getBoolean("bLineMode");
        	 if (m_bLineMode)
        		 m_bEditedOnceTheResults = true;
             m_sOCRResultLineMode = bun.getString("LineModeText");
        }    	
    }
	
    /**
     * From capture-mode enters results-mode.
     * @param bm the bitmap to be processed by the OCR.
     */
	public void EnterResutsMode(Bitmap bm)
	{
		Log.v(TAG, "enter enterResutsMode");
		
    	// start the new layout
    	DisplayOcrResult(); 
    	    	
    	m_sdPicture.setOnDrawerOpenListener(drawerOpenCallback);
    	m_sdPicture.setOnDrawerCloseListener(drawerCloseCallback);
	    
    	Log.v(TAG, "exit enterResutsMode");
	}     
	
	/**
	 * Display the OCR results according to the settings.
	 */
	private void DisplayOcrResult()
	{		
		if (!m_bLineMode && OCR.m_iMeanConfidence < OCR.mConfig.m_iMinOveralConfidence)
			m_LocalMessageHandler.sendEmptyMessageDelayed(R.id.resultsactivity_displayWarning, 1000);
		
		m_etNonEditableText.requestFocus();
		try 
		{
			if (!m_bLineMode)
				m_etNonEditableText.setText(OCR.m_ssOCRResult);
			else
				m_etNonEditableText.setText(m_sOCRResultLineMode);
				
		} catch (Exception e) {
			Log.v(TAG, e.toString());
		}		

	}
	
	
	/**
	 * Enable/Disable the buttons
	 * @param val enable/disable
	 */
	void SetClickableButtons(boolean val)
	{
	    m_btZoomIn.setClickable(val);
	    m_btZoomOut.setClickable(val);
	    m_btEdit.setClickable(val);
	    m_btSelect.setClickable(val);
	    
	    m_btTranslate.setClickable(val);
	    m_btGoogle.setClickable(val);
	    m_btDictionary.setClickable(val);
	    m_btWiki.setClickable(val);
	    m_etNonEditableText.setClickable(val);
	    m_etNonEditableText.setLongClickable(val);
	}

	/**
	 * Resource allocator for the Results-Layout
	 */
	private void AllocateResultsLayoutVariables()
	{
		if (m_bAllocatedResultsLayoutVariables)
		{
        	DisplayStatus(R.id.resultsactivity_statusTranslateAll);			
			return;
		}
		else m_bAllocatedResultsLayoutVariables = true;
		
	    m_edtSpannableOCRResult = m_etNonEditableText.getText(); 
	    
	    // default text size
	    m_etNonEditableText.setTextSize(m_etNonEditableText.getTextSize() + 10);
	
	    // click on text
	    m_etNonEditableText.setOnClickListener(new OnClickListener() 
	    {
			public void onClick(View clickedView) 
			{
				Log.v(TAG, "editText1: onClick"); 
				m_etNonEditableText.invalidate();
		        Message message = m_LocalMessageHandler.obtainMessage(R.id.resultsactivity_clickEditText, null);
			    message.sendToTarget();
				if (m_LocalMessageHandler.sendEmptyMessageDelayed(R.id.resultsactivity_invalidateView, 500))
		        	Log.v(TAG, "succesfully posted message");
			}
		});
	    
	    // long click on text - selecting
	    m_etNonEditableText.setOnLongClickListener(new OnLongClickListener() 
	    {		
			public boolean onLongClick(View v) 
			{
				Log.v(TAG, "editText1: onLongClick");
				ShowDialogAddWordToDictionary();
				return true;
			}
		});
	  
	    //Custom buttons
	    
	    // Edit button
	    LinearLayout.LayoutParams layoutOpt = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    m_btEdit = new CustomImageButton(this, R.drawable.tedit1_48, "Edit", 0, 0);
	    m_btEdit.setOnClickListener(new View.OnClickListener() 
	    {
	        public void onClick(View v) 
	        {
	        	Log.v(TAG, "buttonEdit: onClick");
	        	// display full screen edit text
	        	m_llButtonsResultsLayout.setVisibility(View.GONE);
	        	m_llWordResultsLayout.setVisibility(View.GONE);
		        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, 20);
		        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	        	m_llStatusResultsLayout.setLayoutParams(params);
	        	
		        params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
	        	m_llEditResultsLayout.setLayoutParams(params);
	        	
	        	// clear all spans
	        	m_edtSpannableOCRResult = m_etNonEditableText.getText();
	        	m_edtSpannableOCRResult.clearSpans();
	        	m_etNonEditableText.setVisibility(View.GONE);
	        	m_etEditableText.setVisibility(View.VISIBLE);
	        	m_etEditableText.setText(m_edtSpannableOCRResult);
	        	m_sdPicture.setVisibility(View.GONE);
	
	        	DisplayStatus(R.id.resultsactivity_statusEdit);
	        	m_bEditMode = true;
	    		if (m_LocalMessageHandler.sendEmptyMessageDelayed(R.id.resultsactivity_invalidateView, 500))
	            	Log.v(TAG, "succesfully posted message");
	        }
	    });
	    
	    // Select Button
	    m_btSelect = new CustomImageButton(this, R.drawable.magnet_32, "Select", 48, 48);
	    m_btSelect.setOnClickListener(new View.OnClickListener() 
	    {	
			public void onClick(View v) 
			{
				Log.v(TAG, "buttonSelect: onClick");
				// delete the selected words, if any
				if (m_SelectedWords != null)
				{
					for (int i=0; i < m_SelectedWords.length; i++)
						m_SelectedWords[i].m_bExist = false;
				}
				m_sSelectedText = "";
				
				m_bSelecting = !m_bSelecting;			
				m_edtSpannableOCRResult = m_etNonEditableText.getText();
				m_edtSpannableOCRResult.clearSpans();
				m_etNonEditableText.invalidate();
		        Message message = m_LocalMessageHandler.obtainMessage(R.id.resultsactivity_clickSelectText, null);
			    message.sendToTarget();
				if (m_LocalMessageHandler.sendEmptyMessageDelayed(R.id.resultsactivity_invalidateView, 500))
		        	Log.v(TAG, "succesfully posted message");
				
			}
		});
	    
	    // ZoomIn Button
	    m_btZoomIn = new CustomImageButton(this, R.drawable.zoomin48, "ZoomIn", 0, 0);
	    m_btZoomIn.setOnClickListener(new View.OnClickListener() 
	    {	
			public void onClick(View v) 
			{
				Log.v(TAG, "buttonZoomIn: onClick");
				m_etNonEditableText.setTextSize(m_etNonEditableText.getTextSize() + 1);	
				if (m_LocalMessageHandler.sendEmptyMessageDelayed(R.id.resultsactivity_invalidateView, 500))
		        	Log.v(TAG, "succesfully posted message");
			}
		});
		
	    // ZoomOut Button
	    m_btZoomOut = new CustomImageButton(this, R.drawable.zoomout48, "ZoomOut", 0, 0);
	    m_btZoomOut.setOnClickListener(new View.OnClickListener() 
	    {	
			public void onClick(View v) 
			{
				Log.v(TAG, "buttonZoomOut: onClick");
				m_etNonEditableText.setTextSize(m_etNonEditableText.getTextSize() - 1);	
				if (m_LocalMessageHandler.sendEmptyMessageDelayed(R.id.resultsactivity_invalidateView, 500))
		        	Log.v(TAG, "succesfully posted message");
			}
		});
	
	    m_btEdit.setLayoutParams(layoutOpt);
	    m_btSelect.setLayoutParams(layoutOpt);
	    m_btZoomIn.setLayoutParams(layoutOpt);
	    m_btZoomOut.setLayoutParams(layoutOpt);
	    
	    // padding buttons	    
	    FrameLayout fl = new FrameLayout(this);
	    fl.setLayoutParams( new LinearLayout.LayoutParams(48, 75 + 48));
	
	    // add buttons to layout
	    m_llButtonsResultsLayout.addView(m_btEdit);
	    m_llButtonsResultsLayout.addView(m_btSelect);
	    m_llButtonsResultsLayout.addView(fl);
	    m_llButtonsResultsLayout.addView(m_btZoomIn);
	    m_llButtonsResultsLayout.addView(m_btZoomOut);
	
	    // the sliding drawer
	    m_sdPicture.setOnDrawerOpenListener(drawerOpenCallback);
	    m_sdPicture.setOnDrawerCloseListener(drawerCloseCallback);
	    
	    // add buttons beneath the edit text
	    AddWordButtons();
	    DisplayStatus(R.id.resultsactivity_statusTranslateAll);    	
	}

	/**
	 * Called when we return from Edit-mode to Results-Layout.
	 */
	private void ReturnFromEditToResultsLayout()
	{
		m_llButtonsResultsLayout.setVisibility(View.VISIBLE);
		m_llStatusResultsLayout.setVisibility(View.VISIBLE);
		m_llWordResultsLayout.setVisibility(View.VISIBLE);
		
	    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(48, LayoutParams.FILL_PARENT);
	    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	    m_llButtonsResultsLayout.setLayoutParams(params);
	    
	    params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT , 20);
	    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	    m_llStatusResultsLayout.setLayoutParams(params);
	    
	    params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT , 48);
	    m_llWordResultsLayout.setLayoutParams(params);
	    
	    params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
	    m_llEditResultsLayout.setLayoutParams(params);
	    
	    if (m_bEditMode)
	    {
			m_edtSpannableOCRResult = m_etEditableText.getText();
			m_etNonEditableText.setText(m_edtSpannableOCRResult);
			m_etEditableText.setVisibility(View.GONE);
			m_etNonEditableText.setVisibility(View.VISIBLE);
			DisplayStatus(R.id.resultsactivity_statusTranslateAll);
	    }
	    else // return from browser
	    {
	    	if (m_bSelecting)
	    		DisplayStatus(R.id.resultsactivity_statusSelection);
	    	else
	    	{
	    		m_edtSpannableOCRResult.removeSpan(m_ssOCRResult);
	    		m_sSelectedText = "";
	    		DisplayStatus(R.id.resultsactivity_statusTranslateAll);
	    	}
	    }

	    m_sdPicture.setVisibility(View.VISIBLE);
	}

	/**
	 * Fet word and position when selecting. 
	 * @param index position in the results text.
	 * @return word body encapsulator.
	 */
	private ReturnEncapsulator GetWordFromClickSelecting(int index)
	{
		ReturnEncapsulator ret2 = new ReturnEncapsulator();
		
		m_edtSpannableOCRResult = m_etNonEditableText.getText();
		String text = m_edtSpannableOCRResult.toString(); 
		int start = index;
		int stop = index;
		
		Log.v(TAG, "getWordFromClickSelecting: index="+index +" text.len="+text.length());
		if (index > text.length())
		{
			return ret2;
		}
		
		
		try
		{
			// search start
			while( start >= 0 && 
				   text.charAt(start) != ' ' && 
				   text.charAt(start) != '\n')
			{
				start--;
			}
			start++;
			
			// search stop
			while( stop < text.length() && 
				   text.charAt(stop) != ' ' && 
				   text.charAt(stop) != '\n')
			{
				stop++;
			}
			
	    	Log.v(TAG, "checkWordFromClickSelecting: start="+start + " stop="+stop);
			if (start < stop)
			{
		    	
		    	if (!m_bSelecting)
		    	{
		    		m_edtSpannableOCRResult.removeSpan(m_ssOCRResult);
		    		m_edtSpannableOCRResult.setSpan(m_ssOCRResult, start, stop, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		    	}
		    	else
		    	{
		    		
		    		ReturnEncapsulator ret = checkSelectedWords(start);
		    		if (ret.m_bExist)
		    		{
		    			if (ret.m_iIndex < 0)
		    			{
		    				Log.v(TAG, "checkWordFromClickSelecting: Not found ");
		    			}
		    			else
		    			{
		    				Log.v(TAG, "checkWordFromClickSelecting: Deselect ");
		    				m_edtSpannableOCRResult.removeSpan(m_SelectedWords[ret.m_iIndex].m_ssStyle);
		    			}
		    		}
		    		else
		    		{
		    			Log.v(TAG, "checkWordFromClickSelecting: Select ");
		    			m_edtSpannableOCRResult.setSpan(m_SelectedWords[ret.m_iIndex].m_ssStyle, start, stop, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		    		}
	
		    	}
		    	
		    	Log.v(TAG, "checkWordFromClickSelecting: start="+start + " stop="+stop);
		    	ret2.m_sBody = text.substring(start, stop);
		    	ret2.m_iIndex = start;
		    	Log.v(TAG, "checkWordFromClickSelecting: returning sBody="+ret2.m_sBody + " stop="+ret2.m_iIndex);
		    	
				return ret2;
			}
			else
			{
				m_edtSpannableOCRResult.removeSpan(m_ssOCRResult);
				return ret2;
			}
		}
		catch(Exception e)
		{
			m_edtSpannableOCRResult.removeSpan(m_ssOCRResult);
			return ret2;
		}
	}
	
	/**
	 * Check if word is already selected
	 * @param index word index
	 * @return word encapsulator.
	 */
	private ReturnEncapsulator checkSelectedWords(int index)
	{
		ReturnEncapsulator ret = new ReturnEncapsulator();
		
		for(int i = 0; i < m_SelectedWords.length ; i++)
		{
			Log.v(TAG, " checkSelectedWords: i=" + i + " idx=" + index + " start" + m_SelectedWords[i].m_iStart);
			if (index == m_SelectedWords[i].m_iStart)
			{
				if (m_SelectedWords[i].m_bExist)
				{
					m_SelectedWords[i].m_bExist = false;
					ret.m_bExist = true;
					ret.m_iIndex = i;
					Log.v(TAG, "checkSelectedWords: i="+i + " existed already, deselect it now");
					return ret;
				}
				else
				{
					m_SelectedWords[i].m_bExist = true;
					m_SelectedWords[i].m_ssStyle = new StyleSpan(Typeface.BOLD);
					ret.m_bExist = false;
					ret.m_iIndex = i;					
					Log.v(TAG, "checkSelectedWords: i="+i + " did not exist, create it now");
					return ret;
				}
			}
		}
		Log.v(TAG, "checkSelectedWords: index not found");
		ret.m_bExist = false;
		ret.m_iIndex = -1;					
		return ret;
	}
	
	/**
	 * Create a list of all words and their position in the results text. 
	 * @param text the results text
	 * @return the length of the constructed list
	 */
	private int ConstructSelectedWords(String text)
	{
		
		String WordList[] = text.split("[ \n\t]+");
		m_SelectedWords = new SelectedWords[WordList.length];
		int start = 0;
		for (int i = 0; i< WordList.length; i++)
		{
			m_SelectedWords[i] = new SelectedWords();
			m_SelectedWords[i].m_iStart = text.indexOf(WordList[i], start);
			m_SelectedWords[i].m_sWord = WordList[i];
			m_SelectedWords[i].m_bExist = false;
			start = m_SelectedWords[i].m_iStart + 1;
		}
	
		return WordList.length;
	}
	
	/**
	 * Dynamically create the buttons in the word layout.
	 */
	private void AddWordButtons()
	{
	    int offset = 4;
	    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(48+offset, LayoutParams.WRAP_CONTENT);
	
		// translate button
	    m_btTranslate = new CustomImageButton(this, R.drawable.babelfish_48, "Translate", 0, 0);
	    m_btTranslate.SetMargins(offset, 0);
	    m_btTranslate.setOnClickListener(new View.OnClickListener() 
	    {
			public void onClick(View v) 
			{
				StartTranslateActivity();
			}
		});
	    m_btTranslate.setLayoutParams(params);	    
	    m_llWordResultsLayout.addView(m_btTranslate);
	
		// email button
	    m_btEmail = new CustomImageButton(this, R.drawable.email3_48, "e-mail", 0, 0);
	    m_btEmail.SetMargins(offset, 0);
	    m_btEmail.setOnClickListener(new View.OnClickListener() 
	    {
			public void onClick(View v) 
			{
        	    Intent intent2 = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        	    intent2.putExtra("subject", "[Mezzofanti]");
        	    intent2.putExtra("body", m_etNonEditableText.getText().toString());
        	    startActivity(intent2);
			}
		});
	    m_btEmail.setLayoutParams(params);
	    m_llWordResultsLayout.addView(m_btEmail);

		// sms button
	    m_btSMS = new CustomImageButton(this, R.drawable.htc_48, "sms", 0, 0);
	    m_btSMS.SetMargins(offset, 0);
	    m_btSMS.setOnClickListener(new View.OnClickListener() 
	    {
			public void onClick(View v) 
			{
        		Intent sendIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms://"));
        		sendIntent.putExtra("address", "");
        		sendIntent.putExtra("sms_body", m_etNonEditableText.getText().toString());
        		startActivity(sendIntent);	        		
			}
		});
	    m_btSMS.setLayoutParams(params);
	    m_llWordResultsLayout.addView(m_btSMS);

	    // google button
	    m_btGoogle = new CustomImageButton(this, R.drawable.google1_48, "Google", 0, 0);
	    m_btGoogle.SetMargins(offset, 0);
	    m_btGoogle.setOnClickListener(new View.OnClickListener() 
	    {
			public void onClick(View v) 
			{
        		Intent intent = new Intent(Intent.ACTION_WEB_SEARCH );
        		intent.putExtra(SearchManager.QUERY, m_sSelectedText);
        		startActivity(intent);								
			}
		});
	    m_btGoogle.setLayoutParams(params);
	    m_llWordResultsLayout.addView(m_btGoogle);
	
	    // Dictionary button
	    m_btDictionary = new CustomImageButton(this, R.drawable.dictionary48, "Dictionary", 0, 0);
	    m_btDictionary.SetMargins(offset, 0);
	    m_btDictionary.setOnClickListener(new View.OnClickListener() 
	    {
			public void onClick(View v) 
			{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://en.wiktionary.org/wiki/" + m_sSelectedText));
				startActivity(intent);								
			}
		});
	    
	    m_btDictionary.setLayoutParams(params);
	    m_llWordResultsLayout.addView(m_btDictionary);

		// wiki button
	    m_btWiki = new CustomImageButton(this, R.drawable.wiki48, "Wiki", 0, 0);
	    m_btWiki.SetMargins(offset, 0);
	    m_btWiki.setOnClickListener(new View.OnClickListener() 
	    {
			public void onClick(View v) 
			{        	    
        		String uri = "http://en.wikipedia.org/wiki/" + m_sSelectedText;
        		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        		startActivity(intent);				
			}
		});
	    m_btWiki.setLayoutParams(params);
	    m_llWordResultsLayout.addView(m_btWiki);

	    m_tvWordInfo = new TextView(this);
	    params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT , LayoutParams.FILL_PARENT);
	    params.leftMargin = 10;
	    m_tvWordInfo.setLayoutParams(params);
	    m_tvWordInfo.setTextColor(Color.WHITE);
	    m_llWordResultsLayout.addView(m_tvWordInfo);
	}

	/**
	 * Called from Results-layout, when the user wants to translate some text.
	 */
    private void StartTranslateActivity()
    {
    	Intent intent = new Intent();
    	Bundle bun = new Bundle();
    	    	
    	if (m_sSelectedText != "")
    	{
         	bun.putString("text", m_sSelectedText);    		
    		bun.putBoolean("return_edit_text", false);
    	}
    	else
    	{
         	bun.putString("text", m_etNonEditableText.getText().toString());    		
    		bun.putBoolean("return_edit_text", true);    			
    	}
    	
    	intent.setClass(this, TranslateActivity.class);
    	intent.putExtras(bun);
    	startActivityForResult(intent, R.id.translateactivity_finished);     
    } 
    
	/**
	 * Show/hide buttons in word layout
	 * @param showDict true/false
	 * @param showGoogle true/false
	 * @param showTranslate true/false
	 * @param showWiki true/false
	 */
	private void ShowButtonsmInWordResultsLayout(boolean showDict, boolean showGoogle, boolean showTranslate, boolean showWiki)
	{
		if (showDict)
			m_btDictionary.setVisibility(View.VISIBLE);
		else
			m_btDictionary.setVisibility(View.GONE);
		
		if (showGoogle)
			m_btGoogle.setVisibility(View.VISIBLE);
		else
			m_btGoogle.setVisibility(View.GONE);
		
		if (showTranslate)
			m_btTranslate.setVisibility(View.VISIBLE);
		else
			m_btTranslate.setVisibility(View.GONE);
		
		if (showWiki)
			m_btWiki.setVisibility(View.VISIBLE);
		else
			m_btWiki.setVisibility(View.GONE);
		
	}
	
	/**
	 * Show/hide the Email/Sms buttons.
	 * @param showEmail true/false
	 * @param showSMS true/false
	 */
	private void ShowEmailSmsButtons(boolean showEmail, boolean showSMS)
	{
		if (showEmail)
			m_btEmail.setVisibility(View.VISIBLE);
		else
			m_btEmail.setVisibility(View.GONE);
		
		if (showSMS)
			m_btSMS.setVisibility(View.VISIBLE);
		else
			m_btSMS.setVisibility(View.GONE);
	}
	
	/**
	 * Changes display mode, depending on the internal states: edit-mode, select-mode etc.
	 * @param status internal state
	 */
	private void DisplayStatus(int status)
	{		
		switch(status)
		{
			// this is the default, when m_etNonEditableText is first displayed + translate/sms/email buttons
			case R.id.resultsactivity_statusTranslateAll:
				m_tvStatusBar.setText(getString(R.string.resultsactivity_resultsmode_selected_all_text));
				m_tvWordInfo.setText("");
			    ShowButtonsmInWordResultsLayout(false,false,true,false);
			    ShowEmailSmsButtons(true, true);
			    if (!m_bEditedOnceTheResults)
			    	m_tvWordInfo.setText(getString(R.string.resultsactivity_resultsmode_mean_confidence) + " " + OCR.m_iMeanConfidence + "%");
				break;
			
			// one-word mode	
			case R.id.resultsactivity_statusWord:
				ShowButtonsmInWordResultsLayout(true,true,true,true);
			    ShowEmailSmsButtons(false, false);
				m_tvStatusBar.setText(getString(R.string.resultsactivity_resultsmode_selected_single_word));
				m_tvWordInfo.setText("");
				break;
				
			// multiple-words mode
			case R.id.resultsactivity_statusSelection:
				ShowButtonsmInWordResultsLayout(true,true,true,true);
			    ShowEmailSmsButtons(false, false);
				m_tvStatusBar.setText(getString(R.string.resultsactivity_resultsmode_selected_multiple_words));
				m_tvWordInfo.setText(getString(R.string.resultsactivity_resultsmode_selected_text) + m_sSelectedText);
				break;
				
			// in edit-mode
			case R.id.resultsactivity_statusEdit:
				m_tvWordInfo.setText("");
				m_bEditedOnceTheResults = true;
				if (m_bSelecting)
				{
					m_bSelecting = false;
					for (int i = 0; i < m_SelectedWords.length; i++ )
						m_SelectedWords[i].m_bExist = false;
				}
				m_tvStatusBar.setText(getString(R.string.resultsactivity_resultsmode_edit_mode_back));
				break;
			
			default:
				break;
		}
	}
	
	
    /**
     * Show a prompt dialog to add an word to the dictionary.
     */
	private void ShowDialogAddWordToDictionary()
	{		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setIcon(R.drawable.alert32);
	    builder.setTitle(R.string.resultsactivity_addword2dict_title);
	    builder.setMessage(getString(R.string.resultsactivity_addword2dict_message1)+ " [" + m_sSelectedText + "] " + getString(R.string.resultsactivity_addword2dict_message2) + " " + OCR.mConfig.GetLanguageMore() + " " + getString(R.string.resultsactivity_addword2dict_message3));
	    builder.setPositiveButton(R.string.resultsactivity_add_word_to_dictionary_yes, mAddWordToDictionaryYes);
	    builder.setNegativeButton(R.string.resultsactivity_add_word_to_dictionary_no, mAddWordToDictionaryNo);
	    builder.show();		
	} 
	
	/**
	 * User adds the word to the dictionary.
	 */
    private final DialogInterface.OnClickListener mAddWordToDictionaryYes = new DialogInterface.OnClickListener() 
    {
        public void onClick(android.content.DialogInterface dialogInterface, int i) 
        {
      	  Log.v(TAG, "Add word to dict: [" + m_sSelectedText + "]");      	  
      	  OCR.get().AddUserWord(m_sSelectedText.toLowerCase());
        }
    };

	/**
	 * User dosn't add the word to the dictionary.
	 */
    private final DialogInterface.OnClickListener mAddWordToDictionaryNo = new DialogInterface.OnClickListener() 
    {
        public void onClick(android.content.DialogInterface dialogInterface, int i) 
        {
      	  Log.v(TAG, "Do not add word to dictionary.");
        }
    };
	
	
	/*
	 * ----------------------------------------------------------------------------------------
	 * SlidingDrawer
	 * ----------------------------------------------------------------------------------------
	 */
	SlidingDrawer.OnDrawerOpenListener drawerOpenCallback = new SlidingDrawer.OnDrawerOpenListener()
    {
	    public void onDrawerOpened()
	    {
//		    ImageView png = (ImageView)findViewById(R.id.resultsactivity_IconSlide);
//		    png.setImageResource(R.drawable.right48);
		    SetClickableButtons(false);
	    }
    };	
	
	SlidingDrawer.OnDrawerCloseListener drawerCloseCallback = new SlidingDrawer.OnDrawerCloseListener()
    {
	    public void onDrawerClosed()
	    {
		    SetClickableButtons(true);
	    }
    };
	
	
	/*
	 * ----------------------------------------------------------------------------------------------------
	 * Events Handler 
	 * ---------------------------------------------------------------------------------------------------- 
	 */
		
	private Handler m_LocalMessageHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what)
			{					
				// ---------------------------------------------------------------	
				// on m_etNonEditableText click
				case R.id.resultsactivity_displayWarning:
					AlertDialog.Builder builder = new AlertDialog.Builder(ResultsActivity.this);
				    builder.setIcon(R.drawable.alert32);
				    builder.setTitle(R.string.resultsactivity_ocrbadresults_title);
				    builder.setMessage(getString(R.string.resultsactivity_ocrbadresults_msg1) + " " + OCR.m_iMeanConfidence +" " + getString(R.string.resultsactivity_ocrbadresults_msg2));
				    builder.setPositiveButton(R.string.resultsactivity_ocrbadresults_ok, null);
				    builder.show();	
				break;
				// ---------------------------------------------------------------	
				// on m_etNonEditableText click
				case R.id.resultsactivity_clickEditText:
					
					int selStart = m_etNonEditableText.getSelectionStart();
					int end = m_etNonEditableText.getSelectionEnd();
					// Determine the word "under" selStart...
					Log.v(TAG, "--------------------");
					Log.v(TAG, "in handle sel:" + String.valueOf(selStart) + " end:"+ String.valueOf(end));
					ReturnEncapsulator mWord = GetWordFromClickSelecting(selStart);
					Log.v(TAG, "selected word: " + mWord.m_sBody + " index=" + mWord.m_iIndex);
					
					if (mWord.m_sBody != null)
					{
						if (!m_bSelecting)
						{
							DisplayStatus(R.id.resultsactivity_statusWord);

							m_sSelectedText = mWord.m_sBody;
							if (!m_bEditedOnceTheResults)
							{
								int idx = OCR.GetWordIndex(mWord.m_iIndex); 
								if ( idx >= 0)
								{
									OCR.Word w = OCR.m_asWords[idx];
									m_tvWordInfo.setText(getString(R.string.resultsactivity_selected_word) +"["+ w.m_sBody +"]\n" + 
											getString(R.string.resultsactivity_ocr_trust) + w.m_iConfidence + "\n" +
											getString(R.string.resultsactivity_ocr_valid_word) + " " + OCR.mConfig.GetLanguageMore() + " " + getString(R.string.resultsactivity_ocr_valid_word2) + w.m_bIsValidWord
											);
								}
							}
							else
							{
								m_tvWordInfo.setText(getString(R.string.resultsactivity_selected_word) +"[" + mWord.m_sBody + "]\n" + 
										getString(R.string.resultsactivity_ocr_valid_word) + " "+ OCR.mConfig.GetLanguageMore() + " "+ getString(R.string.resultsactivity_ocr_valid_word2) + " "+ OCR.IsValidComposedWord(mWord.m_sBody.replace(" ", ""))
										);							
							}
						}
						else
						{
							DisplayStatus(R.id.resultsactivity_statusSelection);
							m_tvWordInfo.setText(getString(R.string.resultsactivity_resultsmode_selected_text));
							Log.v(TAG, "\nSelected text: ");
							m_sSelectedText = "";
							for (int i = 0; i < m_SelectedWords.length; i++ )
							{
								if (m_SelectedWords[i].m_bExist)
								{
									Log.v(TAG, "> " + m_SelectedWords[i].m_sWord);
									m_tvWordInfo.append(" " + m_SelectedWords[i].m_sWord);
									m_sSelectedText += m_SelectedWords[i].m_sWord + " ";
								}							
							}
						}
					}
					else
					{
						if (!m_bSelecting)
						{
							m_sSelectedText = "";
							DisplayStatus(R.id.resultsactivity_statusTranslateAll);
						}
					}
					
					break;
				
				// ---------------------------------------------------------------
				// on long click m_etNonEditableText
				case R.id.resultsactivity_clickSelectText:
					if (m_bSelecting)
					{
						Log.v(TAG, "R.id.clickSelectText: m_bSelecting=" + m_bSelecting);
						DisplayStatus(R.id.resultsactivity_statusSelection);
						ConstructSelectedWords(m_edtSpannableOCRResult.toString());
					}
					else
					{
						Log.v(TAG, "R.id.clickSelectText: m_bSelecting=" + m_bSelecting);
						for (int i = 0; i < m_SelectedWords.length; i++ )
							m_SelectedWords[i].m_bExist = false;
						m_sSelectedText = "";
						
						m_tvWordInfo.setText(getString(R.string.resultsactivity_resultsmode_selected_word_none));
						DisplayStatus(R.id.resultsactivity_statusTranslateAll);
					}
					
					break;

				// ---------------------------------------------------------------
				// in Results-mode: the focus is by default on m_etNonEditableText
				case R.id.resultsactivity_invalidateView: 
			        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			        if (m_etNonEditableText != null)
			        	m_etNonEditableText.requestFocus();
					break;
			
				// ---------------------------------------------------------------
					
				default:
					break;
					
			}
			
			super.handleMessage(msg);
		}
		
	};
	
	/*
	 * ----------------------------------------------------------------------------------------
	 * Keyboard
	 * ----------------------------------------------------------------------------------------
	 */
		@Override
	    public boolean onKeyDown(int keyCode, KeyEvent event) 
		{		
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	        	        	        
	        if ( keyCode == KeyEvent.KEYCODE_BACK )
	        { 	// if "BACK-KEY" pressed
	        	Log.v(TAG, "KEY pressed - killing me softly");
	            if (event.getRepeatCount() == 0)
	            {            	
	            	if (m_bEditMode) 
	            	{
	            		// exit form edit mode 		
	            		ReturnFromEditToResultsLayout();
	            		m_bEditMode = false;
	            		return true;
	            	} 
	            	else
	            	{
		        		// if we are in RESULTS_LAYOUT go back to CAPTURE_LAYOUT
	            		// delete the internal caches
	            		System.gc();
	            		finish();
	            	}
	            }
	        }
	        
	        
	        return super.onKeyDown(keyCode, event);
	    }

		// this is for opening the keyboard event
	    @Override
	    public void onConfigurationChanged(Configuration newConfig) 
	    {
	        // do nothing when keyboard open
	        super.onConfigurationChanged(newConfig);
	    }     
		
	    /*
	     * ----------------------------------------------------------------------------------------------------
	     * Activity results 
	     * ---------------------------------------------------------------------------------------------------- 
	     */	
	    	@Override
	    	protected void onActivityResult(int requestCode, int resultCode,
	                Intent data) 
	    	{
	    		Log.v(TAG, "onActivityResult");
	    		switch(requestCode)
	    		{
	    			case R.id.translateactivity_finished:
	    				Log.v(TAG, "onActivityResult requestCode translateactivity_finished");
	    				if (resultCode == RESULT_OK) 
	    				{
	    		            Bundle res = data.getExtras();
	    		            String editedText = res.getString("edit_text");
	    		            m_bEditedOnceTheResults = true;
	    		            m_etNonEditableText.setText(editedText);
	    		            m_edtSpannableOCRResult = m_etNonEditableText.getText();
	    		            m_edtSpannableOCRResult.clearSpans();
	    		            Log.v(TAG, "onActivityResult Edited text: " + editedText);
	    				}
	    				break;
	    		}
	    		
	    	}
	    
    /*
     * ----------------------------------------------------------------------------------------------------
     * Add the menu 
     * ---------------------------------------------------------------------------------------------------- 
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
	          
	          if (Mezzofanti.CheckSDCardState() == false)
	        	  menu.setGroupVisible(PREFERENCES_GROUP_ID, false);
	          
	          return true;
	        } 
	        
	        @Override
	        public boolean onOptionsItemSelected(MenuItem item) 
	        {
	          switch (item.getItemId()) {
	            case SETTINGS_ID: {
	          	  OCR.ReadAvailableLanguages();
	              m_bMenuSettings = true;
	              Intent intent = new Intent(Intent.ACTION_VIEW);
	              intent.setClassName(this, PreferencesActivity.class.getName());
	              startActivity(intent);
	              
	              break;
	            }
	           

	            case HELP_ID: 
	    			AlertDialog.Builder builderH = new AlertDialog.Builder(this);
	    			builderH.setIcon(R.drawable.wizard_48);
	    			builderH.setTitle(getString(R.string.preferences_helpTitle));
	    			builderH.setMessage(getString(R.string.preferences_msg_help_step2) + "\n" + getString(R.string.preferences_itwiz_url));
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
	    	
	    	
	    	
}
