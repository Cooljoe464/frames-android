package com.checkout.sdk.core

import android.content.Context
import android.util.Log
import com.checkout.sdk.CheckoutClient
import com.checkout.sdk.api.ApiFactory
import com.checkout.sdk.api.TokenApi
import com.checkout.sdk.executors.Coroutines
import com.checkout.sdk.request.CardTokenizationRequest
import com.checkout.sdk.request.GooglePayTokenizationRequest
import com.checkout.sdk.response.CardTokenizationFail
import com.checkout.sdk.response.CardTokenizationResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response


class RequestMaker(
    private val key: String,
    private val tokenApi: TokenApi,
    private val coroutines: Coroutines,
    private val tokenCallback: CheckoutClient.TokenCallback,
    private val progressCallback: ProgressCallback? = null
) {

    fun makeTokenRequest(request: CardTokenizationRequest) {
        val deferred = tokenApi.getTokenAsync(key, request)
        coroutines.IOScope.launch {
            val response =
                try {
                    deferred.await()
                } catch (e: Exception) {
                    handleRequestError(e)
                    return@launch
                }

            if (response.isSuccessful) {
                handleResponseSuccessful(response)
            } else {
                handleResponseFailure(response)
            }
        }
        progressCallback?.onProgressChanged(true)
    }

    fun makeGooglePayTokenRequest(request: GooglePayTokenizationRequest) {
        val deferred = tokenApi.getGooglePayTokenAsync(key, request)
        coroutines.IOScope.launch {
            val response =
                try {
                    deferred.await()
                } catch (e: Exception) {
                    handleRequestError(e)
                    return@launch
                }

            if (response.isSuccessful) {
                Log.e("JOHN", "Success: " + response.body()!!.token)
            } else {
                Log.e("JOHN", "Fail: $response")
            }
        }
        progressCallback?.onProgressChanged(true)
    }

    private suspend fun handleRequestError(e: Exception) {
        withContext(coroutines.Main) {
            tokenCallback.onTokenResult(TokenResult.TokenResultNetworkError(e))
            progressCallback?.onProgressChanged(false)
        }
    }

    private suspend fun handleResponseSuccessful(result: Response<CardTokenizationResponse>) {
        withContext(coroutines.Main) {
            tokenCallback.onTokenResult(TokenResult.TokenResultSuccess(result.body()!!))
            progressCallback?.onProgressChanged(false)
        }
    }

    private suspend fun handleResponseFailure(result: Response<CardTokenizationResponse>) {
        val errorString = result.errorBody()!!.string()
        val fail =
            Gson().fromJson(errorString, CardTokenizationFail::class.java)
        withContext(coroutines.Main) {
            tokenCallback.onTokenResult(TokenResult.TokenResultTokenisationFail(fail))
            progressCallback?.onProgressChanged(false)
        }
    }

    interface ProgressCallback {
        fun onProgressChanged(inProgress: Boolean)
    }

    companion object {
        /**
         * Convenience method for simple creation of a RequestMaker
         */
        fun create(context: Context, checkoutClient: CheckoutClient, progressCallback: ProgressCallback? = null): RequestMaker {
            val apiFactory = ApiFactory(context, checkoutClient.environment)
            val tokenApi = apiFactory.api
            return RequestMaker(
                checkoutClient.key,
                tokenApi,
                Coroutines(),
                checkoutClient.tokenCallback,
                progressCallback
            )
        }
    }
}
