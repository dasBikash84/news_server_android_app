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

package com.dasbikash.news_server;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import com.dasbikash.news_server_data.repositories.NewsDataRepository;
import com.dasbikash.news_server_data.repositories.RepositoryFactory;

import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class RoomDatabaseInstrumentedTest {

    Context context;
    NewsDataRepository newsDataRepository;

    @Before
    public void createDb() {
        context = ApplicationProvider.getApplicationContext();
        newsDataRepository = RepositoryFactory.INSTANCE.getNewsDataRepository(context);
    }

}
