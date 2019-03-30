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

package com.dasbikash.news_server.display_models.entity

import java.io.Serializable

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dasbikash.news_server.display_models.mapped_embedded.ImageLinkList

@Entity(
        foreignKeys = [
            ForeignKey(entity = Page::class, parentColumns = ["id"], childColumns = ["pageId"])
        ],
        indices = [
            Index(value = ["pageId"], name = "article_page_id_index")
        ]
)
data class Article  (
    @PrimaryKey
    val id: Int,
    val pageId: Int,
    val title: String,
    val lastModificationTS: Long,
    val imageLinkList: ImageLinkList
): Serializable