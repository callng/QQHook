package moe.ore.android.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import moe.ore.txhook.R
import moe.ore.txhook.app.ui.dp

object Dialog {
    class EditTextAlertBuilder @JvmOverloads constructor(
        ctx: Context,
        @StyleRes themeId: Int = R.style.AppTheme_Dialog
    ) : AlertDialog.Builder(ctx, themeId) {

        private val rootView: View
        private val titleView: TextView
        private val editTextLayout: TextInputLayout
        private val editText: TextInputEditText
        private val buttonPanel: LinearLayout
        private val positiveButton: Button
        private val negativeButton: Button

        private var negativeListener: DialogInterface.OnClickListener? = null
        private var defaultPositiveListener: DialogInterface.OnClickListener? = null
        private var dialog: AlertDialog? = null
        private var positiveListener: EditTextAlertListener? = null

        init {
            val context = this.context
            val card = createBaseCard(context)
            rootView = card.root
            titleView = card.title
            buttonPanel = card.buttonPanel
            positiveButton = card.positive
            negativeButton = card.negative

            editTextLayout = TextInputLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                hint = "text"
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                boxStrokeWidth = context.dp(1)
                boxStrokeWidthFocused = context.dp(1)
            }

            editText = TextInputEditText(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            editTextLayout.addView(editText)
            card.content.addView(editTextLayout)

            super.setView(rootView)

            negativeButton.setOnClickListener {
                negativeListener?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
                dialog?.dismiss()
            }

            positiveButton.setOnClickListener {
                positiveListener?.onSubmit(editText.text)
                defaultPositiveListener?.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                dialog?.dismiss()
            }
        }

        override fun setTitle(title: CharSequence?): EditTextAlertBuilder {
            titleView.text = title
            titleView.visibility = View.VISIBLE
            return this
        }

        override fun setTitle(titleId: Int): AlertDialog.Builder {
            titleView.setText(titleId)
            titleView.visibility = View.VISIBLE
            return this
        }

        override fun setView(layoutResId: Int): AlertDialog.Builder {
            return this
        }

        override fun setView(view: View?): AlertDialog.Builder {
            return this
        }

        fun setHint(text: CharSequence?): AlertDialog.Builder {
            editText.hint = text
            return this
        }

        fun setFloatingText(text: CharSequence?): EditTextAlertBuilder {
            editTextLayout.hint = text
            return this
        }

        fun setTextListener(listener: EditTextAlertListener): EditTextAlertBuilder {
            positiveListener = listener
            return this
        }

        override fun setPositiveButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener?
        ): AlertDialog.Builder {
            buttonPanel.visibility = View.VISIBLE
            positiveButton.visibility = View.VISIBLE
            positiveButton.text = text
            defaultPositiveListener = listener
            return this
        }

        override fun setPositiveButton(@StringRes textId: Int, listener: DialogInterface.OnClickListener): AlertDialog.Builder {
            return setPositiveButton(context.getString(textId), listener)
        }

