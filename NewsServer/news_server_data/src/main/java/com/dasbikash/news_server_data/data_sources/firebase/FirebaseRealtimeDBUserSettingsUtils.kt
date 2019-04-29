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

package com.dasbikash.news_server_data.data_sources.firebase

import android.os.SystemClock
import com.dasbikash.news_server_data.data_sources.data_services.user_details_data_service.UserIpDataService
import com.dasbikash.news_server_data.exceptions.SettingsServerException
import com.dasbikash.news_server_data.exceptions.UserSettingsNotFoundException
import com.dasbikash.news_server_data.exceptions.UserSettingsUploadFailureException
import com.dasbikash.news_server_data.exceptions.WrongCredentialException
import com.dasbikash.news_server_data.models.UserSettingsUpdateDetails
import com.dasbikash.news_server_data.models.UserSettingsUpdateTime
import com.dasbikash.news_server_data.models.room_entity.UserPreferenceData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

internal object FirebaseRealtimeDBUserSettingsUtils{

    private val MAX_SETTINGS_UPLOAD_RETRY = 3
    private val MAX_SETTINGS_UPLOAD_RETRY_SLEEP_PERIOD = 5000L
    private const val FAV_PAGE_ID_LIST_NODE = "favouritePageIds"
    private const val PAGE_GROUP_LIST_NODE = "pageGroups"
    private const val UPDATE_LOG_NODE = "updateLog"

    fun getCurrentUserName():String?{
        FirebaseAuth.getInstance().currentUser?.let {
            return it.displayName
        }
        return null
    }

    fun getUserPreferenceData(): UserPreferenceData {
        val user = FirebaseAuth.getInstance().currentUser
        if (user==null || user.isAnonymous) { throw WrongCredentialException() }

        var data: UserPreferenceData? = null
        val lock = Object()

        var userSettingsException: SettingsServerException? = null

        getUserSettingsNodes(user)
                .rootUserSettingdNode
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        userSettingsException = SettingsServerException(error.message)
                        synchronized(lock) { lock.notify() }
                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            try {
                                data = dataSnapshot.getValue(UserPreferenceData::class.java)!!
                                synchronized(lock) { lock.notify() }
                            } catch (ex: Exception) {
                                userSettingsException = UserSettingsNotFoundException(ex)
                            }
                        }
                    }
                })
        synchronized(lock) { lock.wait() }
        userSettingsException?.let { throw userSettingsException as SettingsServerException }
        return data!!
    }

    private fun getUserSettingsNodes(user: FirebaseUser) = object {
        val rootUserSettingdNode = FirebaseRealtimeDBUtils.mUserSettingsRootReference.child(user.uid)
        val favPageIdListRef = FirebaseRealtimeDBUtils.mUserSettingsRootReference.child(user.uid).child(FAV_PAGE_ID_LIST_NODE)
        val pageGroupListRef = FirebaseRealtimeDBUtils.mUserSettingsRootReference.child(user.uid).child(PAGE_GROUP_LIST_NODE)
        val updateLogRef = FirebaseRealtimeDBUtils.mUserSettingsRootReference.child(user.uid).child(UPDATE_LOG_NODE)
    }

    fun uploadUserPreferenceData(userPreferenceData: UserPreferenceData){

        val user = FirebaseAuth.getInstance().currentUser
        if (user==null || user.isAnonymous) { throw WrongCredentialException() }

        val userSettingsNodes = getUserSettingsNodes(user)
        val lock = Object()
        var remainingTries = MAX_SETTINGS_UPLOAD_RETRY

        do {
            try {
                var userSettingsUploadFailureException: UserSettingsUploadFailureException? = null
                userSettingsNodes.favPageIdListRef
                        .setValue(userPreferenceData.favouritePageIds)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                synchronized(lock) { lock.notify() }
                            } else {
                                userSettingsUploadFailureException = UserSettingsUploadFailureException()
                                synchronized(lock) { lock.notify() }
                            }
                        }
                synchronized(lock) { lock.wait() }
                userSettingsUploadFailureException?.let { throw it }

                userSettingsNodes.pageGroupListRef
                        .setValue(userPreferenceData.pageGroups)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                synchronized(lock) { lock.notify() }
                            } else {
                                userSettingsUploadFailureException = UserSettingsUploadFailureException()
                                synchronized(lock) { lock.notify() }
                            }
                        }
                synchronized(lock) { lock.wait() }
                userSettingsUploadFailureException?.let { throw it }

                var ipAddress: String
                try {
                    ipAddress = UserIpDataService.getIpAddress()
                } catch (ex: Throwable) {
                    ipAddress = UserSettingsUpdateDetails.NULL_IP
                }

                userSettingsNodes.updateLogRef
                        .push()
                        .setValue(UserSettingsUpdateDetails(userIp = ipAddress))
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                synchronized(lock) { lock.notify() }
                            } else {
                                userSettingsUploadFailureException = UserSettingsUploadFailureException()
                                synchronized(lock) { lock.notify() }
                            }
                        }
                synchronized(lock) { lock.wait() }
                userSettingsUploadFailureException?.let { throw it }

                break

            } catch (ex: UserSettingsUploadFailureException) {
//                Log.d(FirebaseRealtimeDBUtils.TAG, "${remainingTries}")
                remainingTries--
                if (remainingTries == 0) {
                    throw ex
                }
                SystemClock.sleep(MAX_SETTINGS_UPLOAD_RETRY_SLEEP_PERIOD)
                continue
            }
        } while (remainingTries > 0)
    }

    fun getLastUserSettingsUpdateTime(): Long {

        val user = FirebaseAuth.getInstance().currentUser
        if (user==null || user.isAnonymous) { throw WrongCredentialException() }

        val userSettingsUpdateLogNode = getUserSettingsNodes(user).updateLogRef
        val lock = Object()

        var lastUpdateTime = 0L
        var settingsServerException: SettingsServerException? = null

        userSettingsUpdateLogNode
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(databaseError: DatabaseError) {
                        settingsServerException = SettingsServerException(databaseError.message)
                        synchronized(lock) { lock.notify() }
                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        try {
                            if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                                lastUpdateTime = dataSnapshot.children.last().getValue(UserSettingsUpdateTime::class.java)?.timeStamp ?: 0L
                            }
                        }catch (ex:Exception){
                            settingsServerException = UserSettingsNotFoundException(ex)
                        }
                        synchronized(lock) { lock.notify() }
                    }
                })
        try {
            synchronized(lock) { lock.wait() }
        }catch (ex:InterruptedException){
            ex.printStackTrace()
        }

        settingsServerException?.let { throw it }

        return lastUpdateTime
    }
}