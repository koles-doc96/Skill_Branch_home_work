package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.extentions.isValidPhone
import ru.skillbranch.kotlinexample.extentions.trimPhone

object UserHolder {

    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        val user =  User.makeUser(fullName, email = email, password = password)
        return if (!map.keys.contains(user.login)) {
            user.also { user -> map[user.login] = user }
        } else throw IllegalArgumentException("A user with this email already exists")
    }


    fun loginUser(login: String, password: String): String? {
        val trimLogin = if( login.trimPhone().isValidPhone() ) login.trimPhone()
        else login.trim().toLowerCase()

        return map[trimLogin]?.run {
            if (checkPassword(password)) userInfo
            else null
        }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        if (!rawPhone.trimPhone().isValidPhone()) throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")

        return if (!map.keys.contains(rawPhone.trimPhone())) {
            User.makeUser(fullName, phone = rawPhone).also { user ->  map[user.login] = user }
        } else throw IllegalArgumentException("A user with this phone already exists")
    }

    fun requestAccessCode(login: String) {
        var currentLogin = login.trim()
        if(login.isValidPhone()) {
            currentLogin = currentLogin.trimPhone()
        }
        map[currentLogin]?.run {
            this.accessCode = this.changePassword()
        }
    }

    fun importUsers(list: List<String>): List<User> =
        list.map {
            val (fullName, email, access, phone) = it.split(";")
            val cuser = User.makeImportUser(fullName, email, access, phone)
                ?.also { user -> map[user.login] = user }
            cuser
        }.filterNotNull()

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }
}