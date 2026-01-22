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

import com.android.providers.tv.TvExtensionProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.media.tv.extension.TvExtensionContract;
import android.media.tv.extension.TvExtensionDbHelper;

import android.media.tv.extension.TvExtensionContract.GeneralSettings;
import android.media.tv.extension.TvExtensionContract.GlobalSettings;
import android.media.tv.extension.TvExtensionContract.DigitalTunerSettings;
import android.media.tv.extension.TvExtensionContract.AnalogTunerSettings;
import android.media.tv.extension.TvExtensionContract.InteractiveSettings;
import android.media.tv.extension.TvExtensionContract.DvbExtensions;
import android.media.tv.extension.TvExtensionContract.CCStyleSettings;
import android.media.tv.extension.TvExtensionContract.TunerOperators;

import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;

import android.util.Log;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import android.database.sqlite.SQLiteDatabase;

@RunWith(JUnit4.class)
public class TvExtensionProviderTest {
    private static final String TAG = TvExtensionProviderTest.class.getSimpleName();

    private static TvExtensionProvider extensionProvider;
    private static Context mContext;

    @BeforeClass
    public static void beforeClass() {
        Log.d(TAG, "before class");
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        extensionProvider = new TvExtensionProvider();
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = TvExtensionContract.AUTHORITY;

        extensionProvider.attachInfo(mContext, providerInfo);
        extensionProvider.onCreate();
    }

    @AfterClass
    public static void afterClass() {
        Log.d(TAG, "afterClass(e)");
    }

    @Before
    public void before() {
        Log.d(TAG, "before()");
    }

    @After
    public void after() {
        Log.d(TAG, "after()");
    }

