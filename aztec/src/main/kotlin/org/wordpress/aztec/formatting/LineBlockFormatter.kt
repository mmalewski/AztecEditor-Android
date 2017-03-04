package org.wordpress.aztec.formatting

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import org.wordpress.aztec.*
import org.wordpress.aztec.AztecText.OnMediaTappedListener
import org.wordpress.aztec.spans.*
import org.xml.sax.Attributes
import java.util.*


class LineBlockFormatter(editor: AztecText) : AztecFormatter(editor) {

    fun applyMoreComment() {
        applyComment(AztecCommentSpan.Comment.MORE)
    }

    fun applyPageComment() {
        applyComment(AztecCommentSpan.Comment.PAGE)
    }

    fun containsHeading(textFormat: TextFormat, selStart: Int, selEnd: Int): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")
        val list = ArrayList<Int>()

        for (i in lines.indices) {
            val lineStart = (0..i - 1).sumBy { lines[it].length + 1 }
            val lineEnd = lineStart + lines[i].length

            if (lineStart >= lineEnd) {
                continue
            }

            /**
             * lineStart  >= selStart && selEnd   >= lineEnd // single line, current entirely selected OR
             *                                                  multiple lines (before and/or after), current entirely selected
             * lineStart  <= selEnd   && selEnd   <= lineEnd // single line, current partially or entirely selected OR
             *                                                  multiple lines (after), current partially or entirely selected
             * lineStart  <= selStart && selStart <= lineEnd // single line, current partially or entirely selected OR
             *                                                  multiple lines (before), current partially or entirely selected
             */
            if ((lineStart >= selStart && selEnd >= lineEnd)
                    || (lineStart <= selEnd && selEnd <= lineEnd)
                    || (lineStart <= selStart && selStart <= lineEnd)) {
                list.add(i)
            }
        }

        if (list.isEmpty()) return false

        return list.any { containHeadingType(textFormat, it) }
    }

    private fun containHeadingType(textFormat: TextFormat, index: Int): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")

        if (index < 0 || index >= lines.size) {
            return false
        }

        val start = (0..index - 1).sumBy { lines[it].length + 1 }
        val end = start + lines[index].length

        if (start >= end) {
            return false
        }

        val spans = editableText.getSpans(start, end, AztecHeadingSpan::class.java)

        for (span in spans) {
            when (textFormat) {
                TextFormat.FORMAT_HEADING_1 ->
                    return span.heading == AztecHeadingSpan.Heading.H1
                TextFormat.FORMAT_HEADING_2 ->
                    return span.heading == AztecHeadingSpan.Heading.H2
                TextFormat.FORMAT_HEADING_3 ->
                    return span.heading == AztecHeadingSpan.Heading.H3
                TextFormat.FORMAT_HEADING_4 ->
                    return span.heading == AztecHeadingSpan.Heading.H4
                TextFormat.FORMAT_HEADING_5 ->
                    return span.heading == AztecHeadingSpan.Heading.H5
                TextFormat.FORMAT_HEADING_6 ->
                    return span.heading == AztecHeadingSpan.Heading.H6
                else -> return false
            }
        }

        return false
    }

    private fun applyComment(comment: AztecCommentSpan.Comment) {
        editor.removeInlineStylesFromRange(selectionStart, selectionEnd)
        editor.removeBlockStylesFromRange(selectionStart, selectionEnd, true)

        val span = AztecCommentSpan(
                editor.context,
                when (comment) {
                    AztecCommentSpan.Comment.MORE -> ContextCompat.getDrawable(editor.context, R.drawable.img_more)
                    AztecCommentSpan.Comment.PAGE -> ContextCompat.getDrawable(editor.context, R.drawable.img_page)
                }
        )
        val ssb = SpannableStringBuilder(comment.html)
        ssb.setSpan(span, 0, comment.html.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        editableText.replace(selectionStart, selectionEnd, ssb)

        editor.setSelection(
                if (selectionEnd < EndOfBufferMarkerAdder.safeLength(editor)) selectionEnd + 1 else selectionEnd)
    }

    fun insertMedia(drawable: Drawable?, attributes: Attributes, onMediaTappedListener: OnMediaTappedListener?) {
        val span = AztecMediaSpan(editor.context, drawable, attributes, onMediaTappedListener)
        span.textView = editor

        val spanBeforeMedia = editableText.getSpans(selectionStart, selectionEnd, AztecBlockSpan::class.java)
        .firstOrNull {
            selectionStart == editableText.getSpanEnd(it)
        }

        val spanAfterMedia = editableText.getSpans(selectionStart, selectionEnd, AztecBlockSpan::class.java)
                .firstOrNull {
                    selectionStart == editableText.getSpanStart(it)
                }

        val mediaStartIndex = selectionStart
        val mediaEndIndex = selectionStart + 1

        editor.disableTextChangedListener()
        editableText.replace(selectionStart, selectionEnd, Constants.IMG_STRING)
        editor.enableTextChangedListener()

        if (spanAfterMedia != null) {
            editableText.setSpan(spanAfterMedia, mediaStartIndex, editableText.getSpanEnd(spanAfterMedia), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (spanBeforeMedia != null) {
            editableText.setSpan(spanBeforeMedia, editableText.getSpanStart(spanBeforeMedia), mediaEndIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        editor.removeInlineStylesFromRange(mediaStartIndex, mediaEndIndex)

        editableText.setSpan(
                span,
                mediaStartIndex,
                mediaEndIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        editableText.setSpan(
                AztecMediaClickableSpan(span),
                mediaStartIndex,
                mediaEndIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        editor.setSelection(mediaEndIndex)
        editor.isMediaAdded = true
    }
}
