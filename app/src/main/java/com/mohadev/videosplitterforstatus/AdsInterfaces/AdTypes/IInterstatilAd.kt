package com.mohadev.videosplitterforstatus.AdsInterfaces.AdTypes

interface IInterstatilAd {
    fun  LoadInterstatilAd()
    fun  ShowInterstatilAd(onAdDismissed: () -> Unit?)
    fun isLoaded(): Boolean
}