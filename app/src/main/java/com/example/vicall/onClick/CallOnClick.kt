package com.example.vicall.onClick

import com.example.vicall.models.ContactModel

interface CallOnClick {
    fun onSelectedLCallClick(contactModel: ContactModel)
}