    public void assertUpdateAndQuery(Uri uri, String column,  int value, String selection,
        String[] selectionArgs) {
        ContentValues values = new ContentValues();
        values.put(column, value);
        int count = extensionProvider.update(uri, values, selection, selectionArgs);
        if(count <= 0)
        {
            Assert.fail("Could not update column " + column + " with value " + value);
        }

        Cursor cursor = extensionProvider.query(uri, null, selection, selectionArgs, null);
        int result = -1;
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(column);
                if(index != -1) {
                    result = cursor.getInt(index);
                    cursor.close();
                    Assert.assertEquals(result, value);
                    return;
                }
            }
            cursor.close();
        }
        else {
            Assert.fail("Could not query column" + column);
        }
        Assert.fail("Failed to update and query the sql database");

    }
    public void assertUpdateAndQuery(Uri uri, String column,  String value, String selection,
        String[] selectionArgs) {
        ContentValues values = new ContentValues();
        values.put(column, value);
        int count = extensionProvider.update(uri, values, selection, selectionArgs);
        if(count <= 0)
        {
            Assert.fail("Could not update column " + column);
        }
        Cursor cursor = extensionProvider.query(uri, null, selection, selectionArgs, null);
        String result = "";

        if(cursor != null) {
            if(cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(column);
                if(index != -1) {
                    result = cursor.getString(index);
                    cursor.close();
                    Assert.assertEquals(result, value);
                    return;
                }
            }
            cursor.close();
            Assert.fail("Failed to query the sql database");
        }
    }

    int getIntFromSqlTable(Uri uri, String column, String selection, String[] selectionArgs) {
        Cursor cursor = extensionProvider.query(uri, null, selection, selectionArgs, null);
        int result = -1;
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(column);
                    result = cursor.getInt(index);
                }
            }
        cursor.close();
        return result;
    }

    public void assertUpdateFails(Uri uri, String column,  int value, String selection,
        String[] selectionArgs) {
        int originalValue = getIntFromSqlTable(uri, column, selection, selectionArgs);
        Boolean didExceptionOccur = false;
        ContentValues values = new ContentValues();
        values.put(column, value);
        try {
            extensionProvider.update(uri, values, selection, selectionArgs);
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            didExceptionOccur = true;
        }

        int newValue = getIntFromSqlTable(uri, column, selection, selectionArgs);
        Assert.assertTrue(didExceptionOccur);
        Assert.assertNotEquals(value, originalValue);
        Assert.assertEquals(newValue, originalValue);

    }

    public void assertSqlTable(Uri uri, String column,  int value, String selection,
        String[] selectionArgs) {
        Cursor cursor = extensionProvider.query(uri, null, selection, selectionArgs, null);
        int result =  -1;
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(column);
                if(index != -1) {
                    result = cursor.getInt(index);
                    cursor.close();
                    Assert.assertEquals(result, value);
                    return;
                }
            }
            cursor.close();
            Assert.fail("failed to query the sql database");
        }

    }

    public void checkBoundaries(Uri uri, String column,  int lowerBound, int upperBound,
        String selection, String[] selectionArgs) {
        assertUpdateAndQuery(uri, column, lowerBound, null, null);
        assertUpdateAndQuery(uri, column, upperBound, null, null);
        assertUpdateFails(uri, column, lowerBound - 1, null, null);
        assertUpdateFails(uri, column, upperBound + 1, null, null);
    }

    @Test
    public void testDigitalTable() {
        assertUpdateAndQuery(DigitalTunerSettings.CONTENT_URI,
            DigitalTunerSettings.COLUMN_DIGITAL_SUBTITLE_DISPLAY,
            DigitalTunerSettings.DIGITAL_SUBTITLE_DISPLAY_ON, null, null);
        assertUpdateAndQuery(DigitalTunerSettings.CONTENT_URI,
            DigitalTunerSettings.COLUMN_DIGITAL_SUBTITLE_TRACK,
            10, null, null);
        assertUpdateAndQuery(DigitalTunerSettings.CONTENT_URI,
            DigitalTunerSettings.COLUMN_SUPERIMPOSE,
            DigitalTunerSettings.SUPERIMPOSE_SETUP_LANG2, null, null);
    }
    @Test
    public void testTunerTable() {
        Uri uri = ContentUris.withAppendedId(TunerOperators.CONTENT_URI, TunerOperators.ANTENNA);
        assertUpdateAndQuery(uri, TunerOperators.COLUMN_OPERATOR_ID, TunerOperators.CABLE, null,
            null);
        assertUpdateAndQuery(uri, TunerOperators.COLUMN_OPERATOR_NAME, "Testing", null, null);
        assertUpdateAndQuery(TunerOperators.CONTENT_URI, TunerOperators.COLUMN_OPERATOR_ID,
            TunerOperators.SATELLITE, TunerOperators.COLUMN_TUNER_NAME + " = ?",
            new String[] { String.valueOf(TunerOperators.CABLE) });

        checkBoundaries(uri, TunerOperators.COLUMN_OPERATOR_ID,  0, 2, null, null);
    }

    @Test
    public void testDigitalTableBounds() {
        checkBoundaries(DigitalTunerSettings.CONTENT_URI,
            DigitalTunerSettings.COLUMN_DIGITAL_SUBTITLE_DISPLAY, 0, 2, null, null);
        checkBoundaries(DigitalTunerSettings.CONTENT_URI,
            DigitalTunerSettings.COLUMN_SUPERIMPOSE, 0, 2, null, null);
    }

     @Test
    public void testAnalogTable() {
        assertUpdateAndQuery(AnalogTunerSettings.CONTENT_URI, AnalogTunerSettings.COLUMN_ID, 1,
            null, null);
        assertUpdateAndQuery(AnalogTunerSettings.CONTENT_URI,
            AnalogTunerSettings.COLUMN_ANALOG_SERVICE_SELECTION, 1, null, null);
        assertUpdateAndQuery(AnalogTunerSettings.CONTENT_URI,
            AnalogTunerSettings.COLUMN_CLOSED_CAPTION_DISPLAY,
            AnalogTunerSettings.ANALOG_DISPLAY_ON, null, null);
        assertUpdateAndQuery(AnalogTunerSettings.CONTENT_URI,
            AnalogTunerSettings.COLUMN_SUBTITLE_DISPLAY, AnalogTunerSettings.ANALOG_SUBTITLE_SCTE,
            null, null);
        assertUpdateFails(AnalogTunerSettings.CONTENT_URI, AnalogTunerSettings.COLUMN_ID, 2,
            null, null);
    }
     @Test
    public void checkAnalogTableBounds() {
        checkBoundaries(AnalogTunerSettings.CONTENT_URI,
            AnalogTunerSettings.COLUMN_CLOSED_CAPTION_DISPLAY, 0, 2, null, null);
        checkBoundaries(AnalogTunerSettings.CONTENT_URI,
            AnalogTunerSettings.COLUMN_SUBTITLE_DISPLAY, 0, 3, null, null);
    }

    @Test
    public void testInteractiveTable() {
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_GINGA_STATE, InteractiveSettings.ENABLE, null,
            null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_GINGA_AUTO_START_APPLICATION_STATE,
            InteractiveSettings.ENABLE, null, null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_ENABLE, InteractiveSettings.ENABLE, null,
            null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_BLOCK_TRACKING, 1, null, null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_DEVICE_ID,
            InteractiveSettings.HAS_DEVICE_ID, null, null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.HBBTV_DEVICE_ID_SEED_TIMESTAMP, 0, null, null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_RESET_DEVICE_ID, "test", null, null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_PRIVACY_POLICY_COOKIES_SETTINGS,
            InteractiveSettings.HBBTV_PRIVACY_POLICY_DEFAULT, null, null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_PRIVACY_POLICY_DO_NOT_TRACK,
            InteractiveSettings.HBBTV_PRIVACY_POLICY_DO_NOT_TRACK_DEFAULT, null, null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_PRIVACY_POLICY_PERSISTENT_STORAGE,
            InteractiveSettings.ENABLE, null, null);
        assertUpdateAndQuery(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_OPAPP_LAUNCH_PARAMS, "test", null, null);
    }

    @Test
    public void testIntactiveTableCheckBounds() {
        checkBoundaries(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_GINGA_STATE, 0, 1, null, null);
        checkBoundaries(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_GINGA_AUTO_START_APPLICATION_STATE, 0, 1, null, null);
        checkBoundaries(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_BLOCK_TRACKING, 0, 1, null, null);
        checkBoundaries(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_DEVICE_ID, 0, 1, null, null);
        checkBoundaries(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_PRIVACY_POLICY_PERSISTENT_STORAGE, 0, 1, null, null);
        checkBoundaries(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_ENABLE, 0, 1, null, null);
        checkBoundaries(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_PRIVACY_POLICY_COOKIES_SETTINGS, 0, 2, null, null);
        checkBoundaries(InteractiveSettings.CONTENT_URI,
            InteractiveSettings.COLUMN_HBBTV_PRIVACY_POLICY_DO_NOT_TRACK, 0, 2, null, null);
    }

    @Test
     public void testDvbTable() {
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI, DvbExtensions.COLUMN_LCN_CONFLICT,
            DvbExtensions.LCN_CONFICT_HAS_OCCURRED, null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI, DvbExtensions.COLUMN_LCN_USER_SETTING_CABLE,
            DvbExtensions.LCN_OFF, null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_LCN_USER_SETTING_TERRESTRIAL, 10, null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_TKGS_AVAIL_COND,
            DvbExtensions.TKGS_AVAILABILITY_COND_CERTIFICATION, null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_TKGS_OPER_MODE, DvbExtensions.TKGS_OPERATING_MODE_CUST, null,
            null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_TKGS_VISIBLE_LOCATOR_LIST, "test", null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_TKGS_HIDDEN_LOCATOR_LIST, "test", null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_TKGS_TABLE_VERSION, 10,  null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_TKGS_USER_MESSAGE, "test", null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_CHANNEL_NUM_ADDED,  10,null, null);
        assertUpdateAndQuery(DvbExtensions.CONTENT_URI,
            DvbExtensions.COLUMN_INDONESIA_EWS_LOCATION_CODE, "indonesia", null, null);
    }

    @Test
     public void testDvbTableCheckBounds() {
        checkBoundaries(DvbExtensions.CONTENT_URI, DvbExtensions.COLUMN_LCN_CONFLICT, 0, 1, null,
            null);
        checkBoundaries(DvbExtensions.CONTENT_URI, DvbExtensions.COLUMN_LCN_USER_SETTING_CABLE, 0,
            2, null, null);
        checkBoundaries(DvbExtensions.CONTENT_URI, DvbExtensions.COLUMN_TKGS_OPER_MODE, 0, 2, null,
            null);
        checkBoundaries(DvbExtensions.CONTENT_URI, DvbExtensions.COLUMN_TKGS_AVAIL_COND, 0, 1, null,
            null);
    }

    @Test
    public void testCCStyleSettingsTable() {
        assertUpdateAndQuery(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_BACKGROUND_COLOR,
            CCStyleSettings.CLOSED_CAPTION_COLOR_BLACK, null, null);
        assertUpdateAndQuery(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_BACKGROUND_OPACITY,
            CCStyleSettings.CLOSED_CAPTION_OPACITY_SOLID, null, null);
        assertUpdateAndQuery(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_EDGE_COLOR,
            CCStyleSettings.CLOSED_CAPTION_COLOR_RED, null, null);
        assertUpdateAndQuery(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_EDGE_TYPE,
            CCStyleSettings.CLOSED_CAPTION_EDGE_TYPE_NONE, null, null);
        assertUpdateAndQuery(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_TEXT_COLOR,
            CCStyleSettings.CLOSED_CAPTION_COLOR_WHITE, null, null);
        assertUpdateAndQuery(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_TEXT_OPACITY,
            CCStyleSettings.CLOSED_CAPTION_OPACITY_SOLID, null, null);
        assertUpdateAndQuery(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_TEXT_SIZE,
            CCStyleSettings.CLOSED_CAPTION_TEXT_SIZE_SMALL, null, null);
        assertUpdateAndQuery(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_FONT_FAMILY,
            CCStyleSettings.CLOSED_CAPTION_FRONT_FAMILY_CASUAL, null, null);
    }

    @Test
    public void testCCStyleBoundaries() {
        checkBoundaries(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_BACKGROUND_COLOR,0, 8, null, null);
        checkBoundaries(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_BACKGROUND_OPACITY,0, 4, null, null);
        checkBoundaries(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_EDGE_COLOR,0, 8, null, null);
        checkBoundaries(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_EDGE_TYPE,0, 6, null, null);
        checkBoundaries(CCStyleSettings.CONTENT_URI,
            CCStyleSettings.CLOSED_CAPTION_TEXT_COLOR,0, 8, null, null);
        checkBoundaries(CCStyleSettings.CONTENT_URI, CCStyleSettings.CLOSED_CAPTION_TEXT_OPACITY, 0,
             4, null, null);
        checkBoundaries(CCStyleSettings.CONTENT_URI, CCStyleSettings.CLOSED_CAPTION_TEXT_SIZE,0, 3,
            null, null);
        checkBoundaries(CCStyleSettings.CONTENT_URI, CCStyleSettings.CLOSED_CAPTION_FONT_FAMILY,0,
            7, null, null);
    }

    @Test
    public void testGeneralSettings() {
    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_AUDIO_TYPE,
        GeneralSettings.AUDIO_TYPE_NORMAL,
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_VISUALLY_IMPAIRED_FADER_CONTROL,
        GeneralSettings.VISUALLY_IMPAIRED_FADER_CONTROL_ON,
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_AUDIO_TYPE,
        GeneralSettings.AUDIO_TYPE_NORMAL,
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_BLOCK_UNRATED_PROG,
        GeneralSettings.BLOCK_UNRATED_PROG_ON,
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_PREFERRED_AUDIO_LANG_PRIMARY,
        "test",
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_PREFERRED_AUDIO_LANG_SECONDARY,
        "test",
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_VISUALLY_IMPAIRED_MIXING_LEVEL,
        10,
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_PREFERRED_SUBTITLE_LANG_PRIMARY,
        "test",
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_PREFERRED_SUBTITLE_LANG_SECONDARY,
        "test",
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_TELETEXT_DIGITAL_LANGUAGE,
        "test",
        null,
        null);

    assertUpdateAndQuery(TvExtensionContract.GeneralSettings.CONTENT_URI,
        GeneralSettings.COLUMN_TELETEXT_DECODING_LANGUAGE,
        2,
        null,
        null);

    }

    @Test
    public void testGlobalSettings() {
    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_CABLE_BROADCASTER_NAME });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        0,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_UI_CHANNEL_UPDATE_MSG });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        "serviceListType",
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_CHANNEL_LIST_TYPE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        "1234",
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_CAM_PIN_CODE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        GlobalSettings.CAM_TYPE_USB,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_CAM_TYPE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_EAS_IS_CHANNEL_CHANGE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_EAS_STATUS });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        "test satellite",
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_SATELLITE_BROADCASTER });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        GlobalSettings.BROADCAST_TUNER_TYPE_SATELLITE,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_TUNER_TYPE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_UI_KEYPAD_SHOW_MENU });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_UI_KEYPAD_IS_FOREGROUND });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        "testing",
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_CURRENT_COUNTRY_REGION });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        0,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_OAD_MARK });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_OAD_UNDONE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_OAD_OFFSET });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        10,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_OAD_PROPERTIES });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        20,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_OAD_SIZE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        GlobalSettings.RECORDING_STATE_PVR_PLAYING,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_RECORDING_STATE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_OAD_REJECT_UPDATE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        1000,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_TIMESHIFT_FREE_SIZE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        GlobalSettings.TIMESHIFT_MODE_ON,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_TIMESHIFT_MODE });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        "test_path",
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_TIMESHIFT_PATH });

    assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
        GlobalSettings.COLUMN_VALUE,
        GlobalSettings.SATELLITE_TYPE_PREFERRED,
        GlobalSettings.COLUMN_KEY + " = ?",
        new String[] { GlobalSettings.KEY_TUNER_SATELLITE_TYPE });
    }

    @Test
    public void testDeleteNotSupported() {
        try {
            extensionProvider.delete(TvExtensionContract.GeneralSettings.CONTENT_URI, null, null);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.delete(TvExtensionContract.DigitalTunerSettings.CONTENT_URI, null,
                null);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.delete(TvExtensionContract.AnalogTunerSettings.CONTENT_URI, null,
                null);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.delete(TvExtensionContract.DvbExtensions.CONTENT_URI, null, null);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.delete(TvExtensionContract.InteractiveSettings.CONTENT_URI, null,
                null);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.delete(TvExtensionContract.TunerOperators.CONTENT_URI, null, null);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.delete(ContentUris.withAppendedId(
                TvExtensionContract.TunerOperators.CONTENT_URI, 1), null, null);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testGetType() {
        Assert.assertEquals("vnd.android.cursor.dir/vnd.android.media.tv.extensions.global_setting",
            extensionProvider.getType(TvExtensionContract.GlobalSettings.CONTENT_URI));
        Assert.assertEquals("vnd.android.cursor.item/vnd.android.media.tv.extensions."
            + "global_setting", extensionProvider.getType(Uri.withAppendedPath(
            TvExtensionContract.GlobalSettings.CONTENT_URI, "a_key")));

        Assert.assertEquals("vnd.android.cursor.item/vnd.android.media.tv.extensions."
            + "general_setting",
            extensionProvider.getType(TvExtensionContract.GeneralSettings.CONTENT_URI));
        Assert.assertEquals("vnd.android.cursor.item/vnd.android.media.tv.extensions."
            +"digital_tuner_setting",
            extensionProvider.getType(TvExtensionContract.DigitalTunerSettings.CONTENT_URI));
        Assert.assertEquals("vnd.android.cursor.item/vnd.android.media.tv.extensions."
            + "analog_tuner_setting",
            extensionProvider.getType(TvExtensionContract.AnalogTunerSettings.CONTENT_URI));
        Assert.assertEquals("vnd.android.cursor.item/vnd.android.media.tv.extensions.dvb_extension",
            extensionProvider.getType(TvExtensionContract.DvbExtensions.CONTENT_URI));
        Assert.assertEquals("vnd.android.cursor.item/vnd.android.media.tv.extensions."
            + "interactive_setting",
            extensionProvider.getType(TvExtensionContract.InteractiveSettings.CONTENT_URI));

        Assert.assertEquals("vnd.android.cursor.dir/vnd.android.media.tv.extensions."
            + "tuner_operators",
            extensionProvider.getType(TvExtensionContract.TunerOperators.CONTENT_URI));
        Assert.assertEquals("vnd.android.cursor.item/vnd.android.media.tv.extensions."
            + "tuner_operators",
            extensionProvider.getType(ContentUris.withAppendedId(
            TvExtensionContract.TunerOperators.CONTENT_URI, 1)));
    }

    @Test
    public void testInsertNotSupported() {
        ContentValues values = new ContentValues();
        try {
            extensionProvider.insert(TvExtensionContract.GeneralSettings.CONTENT_URI, values);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expectedTvExtensionContract.DigitalTunerSettings.SUPERIMPOSE_SETUP_LANG2,,
        }
        try {
            extensionProvider.insert(TvExtensionContract.DigitalTunerSettings.CONTENT_URI, values);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.insert(TvExtensionContract.AnalogTunerSettings.CONTENT_URI, values);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.insert(TvExtensionContract.DvbExtensions.CONTENT_URI, values);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.insert(TvExtensionContract.InteractiveSettings.CONTENT_URI, values);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            extensionProvider.insert(TvExtensionContract.TunerOperators.CONTENT_URI, values);
            Assert.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testGobalKey() {
       String settingKey = "user_preferred_audio_language";
       String newValue = "es";
       Uri settingUri = Uri.withAppendedPath(
           TvExtensionContract.GlobalSettings.CONTENT_URI,
           settingKey
       );

    }
    @Test
    public void testInsertGlobalSettings() {
        ContentValues values = new ContentValues();
        String key = "test1";
        int originalValue = 10;
        values.put(TvExtensionContract.GlobalSettings.COLUMN_KEY, key);
        values.put(TvExtensionContract.GlobalSettings.COLUMN_VALUE, originalValue);
        extensionProvider.insert(TvExtensionContract.GlobalSettings.CONTENT_URI, values);
        assertUpdateAndQuery(GlobalSettings.CONTENT_URI,
            GlobalSettings.COLUMN_VALUE,
            originalValue,
            GlobalSettings.COLUMN_KEY + " = ?",
            new String[] { key });
         Uri deleteUri = Uri.parse("content://" + TvExtensionContract.AUTHORITY)
                 .buildUpon()
                 .appendPath(TvExtensionContract.GLOBAL_SETTING_TABLE)
                 .appendPath(key)
                 .build();

        extensionProvider.delete(deleteUri, null, null);
        String selection =  GlobalSettings.COLUMN_KEY + " = ?";
        int value = getIntFromSqlTable(GlobalSettings.CONTENT_URI, GlobalSettings.COLUMN_VALUE,
            selection, new String[] { key });
        Assert.assertNotEquals(value, originalValue);
    }
}

