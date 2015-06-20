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

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.widget.Button;

import java.util.Map;

import com.uah.servicioocr.R;


/**
 * Language information for the Google Translate API.
 */
public final class Languages {
    
    /**
     * Reference at http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
     */
    public static enum Language {
                
        BULGARIAN("bg", "Bulgarian", R.drawable.bg),
        CATALAN("ca", "Catalan",R.drawable.catalonia),
        CHINESE("zh", "Chinese", R.drawable.cn),
        CHINESE_SIMPLIFIED("zh-CN", "Chinese simplified", R.drawable.cn),
        CHINESE_TRADITIONAL("zh-TW", "Chinese traditional", R.drawable.tw),
        CROATIAN("hr", "Croatian", R.drawable.hr),
        CZECH("cs", "Czech", R.drawable.cs),
        DANISH("da", "Danish", R.drawable.dk),        
        DUTCH("nl", "Dutch", R.drawable.nl),
        ENGLISH("en", "English", R.drawable.us),
        FILIPINO("tl", "Filipino", R.drawable.ph),
        FINNISH("fi", "Finnish", R.drawable.fi),
        FRENCH("fr", "French", R.drawable.fr),
        GERMAN("de", "German", R.drawable.de),
        GREEK("el", "Greek", R.drawable.gr),
        INDONESIAN("id", "Indonesian", R.drawable.id),        
        ITALIAN("it", "Italian", R.drawable.it),
        JAPANESE("ja", "Japanese", R.drawable.jp),
        KOREAN("ko", "Korean", R.drawable.kr),
        LITHUANIAN("lt", "Lithuanian", R.drawable.lt),
        NORWEGIAN("no", "Norwegian", R.drawable.no),
        POLISH("pl", "Polish", R.drawable.pl),
        PORTUGUESE("pt", "Portuguese", R.drawable.pt),
        ROMANIAN("ro", "Romanian", R.drawable.ro),
        RUSSIAN("ru", "Russian", R.drawable.ru),
        SERBIAN("sr", "Serbian", R.drawable.sr),
        SLOVAK("sk", "Slovak", R.drawable.sk),
        SLOVENIAN("sl", "Slovenian", R.drawable.sl),
        SPANISH("es", "Spanish", R.drawable.es),
        SWEDISH("sv", "Swedish", R.drawable.sv),
        TAGALOG("tl", "Tagalog", R.drawable.ph),
        UKRAINIAN("uk", "Ukrainian", R.drawable.ua),
        ;
        
        private String mShortName;
        private String mLongName;
        private int mFlag;
        
        public static Map<String, String> mLongNameToShortName = Maps.newHashMap();
        public static Map<String, Language> mShortNameToLanguage = Maps.newHashMap();
        
        static 
        {
            for (Language language : values()) {
                mLongNameToShortName.put(language.getLongName(), language.getShortName());
                mShortNameToLanguage.put(language.getShortName(), language);
            }
        }
        
        private Language(String shortName, String longName, int flag) {
            init(shortName, longName, flag);
        }
        
        private Language(String shortName, String longName) {
            init(shortName, longName, -1);
        }

        private void init(String shortName, String longName, int flag) {
            mShortName = shortName;
            mLongName = longName;
            mFlag = flag;
            
        }

        public String getShortName() {
            return mShortName;
        }

        public String getLongName() {
            return mLongName;
        }
        
        public int getFlag() {
            return mFlag;
        }

        @Override
        public String toString() {
            return mLongName;
        }
        
        public static Language findLanguageByShortName(String shortName) {
            return mShortNameToLanguage.get(shortName);
        }
        
        public void configureButton(Activity activity, Button button) {
            button.setTag(this);
            button.setText(getLongName());
            int f = getFlag();
            if (f != -1) {
                Drawable flag = ((Activity)activity).getResources().getDrawable(f);
                button.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
                button.setCompoundDrawablePadding(5);
            }
        }
    }

    public static String getShortName(String longName) {
        return Language.mLongNameToShortName.get(longName);
    }

}

