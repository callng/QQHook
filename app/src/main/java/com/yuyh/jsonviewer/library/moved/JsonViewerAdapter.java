package com.yuyh.jsonviewer.library.moved;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Objects;

import moe.ore.android.dialog.Dialog;
import moe.ore.android.util.AndroidUtil;
import moe.ore.txhook.app.ParserActivity;
import moe.ore.txhook.app.model.CapturePacket;
import moe.ore.txhook.helper.HexUtil;

/**
 * Created by yuyuhang on 2017/11/29.
 */
public class JsonViewerAdapter extends BaseJsonViewerAdapter<JsonViewerAdapter.JsonItemViewHolder> {

    private JSONObject mJSONObject;
    private JSONArray mJSONArray;

    public JsonViewerAdapter(String jsonStr) {

        Object object = null;
        try {
            object = new JSONTokener(jsonStr).nextValue();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (object instanceof JSONObject) {
            mJSONObject = (JSONObject) object;
        } else if (object instanceof JSONArray) {
            mJSONArray = (JSONArray) object;
        } else {
            throw new IllegalArgumentException("jsonStr is illegal.");
        }
    }

    public JsonViewerAdapter(JSONObject jsonObject) {
        this.mJSONObject = jsonObject;
        if (mJSONObject == null) {
            throw new IllegalArgumentException("jsonObject can not be null.");
        }
    }

    public JsonViewerAdapter(JSONArray jsonArray) {
        this.mJSONArray = jsonArray;
        if (mJSONArray == null) {
            throw new IllegalArgumentException("jsonArray can not be null.");
        }
    }

    @NonNull
    @Override
    public JsonItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new JsonItemViewHolder(new JsonItemView(parent.getContext()));
    }

    @Override
    public void onBindViewHolder(JsonItemViewHolder holder, int position) {
        JsonItemView itemView = holder.itemView;
        itemView.setTextSize(TEXT_SIZE_DP);
        itemView.setRightColor(BRACES_COLOR);
        if (mJSONObject != null) {
            if (position == 0) {
                itemView.hideLeft();
                itemView.hideIcon();
                itemView.showRight("{");
                return;
            } else if (position == getItemCount() - 1) {
                itemView.hideLeft();
                itemView.hideIcon();
                itemView.showRight("}");
                return;
            } else if (mJSONObject.names() == null) {
                return;
            }

            String key = Objects.requireNonNull(mJSONObject.names()).optString(position - 1); // iterate keys
            Object value = mJSONObject.opt(key);

            // no trailing comma for last item
            handleJsonObject(key, value, itemView, position < getItemCount() - 2, 1);
        }

        if (mJSONArray != null) {
            if (position == 0) {
                itemView.hideLeft();
                itemView.hideIcon();
                itemView.showRight("[");
                return;
            } else if (position == getItemCount() - 1) {
                itemView.hideLeft();
                itemView.hideIcon();
                itemView.showRight("]");
                return;
            }

            Object value = mJSONArray.opt(position - 1); // iterate array
            // no trailing comma for last item
            handleJsonArray(value, itemView, position < getItemCount() - 2, 1);
        }
    }

    @Override
    public int getItemCount() {
        if (mJSONObject != null) {
            if (mJSONObject.names() != null) {
                return Objects.requireNonNull(mJSONObject.names()).length() + 2;
            } else {
                return 2;
            }
        }
        if (mJSONArray != null) {
            return mJSONArray.length() + 2;
        }
        return 0;
    }

