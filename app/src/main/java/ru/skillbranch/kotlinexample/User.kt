package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {

    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()


    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")


    private var phone: String? = null
        set(value) {
            if (value.isNullOrBlank()) field = null
            else field = value?.replace("[^+\\d]".toRegex(), replacement = "")
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    private lateinit var passwordHash: String

    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        phone = rawPhone
        login = if( email.isNullOrEmpty() ) phone!!
        else email

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: ${if(email.isNullOrEmpty()) null else email}
            phone: $phone
            meta: $meta
        """.trimIndent()
    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        salt = defineSalt()
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }


    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary email constructor")
        salt = defineSalt()
        passwordHash = encrypt(password)
    }

    //for csv email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        hash: String,
        salt: String?,
        phone: String?
    ) : this(firstName, lastName, email = email, rawPhone = phone, meta = mapOf("src" to "csv")) {
        if(!salt.isNullOrBlank()) this.salt = salt
        accessCode = salt //?
        passwordHash = hash
    }

//    //for csv phone
//    constructor(
//        firstName: String,
//        lastName: String?,
//        hash: String,
//        salt: String?,
//        phone: String?
//    ) : this(firstName, lastName, rawPhone = phone, meta = mapOf("src" to "csv")) {
//        if(!salt.isNullOrBlank()) this.salt = salt
//        accessCode = salt //?
//        passwordHash = hash
//    }


    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private lateinit var salt: String

    fun defineSalt() =  ByteArray(16).also {
        SecureRandom().nextBytes(it)
    }.toString()
//    private val salt: String by lazy {
//        ByteArray(16).also {
//            SecureRandom().nextBytes(it)
//        }.toString()
//    }

    private fun encrypt(password: String) = salt.plus(password).md5()

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) //16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun checkPhone(phone: String): Boolean = this.phone == phone

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not much the current password")
    }

    fun changePassword(): String {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        return code
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone be not null or blank")
            }
        }

        fun makeImportUser(
            fullName: String? = null,
            email: String? = null,
            access: String? = null,
            phone: String? = null
        ): User? {
            if (fullName.isNullOrBlank() || access.isNullOrBlank()) return null

            val (firstName, lastName) = fullName.fullNameToPair()
            val (salt, hash) = access.split(":")

            return User(firstName, lastName, email, hash, salt, phone)
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Fullname must contain only first name and last name^ current split result ${this@fullNameToPair}")
                    }
                }
        }
    }
}