        override fun setNegativeButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            buttonPanel.visibility = View.VISIBLE
            negativeButton.visibility = View.VISIBLE
            negativeButton.text = text
            negativeListener = listener
            return this
        }

        override fun setNegativeButton(@StringRes textId: Int, listener: DialogInterface.OnClickListener): AlertDialog.Builder {
            return setNegativeButton(context.getString(textId), listener)
        }

        override fun create(): AlertDialog {
            dialog = super.create()
            return dialog as AlertDialog
        }

        override fun show(): AlertDialog {
            create()
            dialog?.show()
            return dialog!!
        }

        fun interface EditTextAlertListener {
            fun onSubmit(text: CharSequence?)
        }
    }

    class ListAlertBuilder @JvmOverloads constructor(
        ctx: Context,
        @StyleRes themeId: Int = R.style.AppTheme_Dialog
    ) : AlertDialog.Builder(ctx, themeId) {

        private val rootView: View
        private val titleView: TextView
        private val listView: ListView
        private val buttonPanel: LinearLayout
        private val negativeButton: Button

        private var negativeListener: DialogInterface.OnClickListener? = null
        private var dialog: AlertDialog? = null
        private val items: LinkedHashMap<String, ((AlertDialog, View, Int) -> Unit)?> = linkedMapOf()

        init {
            val context = this.context
            val card = createBaseCard(context)
            rootView = card.root
            titleView = card.title
            buttonPanel = card.buttonPanel
            negativeButton = card.negative

            listView = ListView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    context.dp(280)
                ).also { it.topMargin = context.dp(10) }
                dividerHeight = context.dp(1)
            }

            card.content.addView(listView)
            super.setView(rootView)

            negativeButton.setOnClickListener {
                negativeListener?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
                dialog?.dismiss()
            }
        }

        override fun setTitle(title: CharSequence?): ListAlertBuilder {
            titleView.text = title
            titleView.visibility = View.VISIBLE
            return this
        }

        override fun setTitle(titleId: Int): AlertDialog.Builder {
            titleView.setText(titleId)
            titleView.visibility = View.VISIBLE
            return this
        }

        override fun setView(layoutResId: Int): AlertDialog.Builder {
            return this
        }

        override fun setView(view: View?): AlertDialog.Builder {
            return this
        }

        fun addItem(name: String, block: ((dialog: AlertDialog, v: View, pos: Int) -> Unit)?): ListAlertBuilder {
            items[name] = block
            return this
        }

        override fun setNegativeButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            buttonPanel.visibility = View.VISIBLE
            negativeButton.visibility = View.VISIBLE
            negativeButton.text = text
            negativeListener = listener
            return this
        }

        override fun setNegativeButton(@StringRes textId: Int, listener: DialogInterface.OnClickListener): AlertDialog.Builder {
            return setNegativeButton(context.getString(textId), listener)
        }

        override fun create(): AlertDialog {
            dialog = super.create()

            val names = items.keys.toList()
            val callbacks = items.values.toList()

            listView.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, names)
            listView.setOnItemClickListener { _, view, position, _ ->
                val callback = callbacks[position]
                if (callback != null) {
                    callback.invoke(dialog!!, view, position)
                } else {
                    dialog?.dismiss()
                }
            }

            return dialog as AlertDialog
        }

        override fun show(): AlertDialog {
            create()
            dialog?.show()
            return dialog!!
        }
    }

    class CommonAlertBuilder @JvmOverloads constructor(
        ctx: Context,
        @StyleRes themeId: Int = R.style.AppTheme_Dialog
    ) : AlertDialog.Builder(ctx, themeId) {

        private val rootView: View
        private val titleView: TextView
        private val messageView: TextView
        private val messagePanel: LinearLayout
        private val buttonPanel: LinearLayout
        private val positiveButton: Button
        private val negativeButton: Button
        private val neutralButton: Button

        private var positiveListener: DialogInterface.OnClickListener? = null
        private var negativeListener: DialogInterface.OnClickListener? = null
        private var neutralListener: DialogInterface.OnClickListener? = null

        private var dialog: AlertDialog? = null

        init {
            val context = this.context
            val card = createBaseCard(context)
            rootView = card.root
            titleView = card.title
            buttonPanel = card.buttonPanel
            positiveButton = card.positive
            negativeButton = card.negative
            neutralButton = card.neutral
            messagePanel = card.content

            messageView = TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
                setPadding(context.dp(4), context.dp(4), context.dp(4), context.dp(4))
                setTextColor(0xFF334155.toInt())
                textSize = 14f
                visibility = View.GONE
            }
            messagePanel.addView(messageView)

            super.setView(rootView)

            titleView.visibility = View.GONE
            negativeButton.visibility = View.GONE
            positiveButton.visibility = View.GONE
            neutralButton.visibility = View.GONE
            buttonPanel.visibility = View.GONE

            positiveButton.setOnClickListener {
                positiveListener?.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                dialog?.dismiss()
            }
            negativeButton.setOnClickListener {
                negativeListener?.onClick(dialog, DialogInterface.BUTTON_NEGATIVE)
                dialog?.dismiss()
            }
            neutralButton.setOnClickListener {
                neutralListener?.onClick(dialog, DialogInterface.BUTTON_NEUTRAL)
                dialog?.dismiss()
            }
        }

        override fun setTitle(title: CharSequence?): AlertDialog.Builder {
            titleView.text = title
            titleView.visibility = View.VISIBLE
            return this
        }

        override fun setTitle(titleId: Int): AlertDialog.Builder {
            titleView.setText(titleId)
            titleView.visibility = View.VISIBLE
            return this
        }

        override fun setView(layoutResId: Int): AlertDialog.Builder {
            return this
        }

        override fun setView(view: View?): AlertDialog.Builder {
            return this
        }

        override fun setMessage(message: CharSequence?): AlertDialog.Builder {
            messageView.text = message
            messageView.visibility = View.VISIBLE
            return this
        }

        override fun setMessage(@StringRes messageId: Int): AlertDialog.Builder {
            return setMessage(context.getString(messageId))
        }

        override fun setPositiveButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            buttonPanel.visibility = View.VISIBLE
            positiveButton.visibility = View.VISIBLE
            positiveButton.text = text
            positiveListener = listener
            return this
        }

        override fun setPositiveButton(@StringRes textId: Int, listener: DialogInterface.OnClickListener): AlertDialog.Builder {
            return setPositiveButton(context.getString(textId), listener)
        }

        override fun setNegativeButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            buttonPanel.visibility = View.VISIBLE
            negativeButton.visibility = View.VISIBLE
            negativeButton.text = text
            negativeListener = listener
            return this
        }

        override fun setNegativeButton(@StringRes textId: Int, listener: DialogInterface.OnClickListener): AlertDialog.Builder {
            return setNegativeButton(context.getString(textId), listener)
        }

        override fun setNeutralButton(
            text: CharSequence?,
            listener: DialogInterface.OnClickListener
        ): AlertDialog.Builder {
            buttonPanel.visibility = View.VISIBLE
            neutralButton.visibility = View.VISIBLE
            neutralButton.text = text
            neutralListener = listener
            return this
        }

        override fun setNeutralButton(@StringRes textId: Int, listener: DialogInterface.OnClickListener): AlertDialog.Builder {
            return setNeutralButton(context.getString(textId), listener)
        }

        override fun create(): AlertDialog {
            dialog = super.create()
            return dialog as AlertDialog
        }

        override fun show(): AlertDialog {
            create()
            dialog?.show()
            return dialog!!
        }
    }

    private fun createBaseCard(context: Context): DialogCard {
        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(context.dp(8), context.dp(8), context.dp(8), context.dp(8))
        }

        val card = MaterialCardView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            radius = context.dp(14).toFloat()
            cardElevation = context.dp(8).toFloat()
            setCardBackgroundColor(0xFFFFFFFF.toInt())
        }

        val shell = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(12), context.dp(16), context.dp(10))
        }

        val content = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setTextColor(0xFF111827.toInt())
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            visibility = View.GONE
        }

        val buttonPanel = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, context.dp(8), 0, 0)
            visibility = View.GONE
        }

        val neutral = dialogButton(context, R.color.tx_appbar_color)
        val positive = dialogButton(context, R.color.tx_appbar_color)
        val negative = dialogButton(context, R.color.red500)

        buttonPanel.addView(neutral)
        buttonPanel.addView(positive)
        buttonPanel.addView(negative)

        content.addView(title)
        shell.addView(content)
        shell.addView(buttonPanel)
        card.addView(shell)
        root.addView(card)

        return DialogCard(root, content, title, buttonPanel, positive, negative, neutral)
    }

    private fun dialogButton(context: Context, colorRes: Int): Button {
        return Button(context, null, android.R.attr.buttonBarButtonStyle).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setTextColor(context.getColor(colorRes))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            visibility = View.GONE
            setAllCaps(false)
        }
    }

    private data class DialogCard(
        val root: View,
        val content: LinearLayout,
        val title: TextView,
        val buttonPanel: LinearLayout,
        val positive: Button,
        val negative: Button,
        val neutral: Button,
    )
}

