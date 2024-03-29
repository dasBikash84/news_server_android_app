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

package com.dasbikash.news_server.view_controllers.view_helpers

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dasbikash.news_server.R
import com.dasbikash.news_server.utils.DisplayUtils
import com.dasbikash.news_server.view_controllers.PageViewActivity
import com.dasbikash.news_server.view_models.HomeViewModel
import com.dasbikash.news_server_data.exceptions.DataNotFoundException
import com.dasbikash.news_server_data.exceptions.DataServerException
import com.dasbikash.news_server_data.exceptions.NoInternertConnectionException
import com.dasbikash.news_server_data.models.room_entity.Article
import com.dasbikash.news_server_data.models.room_entity.Page
import com.dasbikash.news_server_data.repositories.RepositoryFactory
import com.dasbikash.news_server_data.utills.ImageLoadingDisposer
import com.dasbikash.news_server_data.utills.ImageUtils
import com.dasbikash.news_server_data.utills.LoggerUtils
import com.dasbikash.news_server_data.utills.NetConnectivityUtility
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.CompositeException
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.util.*

class PagePreviewListAdapter(lifecycleOwner: LifecycleOwner, @LayoutRes val holderResId: Int,
                             val homeViewModel: HomeViewModel, val showNewsPaperName: Int = 0, val pageTitleLineCount: Int = 1) :
        ListAdapter<Page, LatestArticlePreviewHolder>(PageDiffCallback), DefaultLifecycleObserver {

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    val mDisposable = CompositeDisposable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LatestArticlePreviewHolder {
        val view = LayoutInflater.from(parent.context).inflate(holderResId, parent, false)
        return LatestArticlePreviewHolder(view, showNewsPaperName, pageTitleLineCount, mDisposable)
    }

    override fun onBindViewHolder(holder: LatestArticlePreviewHolder, position: Int) {

        val page = getItem(position)
        holder.disableDisplay(page)

        val uuid = UUID.randomUUID()
        val appSettingsRepository = RepositoryFactory.getAppSettingsRepository(holder.itemView.context)

        mDisposable.add(
                homeViewModel.getLatestArticleProvider(Pair(uuid, page))
                        .filter { it.first == uuid }
                        .map {
                            it.second?.let {
                                val dateString = DisplayUtils
                                        .getArticlePublicationDateString(
                                                it, appSettingsRepository.getLanguageByPage(page), holder.itemView.context)
                                return@map Pair(dateString, it)
                            }
                            return@map Any()
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object : DisposableObserver<Any>() {
                            override fun onComplete() {
                                LoggerUtils.debugLog("onComplete for page ${page.name} Np: ${page.newspaperId} L2", this::class.java)
                            }

                            @Suppress("UNCHECKED_CAST")
                            override fun onNext(articleData: Any) {
                                if (articleData is Pair<*, *>) {
                                    LoggerUtils.debugLog("art displayed for page: ${page.name} Np: ${page.newspaperId}", this::class.java)
                                    val articleDataResult = articleData as Pair<String?, Article>
                                    holder.bind(page, articleDataResult.first, articleDataResult.second)
                                }
                            }

                            override fun onError(e: Throwable) {
                                if (e is CompositeException) {
                                    if (e.exceptions.filter { it is NoInternertConnectionException }.count() > 0) {
                                        NetConnectivityUtility.showNoInternetToast(holder.itemView.context)
                                    } else if (e.exceptions.filter { it is DataNotFoundException }.count() > 0) {
                                        LoggerUtils.debugLog("DataNotFoundException", this::class.java)
                                    } else if (e.exceptions.filter { it is DataServerException }.count() > 0) {
                                        LoggerUtils.debugLog("DataServerException", this::class.java)
                                    }
                                }
                                LoggerUtils.debugLog(e.message + "${e::class.java.simpleName} for page Np: ${page.newspaperId} ${page.name} L2", this::class.java)
                            }
                        })
        )
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        LoggerUtils.debugLog("Disposing", this::class.java)
        mDisposable.clear()
    }

    override fun onResume(owner: LifecycleOwner) {
        LoggerUtils.debugLog("onResume", this::class.java)
        notifyDataSetChanged()
    }

    override fun onPause(owner: LifecycleOwner) {
        LoggerUtils.debugLog("Disposing", this::class.java)
        mDisposable.clear()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        LoggerUtils.debugLog("Disposing", this::class.java)
        mDisposable.clear()
    }

    override fun onViewRecycled(holder: LatestArticlePreviewHolder) {
        super.onViewRecycled(holder)
        holder.disposeImageLoader()
    }

}

