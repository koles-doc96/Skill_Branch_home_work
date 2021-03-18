package ru.skillbranch.kotlinexample.extentions

fun String.isValidPhone() =
    trimPhone().startsWith("+") && trimPhone().length == 12
fun String.trimPhone() = this.replace("[^+\\d]".toRegex(), replacement = "")