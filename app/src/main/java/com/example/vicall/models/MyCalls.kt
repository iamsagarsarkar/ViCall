package com.example.vicall.models

class MyCalls(var userId : String,
              var userName : String,
              var userImage : String,
              var userPhone: String,
              var date : String,
              var callState : String ) {
    override fun equals(obj: Any?): Boolean {
        return this.userId == (obj as MyCalls).userId && this.userName == obj.userName && this.userImage == obj.userImage && this.userPhone == obj.userPhone && this.date == obj.date && this.callState == obj.callState
    }
}