    /**
     * Handle value when parent is JsonObject (value has key).
     *
     * @param value
     * @param key
     * @param itemView
     * @param appendComma
     * @param hierarchy
     */
    private void handleJsonObject(String key, Object value, JsonItemView itemView, boolean appendComma, int hierarchy) {
        SpannableStringBuilder keyBuilder = new SpannableStringBuilder(Utils.getHierarchyStr(hierarchy));

        if (key.matches("(.*)-[0-9]*")) {
            key = key.split("-")[0];
        }

        keyBuilder.append("\"").append(key).append("\"").append(":");
        keyBuilder.setSpan(new ForegroundColorSpan(KEY_COLOR), 0, keyBuilder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        keyBuilder.setSpan(new ForegroundColorSpan(BRACES_COLOR), keyBuilder.length() - 1, keyBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        itemView.showLeft(keyBuilder);

        handleValue(value, itemView, appendComma, hierarchy);
    }

    /**
     * Handle value when parent is JsonArray (value has no key).
     *
     * @param value
     * @param itemView
     * @param appendComma whether to append a trailing comma (last value has no comma)
     * @param hierarchy   indentation level
     */
    private void handleJsonArray(Object value, JsonItemView itemView, boolean appendComma, int hierarchy) {
        itemView.showLeft(new SpannableStringBuilder(Utils.getHierarchyStr(hierarchy)));

        handleValue(value, itemView, appendComma, hierarchy);
    }

    /**
     * @param value
     * @param itemView
     * @param appendComma whether to append a trailing comma (last key:value has no comma)
     * @param hierarchy   indentation level
     */
    private void handleValue(Object value, JsonItemView itemView, boolean appendComma, int hierarchy) {
        SpannableStringBuilder valueBuilder = new SpannableStringBuilder();
        Context cx = itemView.getContext();
        if (value instanceof Number) {
            itemView.setOnClickListener(new JsonItemClickListener(value, itemView, appendComma, hierarchy + 1));
            valueBuilder.append(value.toString());
            valueBuilder.setSpan(new ForegroundColorSpan(NUMBER_COLOR), 0, valueBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (value instanceof Boolean) {
            itemView.setOnClickListener(new JsonItemClickListener(value, itemView, appendComma, hierarchy + 1));
            valueBuilder.append(value.toString());
            valueBuilder.setSpan(new ForegroundColorSpan(BOOLEAN_COLOR), 0, valueBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (value instanceof JSONObject) {
            itemView.showIcon(true);
            valueBuilder.append("Object{...}");
            valueBuilder.setSpan(new ForegroundColorSpan(BRACES_COLOR), 0, valueBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            itemView.setOnClickListener(new JsonItemClickListener(value, itemView, appendComma, hierarchy + 1));
        } else if (value instanceof JSONArray) {
            itemView.showIcon(true);
            valueBuilder.append("Array[").append(String.valueOf(((JSONArray) value).length())).append("]");
            int len = valueBuilder.length();
            valueBuilder.setSpan(new ForegroundColorSpan(BRACES_COLOR), 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            valueBuilder.setSpan(new ForegroundColorSpan(NUMBER_COLOR), 6, len - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            valueBuilder.setSpan(new ForegroundColorSpan(BRACES_COLOR), len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            itemView.setOnClickListener(new JsonItemClickListener(value, itemView, appendComma, hierarchy + 1));
        } else if (value instanceof String) {
            itemView.hideIcon();

            itemView.setOnClickListener(new JsonItemClickListener(value, itemView, appendComma, hierarchy + 1));

            if (((String) value).startsWith("[hex]")) {
                value = ((String) value).substring(5);
            }

            valueBuilder.append("\"").append(value.toString()).append("\"");
            if (Utils.isUrl(value.toString())) {
                valueBuilder.setSpan(new ForegroundColorSpan(TEXT_COLOR), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                valueBuilder.setSpan(new ForegroundColorSpan(URL_COLOR), 1, valueBuilder.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                valueBuilder.setSpan(new ForegroundColorSpan(TEXT_COLOR), valueBuilder.length() - 1, valueBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                valueBuilder.setSpan(new ForegroundColorSpan(TEXT_COLOR), 0, valueBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

        } else if (valueBuilder.length() == 0 || value == null) {
            itemView.hideIcon();
            valueBuilder.append("null");
            valueBuilder.setSpan(new ForegroundColorSpan(NULL_COLOR), 0, valueBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (appendComma) {
            valueBuilder.append(",");
        }

        itemView.showRight(valueBuilder);
    }

    class JsonItemClickListener implements View.OnClickListener {
        private final Context context;
        private final boolean isJsonObject;
        private Object value;
        private final JsonItemView itemView;
        private final boolean appendComma;
        private final int hierarchy;

        private final boolean originalHex;
        private boolean isCollapsed = true;
        private final boolean isJsonArray;

        JsonItemClickListener(Object value, JsonItemView itemView, boolean appendComma, int hierarchy) {
            this.value = value;
            this.itemView = itemView;
            this.appendComma = appendComma;
            this.hierarchy = hierarchy;
            this.isJsonArray = value instanceof JSONArray;
            this.isJsonObject = value instanceof JSONObject;
            this.context = itemView.getContext();
            if (value instanceof String) {
                originalHex = value.toString().startsWith("[hex]");
            } else {
                originalHex = false;
            }
        }

        @Override
        public void onClick(View view) {
            if (isJsonObject || isJsonArray ){
                if (itemView.getChildCount() == 1) { // collapsed -> expand
                    isCollapsed = false;
                    itemView.showIcon(false);
                    itemView.setTag(itemView.getRightText());
                    itemView.showRight(isJsonArray ? "[" : "{");
                    JSONArray array = isJsonArray ? (JSONArray) value : ((JSONObject) value).names();
                    for (int i = 0; array != null && i < array.length(); i++) {
                        JsonItemView childItemView = new JsonItemView(itemView.getContext());
                        childItemView.setTextSize(TEXT_SIZE_DP);
                        childItemView.setRightColor(BRACES_COLOR);
                        Object childValue = array.opt(i);
                        if (isJsonArray) {
                            handleJsonArray(childValue, childItemView, i < array.length() - 1, hierarchy);
                        } else {
                            handleJsonObject((String) childValue, ((JSONObject) value).opt((String) childValue), childItemView, i < array.length() - 1, hierarchy);
                        }
                        itemView.addViewNoInvalidate(childItemView);
                    }

                    JsonItemView childItemView = new JsonItemView(itemView.getContext());
                    childItemView.setTextSize(TEXT_SIZE_DP);
                    childItemView.setRightColor(BRACES_COLOR);
                    StringBuilder builder = new StringBuilder(Utils.getHierarchyStr(hierarchy - 1));
                    builder.append(isJsonArray ? "]" : "}").append(appendComma ? "," : "");
                    childItemView.showRight(builder);
                    itemView.addViewNoInvalidate(childItemView);
                    itemView.requestLayout();
                    itemView.invalidate();
                } else {                            // toggle collapse/expand
                    CharSequence temp = itemView.getRightText();
                    itemView.showRight((CharSequence) itemView.getTag());
                    itemView.setTag(temp);
                    itemView.showIcon(!isCollapsed);
                    for (int i = 1; i < itemView.getChildCount(); i++) {
                        itemView.getChildAt(i).setVisibility(isCollapsed ? View.VISIBLE : View.GONE);
                    }
                    isCollapsed = !isCollapsed;
                }
            }
            else {
                Dialog.ListAlertBuilder builder = new Dialog.ListAlertBuilder(context);

                if (value instanceof String) {
                    String v = value.toString().substring(5);
                    boolean isHexNum = value.toString().startsWith("[16n]");
                    boolean isHex = value.toString().startsWith("[hex]");
                    boolean isNoHex = value.toString().startsWith("[str]");

                    String subV = v;
                    if (subV.length() >= 20) {
                        subV = v.substring(0, 20) + "...";
                    }

                    builder.setTitle(subV);
                    builder.addItem("\u590d\u5236\u5230\u526a\u5207\u677f", (dialog, view1, integer) -> {
                        dialog.dismiss();
                        if (originalHex && isNoHex) {
                            AndroidUtil.copyText(context, new String(HexUtil.Hex2Bin(v)));
                        } else AndroidUtil.copyText(context, v);
                        return null;
                    });
                    // <<============================ quick parser actions
                    if (isHex) {
                        builder.addItem("JCE\u6570\u636e\u89e3\u6790", (dialog, view1, integer) -> {
                            dialog.dismiss();
                            Intent intent = new Intent(context, ParserActivity.class);
                            intent.putExtra("data", CapturePacket.CREATOR.create(HexUtil.Hex2Bin(v)));
                            intent.putExtra("jce", true);
                            context.startActivity(intent);
                            return null;
                        });
                        builder.addItem("Protobuf\u89e3\u6790", (dialog, view1, integer) -> {
                            dialog.dismiss();
                            Intent intent = new Intent(context, ParserActivity.class);
                            intent.putExtra("data", CapturePacket.CREATOR.create(HexUtil.Hex2Bin(v)));
                            intent.putExtra("jce", false);
                            context.startActivity(intent);
                            return null;
                        });
                    }
                    if (isHex || isNoHex) {
                        builder.addItem(isHex ? "\u8f6c\u4e3a\u5b57\u7b26\u4e32" : "\u8f6c\u4e3aHex", (dialog, view1, integer) -> {
                            dialog.dismiss();
                            if (isNoHex) {
                                value = "[hex]" + v;
                                SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                                stringBuilder.append("\"")
                                        .append(v)
                                        .append("\"");
                                stringBuilder.setSpan(
                                        new ForegroundColorSpan(TEXT_COLOR), 0,
                                        stringBuilder.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                                itemView.showRight(stringBuilder);
                            } else {
                                value = "[str]" + v;
                                SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                                stringBuilder.append("\"")
                                        .append(new String(HexUtil.Hex2Bin(v)))
                                        .append("\"");
                                stringBuilder.setSpan(
                                        new ForegroundColorSpan(TEXT_COLOR), 0,
                                        stringBuilder.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                );
                                itemView.showRight(stringBuilder);
                            }
                            return null;
                        });
                    } else if (isHexNum) {
                        builder.addItem("\u8f6c\u4e3a10\u8fdb\u5236", (dialog, view1, whitch) -> {
                            dialog.dismiss();
                            value = Long.parseLong(v);
                            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                            stringBuilder.append("")
                                    .append(v)
                                    .append("");
                            stringBuilder.setSpan(
                                    new ForegroundColorSpan(NUMBER_COLOR), 0,
                                    stringBuilder.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            itemView.showRight(stringBuilder);
                            return null;
                        });
                    }
                } else {
                    String v = value.toString();
                    builder.setTitle(v);
                    builder.addItem("\u590d\u5236\u5230\u526a\u5207\u677f", (dialog, view1, integer) -> {
                        dialog.dismiss();
                        AndroidUtil.copyText(context, v);
                        return null;
                    });
                    if (value instanceof Long || value instanceof Integer) {
                        builder.addItem("\u8f6c\u4e3a16\u8fdb\u5236", (dialog, view1, integer) -> {
                            dialog.dismiss();
                            value = "[16n]" + v;
                            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                            stringBuilder.append("0x")
                                    .append(Long.toHexString(Long.parseLong(v)))
                                    .append("");
                            stringBuilder.setSpan(
                                    new ForegroundColorSpan(NUMBER_COLOR), 0,
                                    stringBuilder.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            itemView.showRight(stringBuilder);
                            return null;
                        });
                    }
                }

                builder.show();
            }
        }
    }

    static class JsonItemViewHolder extends RecyclerView.ViewHolder {
        JsonItemView itemView;

        JsonItemViewHolder(JsonItemView itemView) {
            super(itemView);
            setIsRecyclable(false);
            this.itemView = itemView;
        }
    }
}
