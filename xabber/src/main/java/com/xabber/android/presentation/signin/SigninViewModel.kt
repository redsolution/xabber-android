package com.xabber.android.presentation.signin

import androidx.lifecycle.ViewModel
import com.xabber.android.R
import com.xabber.android.presentation.signin.feature.Feature
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

const val JID_REGEX = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"

class SigninViewModel : ViewModel() {

    var _features = mutableListOf(
        Feature(R.string.feature_name_1),
        Feature(R.string.feature_name_2),
        Feature(R.string.feature_name_3),
        Feature(R.string.feature_name_4),
        Feature(R.string.feature_name_5),
        Feature(R.string.feature_name_6),
        Feature(R.string.feature_name_7),
        Feature(R.string.feature_name_8),
        Feature(R.string.feature_name_9),
        Feature(R.string.feature_name_10),
    )

    var isServerFeatures = false

    val features: Observable<MutableList<Feature>>
        get() = Observable.fromIterable(_features)
            .map {
                val toIndex = _features.indexOf(it) + 1
                if (toIndex < 4 && !isServerFeatures)
                    _features.subList(0, toIndex)
                else {
                    isServerFeatures = true
                    _features.subList(3, toIndex)
                }
            }
            .concatMap {
                Observable.just(it).delay(1, TimeUnit.SECONDS)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun isJidValid(jid: String): Boolean = JID_REGEX.toRegex().matches(jid)
}
