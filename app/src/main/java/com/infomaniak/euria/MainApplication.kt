/*
 * Infomaniak Euria - Android
 * Copyright (C) 2025 Infomaniak Network SA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.infomaniak.euria

import android.app.Application
import com.infomaniak.core.AssociatedUserDataCleanable
import com.infomaniak.core.crossapplogin.back.internal.deviceinfo.DeviceInfoUpdateManager
import com.infomaniak.core.network.ApiEnvironment
import com.infomaniak.core.network.NetworkConfiguration
import com.infomaniak.core.sentry.SentryConfig.configureSentry
import com.infomaniak.euria.services.DeviceInfoUpdateWorker
import com.infomaniak.euria.utils.AccountUtils
import com.infomaniak.euria.utils.NotificationUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import splitties.init.injectAsAppCtx
import javax.inject.Inject

@HiltAndroidApp
open class MainApplication : Application() {

    @Inject
    lateinit var notificationUtils: NotificationUtils

    val applicationScope = CoroutineScope(Dispatchers.Default + CoroutineName("MainApplication"))

    init {
        injectAsAppCtx()
        
        // New modules configuration
        NetworkConfiguration.init(
            appId = BuildConfig.APPLICATION_ID,
            appVersionCode = BuildConfig.VERSION_CODE,
            appVersionName = BuildConfig.VERSION_NAME,
            apiEnvironment = if (BuildConfig.DEBUG) ApiEnvironment.PreProd else ApiEnvironment.Prod,
        )
    }

    override fun onCreate() {
        super.onCreate()

        userDataCleanableList = listOf<AssociatedUserDataCleanable>(DeviceInfoUpdateManager)

        AccountUtils.init()

        this.configureSentry(isDebug = BuildConfig.DEBUG, isSentryTrackingEnabled = true)
        notificationUtils.initNotificationChannel()

        applicationScope.launch {
            DeviceInfoUpdateManager.scheduleWorkerOnDeviceInfoUpdate<DeviceInfoUpdateWorker>()
        }
    }

    companion object {
        @JvmStatic
        var userDataCleanableList: List<AssociatedUserDataCleanable> = emptyList()
            protected set
    }
}
