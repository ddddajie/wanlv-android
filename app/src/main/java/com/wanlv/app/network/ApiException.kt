package com.wanlv.app.network

class ApiException(message: String, val code: Int? = null) : Exception(message)
