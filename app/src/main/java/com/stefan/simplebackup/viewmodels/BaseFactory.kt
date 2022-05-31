package com.stefan.simplebackup.viewmodels

interface BaseFactory {

    fun <VM : BaseViewModel> create(modelClass: Class<VM>): VM
}