package com.github.james9909.warplus

import com.github.kittinunf.result.Result

typealias Ok<V> = Result.Success<V>
typealias Err<E> = Result.Failure<E>