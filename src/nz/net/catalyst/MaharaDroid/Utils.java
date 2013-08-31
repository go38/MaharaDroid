/*  
 *  MaharaDroid -  Artefact uploader
 * 
 *  This file is part of MaharaDroid.
 * 
 *  Copyright [2010] [Catalyst IT Limited]  
 *  
 *  This file is free software: you may copy, redistribute and/or modify it  
 *  under the terms of the GNU General Public License as published by the  
 *  Free Software Foundation, either version 3 of the License, or (at your  
 *  option) any later version.  
 *  
 *  This file is distributed in the hope that it will be useful, but  
 *  WITHOUT ANY WARRANTY; without even the implied warranty of  
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU  
 *  General Public License for more details.  
 *  
 *  You should have received a copy of the GNU General Public License  
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 */

package nz.net.catalyst.MaharaDroid;

import org.json.JSONException;
import org.json.JSONObject;

import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.ui.ArtefactExpandableListAdapterActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

public class Utils {
    static final String TAG = LogConfig.getLogTag(Utils.class);
    // whether DEBUG level logging is enabled (whether globally, or explicitly
    // for this log tag)
    static final boolean DEBUG = LogConfig.isDebug(TAG);
    // whether VERBOSE level logging is enabled
    static final boolean VERBOSE = LogConfig.VERBOSE;

    public static boolean canUpload(Context context) {

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean allowWiFi = false, allowMobile = false;

        // Haven't confirmed upload conditions.
        // TODO validate it's OK to have this here.
        if (!mPrefs.getBoolean("Upload Conditions Confirmed", false)) {
            return false;
        }

        String mSetting = mPrefs.getString(context.getResources().getString(R.string.pref_upload_connection_key), "");

        // Check for no setting - default to phone
        if (mSetting.length() == 0) {
            allowWiFi = allowMobile = true;
        }
        if (mSetting.contains("wifi"))
            allowWiFi = true;
        if (mSetting.contains("mobile"))
            allowMobile = true;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info != null) {
            int netType = info.getType();

            if (netType == ConnectivityManager.TYPE_WIFI) {
                if (allowWiFi && info.isConnected())
                    return true;
            } else if (netType == ConnectivityManager.TYPE_MOBILE) {
                if (allowMobile && info.isConnected())
                    return true;
            }
        } else {
            // Assume we're a mobile (we're an Android after all)
            return (allowMobile);
        }

        return false;
    }

    public static String getUploadURLPref(Context context) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        String upload_url = mPrefs.getString(context.getResources().getString(R.string.pref_upload_url_key),
                context.getResources().getString(R.string.pref_upload_url_default)).trim();

        // If the part overrides the whole - just go with the part.
        if (upload_url.startsWith("http://")) {
            if (DEBUG)
                Log.d(TAG, "setting upload url to '" + upload_url + "'");
            return upload_url;
        }

        String base_url = mPrefs.getString(context.getResources().getString(R.string.pref_base_url_key),
                context.getResources().getString(R.string.pref_base_url_default)).trim().toLowerCase();
        if (!base_url.startsWith("http"))
            base_url = "http://" + base_url;

        if (!base_url.endsWith("/") && !upload_url.startsWith("/"))
            base_url = base_url + "/";
        // multiple joining '//' are fine
        upload_url = base_url + upload_url;

        if (DEBUG)
            Log.d(TAG, "setting upload url to '" + upload_url + "'");
        return upload_url;
    }

    public static String updateTokenFromResult(JSONObject json, Context context) {
        String newToken = null;
        if (json == null || json.has("fail")) {
            String err_str = null;
            try {
                err_str = (json == null) ? "Unknown Failure" : json.getString("fail");
            } catch (JSONException e) {
                err_str = "Unknown Failure";
            }
            Log.e(TAG, "Auth fail: " + err_str);

        } else if (json.has("success")) {
            try {
                newToken = json.getString("success");

                SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                mPrefs.edit()
                        .putString(context.getResources().getString(R.string.pref_auth_token_key), newToken)
                        .commit();

                // Here we want to check a check-sum for 'last-modified' and if
                // newer content exists
                // then process out new user-data
                Log.i(TAG, "Token found, re-keying auth-token");

            } catch (JSONException e) {
                Log.e(TAG, "Failed to get success token from result.");
            }
        }
        return newToken;
    }

    /**
     * Show a notification while this service is running.
     */
    public static void showNotification(int id, CharSequence title, CharSequence description, Intent intent, Context context) {
        NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon_notify, title,
                System.currentTimeMillis());

        PendingIntent contentIntent = null;
        // The PendingIntent to launch our activity if the user selects this
        // notification
        if (intent == null) {
            contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, ArtefactExpandableListAdapterActivity.class), 0);
        } else {
            contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
        }
        if (description == null) {
            description = title;
        }

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(context, title, description, contentIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Send the notification.
        mNM.notify(id, notification);
    }

    public static void cancelNotification(int id, Context context) {
        NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNM.cancel(id);
    }

    public static Intent makeCameraIntent(Context context) {

        // define the file-name to save photo taken by Camera activity
        String fileName = GlobalResources.TEMP_PHOTO_FILENAME;

        if (VERBOSE)
            Log.v(TAG, "invoking camera (" + fileName + ")");

        // create parameters for Intent with filename
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image capture by camera for MaharaDroid");

        // imageUri is the current activity attribute, define and save it for
        // later usage (also in onSaveInstanceState)
        GlobalResources.TEMP_PHOTO_URI = context.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (VERBOSE)
            Log.v(TAG, "imageUri is '" + GlobalResources.TEMP_PHOTO_URI.toString() + "'");

        // create new Intent
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(MediaStore.EXTRA_OUTPUT, GlobalResources.TEMP_PHOTO_URI);
        i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        return i;
    }

    public static Bitmap getFileThumbData(Context context, String filename) {
        if (filename == null)
            return null;

        Uri uri = Uri.parse(filename);
        Bitmap bm = null;

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            // Get the filename of the media file and use that as the default
            // title.
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(uri, new String[] { android.provider.MediaStore.MediaColumns._ID }, null, null, null);
            if (cursor != null) {
                if (VERBOSE)
                    Log.v(TAG, "getFileThumbData cursor query succeeded for '" + filename + "'");
                cursor.moveToFirst();
                try {
                    Long id = cursor.getLong(0);
                    cursor.close();

                    if (uri.getPath().contains("images")) {
                        // Default to try image thumbnail ..
                        bm = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
                    } else if (uri.getPath().contains("video")) {
                        // else look for a video thumbnail
                        bm = MediaStore.Video.Thumbnails.getThumbnail(cr, id, MediaStore.Video.Thumbnails.MICRO_KIND, null);
                    }
                } catch (android.database.CursorIndexOutOfBoundsException e) {
                    if (DEBUG)
                        Log.d(TAG, "getFileThumbData couldn't get content from file cursor");
                }
                cursor.close();
            }
        }

        return bm;
    }
}
