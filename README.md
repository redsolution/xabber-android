<a href="https://play.google.com/store/apps/details?id=com.xabber.android.beta"><img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="72"></a>
<a href="https://f-droid.org/repository/browse/?fdfilter=xabber&fdid=com.xabber.androiddev"><img src="https://f-droid.org/badge/get-it-on.png" height="72"></a>
## Xabber - XMPP client for Android

Open source Jabber (XMPP) client with multi-account support, clean and simple interface.
Being both free (as in freedom!) and ad-free, [Xabber](https://www.xabber.com/) is designed to be the best Jabber client for Android.

## Build instructions [![Build Status](https://travis-ci.org/redsolution/xabber-android.svg?branch=develop)](https://travis-ci.org/redsolution/xabber-android)

**1. Prepare**

Xabber uses Gradle build system. The only specific thing is git submodule for ge0rg/MemorizingTrustManager library. To make it work use following commands:

 ```
 git submodule init
 git submodule update
 ```
 And MemorizingTrustManager would be cloned to your local repository.
 
**2. Build**
 
 You may build a **dev** flavor of project. This does not require additional files.
 
 Or, if you want to build other flavors, you need to copy the file: `xabber-android/xabber/src/dev/google-services.json` to the `xabber-android/xabber` folder and copy the file `xabber-android/xabber/src/dev/res/values/social_keys.xml` to the folder `xabber-android/xabber/src/main/res/values/`.

## Translations [![Crowdin](https://d322cqt584bo4o.cloudfront.net/xabber/localized.svg)](https://crowdin.com/project/xabber)

We use crowdin.com as our translation system.
All related resources are automatically generated from files got with crowdin.com.
If you want to update any translation go to Xabber page https://crowdin.com/project/xabber and request to join our translation team
Please don't create pull requests with translation fixes as any changes will be overwritten with the next update from crowdin.com.

## Donate

If you want to support Xabber development you can buy [Xabber VIP](https://play.google.com/store/apps/details?id=com.xabber.androidvip) (it is the same as regular stable version of Xabber except for gold coin on logo) or send payment via Paypal or Bitcoin `1Dvexs9YQ2bab1ZNk2eSuvHxyZ2QUedzug`

[![Donate with PayPal](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=G9AYTUSXCWRVL)

## Feedback

info [at] xabber.com

<a href="https://twitter.com/xabber_xmpp">Twitter</a>
