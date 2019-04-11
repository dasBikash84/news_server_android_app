/*
 * Copyright 2019 www.dasbikash.org. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dasbikash.news_server.repositories;

import android.content.Context;
import android.util.Log;

import com.dasbikash.news_server.data_sources.AppSettingsDataService;
import com.dasbikash.news_server.data_sources.DataServiceImplProvider;
import com.dasbikash.news_server.data_sources.UserSettingsDataService;
import com.dasbikash.news_server.database.NewsServerDatabase;
import com.dasbikash.news_server.display_models.entity.Country;
import com.dasbikash.news_server.display_models.entity.DefaultAppSettings;
import com.dasbikash.news_server.display_models.entity.Language;
import com.dasbikash.news_server.display_models.entity.Newspaper;
import com.dasbikash.news_server.display_models.entity.Page;
import com.dasbikash.news_server.display_models.entity.PageGroup;
import com.dasbikash.news_server.exceptions.NoInternertConnectionException;
import com.dasbikash.news_server.exceptions.OnMainThreadException;
import com.dasbikash.news_server.utils.SharedPreferenceUtils;

import java.util.ArrayList;
import java.util.List;

public final class HomeViewModelRepo {

    private static final String TAG = "InitFragment";

    private NewsServerDatabase mDatabase;

//    private SettingsDataService mSettingsDataService;
    private Context mContext;

    private AppSettingsDataService mAppSettingsDataService;
    private UserSettingsDataService mUserSettingsDataService;


    public HomeViewModelRepo(final Context context) {
        mDatabase = NewsServerDatabase.getDatabase(context);
//        mSettingsDataService = new SettingsDataService(context);
        mAppSettingsDataService = DataServiceImplProvider.getAppSettingsDataServiceImpl();
        mUserSettingsDataService = DataServiceImplProvider.getUserSettingsDataServiceImpl();
        mContext = context;
    }

    public boolean isAppSettingsUpdated() {
        long localAppSettingsUpdateTime = getLocalAppSettingsUpdateTime();
        long serverAppSettingsUpdateTime = getServerAppSettingsUpdateTime();
        return serverAppSettingsUpdateTime > localAppSettingsUpdateTime;
    }

    private long getLocalAppSettingsUpdateTime() {
        long appSettingsUpdateTimestamp = SharedPreferenceUtils.INSTANCE.getAppSettingsUpdateTimestamp(mContext);
        Log.d(TAG, "getLocalAppSettingsUpdateTime: "+appSettingsUpdateTimestamp);
        return appSettingsUpdateTimestamp;
    }

    private long getServerAppSettingsUpdateTime() {
        Long serverAppSettingsUpdateTime = mAppSettingsDataService.getAppSettingsUpdateTime(mContext);
        Log.d(TAG, "getServerAppSettingsUpdateTime: "+serverAppSettingsUpdateTime);
        SharedPreferenceUtils.INSTANCE.saveGlobalSettingsUpdateTimestamp(mContext,serverAppSettingsUpdateTime);
        return serverAppSettingsUpdateTime;
    }

    public boolean isSettingsDataLoaded() {
        return getLanguageCount()>0 && getCountryCount()>0 &&
                getNewsPaperCount()>0 && getPageCount()>0 &&
                getPageGroupCount()>0;
    }

    private int getCountryCount() {
        int count = mDatabase.getCountryDao().getCount();
        Log.d(TAG, "getCountryCount: "+count);
        return count;
    }

    private int getLanguageCount() {
        int count = mDatabase.getLanguageDao().getCount();
        Log.d(TAG, "getLanguageCount: "+count);
        return count;
    }

    private int getNewsPaperCount() {
        int count = mDatabase.getNewsPaperDao().getCount();
        Log.d(TAG, "getNewsPaperCount: "+count);
        return count;
    }

    private int getPageCount() {
        int count = mDatabase.getPageDao().getCount();
        Log.d(TAG, "getPageCount: "+count);
        return count;
    }

    private int getPageGroupCount() {
        int count = mDatabase.getPageGroupDao().getCount();
        Log.d(TAG, "getPageGroupCount: "+count);
        return count;
    }

    public void loadAppSettings()
            throws OnMainThreadException, NoInternertConnectionException {
        Log.d(TAG, "loadAppSettings: ");
        DefaultAppSettings appSettings =
                mAppSettingsDataService.getAppSettings(mContext);
//                mSettingsDataService.getServerAppSettings();

        List<Language> languages = new ArrayList<>(appSettings.getLanguages().values());
        mDatabase.getLanguageDao().addLanguages(languages);
        Log.d(TAG, "loadAppSettings: languages"+languages);

        ArrayList<Country> countries = new ArrayList<>(appSettings.getCountries().values());
        mDatabase.getCountryDao().addCountries(countries);
        Log.d(TAG, "loadAppSettings: countries"+countries);

        ArrayList<Newspaper> newspapers = new ArrayList<>(appSettings.getNewspapers().values());
        mDatabase.getNewsPaperDao().addNewsPapers(newspapers);
        Log.d(TAG, "loadAppSettings: newspapers"+newspapers);

        ArrayList<Page> pages = new ArrayList<>(appSettings.getPages().values());
        mDatabase.getPageDao().addPages(pages);
        Log.d(TAG, "loadAppSettings: pages"+pages);

        ArrayList<PageGroup> newsCategories = new ArrayList<>(appSettings.getPage_groups().values());
        mDatabase.getPageGroupDao().addPageGroups(newsCategories);
        Log.d(TAG, "loadAppSettings: newsCategories"+newsCategories);

        ArrayList<Long> settingUpdateTimes = new ArrayList<>(appSettings.getUpdate_time().values());
        SharedPreferenceUtils.INSTANCE.saveGlobalSettingsUpdateTimestamp(mContext,settingUpdateTimes.get(settingUpdateTimes.size()-1));
    }
}
