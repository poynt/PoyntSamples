package com.godaddy.commerce.services.sample.common.viewmodel

data class CommonState(
    val loading: Boolean = false,
    val error: String? = null
){
    val isErrorVisible get() = error.isNullOrBlank().not()
}