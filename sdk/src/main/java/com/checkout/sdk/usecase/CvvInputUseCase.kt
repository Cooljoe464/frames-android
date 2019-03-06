package com.checkout.sdk.usecase

import com.checkout.sdk.architecture.UseCase
import com.checkout.sdk.store.DataStore


class CvvInputUseCase(
    private val dataStore: DataStore,
    private val cvv: String
) : UseCase<Boolean> {

    override fun execute(): Boolean {
        dataStore.cardCvv = cvv
        return false
    }
}