package com.example.myapplicationbft

data class LocationJson(internal val latitude:String, internal val longitude:String, val alt:String, val date:String, val id:String) {
    companion object {
        const val EXCHANGE_NAME = "bft-exchange"
    }
}