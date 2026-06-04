package com.wanlv.app.pojo.vo

data class PageVO<T>(
    val total: Long,
    val records: List<T>
)
