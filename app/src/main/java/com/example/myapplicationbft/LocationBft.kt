package com.example.myapplicationbft

data class LocationBft(internal val longitude:String, internal val latitude:String, val alt:String, val date:String, val id:String,val bmsAssetId:String) {
    companion object {
        const val EXCHANGE_NAME = "bft-exchange"
    }
}