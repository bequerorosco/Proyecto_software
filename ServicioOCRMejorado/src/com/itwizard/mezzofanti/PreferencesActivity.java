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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

/**
 * This activity implements the Settings/Menu button.
 *
 */
public final class PreferencesActivity extends android.preference.PreferenceActivity
    implements OnSharedPreferenceChangeListener, LDActivity
    {
		private static final String TAG = "MLOG: PreferencesActivity.java: ";
		private ProgressDialog m_ProgressDialog = null;							// local progress dialog for language download
		private DownloadManager m_DownloadManager = null;						// the download manager (for the langs)
		private SharedPreferences m_AppSharedPrefs = null;						// the application shared preferences, 
																				// used to save/load the settings that are not visible in the menu
		
		public static final String KEY_SKIP_INTRO_AT_STARTUP = "preferences_skip_intro_at_startup";
		public static final String KEY_USE_IMAGE_LIGHT_FILTER = "preferences_use_image_ligth_filter";
		public static final String KEY_SPEED_QUALITY = "preferences_speed_quality";
		public static final String KEY_SET_OCR_LANGUAGE = "preferences_set_OCR_language";
		public static final String KEY_DOWNLOAD_LANGUAGE = "preferences_download_language";
		public static final String KEY_MIN_OVERALL_CONFIDENCE = "preferences_min_overall_confidence";
		public static final String KEY_MIN_WORD_CONFIDENCE = "preferences_min_word_confidence";
		public static final String KEY_RESTORE_FACTORY_SETTINGS = "preferences_restore_factory_settings";
		public static final String KEY_TRANSLATE_FROM_SETTINGS = "preferences_translate_from";
		public static final String KEY_TRANSLATE_TO_SETTINGS = "preferences_translate_to";
		
		// the options in the menu
		private CheckBoxPreference m_cbpSkipIntroAtStartup;
		private CheckBoxPreference m_cbpUseImageLightFilter;
		private ListPreference m_lpSpeedQuality;
		private ListPreference m_lpSetOcrLanguage;
		private ListPreference m_lpDownloadLanguage;
		private EditTextPreference m_etpMinOverallConfidence;
		private EditTextPreference m_etpMinWordConfidence;
		private Preference m_pfRestoreFactorySettings;
		private Preference m_pfTranslateFromLang;
		private Preference m_pfTranslateToLang;
		
		
	  @Override
	  protected void onCreate(Bundle icicle) 
	  {
		  try 
		  {
			    super.onCreate(icicle);
			    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//			    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//			    addPreferencesFromResource(R.layout.preferences);
			
			    PreferenceScreen preferences = getPreferenceScreen();
			    m_AppSharedPrefs = preferences.getSharedPreferences();
			    preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
			    m_cbpSkipIntroAtStartup = (CheckBoxPreference) preferences.findPreference(KEY_SKIP_INTRO_AT_STARTUP);
			    m_cbpUseImageLightFilter = (CheckBoxPreference) preferences.findPreference(KEY_USE_IMAGE_LIGHT_FILTER);
			    m_lpSpeedQuality = (ListPreference) preferences.findPreference(KEY_SPEED_QUALITY);
			    m_lpSetOcrLanguage = (ListPreference) preferences.findPreference(KEY_SET_OCR_LANGUAGE);
			    m_lpDownloadLanguage = (ListPreference) preferences.findPreference(KEY_DOWNLOAD_LANGUAGE);
			    m_etpMinOverallConfidence = (EditTextPreference) preferences.findPreference(KEY_MIN_OVERALL_CONFIDENCE);
			    m_etpMinWordConfidence = (EditTextPreference) preferences.findPreference(KEY_MIN_WORD_CONFIDENCE);
			    m_pfRestoreFactorySettings = (Preference) preferences.findPreference(KEY_RESTORE_FACTORY_SETTINGS);
			    m_pfTranslateFromLang = (Preference) preferences.findPreference(KEY_TRANSLATE_FROM_SETTINGS);
			    m_pfTranslateToLang = (Preference) preferences.findPreference(KEY_TRANSLATE_TO_SETTINGS);
		
			    m_etpMinOverallConfidence.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
		
					public boolean onPreferenceChange(Preference preference,
							Object newValue) 
					{
						Log.v(TAG, "onPreferenceChange m_etpMinOverallConfidence --------------------------");
				        Message message = m_LocalMessageHandler.obtainMessage(R.id.preferences_MinOverallConfidence, null);
					    message.sendToTarget();
						return true;
					}
			    	
			    });
			    
			    m_etpMinWordConfidence.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			    {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) 
					{
						Log.v(TAG, "onPreferenceChange m_etpMinWordConfidence--------------------------");
				        Message message = m_LocalMessageHandler.obtainMessage(R.id.preferences_MinWordConfidence, null);
					    message.sendToTarget();
						return true;	
					}
			    	
			    });
		
				m_pfRestoreFactorySettings.setOnPreferenceClickListener(new OnPreferenceClickListener()
				  {
					public boolean onPreferenceClick(Preference preference) {
						Log.v(TAG, "setOnPreferenceClickListener CLICK");
						ShowRestoreFactorySettingsDialog();
						return false;
					}
				  });
				
				
				String langfrom = m_AppSharedPrefs.getString(KEY_TRANSLATE_FROM_SETTINGS, Languages.Language.ENGLISH.getLongName());  		
		
				m_pfTranslateFromLang.setTitle(this.getString(R.string.preferences_translate_from_settings) + ": " + langfrom);
				
				m_pfTranslateFromLang.setOnPreferenceClickListener(new OnPreferenceClickListener()
				  {
					public boolean onPreferenceClick(Preference preference) {
						Log.v(TAG, "setOnPreferenceClickListener CLICK");
						ShowLanaguageDialog(true);
						return false;
					}
				  });
		
				String langto = m_AppSharedPrefs.getString(KEY_TRANSLATE_TO_SETTINGS, Languages.Language.SPANISH.getLongName());
				m_pfTranslateToLang.setTitle(this.getString(R.string.preferences_translate_to_settings) + ": " + langto);
				m_pfTranslateToLang.setOnPreferenceClickListener(new OnPreferenceClickListener()
				  {
					public boolean onPreferenceClick(Preference preference) {
						Log.v(TAG, "setOnPreferenceClickListener CLICK");
						ShowLanaguageDialog(false);
						return false;
					}
				  });
		
				m_DownloadManager = new DownloadManager();
				m_DownloadManager.SetMessageHandler(m_LocalMessageHandler);
				CreateDownloadableLangsSubMenu();				
		
				CreateSettingsMenu();
		  }
		  catch (Exception ex)
		  {
			  Log.v(TAG, "exception: " + ex.toString());
		  }
	  }

	  /**
	   * Show the language dialog for selecting the language by flag.
	   * @param from true=language_from false=language_to
	   */
	  private void ShowLanaguageDialog(boolean from)
	  {
			LanguageDialog ld = new LanguageDialog(this); 
			ld.SetFrom(from);
        	ld.show();					  
	  }
	  
	  /**
	   * Show an alert on the screen.
	   * @param title the alert title
	   * @param message the alert body
	   */
	  private void ShowAlert(String title, String message)
	  {
		  AlertDialog.Builder builder = new AlertDialog.Builder(this);
	      builder.setTitle(title);
	      builder.setMessage(message);
	      builder.setPositiveButton(R.string.preferences_restore_factory_settings_button_ok, null);
	      builder.show();							  
	  }
	  
	  /**
	   * display a dialog to ask for the "Restore-factory-settings" approval
	   */
	  private void ShowRestoreFactorySettingsDialog()
	  {
		  AlertDialog.Builder builder = new AlertDialog.Builder(this);
	      builder.setIcon(R.drawable.alert32);
	      builder.setTitle(R.string.preferencesactivity_rfs_title);
	      builder.setMessage(R.string.preferencesactivity_rfs_body);
	      builder.setPositiveButton(R.string.preferences_restore_factory_settings_button_ok, m_pfRestoreFactorySettingsListener);
	      builder.setNegativeButton(R.string.preferences_restore_factory_settings_button_cancel, null);
	      builder.show();
	  }

	  /**
	   * if the restore-factory-settings button is pressed
	   */
      private final DialogInterface.OnClickListener m_pfRestoreFactorySettingsListener = new DialogInterface.OnClickListener() 
      {
          public void onClick(android.content.DialogInterface dialogInterface, int i) 
          {
        	  Log.v(TAG, "do restore factory settings");
        	  OCR.mConfig.LoadFabricDefaults();
        	  Log.v(TAG, "m_pfRestoreFactorySettingsListener: " + OCR.mConfig.GetLanguage());
        	  CreateSettingsMenu();
          }
      };
	      
    /**
     * create the settings menu
     */
    private void CreateSettingsMenu()
    {
	    try
	    {
		    // get settings from file and set it
	    	
		    m_cbpSkipIntroAtStartup.setChecked(Mezzofanti.m_bSkipIntroAtStartup);		    
		    m_cbpUseImageLightFilter.setChecked(OCR.mConfig.m_bUseBWFilter);
		    
		    CharSequence entries[] = new CharSequence[2];
		    CharSequence entriesLarge[] = new CharSequence[2];
		    entriesLarge[0] = getString(R.string.preferencesactivity_imgsz_optimal);
		    entriesLarge[1] = getString(R.string.preferencesactivity_imgsz_medium);
		    entries[0] = "2";
		    entries[1] = "4";
		    m_lpSpeedQuality.setEntries(entriesLarge);
		    m_lpSpeedQuality.setEntryValues(entries);
		    m_lpSpeedQuality.setValue("" + OCR.mConfig.GetImgDivisor());
		    

		    CreateValidLangsSubMenu();
		    
		    m_etpMinOverallConfidence.setText("" + OCR.mConfig.m_iMinOveralConfidence);
		    m_etpMinWordConfidence.setText("" + OCR.mConfig.m_iMinWordConfidence);		    
	    }
	    catch (Exception ex)
	    {
	    	Log.v(TAG, "Exception: " + ex.toString());
	    }
    	
    }
    
    /**
     * Create the downloadable-languages submenu.
     */
    private void CreateDownloadableLangsSubMenu()
    {
		if (m_DownloadManager.DownloadLanguageBrief(Mezzofanti.DOWNLOAD_URL, "languages.txt"))
		{
			// downloaded file correctly
			OCR.ReadAvailableLanguages();
			int len = m_DownloadManager.m_ServerLanguages.length;
		    CharSequence entriesLarge[] = new CharSequence[len];
		    CharSequence entries[] = new CharSequence[len];
		    for (int i=0; i<len; i++)
		    {
		    	if (OCR.mConfig.IsLanguageInstalled(m_DownloadManager.m_ServerLanguages[i].sExtName))
		    		entriesLarge[i] = m_DownloadManager.m_ServerLanguages[i].sFullName + " - " + (m_DownloadManager.m_ServerLanguages[i].lDownloadSz/1024) + "KB" + getString(R.string.preferencesactivity_reinstall);
		    	else
		    		entriesLarge[i] = m_DownloadManager.m_ServerLanguages[i].sFullName + " - " + (m_DownloadManager.m_ServerLanguages[i].lDownloadSz/1024) + "KB";
			    entries[i] = "" + i;
		    }		    
		    m_lpDownloadLanguage.setEntries(entriesLarge);
		    m_lpDownloadLanguage.setEntryValues(entries);
		    
		    m_lpDownloadLanguage.setOnPreferenceChangeListener(
		    	new OnPreferenceChangeListener() 
			    {
					//@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) 
					{
						m_LocalMessageHandler.sendEmptyMessage(R.id.preferences_selectedLang2Download);
						return true;
					}
				}
		    );
		    
		}
		else
		{
		    CharSequence entriesLarge[] = new CharSequence[1];
		    entriesLarge[0] = getString(R.string.preferencesactivity_cannotaccessinternet);
		    m_lpDownloadLanguage.setEntries(entriesLarge);
		    m_lpDownloadLanguage.setEntryValues(entriesLarge);
			ShowAlert(getString(R.string.preferencesactivity_warning), getString(R.string.preferencesactivity_problems));
		}    	
    }

    /**
     * Create the available-languages submenu.
     */
    private void CreateValidLangsSubMenu()
    {
    	OCR.ReadAvailableLanguages();
	    String[] svLangs = OCR.mConfig.GetvLanguages();
	    m_lpSetOcrLanguage.setEntries(svLangs);
	    m_lpSetOcrLanguage.setEntryValues(OCR.mConfig.m_asLanguages);
	    m_lpSetOcrLanguage.setValue(OCR.mConfig.GetLanguage());
    	
    }
      
    /**
     * show the range dialog for the "confidence" selectors
     */
	private void ShowRangeDialog()
	{
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.alert32);
        builder.setTitle(R.string.preferencesactivity_rangedialog_title);
        builder.setMessage(R.string.preferencesactivity_rangedialog_body);
        builder.setPositiveButton(R.string.preferences_button_cancel, null);
        builder.show();
	}

	/**
	 * the local message handler
	 */
	private Handler m_LocalMessageHandler = new Handler() 
	{
		
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what)
			{
				// download manager started unziping
				case R.id.downloadmanager_unziping:
					m_ProgressDialog.setMessage(getString(R.string.preferencesactivity_pd_body2));					
					break;
					
				// download manager finished with an error
				case R.id.downloadmanager_downloadFinishedError:
					m_ProgressDialog.dismiss();
					ShowAlert(getString(R.string.preferencesactivity_download_title), getString(R.string.preferencesactivity_downloaderr_body));
					break;
					
				// download manager finished with an error
				case R.id.downloadmanager_downloadFinishedErrorSdcard:
					m_ProgressDialog.dismiss();
					ShowAlert(getString(R.string.preferencesactivity_download_title), getString(R.string.preferencesactivity_downloaderrsdcard_body));
					break;
					
				// download manager finished ok 
				case R.id.downloadmanager_downloadFinishedOK:
					m_ProgressDialog.dismiss();
					CreateValidLangsSubMenu();
					CreateDownloadableLangsSubMenu();
					ShowAlert(getString(R.string.preferencesactivity_download_title), getString(R.string.preferencesactivity_downloadok_body));
					int index = Integer.parseInt(m_lpDownloadLanguage.getValue());
					String lang = m_DownloadManager.m_ServerLanguages[index].sExtName;
					Log.v(TAG, "Installed " + lang);
					OCR.get().SetLanguage(lang);
					
					// save lang in file
			    	SharedPreferences.Editor spe = m_AppSharedPrefs.edit();
		        	spe.putString(KEY_SET_OCR_LANGUAGE, lang);
			    	spe.commit();    	
					
					Log.v(TAG, "mconfig lang=" + OCR.mConfig.GetLanguageMore());
					break;
					
				// user selected a language to download
				case R.id.preferences_selectedLang2Download:
					m_DownloadManager.DownloadLanguageJob(Integer.parseInt(m_lpDownloadLanguage.getValue()));
					CreateProgressDialog(m_lpDownloadLanguage.getEntry());			    	
					m_DownloadManager.SetProgressDialog(m_ProgressDialog);
					break;
					
				// user wants to set the confidence
				case R.id.preferences_MinOverallConfidence:
					try
					{
						if ( Integer.parseInt(m_etpMinOverallConfidence.getText()) >=0 && Integer.parseInt(m_etpMinOverallConfidence.getText()) <=100)
						{
							Log.v("TEST",">" + Integer.parseInt(m_etpMinOverallConfidence.getText()));
						}
						else
						{
							Log.v("TEST",">" + Integer.parseInt(m_etpMinOverallConfidence.getText()));
							ShowRangeDialog();
							// set default value
							m_etpMinOverallConfidence.setText("60");	
						}
					} 
					catch(Exception e) 
					{
						Log.v("TEST",">" + Integer.parseInt(m_etpMinOverallConfidence.getText()));
						ShowRangeDialog();
						// set default value
						m_etpMinOverallConfidence.setText("60");	
					}

					break;
					
				// user wants to set the confidence
				case R.id.preferences_MinWordConfidence:
					try
					{
						if ( Integer.parseInt(m_etpMinWordConfidence.getText()) >=0 && Integer.parseInt(m_etpMinWordConfidence.getText()) <=100)
						{
							Log.v("TEST",">" + Integer.parseInt(m_etpMinWordConfidence.getText()));
						}
						else
						{
							Log.v("TEST",">" + Integer.parseInt(m_etpMinWordConfidence.getText()));
							ShowRangeDialog();
							// set default value
							m_etpMinWordConfidence.setText("50");	
						}
					} 
					catch(Exception e) 
					{
						Log.v("TEST",">" + Integer.parseInt(m_etpMinWordConfidence.getText()));
						ShowRangeDialog();
						// set default value
						m_etpMinWordConfidence.setText("50");	
					}
					break;
					
			}
		}
	};

	
	/**
	 * create a progress dialog for the download menu
	 * @param lang the language that is downloaded
	 */
	private void CreateProgressDialog(CharSequence lang)
	{
		
		m_ProgressDialog = new ProgressDialog(this);
		m_ProgressDialog.setTitle(R.string.preferencesactivity_pd_title);
		m_ProgressDialog.setMessage(getString(R.string.preferencesactivity_pd_body1) + " " + lang);
		m_ProgressDialog.setCancelable(true);
		m_ProgressDialog.setMax(100);
		m_ProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		m_ProgressDialog.show();
		
		m_ProgressDialog.setOnCancelListener(new OnCancelListener() {
    		public void onCancel(DialogInterface dialog) 
    		{
    			m_DownloadManager.CancelDownload();    			
	        }    		    		
    	});		
	}
	
	//@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) 
	{		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if ( keyCode == KeyEvent.KEYCODE_BACK )
        { 	// if "BACK-KEY" pressed
            if (event.getRepeatCount() == 0)
            {
            	finish();
            }
        }
        
        return super.onKeyDown(keyCode, event);
    }

	/**
	 * user selected a translate-language from settings  
	 */
	public void SetNewLanguage(Language language, boolean from) 
	{
    	SharedPreferences.Editor spe = m_AppSharedPrefs.edit();
		if (from)
		{
        	String fromName = language.getLongName();
        	Log.v(TAG, "Set lang-from: " + fromName);
        	spe.putString(KEY_TRANSLATE_FROM_SETTINGS, fromName);
    		m_pfTranslateFromLang.setTitle(this.getString(R.string.preferences_translate_from_settings) + ": " + fromName);
		}
		else
		{
        	String toName = language.getLongName();
        	Log.v(TAG, "Set lang-in: " + toName);
        	spe.putString(KEY_TRANSLATE_TO_SETTINGS, toName);
    		m_pfTranslateToLang.setTitle(this.getString(R.string.preferences_translate_to_settings) + ": " + toName);
		}
    	spe.commit();    	
	}	
}
