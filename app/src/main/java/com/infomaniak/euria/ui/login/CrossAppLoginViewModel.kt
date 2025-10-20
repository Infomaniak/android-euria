package com.infomaniak.euria.ui.login

import com.infomaniak.core.crossapplogin.back.BaseCrossAppLoginViewModel
import com.infomaniak.euria.BuildConfig
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
class CrossAppLoginViewModel() : BaseCrossAppLoginViewModel(BuildConfig.APPLICATION_ID, BuildConfig.CLIENT_ID)
