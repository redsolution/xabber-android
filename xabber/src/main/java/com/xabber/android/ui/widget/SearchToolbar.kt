package com.xabber.android.ui.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.xabber.android.R

class SearchToolbar : RelativeLayout {

    @ColorInt
    var color: Int = 0
        set(value) {
            field = value
            setBackgroundColor(color)
        }

    var title: String? = null
        set(value) {
            field = value
            findViewById<TextView>(R.id.search_toolbar_title).text = value
        }

    var searchText: String? = null
        private set

    var onBackPressedListener: OnBackPressedListener? = null
    var onTextChangedListener: OnTextChangedListener? = null

    private lateinit var greetingsView: RelativeLayout
    private lateinit var searchView: RelativeLayout
    private lateinit var searchEditText: EditText

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        inflate(context, R.layout.search_toolbar, this)

        greetingsView = findViewById(R.id.search_toolbar_greetings_view)
        searchView = findViewById(R.id.search_toolbar_search_view)
        searchEditText = findViewById(R.id.search_toolbar_edittext)

        val clearButton = findViewById<ImageView>(R.id.search_toolbar_clear_button)

        clearButton.setOnClickListener { searchEditText.setText("") }

        findViewById<ImageView>(R.id.toolbar_search_back_button).setOnClickListener {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                searchEditText.windowToken, 0
            )
            onBackPressedListener?.onBackPressed()
        }

        greetingsView.visibility = View.VISIBLE

        findViewById<ImageView>(R.id.search_toolbar_search_button).setOnClickListener { setSearch() }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearButton.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
                onTextChangedListener?.onTextChanged(s.toString())
                searchText = s.toString()
            }
        })
    }

    fun setSearch(isActiveSearch: Boolean = true) {
        if (isActiveSearch) {
            greetingsView.visibility = View.GONE
            searchView.visibility = View.VISIBLE
            searchEditText.requestFocusFromTouch()
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                searchEditText, InputMethodManager.SHOW_IMPLICIT
            )
        } else {
            greetingsView.visibility = View.GONE
            searchView.visibility = View.VISIBLE
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                searchEditText.windowToken, 0
            )
        }
    }

    fun interface OnBackPressedListener {
        fun onBackPressed()
    }

    fun interface OnTextChangedListener {
        fun onTextChanged(text: String)
    }

}