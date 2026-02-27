package com.xabber.android.data.dto

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class XabberAccountDto(
    @Expose
    @SerializedName("username")
    var username: String? = null,
    @Expose
    @SerializedName("registration_date")
    var registrationDate: String? = null,
    @Expose
    @SerializedName("domain")
    var domain: String? = null,
    @Expose
    @SerializedName("last_name")
    var lastName: String? = null,
    @Expose
    @SerializedName("full_id")
    var fullId: String? = null,
    @Expose
    @SerializedName("can_register_team")
    var canRegisterTeam: Boolean? = null,
    @Expose
    @SerializedName("has_debt")
    var hasDebt: Boolean? = null,
    @Expose
    @SerializedName("xmpp_users")
    var xmppUsers: ArrayList<XmppUsers> = arrayListOf(),
    @Expose
    @SerializedName("email_list")
    var emailList: ArrayList<String> = arrayListOf(),
    @Expose
    @SerializedName("has_password")
    var hasPassword: Boolean? = null,
    @Expose
    @SerializedName("account_status")
    var accountStatus: String? = null,
    @Expose
    @SerializedName("timezone")
    var timezone: String? = null,
    @Expose
    @SerializedName("preferences")
    var preferences: Preferences? = Preferences(),
    @Expose
    @SerializedName("social_bindings")
    var socialBindings: ArrayList<String> = arrayListOf(),
    @Expose
    @SerializedName("first_name")
    var firstName: String? = null,
    @Expose
    @SerializedName("language_verbose")
    var languageVerbose: String? = null,
    @Expose
    @SerializedName("language")
    var language: String? = null,
    @Expose
    @SerializedName("can_register_free_xmpp_account")
    var canRegisterFreeXmppAccount: Boolean? = null,
    @Expose
    @SerializedName("token")
    var token: String? = null,
    @Expose
    @SerializedName("my_teams")
    var myTeams: ArrayList<String> = arrayListOf(),
    @Expose
    @SerializedName("pk")
    var pk: Int? = null,
    @Expose
    @SerializedName("transferred_teams")
    var transferredTeams: ArrayList<String> = arrayListOf(),
    @Expose
    @SerializedName("type")
    var type: String? = null,
    @Expose
    @SerializedName("default_services")
    var defaultServices: DefaultServices? = DefaultServices(),
    @Expose
    @SerializedName("xmpp_binding")
    var xmppBinding: XmppBinding? = XmppBinding()
)

data class XmppUsers(
    @Expose
    @SerializedName("id") var id: Int? = null,
    @Expose
    @SerializedName("jid")
    var jid: String? = null,
    @Expose
    @SerializedName("type")
    var type: String? = null,
    @Expose
    @SerializedName("username")
    var username: String? = null,
    @Expose
    @SerializedName("host")
    var host: String? = null,
    @Expose
    @SerializedName("has_debt")
    var hasDebt: Boolean? = null
)

data class Preferences(
    @Expose
    @SerializedName("email_when_expires")
    var emailWhenExpires: Boolean? = null,
    @Expose
    @SerializedName("email_news") var emailNews: Boolean? = null
)

data class DefaultServices(
    @Expose
    @SerializedName("max_free_xmpp_users")
    var maxFreeXmppUsers: Int? = null
)

data class XmppBinding(
    @Expose
    @SerializedName("id")
    var id: Int? = null,
    @Expose
    @SerializedName("verified")
    var verified: Boolean? = null,
    @Expose
    @SerializedName("is_native")
    var isNative: Boolean? = null,
    @Expose
    @SerializedName("jid")
    var jid: String? = null
)