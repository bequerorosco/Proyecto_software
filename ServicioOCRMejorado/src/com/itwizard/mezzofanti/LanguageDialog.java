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

import com.itwizard.mezzofanti.Languages.Language;
import com.uah.servicioocr.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

/**
 * This dialog displays a list of languages and then tells the calling activity which language
 * was selected. 
 */
public class LanguageDialog extends AlertDialog implements OnClickListener 
{
    private LDActivity mActivity;
    private boolean mFrom;
	private static final String TAG = "MLOG: LanguageDialog.java: ";

    /**
     * Interface that allows to use the LanguageDialog by any other class (ex: TranslateActivity and PreferencesActivity)
     */
    public interface LDActivity 
    {
    	void SetNewLanguage(Language language, boolean from);    	
    };

    /**
     * constructor
     * @param activity the parent activity 
     */
    protected LanguageDialog(LDActivity activity) 
    {
        super((Activity)activity);

        mActivity = activity;
        
        LinearLayout current = null;
        Language[] languages = Language.values();
        for (int i = 0; i < languages.length; i++) {
            current = new LinearLayout((Activity)activity);
            current.setOrientation(LinearLayout.HORIZONTAL);
        }
        setTitle(" ");  // set later, but necessary to put a non-empty string here
    }

    public void onClick(View v) 
    {
        mActivity.SetNewLanguage((Language) v.getTag(), mFrom);
        Log.v(TAG,"Language " + v.getTag().toString());
    	dismiss();
    }

    /**
     * Set the from field - indicates "translate-from" / "translate-in" 
     * @param from true="translate-from" / false="translate-in"
     */
    public void SetFrom(boolean from) 
    {
        Log.v(TAG, "From set to " + from);
        mFrom = from;
        setTitle(from ? R.string.languagedialog_translateFrom : R.string.languagedialog_translateIn); 
    }
    
}
