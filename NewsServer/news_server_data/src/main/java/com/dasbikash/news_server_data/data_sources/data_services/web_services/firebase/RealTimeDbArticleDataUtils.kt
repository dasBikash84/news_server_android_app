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

package com.dasbikash.news_server_data.data_sources.data_services.web_services.firebase

import com.dasbikash.news_server_data.data_sources.NewsDataService
import com.dasbikash.news_server_data.exceptions.DataNotFoundException
import com.dasbikash.news_server_data.exceptions.DataServerException
import com.dasbikash.news_server_data.models.room_entity.Article
import com.dasbikash.news_server_data.models.room_entity.Page
import com.dasbikash.news_server_data.utills.LoggerUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

internal object RealTimeDbArticleDataUtils {

    private const val DB_PUBLICATION_TIME_FIELD_NAME = "publicationTimeRTDB"

    fun getLatestArticlesByPage(page: Page, articleRequestSize: Int=1): List<Article> {

        val query = RealtimeDBUtils.mArticleDataRootReference
                            .child(page.id)
                            .orderByChild(DB_PUBLICATION_TIME_FIELD_NAME)
                            .limitToLast(articleRequestSize)
        
        return getArticlesForQuery(query, page)
    }

    fun getArticlesBeforeLastArticle(page: Page, lastArticle: Article, articleRequestSize: Int): List<Article> {
        
        val query = RealtimeDBUtils.mArticleDataRootReference
                                    .child(page.id)
                                    .orderByChild(DB_PUBLICATION_TIME_FIELD_NAME)
                                    .endAt(lastArticle.publicationTimeRTDB!!.toDouble(), DB_PUBLICATION_TIME_FIELD_NAME)
                                    .limitToLast(articleRequestSize+1)
        
        return getArticlesForQuery(query, page)
    }

    fun getArticlesAfterLastArticle(page: Page, lastArticle: Article, articleRequestSize: Int): List<Article> {

        var query = RealtimeDBUtils.mArticleDataRootReference
                        .child(page.id)
                        .orderByChild(DB_PUBLICATION_TIME_FIELD_NAME)
                        .startAt(lastArticle.publicationTimeRTDB!!.toDouble(), DB_PUBLICATION_TIME_FIELD_NAME)

        if (articleRequestSize != NewsDataService.DEFAULT_ARTICLE_REQUEST_SIZE){
            query = query.limitToFirst(articleRequestSize+1)
        }
        return getArticlesForQuery(query, page)
    }

    private fun getArticlesForQuery(query: Query,page: Page): List<Article> {

        val lock = Object()
        val articles = mutableListOf<Article>()
        var dataServerException: DataServerException? = null

        query.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                    LoggerUtils.debugLog("onCancelled. Error msg: ${error.message}",this::class.java)
                    dataServerException = DataNotFoundException(error.message)
                    synchronized(lock) { lock.notify() }
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        LoggerUtils.debugLog("onDataChange",this::class.java)
                        LoggerUtils.debugLog("data: ${dataSnapshot.value}",this::class.java)
                        dataSnapshot.children.asSequence()
                                .forEach {
                                    articles.add(it.getValue(Article::class.java)!!)
                                }
                    }
                    synchronized(lock) { lock.notify() }
                }
            })

        LoggerUtils.debugLog("getArticlesForQuery for: ${page.id} before wait",this::class.java)
        synchronized(lock) { lock.wait(NewsDataService.WAITING_MS_FOR_NET_RESPONSE) }

        LoggerUtils.debugLog("getArticlesForQuery for: ${page.id} before throw it",this::class.java)
        dataServerException?.let { throw it }

        LoggerUtils.debugLog("getArticlesForQuery for: ${page.id} before throw DataNotFoundException()",this::class.java)
        if (articles.size == 0 ){
            throw DataNotFoundException()
        }

        LoggerUtils.debugLog("getArticlesForQuery for: ${page.id} before return",this::class.java)
        return articles
    }
}