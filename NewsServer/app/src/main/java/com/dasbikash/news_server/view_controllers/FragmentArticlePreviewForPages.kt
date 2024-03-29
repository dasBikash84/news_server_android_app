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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.dasbikash.news_server.R
import com.dasbikash.news_server.view_controllers.view_helpers.PagePreviewListAdapter
import com.dasbikash.news_server.view_models.HomeViewModel
import com.dasbikash.news_server_data.models.room_entity.Page


class FragmentArticlePreviewForPages : Fragment() {

    val mPageList = mutableListOf<Page>()
    @LayoutRes
    private var mArticlePreviewResId: Int? = 0
    private var mShowNewsPaperName: Int? = 0

    private lateinit var mPageListPreviewHolderRV:RecyclerView

    private lateinit var mPagePreviewListAdapter: PagePreviewListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_page_list_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPageList.addAll(arguments!!.getParcelableArrayList<Page>(ARG_PAGE_LIST)!!)
        mArticlePreviewResId = arguments!!.getInt(ARG_ARTICLE_PREVIEW_RES_ID)
        mShowNewsPaperName = arguments!!.getInt(ARG_SHOW_NP_NAME_FLAG)

        mPageListPreviewHolderRV = view.findViewById(R.id.mPageListPreviewHolder)

        if (mPageList.size == 1) {
            mPageListPreviewHolderRV.minimumWidth = resources.displayMetrics.widthPixels
        }

        mPagePreviewListAdapter =
                PagePreviewListAdapter(this,mArticlePreviewResId!!,ViewModelProviders.of(activity!!).get(HomeViewModel::class.java),
                                            mShowNewsPaperName!!, PAGE_TITLE_LINE_COUNT)

        mPageListPreviewHolderRV.adapter = mPagePreviewListAdapter
    }

    override fun onResume() {
        super.onResume()

        mPagePreviewListAdapter.submitList(mPageList.filter {
            @Suppress("SENSELESS_COMPARISON")
            it!=null
        }.toList())
    }

    companion object {

        val ARG_PAGE_LIST = "com.dasbikash.news_server.views.FragmentArticlePreviewForPages.ARG_PAGE_LIST"
        val ARG_ARTICLE_PREVIEW_RES_ID = "com.dasbikash.news_server.views.FragmentArticlePreviewForPages.ARG_ARTICLE_PREVIEW_RES_ID"
        val ARG_SHOW_NP_NAME_FLAG = "com.dasbikash.news_server.views.FragmentArticlePreviewForPages.ARG_SHOW_NP_NAME_FLAG"
        val PAGE_TITLE_LINE_COUNT = 2

        private fun getInstance(pages: List<Page>, @LayoutRes resId: Int,showNewsPaperName:Int): FragmentArticlePreviewForPages {

            if (pages.isEmpty()) {
                throw IllegalArgumentException()
            }

            val args = Bundle()
            args.putParcelableArrayList(ARG_PAGE_LIST, ArrayList(pages))
            args.putInt(ARG_ARTICLE_PREVIEW_RES_ID, resId)
            args.putInt(ARG_SHOW_NP_NAME_FLAG, showNewsPaperName)
            val fragment = FragmentArticlePreviewForPages()
            fragment.setArguments(args)

            return fragment
        }

        fun getInstanceForScreenFillPreview(page: Page,showNewsPaperName:Int=0): FragmentArticlePreviewForPages {
            return getInstance(listOf(page), R.layout.view_article_preview_holder_parent_width,showNewsPaperName)
        }

        fun getInstanceForCustomWidthPreview(pages: List<Page>,showNewsPaperName:Int=0): FragmentArticlePreviewForPages {
            return getInstance(pages, R.layout.view_article_preview_holder_custom_width,showNewsPaperName)
        }

    }
}