package com.example.match3

import android.app.Activity
import android.content.Context
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

class YandexRewardedAdsController(private val context: Context) : AdsController {

    private var rewardedAdLoader: RewardedAdLoader? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var currentOnReward: (() -> Unit)? = null

    init {
        YandexAds.initialize(context) {
            loadAd()
        }
        rewardedAdLoader = RewardedAdLoader(context)
    }

    private fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true

        val adUnitId = "demo-rewarded-yandex"
        val adRequest = AdRequest.Builder(adUnitId).build()

        rewardedAdLoader?.loadAd(adRequest, object : RewardedAdLoadListener {
            override fun onAdLoaded(ad: RewardedAd) {
                isLoading = false
                rewardedAd = ad

                ad.setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdShown() {}

                    override fun onAdFailedToShow(adError: AdError) {
                        rewardedAd = null
                        currentOnReward = null
                        loadAd()
                    }

                    override fun onAdDismissed() {
                        rewardedAd = null
                        loadAd()
                    }

                    override fun onAdClicked() {}

                    override fun onAdImpression(impressionData: ImpressionData?) {}

                    override fun onRewarded(reward: Reward) {
                        currentOnReward?.invoke()
                        currentOnReward = null
                    }
                })
            }

            override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                isLoading = false
                rewardedAd = null
            }
        })
    }

    override fun showRewarded(onReward: () -> Unit, onFailed: (String) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onFailed("Реклама ещё загружается. Попробуйте через пару секунд.")
            loadAd()
            return
        }

        val activity = context as? Activity
        if (activity == null) {
            onFailed("Не удалось получить Activity для показа.")
            return
        }

        currentOnReward = onReward
        ad.show(activity)
    }
}