class LatestArticlePreviewHolder(itemView: View, val showNewsPaperName: Int = 0
                                 , pageTitleLineCount: Int = 1, val compositeDisposable: CompositeDisposable)
    : RecyclerView.ViewHolder(itemView) {

    val pageTitle: TextView
    val articlePreviewImage: ImageView
    val articleTitle: TextView
    val articlePublicationTime: TextView

    val articlePreviewImagePlaceHolder: ImageView
    val articleTitlePlaceHolder: TextView
    val articlePublicationTimePlaceHolder: TextView


    var imageLoadingDisposer: Disposable? = null

    lateinit var mArticle: Article

    init {
        pageTitle = itemView.findViewById(R.id.page_title)
        articlePreviewImage = itemView.findViewById(R.id.article_preview_image)
        articleTitle = itemView.findViewById(R.id.article_title)
        articlePublicationTime = itemView.findViewById(R.id.article_time)

        articlePreviewImagePlaceHolder = itemView.findViewById(R.id.article_preview_ph_image)
        articleTitlePlaceHolder = itemView.findViewById(R.id.article_title_ph)
        articlePublicationTimePlaceHolder = itemView.findViewById(R.id.article_time_ph)

        pageTitle.setLines(pageTitleLineCount)
        itemView.setOnClickListener {}
    }

    lateinit var mPage: Page

    fun disposeImageLoader() {
        imageLoadingDisposer?.dispose()
    }

    @SuppressLint("CheckResult")
    fun disableDisplay(page: Page) {

        showPlaceHolders()
        itemView.setOnClickListener {}

        mPage = page

        if (showNewsPaperName > 0) {
            Observable.just(page)
                    .subscribeOn(Schedulers.io())
                    .map {
                        val appSettingsRepository = RepositoryFactory.getAppSettingsRepository(itemView.context)
                        appSettingsRepository.getNewspaperByPage(page)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        pageTitle.text = StringBuilder(page.name!!).append(" | ").append(it.name).toString()
                        pageTitle.visibility = View.VISIBLE
                    }
        } else {
            pageTitle.text = page.name
            pageTitle.visibility = View.VISIBLE
        }
    }

    fun bind(page: Page, dateString: String?, article: Article) {

        mArticle = article

        articleTitle.text = mArticle.title
        showPageTitle()
        articlePublicationTime.text = dateString
        showArticlePublicationTime()

        LoggerUtils.debugLog("Page: ${page.name!!} dateString: ${dateString}", this::class.java)

        imageLoadingDisposer = ImageLoadingDisposer(articlePreviewImage)
        compositeDisposable.add(imageLoadingDisposer!!)
        ImageUtils.customLoader(articlePreviewImage, mArticle.previewImageLink,
                R.drawable.pc_bg, R.drawable.app_big_logo,
                {
                    compositeDisposable.delete(imageLoadingDisposer!!)
                    imageLoadingDisposer = null
                    showArticlePreviewImage()
                })

        //Add click listner
        itemView.setOnClickListener(View.OnClickListener {
            itemView.context.startActivity(
                    PageViewActivity.getIntentForPageDisplay(
                            itemView.context, page
                    )
            )
        })
    }

    private fun showPlaceHolders() {
        hideArticlePreviewImage()
        hideArticleTitle()
        hideArticlePublicationTime()
    }

    private fun showPageTitle() {
        articleTitle.visibility = View.VISIBLE
        articleTitlePlaceHolder.visibility = View.GONE
    }

    private fun showArticlePublicationTime() {
        articlePublicationTime.visibility = View.VISIBLE
        articlePublicationTimePlaceHolder.visibility = View.GONE
    }

    private fun showArticlePreviewImage() {
        articlePreviewImage.visibility = View.VISIBLE
        articlePreviewImagePlaceHolder.visibility = View.GONE
    }

    private fun hideArticlePreviewImage() {
        articlePreviewImage.visibility = View.GONE
        articlePreviewImagePlaceHolder.visibility = View.VISIBLE
    }

    private fun hideArticleTitle() {
        articleTitle.visibility = View.GONE
        articleTitlePlaceHolder.visibility = View.VISIBLE
    }

    private fun hideArticlePublicationTime() {
        articlePublicationTime.visibility = View.GONE
        articlePublicationTimePlaceHolder.visibility = View.VISIBLE
    }
}