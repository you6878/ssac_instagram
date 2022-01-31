package com.howlab.howlstagram.model

data class PushModel (
    var to : String? = null,
    var notification : Notification = Notification()){
    data class Notification(
        var title : String? = null,
        var body : String? = null
    )
}