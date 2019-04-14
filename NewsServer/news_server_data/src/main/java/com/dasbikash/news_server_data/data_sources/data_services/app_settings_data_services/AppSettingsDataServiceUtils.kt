/*
 * Copyright 2019 das.bikash.dev@gmail.com. All rights reserved.
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

package com.dasbikash.news_server_data.data_sources.data_services.app_settings_data_services

import android.content.Context
import android.util.Log
import com.dasbikash.news_server.utils.SharedPreferenceUtils
import com.dasbikash.news_server_data.display_models.entity.DefaultAppSettings
import com.dasbikash.news_server_data.display_models.entity.Newspaper
import com.dasbikash.news_server_data.display_models.entity.Page

internal object AppSettingsDataServiceUtils {

    val TAG = "DbTest"

    fun processDefaultAppSettingsData(defaultAppSettings: DefaultAppSettings):
            DefaultAppSettings {

        defaultAppSettings.newspapers?.let {

            val filteredNewspaperMap = HashMap<String, Newspaper>()
            val filteredPageMap = HashMap<String, Page>()


            it.values
                    .filter { it.active }
                    .forEach {
                        filteredNewspaperMap.put(it.id, it)
                    }
            defaultAppSettings.newspapers = filteredNewspaperMap

            val inActiveNewspaperIds =
                    it.values.filter { !it.active }.map { it.id }.toCollection(mutableListOf<String>())

            Log.d(TAG,"inActiveNewspaperIds: ${inActiveNewspaperIds.size}")


            defaultAppSettings.pages?.let {

                val allPages = it.values

                val inactiveTopPageIds =
                        allPages
                                .asSequence()
                                .filter { it.parentPageId == Page.TOP_LEVEL_PAGE_PARENT_ID && !it.active }
                                .map { it.id }
                                .toCollection(mutableListOf<String>())

                Log.d(TAG,"inactiveTopPageIds: ${inactiveTopPageIds.size}")
                allPages
                        .asSequence()
                        .filter {
                            it.active &&
                            !inActiveNewspaperIds.contains(it.newsPaperId) &&
                            !inactiveTopPageIds.contains(it.parentPageId)
                        }
                        .forEach { filteredPageMap.put(it.id, it) }

                filteredPageMap
                        .values
                        .filter {
                            it.parentPageId == Page.TOP_LEVEL_PAGE_PARENT_ID
                        }
                        .forEach {
                            val thisPage = it
                            if (filteredPageMap.values.count {it.parentPageId == thisPage.id} > 0){
                                thisPage.hasChild = true
                            }
                        }

                defaultAppSettings.pages = filteredPageMap
                Log.d(TAG,"filteredPageMap: ${filteredPageMap.size}")

            }
        }
        return defaultAppSettings
    }

    fun getLocalAppSettingsUpdateTime(context: Context): Long{
        return SharedPreferenceUtils.getLocalAppSettingsUpdateTimestamp(context)
    }
    fun saveLocalAppSettingsUpdateTime(context: Context, updateTime:Long){
        return SharedPreferenceUtils.saveGlobalSettingsUpdateTimestamp(context,updateTime)
    }

}