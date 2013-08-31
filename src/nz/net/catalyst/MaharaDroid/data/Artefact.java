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

package nz.net.catalyst.MaharaDroid.data;

import java.io.File;

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.ui.ArtifactSettingsActivity;
import nz.net.catalyst.MaharaDroid.upload.TransferService;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Artefact extends Object implements Parcelable {
    static final String TAG = LogConfig.getLogTag(Artefact.class);
    // whether DEBUG level logging is enabled (whether globally, or explicitly
    // for this log tag)
    static final boolean DEBUG = LogConfig.isDebug(TAG);
    // whether VERBOSE level logging is enabled
    static final boolean VERBOSE = LogConfig.VERBOSE;

    private long id = 0;
    private long time;
    private String filename;
    private String title;
    private String description;
    private String tags;
    private Long saved_id;
    private String journal_id;
    private boolean is_draft = false;
    private boolean allow_comments = false;

    public Long getId() {
        return id;
    }

    public Long getTime() {
        return time;
    }

    public String getFilename() {
        return filename;
    }

    public String getBaseFilename(Context context) {
        // Returns the base of the actual file name - not the content store file
        // (which would just be it's content:// ... /ID)
        String filepath = this.getFilePath(context);

        return (filepath == null) ? null : filepath.substring(filepath.lastIndexOf("/") + 1);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public String getJournalId() {
        return journal_id;
    }

    public boolean getIsDraft() {
        return is_draft;
    }

    public boolean getAllowComments() {
        return allow_comments;
    }

    public void setId(Long i) {
        id = i;
    }

    public void setTime(Long tm) {
        time = tm;
    }

    public void setFilename(String f) {
        filename = f;
    }

    public void setTitle(String t) {
        title = t;
    }

    public void setDescription(String d) {
        description = d;
    }

    public void setTags(String t) {
        tags = t;
    }

    public void setJournalId(String j) {
        journal_id = j;
    }

    public void setIsDraft(Boolean d) {
        is_draft = d;
    }

    public void setAllowComments(Boolean a) {
        allow_comments = a;
    }

    public String getGroup() {
        // In the meantime just set the article ID, i.e force no grouping
        return this.title;
    }

    public boolean isJournal() {
        return (journal_id != null && Long.parseLong(journal_id) > 0);
    }

    public boolean hasAttachment() {
        return (filename != null);
    }

    public boolean canUpload() {
        // journal must have title and description otherwise a title and
        // filename will do
        if ((title != null && title.trim().length() > 0)
                && ((isJournal() && (description != null && description.trim().length() > 0))
                || filename != null)) {
            return true;
        }
        return false;
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // TODO Auto-generated method stub
        dest.writeLong(id);
        dest.writeString(filename);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(tags);
        dest.writeLong(time);
        dest.writeString(journal_id);
        dest.writeBooleanArray(new boolean[] { is_draft, allow_comments });
        Log.d("Artefact", "writeToParcel: is_draft: " + is_draft);
        Log.d("Artefact", "writeToParcel: allow comments: " + allow_comments);

    }

    /**
     * Required for Parcelables
     */
    public static final Parcelable.Creator<Artefact> CREATOR = new Parcelable.Creator<Artefact>() {
        @Override
		public Artefact createFromParcel(Parcel in) {
            return new Artefact(in);
        }

        @Override
		public Artefact[] newArray(int size) {
            return new Artefact[size];
        }
    };

    /**
     * For use by CREATOR
     *
     * @param in
     */
    private Artefact(Parcel in) {
        id = in.readLong();
        filename = in.readString();
        title = in.readString();
        description = in.readString();
        tags = in.readString();
        time = in.readLong();
        journal_id = in.readString();
        boolean[] b = new boolean[2];
        in.readBooleanArray(b);
        is_draft = b[0];
        allow_comments = b[1];

        Log.d("Artefact", "instantiate from parcel: is_draft: " + is_draft);
        Log.d("Artefact", "instantiate from parcel: allow comments: " + allow_comments);
    }

    public Artefact(String f, String t, String d, String tgs, String j, boolean dr, boolean a) {
        filename = f;
        title = t;
        description = d;
        tags = tgs;
        journal_id = j;
        is_draft = dr;
        allow_comments = a;
        time = System.currentTimeMillis();
    }

    public Artefact(Long i, Long tm, String f, String t, String d, String tgs, Long sid, String j, boolean dr, boolean a) {
        id = i;
        filename = f;
        title = t;
        description = d;
        tags = tgs;
        time = tm;
        saved_id = sid;
        journal_id = j;
        is_draft = dr;
        allow_comments = a;
    }

    public Artefact(Long i, String f, String t, String d, String tgs, Long sid, String j, boolean dr, boolean a) {
        id = i; // may be null for a new item.
        filename = f;
        title = t;
        description = d;
        tags = tgs;
        saved_id = sid;
        journal_id = j;
        is_draft = dr;
        allow_comments = a;
        time = System.currentTimeMillis();
    }

    public Artefact(String f) {
        filename = f;
        time = System.currentTimeMillis();
    }

    public void upload(Boolean auto, Context mContext) {
        Intent i = new Intent(mContext, TransferService.class);
        i.putExtra("artefact", this);
        mContext.startService(i);
    }

    public void delete(Context mContext) {
        ArtefactUtils.deleteSavedArtefact(mContext, id);
    }

    public void view(Context mContext) {
        try {
            Intent i;
            String mimetype = this.getFileMimeType(mContext);
            if (mimetype == null) {
                i = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(this.getFilename()));
            } else {
                i = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(this.getFilename()), mimetype);
                if (DEBUG)
                    Log.d(TAG, "Artefact mimetype '" + mimetype + "'");
            }
            mContext.startActivity(i);
        } catch (ActivityNotFoundException e) {
            // If activity not found you need to set SetDataAndType .. ideally
            // we'd get the type
            // from the content query
            Log.e(TAG, "Artefact view error - not activity found for '" + this.getFilename() + "'");
        }
    }

    public void edit(Context mContext) {
        Intent i = new Intent(mContext, ArtifactSettingsActivity.class);
        i.putExtra("artefact", this);
        mContext.startActivity(i);
    }

    public void save(Context mContext) {
        if (id != 0) { // update
            Log.d("Artefact", "save: is_draft: " + is_draft);
            Log.d("Artefact", "save: allow comments: " + allow_comments);
            ArtefactUtils.update(mContext, id, filename, title, description, tags, saved_id, journal_id, is_draft, allow_comments);

        } else { // add
            ArtefactUtils.add(mContext, filename, title, description, tags, journal_id, is_draft, allow_comments);
        }
    }

    public void load(Context mContext, Long id) {
        Artefact ta = ArtefactUtils.loadSavedArtefact(mContext, id);

        this.id = ta.getId();
        this.time = ta.getTime();
        filename = ta.getFilename();
        title = ta.getTitle();
        description = ta.getDescription();
        tags = ta.getTags();
        saved_id = ta.saved_id;
        journal_id = ta.getJournalId();
        is_draft = ta.getIsDraft();
        allow_comments = ta.getAllowComments();

    }

    public String getFilePath(Context context) {
        if (filename == null)
            return null;

        Uri uri = Uri.parse(filename);

        String file_path = null;

        if (DEBUG)
            Log.d(TAG, "URI = '" + uri.toString() + "', scheme = '" + uri.getScheme() + "'");

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            // Get the filename of the media file and use that as the default
            // title.
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(uri, new String[] { android.provider.MediaStore.MediaColumns.DATA,
                    android.provider.MediaStore.MediaColumns.MIME_TYPE }, null, null, null);
            if (cursor != null) {
                if (DEBUG)
                    Log.d(TAG, "cursor query succeeded");
                cursor.moveToFirst();
                try {
                    file_path = cursor.getString(0);
                } catch (android.database.CursorIndexOutOfBoundsException e) {
                    if (DEBUG)
                        Log.d(TAG, "couldn't get file_path from cursor");
                    return null;
                }
                cursor.close();
            } else {
                if (DEBUG)
                    Log.d(TAG, "cursor query failed");
                return null;
            }
        } else {
            if (DEBUG)
                Log.d(TAG, "Not content scheme - returning native path");
            // Not a content query
            file_path = uri.getPath();
            File t = new File(file_path);
            if (!t.exists())
                return null;
        }

        // Online image not in gallery
        // TODO check
        // http://jimmi1977.blogspot.co.nz/2012/01/android-api-quirks-getting-image-from.html
        // for workaround
        if (file_path == "null") {
            return null;
        }
        if (DEBUG)
            Log.d(TAG, "file path valid [" + file_path + "]");
        return file_path;
    }

    public String getFileMimeType(Context context) {
        if (filename == null)
            return null;

        Uri uri = Uri.parse(filename);

        String mimetype = null;

        if (DEBUG)
            Log.d(TAG, "URI = '" + uri.toString() + "', scheme = '" + uri.getScheme() + "'");

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            // Get the filename of the media file and use that as the default
            // title.
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(uri, new String[] { android.provider.MediaStore.MediaColumns.MIME_TYPE }, null, null, null);
            if (cursor != null) {
                if (DEBUG)
                    Log.d(TAG, "cursor query succeeded");
                cursor.moveToFirst();
                try {
                    mimetype = cursor.getString(0);
                } catch (android.database.CursorIndexOutOfBoundsException e) {
                    if (DEBUG)
                        Log.d(TAG, "couldn't get file_path from cursor");
                    return null;
                }
                cursor.close();
            } else {
                if (DEBUG)
                    Log.d(TAG, "cursor query failed");
                return null;
            }
            // TODO .. Yuck - extend this?
        } else if (uri.getLastPathSegment().endsWith(".mp4")) {
            mimetype = "video/mp4";
        } else if (uri.getLastPathSegment().endsWith(".mp3")) {
            mimetype = "audio/mp3";
        } else if (uri.getLastPathSegment().endsWith(".m4a")) {
            mimetype = "audio/mpeg";
        }

        return mimetype;
    }

    public Bitmap getFileThumbData(Context context) {
        return Utils.getFileThumbData(context, this.getFilename());
    }
}
