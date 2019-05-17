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

package com.dasbikash.news_server.view_controllers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.dasbikash.news_server.R
import com.dasbikash.news_server.utils.DialogUtils
import com.dasbikash.news_server.utils.DisplayUtils
import com.dasbikash.news_server.utils.OptionsIntentBuilderUtility
import com.dasbikash.news_server.view_controllers.interfaces.HomeNavigator
import com.dasbikash.news_server.view_controllers.interfaces.NavigationHost
import com.dasbikash.news_server.view_controllers.interfaces.WorkInProcessWindowOperator
import com.dasbikash.news_server_data.repositories.RepositoryFactory
import com.dasbikash.news_server_data.repositories.UserSettingsRepository
import com.dasbikash.news_server_data.utills.NetConnectivityUtility
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.StringBuilder

class HomeActivity : AppCompatActivity(),
        NavigationHost, HomeNavigator, SignInHandler,WorkInProcessWindowOperator {

    private lateinit var mToolbar: Toolbar
    private lateinit var mAppBar: AppBarLayout
    private lateinit var mCoordinatorLayout: CoordinatorLayout

    private lateinit var mUserSettingsRepository:UserSettingsRepository

    private lateinit var mLogInMenuHolder:ConstraintLayout

    private lateinit var mLogInButton:MaterialButton
    private lateinit var mUserDetailsTextView: AppCompatTextView
    private lateinit var mSignOutButton:MaterialButton
//    private lateinit var mUserSettingEditButton:MaterialButton

    private val LOG_IN_REQ_CODE = 7777

    override fun showBottomNavigationView(show: Boolean) {
        when (show) {
            true -> mBottomNavigationView.visibility = View.VISIBLE
            false -> mBottomNavigationView.visibility = View.GONE
        }
    }

    val mBottomNavigationView: BottomNavigationView by lazy {
        findViewById(R.id.bottomNavigationView) as BottomNavigationView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        mUserSettingsRepository = RepositoryFactory.getUserSettingsRepository(this)

        findViewItems()
        setSupportActionBar(mToolbar)
        setViewItemOnClickListners()
        setUpBottomNavigationView()
        initApp()

        if (supportFragmentManager.findFragmentById(R.id.main_frame) == null) {
            mBottomNavigationView.visibility = View.INVISIBLE
            mAppBar.visibility = View.INVISIBLE
            loadInitFragment()
        }
    }

    private fun setViewItemOnClickListners() {
        mLogInButton.setOnClickListener {
            launchSignInActivity()
            mLogInMenuHolder.visibility = View.GONE
        }
        mSignOutButton.setOnClickListener {
            launchSignOutDialog()
            mLogInMenuHolder.visibility = View.GONE
        }
        mUserDetailsTextView.setOnClickListener({})
//        mUserSettingEditButton.setOnClickListener {
//            launchUserSettingEditDialog()
//            mLogInMenuHolder.visibility = View.GONE
//        }

        mLogInMenuHolder.setOnClickListener { mLogInMenuHolder.visibility = View.GONE }
    }

    private fun findViewItems() {
        mAppBar = findViewById(R.id.app_bar_layout)
        mToolbar = findViewById(R.id.app_bar)
        mLogInMenuHolder = findViewById(R.id.log_in_menu_holder)
        mCoordinatorLayout = findViewById(R.id.activity_home_coordinator_container)

        mLogInButton = findViewById(R.id.log_in_sign_up_button)
        mUserDetailsTextView = findViewById(R.id.user_name_text)
        mSignOutButton = findViewById(R.id.sign_out_button)
//        mUserSettingEditButton = findViewById(R.id.customize_button)
    }

    private fun initApp() {
        NetConnectivityUtility.initialize(applicationContext)
    }


    private fun setUpBottomNavigationView() {

        mBottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            var handled: Boolean
            handled = when (menuItem.itemId) {
                R.id.bottom_menu_item_home -> {
                    if (!(supportFragmentManager.findFragmentById(R.id.main_frame) is HomeFragment)) {
                        mAppBar.visibility = View.GONE
                        loadHomeFragment()
                    }
                    true
                }
                R.id.bottom_menu_item_page_group -> {
                    if (!(supportFragmentManager.findFragmentById(R.id.main_frame) is PageGroupFragment)) {
                        mAppBar.visibility = View.VISIBLE
                        loadPageGroupFragment()
                    }
                    true
                }
                R.id.bottom_menu_item_favourites -> {
                    if (!(supportFragmentManager.findFragmentById(R.id.main_frame) is FavouritesFragment)) {
                        mAppBar.visibility = View.VISIBLE
                        loadFavouritesFragment()
                    }
                    true
                }
                R.id.bottom_menu_item_saved_articles -> {
                    if (!(supportFragmentManager.findFragmentById(R.id.main_frame) is SavedArticlesFragment)) {
                        mAppBar.visibility = View.VISIBLE
                        loadSavedArticlesFragment()
                    }
                    true
                }
                /*R.id.bottom_menu_item_more -> {
                    if (!(supportFragmentManager.findFragmentById(R.id.main_frame) is MoreFragment)) {
                        mAppBar.visibility = View.VISIBLE
                        loadMoreFragment()
                    }
                    true
                }*/

                else -> false
            }
            handled
        }
    }

    override fun addFragment(fragment: Fragment) {
        val transaction = supportFragmentManager
                .beginTransaction()
                .add(R.id.main_frame, fragment)
        transaction.commit()
    }

    override fun removeFragment(fragment: Fragment) {
        val transaction = supportFragmentManager
                .beginTransaction()
                .remove(fragment)

        transaction.commit()

    }

    override fun showAppBar(show: Boolean) {
        if(show) {
            mAppBar.visibility = View.VISIBLE
        }else{
            mAppBar.visibility = View.GONE
        }
    }

    override fun disableBackPress(disable: Boolean) {
        disableBackPressFlag = disable
    }
    var disableBackPressFlag = false
    var mWaitWindowShown = false
    var mWaitWindow :Fragment? = null
    override fun loadWorkInProcessWindow() {
        mWaitWindow = FragmentWorkInProcess()
        showBottomNavigationView(false)
        addFragment(mWaitWindow!!)
        mWaitWindowShown = true
        disableBackPress(true)
    }

    override fun removeWorkInProcessWindow() {
        mWaitWindowShown = false
        removeFragment(mWaitWindow!!)
        mWaitWindow = null
        showBottomNavigationView(true)
        disableBackPress(false)
    }

    override fun onBackPressed() {
        if (!disableBackPressFlag) {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        disableBackPressFlag = false
    }

    /**
     * Trigger a navigation to the specified fragment, optionally adding a transaction to the back
     * stack to make this navigation reversible.
     *
     * @param fragment
     * @param addToBackstack
     */
    override fun navigateTo(fragment: Fragment, addToBackstack: Boolean) {
        if (!mWaitWindowShown) {
            val transaction = supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_frame, fragment)

            if (addToBackstack) {
                transaction.addToBackStack(null)
            }

            transaction.commit()
        }
    }

    fun loadInitFragment() {
        navigateTo(InitFragment())
    }

    override fun loadHomeFragment() {
        navigateTo(HomeFragment())
        mBottomNavigationView.visibility = View.VISIBLE
    }

    override fun loadPageGroupFragment() {
        navigateTo(PageGroupFragment())
    }

    override fun loadFavouritesFragment() {
        navigateTo(FavouritesFragment())
    }

    override fun loadSavedArticlesFragment() {
        navigateTo(SavedArticlesFragment())
    }

    override fun loadMoreFragment() {
        navigateTo(MoreFragment())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!mWaitWindowShown) {
            when (item.itemId) {
                R.id.share_app_menu_item -> {
                    shareAppMenuItemAction()
                    return true
                }
                R.id.settings_menu_item -> {
                    loadSavedArticlesFragment()
                    return true
                }
                R.id.log_in_app_menu_item -> {
                    logInAppMenuItemAction()
                    return true
                }
            }
        }
        return false
    }

    private fun launchSignOutDialog(){
        DialogUtils.createAlertDialog(this, DialogUtils.AlertDialogDetails(
                title = "Sign Out?",positiveButtonText = "Yes",negetiveButtonText = "Cancel",doOnPositivePress = { signOutAction() }
        )).show()
    }

    private fun launchUserSettingEditDialog() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun logInAppMenuItemAction() {

        if (mLogInMenuHolder.visibility == View.GONE){
            mLogInMenuHolder.visibility = View.VISIBLE
            mLogInMenuHolder.bringToFront()
            if(mUserSettingsRepository.checkIfLoggedIn()){
                mSignOutButton.visibility = View.VISIBLE
                mLogInButton.visibility = View.GONE
                mUserDetailsTextView.visibility = View.GONE
                mUserSettingsRepository.getCurrentUserName()?.let {
                    DisplayUtils.displayHtmlText(mUserDetailsTextView,StringBuilder("Logged in as <u>").append(it).append("</u>").toString())
                    mUserDetailsTextView.visibility = View.VISIBLE
                }
            }else{
                mSignOutButton.visibility = View.GONE
                mLogInButton.visibility = View.VISIBLE
                mUserDetailsTextView.visibility = View.GONE
            }
        }else{
            mLogInMenuHolder.visibility = View.GONE
        }
    }

    var actionAfterSuccessfulLogIn : (() -> Unit)? = null

    override fun launchSignInActivity(doOnSignIn:()->Unit) {
        val intent = mUserSettingsRepository.getLogInIntent()
        intent?.let {
            startActivityForResult(intent, LOG_IN_REQ_CODE)
        }
        actionAfterSuccessfulLogIn = doOnSignIn
    }

    private fun signOutAction() {
        mUserSettingsRepository.signOutUser()
        Snackbar.make(mCoordinatorLayout,"You have just signed out",Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_layout_basic, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun shareAppMenuItemAction() {
        startActivity(OptionsIntentBuilderUtility.getShareAppIntent(this))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOG_IN_REQ_CODE) {

            Observable.just(Pair(resultCode,data))
                    .subscribeOn(Schedulers.io())
                    .map { mUserSettingsRepository.processSignInRequestResult(it,this) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<Pair<UserSettingsRepository.SignInResult, Throwable?>> {
                        override fun onComplete() {
                            actionAfterSuccessfulLogIn = null
                        }
                        override fun onSubscribe(d: Disposable) {
                        }
                        override fun onNext(processingResult: Pair<UserSettingsRepository.SignInResult,Throwable?>) {
                            when(processingResult.first){
                                UserSettingsRepository.SignInResult.SUCCESS -> {
                                    Log.d(TAG,"User settings data saved.")
                                    actionAfterSuccessfulLogIn?.let {
                                        it()
                                    }
                                }
                                UserSettingsRepository.SignInResult.USER_ABORT -> Log.d(TAG,"Log in canceled by user")
                                UserSettingsRepository.SignInResult.SERVER_ERROR -> Log.d(TAG,"Log in error. Details:${processingResult.second}")
                                UserSettingsRepository.SignInResult.SETTINGS_UPLOAD_ERROR -> Log.d(TAG,"Error while saving User settings data. Details:${processingResult.second}")
                            }
                        }
                        override fun onError(e: Throwable) {
                            actionAfterSuccessfulLogIn = null
                            Log.d(TAG,"Error while User settings data saving. Error: ${e}")
                        }
                    })
        }
    }
    companion object{
        val TAG = "HomeActivity"
    }

}

interface SignInHandler{
    fun launchSignInActivity(doOnSignIn:()->Unit = {})
}
