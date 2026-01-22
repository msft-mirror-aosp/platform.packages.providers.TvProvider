/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.providers.tv;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.tv.extension.TvExtensionContract;
import android.media.tv.extension.TvExtensionDbHelper;
import android.net.Uri;
import android.util.Log;

/**
 * The TvExtensionProvider manages access to the tv_extension.db
 */
public class TvExtensionProvider extends ContentProvider {

    private TvExtensionDbHelper mDbHelper;
    private static final String TAG = "TvExensionProvider";

    // --- URI Matcher Constants ---
    // 1. Global Key-Value
    private static final int GLOBAL = 100;
    private static final int GLOBAL_KEY = 101;

    // 2. Single-Row Settings Tables
    private static final int GENERAL = 200;
    private static final int DIGITAL_TUNER = 300;
    private static final int ANALOG_TUNER = 400;
    private static final int DVB_EXT = 500;
    private static final int INTERACTIVE = 600;
    private static final int CC_STYLE_SETTINGS = 700;

    // 3. Fixed Multi-Row
    private static final int OPERATORS = 800;
    private static final int OPERATORS_ID = 801;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        // content://.../global_setting
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.GLOBAL_SETTING_TABLE,
            GLOBAL);
        // content://.../global_setting/[key_string]
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.GLOBAL_SETTING_TABLE
            + "/*", GLOBAL_KEY);

        // content://.../general_setting
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.GENERAL_SETTING_TABLE,
            GENERAL);

        // content://.../digital_tuner_setting
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.
            DIGITAL_TUNER_SETTING_TABLE, DIGITAL_TUNER);

        // content://.../analog_tuner_setting
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.
            ANALOG_TUNER_SETTING_TABLE, ANALOG_TUNER);

        // content://.../dvb_extension
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.
            DVB_EXTENSION_TABLE, DVB_EXT);

        // content://.../interactive_setting
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.
            INTERACTIVE_SETTING_TABLE, INTERACTIVE);

        // content://.../interactive_setting
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.
            CC_STYLE_SETTINGS, CC_STYLE_SETTINGS);


        // content://.../tuner_operators
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.
            TUNER_OPERATOR_TABLE, OPERATORS);

        // content://.../tuner_operators/[row_id]
        sUriMatcher.addURI(TvExtensionContract.AUTHORITY, TvExtensionContract.
            TUNER_OPERATOR_TABLE + "/#", OPERATORS_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new TvExtensionDbHelper(getContext());
        return true;
    }

    /**
     * Query for a given, returning a {@link Cursor} over the result set.
     *
     * @param uri The content:// URI of the query request.
     * @param projection A list of which columns to return. Passing null will
     *                   return all columns, which is discouraged to prevent reading
     *                   data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an SQL WHERE clause
     *                  (excluding the WHERE itself). Passing null will return all rows for the
     *                  given table.
     * @param selectionArgs You may include '?s' in selection, which will be replaced by the values
     *                      from selectionArgs, in the order that they appear in the selection.
     *                      The values will be bound as Strings. If selection is null or does not
     *                      contain '?s' then selectionArgs may be null.
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause (excluding the
     *                  ORDER BY itself). Passing null will use the default sort order, which may
     *                  be unordered.
     * @return A {@link Cursor} object, which is positioned before the first entry.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
        String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int match = sUriMatcher.match(uri);
        switch (match) {
            // --- Global Setting Key Value Table ---
            case GLOBAL-> qb.setTables(TvExtensionContract.GLOBAL_SETTING_TABLE);
            case GLOBAL_KEY-> {
                qb.setTables(TvExtensionContract.GLOBAL_SETTING_TABLE);
                qb.appendWhere(TvExtensionContract.GlobalSettings.COLUMN_KEY + "='"
                     + uri.getLastPathSegment() + "'");
            }
            // --- Single Row Tables ---
            case GENERAL -> qb.setTables(TvExtensionContract.GENERAL_SETTING_TABLE);
            case DIGITAL_TUNER -> qb.setTables(TvExtensionContract.DIGITAL_TUNER_SETTING_TABLE);
            case ANALOG_TUNER -> qb.setTables(TvExtensionContract.ANALOG_TUNER_SETTING_TABLE);
            case DVB_EXT -> qb.setTables(TvExtensionContract.DVB_EXTENSION_TABLE);
            case INTERACTIVE -> qb.setTables(TvExtensionContract.INTERACTIVE_SETTING_TABLE);
            case CC_STYLE_SETTINGS -> qb.setTables(TvExtensionContract.CC_STYLE_SETTINGS);

            // --- Fixed Multi-Row ---
            case OPERATORS -> qb.setTables(TvExtensionContract.TUNER_OPERATOR_TABLE);
            case OPERATORS_ID ->{
                qb.setTables(TvExtensionContract.TUNER_OPERATOR_TABLE);
                qb.appendWhere(TvExtensionContract.TunerOperators.COLUMN_TUNER_NAME + "="
                     + uri.getLastPathSegment());
            }

            default->
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (getContext() != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    /**
     * insert() should only be used by global_settings tables,
     * other tables should only use update().
     * @param uri The content:// URI of the insertion request.
     * @param values A set of column_name/value pairs to add to the database.
     * @return The URI of the newly inserted row.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);

        switch (match) {
            // Block adding new rows.
            case GLOBAL -> {
                // Allow extending the table with new keys
                long id = db.insertWithOnConflict(
                    TvExtensionContract.GLOBAL_SETTING_TABLE,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                );
                if (id > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return ContentUris.withAppendedId(uri, id);
                }
            }
            case OPERATORS, GENERAL, DIGITAL_TUNER, ANALOG_TUNER, DVB_EXT, CC_STYLE_SETTINGS,
                 INTERACTIVE, GLOBAL_KEY ->
                throw new UnsupportedOperationException("INSERT is not allowed."
                    + " Use update() on the existing row.");
            default -> throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return Uri.EMPTY;
    }

   /**
     * A method for updating values stored by the Tv Extension Provider.
     *
     * @param uri The content:// URI of the update request.
     * @param values A map from column names to new column values. Null is a valid value that will
     *               be translated to NULL.
     * @param selection A filter declaring which rows to return, formatted as an SQL WHERE clause
     *                  (excluding the WHERE itself). Passing null will return all rows for the
     *                  given table.
     * @param selectionArgs You may include '?s' in selection, which will be replaced by the values
     *                      from selectionArgs, in the order that they appear in the selection.
     *                      The values will be bound as Strings. If selection is null or does not
     *                      contain '?s' then selectionArgs may be null.
     * @return The number of rows affected.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count;
        String tableName;

        int match = sUriMatcher.match(uri);

        switch (match) {
            case GLOBAL -> {
                tableName = TvExtensionContract.GLOBAL_SETTING_TABLE;
                values.remove(TvExtensionContract.GlobalSettings.COLUMN_KEY);
            }
            case GLOBAL_KEY -> {
                tableName = TvExtensionContract.GLOBAL_SETTING_TABLE;
                // Remove keys so can't update keys
                values.remove(TvExtensionContract.GlobalSettings.COLUMN_KEY);
                // Merge the URI-based ID with any custom filters provided
                selection = DatabaseUtils.concatenateWhere(selection,
                                TvExtensionContract.GlobalSettings.COLUMN_KEY + "=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs,
                                new String[]{uri.getLastPathSegment()});
            }
            case OPERATORS_ID -> {
                tableName = TvExtensionContract.TUNER_OPERATOR_TABLE;
                selection = TvExtensionContract.TunerOperators.COLUMN_TUNER_NAME + "=?";
                selectionArgs = new String[]{uri.getLastPathSegment()};
            }
            case CC_STYLE_SETTINGS -> {
                tableName = TvExtensionContract.CC_STYLE_SETTINGS;
                selection = TvExtensionContract.CCStyleSettings.COLUMN_ID + "=?";
                selectionArgs = new String[]{"1"};
            }
            case GENERAL -> {
                tableName = TvExtensionContract.GENERAL_SETTING_TABLE;
                selection = TvExtensionContract.GeneralSettings.COLUMN_ID + "=?";
                selectionArgs = new String[]{"1"};
            }
            case DIGITAL_TUNER -> {
                tableName = TvExtensionContract.DIGITAL_TUNER_SETTING_TABLE;
                selection = TvExtensionContract.DigitalTunerSettings.COLUMN_ID + "=?";
                selectionArgs = new String[]{"1"};
            }
            case ANALOG_TUNER -> {
                tableName = TvExtensionContract.ANALOG_TUNER_SETTING_TABLE;
                selection = TvExtensionContract.AnalogTunerSettings.COLUMN_ID + "=?";
                selectionArgs = new String[]{"1"};
            }
            case DVB_EXT -> {
                tableName = TvExtensionContract.DVB_EXTENSION_TABLE;
                selection = TvExtensionContract.DvbExtensions.COLUMN_ID + "=?";
                selectionArgs = new String[]{"1"};
            }
            case INTERACTIVE -> {
                tableName = TvExtensionContract.INTERACTIVE_SETTING_TABLE;
                selection = TvExtensionContract.InteractiveSettings.COLUMN_ID + "=?";
                selectionArgs = new String[]{"1"};
            }
            case OPERATORS -> {
                tableName = TvExtensionContract.TUNER_OPERATOR_TABLE;
            }

            default -> throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        Log.d(TAG, "updating uri " +  uri.toString() + " match: " + match + " tablename: "
             + tableName);
        count = db.update(tableName, values, selection, selectionArgs);
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    /**
     * delete() cannot be used for any operations.
     * @param uri The full URI to query, including a row ID (if a specific
     *            record is requested).
     * @param selection A filter declaring which rows to return, formatted as an SQL WHERE clause
     *                  (excluding the WHERE itself). Passing null will return all rows for the
     *                  given table.
     * @param selectionArgs You may include '?s' in selection, which will be replaced by the values
     *                      from selectionArgs, in the order that they appear in the selection.
     *                      The values will be bound as Strings. If selection is null or does not
     *                      contain '?s' then selectionArgs may be null.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int count = 0;
        switch (match) {
            // Block for other tables.
            case GLOBAL, GLOBAL_KEY-> {
                String targetKey = uri.getLastPathSegment();

                if (TvExtensionContract.GlobalSettings.isGlobalSettingsKey(targetKey)) {
                    throw new UnsupportedOperationException("Cannot delete system default key: "
                        + targetKey);
                }

                String whereClause = TvExtensionContract.GlobalSettings.COLUMN_KEY + "=?";
                String[] whereArgs = new String[]{targetKey};

                count = db.delete(TvExtensionContract.GLOBAL_SETTING_TABLE, whereClause, whereArgs);
            }


            case GENERAL, DIGITAL_TUNER, ANALOG_TUNER, DVB_EXT, INTERACTIVE, CC_STYLE_SETTINGS->
                throw new UnsupportedOperationException("Cannot delete single-row settings."
                + " Use update() to reset values.");
            case OPERATORS, OPERATORS_ID->
                throw new UnsupportedOperationException("Cannot delete fixed-row settings."
                 + " Use update() to reset values.");
            default->
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        return count;
    }

    /**
     * Returns the type string.
     * @param uri The full URI of the query.
     * @return The type.
     */
    @Override
    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);
        return switch (match) {
            case GLOBAL->
                "vnd.android.cursor.dir/vnd.android.media.tv.extensions.global_setting";
            case OPERATORS->
                "vnd.android.cursor.dir/vnd.android.media.tv.extensions.tuner_operators";
            case GLOBAL_KEY->
                "vnd.android.cursor.item/vnd.android.media.tv.extensions.global_setting";
            case OPERATORS_ID->
                "vnd.android.cursor.item/vnd.android.media.tv.extensions.tuner_operators";
            case GENERAL->
                "vnd.android.cursor.item/vnd.android.media.tv.extensions.general_setting";
            case DIGITAL_TUNER->
                "vnd.android.cursor.item/vnd.android.media.tv.extensions.digital_tuner_setting";
            case ANALOG_TUNER->
                "vnd.android.cursor.item/vnd.android.media.tv.extensions.analog_tuner_setting";
            case DVB_EXT->
                "vnd.android.cursor.item/vnd.android.media.tv.extensions.dvb_extension";
            case INTERACTIVE->
                "vnd.android.cursor.item/vnd.android.media.tv.extensions.interactive_setting";
            case CC_STYLE_SETTINGS->
                "vnd.android.cursor.item/vnd.android.media.tv.extensions.cc_style_settings";
            default-> null;
        };
    }

    private void notifyChange(Uri uri) {
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }
}