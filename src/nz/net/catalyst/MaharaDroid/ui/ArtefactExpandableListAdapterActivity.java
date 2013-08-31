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

package nz.net.catalyst.MaharaDroid.ui;

import java.util.ArrayList;
import java.util.Date;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.ArtefactUtils;
import nz.net.catalyst.MaharaDroid.data.SyncUtils;
import nz.net.catalyst.MaharaDroid.ui.about.AboutActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ArtefactExpandableListAdapterActivity extends Activity {
    static final String TAG = LogConfig.getLogTag(ArtefactExpandableListAdapterActivity.class);
    // whether DEBUG level logging is enabled (whether globally, or explicitly
    // for this log tag)
    static final boolean DEBUG = LogConfig.isDebug(TAG);
    // whether VERBOSE level logging is enabled
    static final boolean VERBOSE = LogConfig.VERBOSE;

    private static Context mContext;

    // private ArrayList<Artefact> items = new ArrayList<Artefact>();
    private ArtefactExpandableListAdapter adapter;

    private ExpandableListView listview;

    ArtefactContentObserver acObserver = new ArtefactContentObserver(new Handler());

    private class ArtefactContentObserver extends ContentObserver {

        public ArtefactContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (VERBOSE)
                Log.v(TAG, "ArtefactContentObserver: onChange() called");
            updateView();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.artefacts);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);

        this.getContentResolver().registerContentObserver(ArtefactUtils.URI, true, acObserver);

        // Load artefacts on the UI
        updateView();

        // A content view has now be set so lets set the title.
        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.app_name));

        if (VERBOSE) Log.v(TAG, "onCreate() called");
    }

    @Override
    public void onResume() {
        //if (DEBUG) Log.d(TAG, "in onResume");

        super.onResume();

        /* Opens the options menu (normally opened when you touch the menu button)
         * If openOptionsMenu() called in activity - can break app as window etc not fully built
         * so do this way
         */
        //new Handler().postDelayed(new Runnable() {
        //    public void run() {
        //        openOptionsMenu();
        //    }
        //}, 1000);

    // if ( VERBOSE ) Log.v(TAG, "onResume() calls loadSavedArtefacts");
    // updateView();
    }

    @Override
	public void onDestroy() {
        super.onDestroy();

        this.getContentResolver().unregisterContentObserver(acObserver);
    }

    private void updateView() {
        // First lets get our DB object
        // Lets see how many saved artefacts we have
        Artefact[] a_array = ArtefactUtils.loadSavedArtefacts(mContext);

        if (DEBUG)
            Log.d(TAG, "returned " + a_array.length + " items");

        // If none then we show introduction screen
        if (a_array == null || a_array.length <= 0) {
            //if (DEBUG) Log.d(TAG, "showing introduction");
            // Show the introduction screen, hide saved items list
            ((RelativeLayout) findViewById(R.id.introduction)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.artefacts_help)).setText(Html.fromHtml(getString(R.string.artefacts_help)));
            ((RelativeLayout) findViewById(R.id.artefacts)).setVisibility(View.GONE);

            // Else we have some artefacts to show lets load them up in our
            // ExpandableListAdapter
        } else {
            //if (DEBUG) Log.d(TAG, "showing saved items");
            adapter = new ArtefactExpandableListAdapter(this, new ArrayList<String>(),
                    new ArrayList<ArrayList<Artefact>>());
            listview = (ExpandableListView) findViewById(R.id.listView);
            listview.setAdapter(adapter);

            // Hide the introduction bits, show saved items list
            ((RelativeLayout) findViewById(R.id.introduction)).setVisibility(View.GONE);
            ((RelativeLayout) findViewById(R.id.artefacts)).setVisibility(View.VISIBLE);

            for (int i = 0; i < a_array.length && a_array[i] != null; i++) {
                if (DEBUG)
                    Log.d(TAG, "adding item " + a_array[i].getFilename() + " [" + i + "]");
                adapter.addItem(a_array[i]);
            }

            // notifiyDataSetChanged triggers the re-draw
            // Set this blank adapter to the list view
            adapter.notifyDataSetChanged();
            listview.invalidate();
        }
    }


    public void myOnClickStartBut(View v) {
        openOptionsMenu();
    }


    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
        //if (DEBUG) Log.d(TAG, "in onCreateOptionsMenu");

        boolean result = super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.artefact_options, menu);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.option_delete:
            ArtefactUtils.deleteAllSavedArtefacts(mContext);
            updateView();
            break;
        case R.id.option_upload:
            ArtefactUtils.uploadAllSavedArtefacts(mContext);
            updateView();
            break;
        case R.id.about:
            startActivity(new Intent(this, AboutActivity.class));
            break;
        case R.id.option_pref:
            intent = new Intent(this, EditPreferences.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            break;
        case R.id.option_account:
            startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS).putExtra(Settings.EXTRA_AUTHORITIES, new String[] { GlobalResources.SYNC_AUTHORITY }));
            break;
        case R.id.option_camera:
            startActivityForResult(Utils.makeCameraIntent(mContext), GlobalResources.REQ_CAMERA_RETURN);
            break;
        case R.id.option_gallery:
            intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, GlobalResources.REQ_GALLERY_RETURN);
            break;
        case R.id.option_compose:
            intent = new Intent(this, ArtifactSettingsActivity.class);
            intent.putExtra("writejournal", true);
            startActivity(intent);
            break;
        }
        return true;
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            String imageFile = null;
            Intent i = new Intent(this, ArtifactSettingsActivity.class);

            switch (requestCode) {
            case GlobalResources.REQ_CAMERA_RETURN:
                if (intent == null) {
                    Log.w(TAG, "Empty intent received from request code '" + requestCode + "'");
                    imageFile = GlobalResources.TEMP_PHOTO_URI.toString();
                } else {
                    imageFile = ((Uri) intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)).toString();
                }
                break;
            case GlobalResources.REQ_GALLERY_RETURN:
                imageFile = intent.getData().toString();
                break;
            }

            i.putExtra("uri", new String[] { imageFile });
            startActivity(i);
        }
    }

    // public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo
    // menuInfo) {
    // adapter.onCreateContextMenu(menu, v, menuInfo);
    // }
    // public boolean onContextItemSelected(MenuItem item) {
    // adapter.onContextItemSelected(item);
    // return false;
    // }
    public class ArtefactExpandableListAdapter extends BaseExpandableListAdapter implements OnClickListener {
        @Override
        public boolean areAllItemsEnabled()
        {
            return true;
        }

        private final Context mContext;

        private final ArrayList<String> groups;

        private final ArrayList<ArrayList<Artefact>> children;

        public ArtefactExpandableListAdapter(Context context, ArrayList<String> groups,
                ArrayList<ArrayList<Artefact>> children) {
            this.mContext = context;
            this.groups = groups;
            this.children = children;
        }

        public void addItem(Artefact art) {
            if (!groups.contains(art.getGroup())) {
                groups.add(art.getGroup());
                if (VERBOSE)
                    Log.v(TAG, "adding item '" + art.getGroup() + "'");
            }
            int index = groups.indexOf(art.getGroup());
            if (children.size() < index + 1) {
                children.add(new ArrayList<Artefact>());
            }
            children.get(index).add(art);

        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return children.get(groupPosition).get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        // Return a child view. You can load your custom layout here.
        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            Artefact art = (Artefact) getChild(groupPosition, childPosition);
            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.artefact_row_child, null);
            }
            Date date = new Date(art.getTime());

            LinearLayout l;
            // TODO General YUCK .. need to clean up and create a Journal /
            // MaharaProvide class / utility methods
            // && Long.valueOf(art.getJournalId()) <= 0
            if (art.isJournal()) {
                String[][] journals = SyncUtils.getJournals("", mContext); // TODO
                                                                           // consider
                                                                           // refreshing
                                                                           // onResume
                if (journals != null) {
                    String[] journalKeys = journals[0];
                    String[] journalValues = journals[1];

                    for (int i = 0; i < journalKeys.length && journalValues[i] != null; i++) {
                        if (art.getJournalId().equals(journalKeys[i])) {
                            ((TextView) convertView.findViewById(R.id.txtArtefactJournal)).setText(journalValues[i]);
                            break;
                        }
                    }
                    ((CheckBox) convertView.findViewById(R.id.txtArtefactIsDraft)).setChecked(art.getIsDraft());
                    ((CheckBox) convertView.findViewById(R.id.txtArtefactAllowComments)).setChecked(art.getAllowComments());
                }
                // TDODO hide layout
                l = (LinearLayout) convertView.findViewById(R.id.ArtefactJournalLayout);
                if (l != null)
                    l.setVisibility(LinearLayout.VISIBLE);
                l = (LinearLayout) convertView.findViewById(R.id.ArtefactJournalExtrasLayout);
                if (l != null)
                    l.setVisibility(LinearLayout.VISIBLE);

                ((TextView) convertView.findViewById(R.id.txtArtefactDescriptionLabel)).setText(mContext.getResources().getString(R.string.upload_journal_description_label));

            } else {
                // TDODO hide layout
                l = (LinearLayout) convertView.findViewById(R.id.ArtefactJournalLayout);
                if (l != null)
                    l.setVisibility(LinearLayout.GONE);
                l = (LinearLayout) convertView.findViewById(R.id.ArtefactJournalExtrasLayout);
                if (l != null)
                    l.setVisibility(LinearLayout.GONE);

                ((TextView) convertView.findViewById(R.id.txtArtefactDescriptionLabel)).setText(mContext.getResources().getString(R.string.upload_file_description_label));
            }
            ((TextView) convertView.findViewById(R.id.txtArtefactTime)).setText(date.toString());
            ((TextView) convertView.findViewById(R.id.txtArtefactDescription)).setText(art.getDescription());
            ((TextView) convertView.findViewById(R.id.txtArtefactTags)).setText(art.getTags());

            l = (LinearLayout) convertView.findViewById(R.id.ArtefactFileLayout);
            if (art.getFilename() != null) {
                ((TextView) convertView.findViewById(R.id.txtArtefactFilename)).setText(art.getFilename());

                ImageView iv = (ImageView) convertView.findViewById(R.id.txtArtefactFileThumb);
                iv.setClickable(true);
                iv.setOnClickListener(this);
                iv.setTag(art);

                Bitmap bm = art.getFileThumbData(mContext);
                if (bm != null) {
                    iv.setImageBitmap(bm);
                    iv.invalidate();
                }
                if (l != null)
                    l.setVisibility(LinearLayout.VISIBLE);
            } else {
                if (l != null)
                    l.setVisibility(LinearLayout.GONE);
            }

            ((Button) convertView.findViewById(R.id.btnEdit)).setOnClickListener(this);
            ((Button) convertView.findViewById(R.id.btnEdit)).setTag(art);
            ((Button) convertView.findViewById(R.id.btnDelete)).setOnClickListener(this);
            ((Button) convertView.findViewById(R.id.btnDelete)).setTag(art);
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return children.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groups.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return groups.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        // Return a group view. You can load your custom layout here.
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            String group = (String) getGroup(groupPosition);
            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.artefact_row, null);
            }
            TextView tv = (TextView) convertView.findViewById(R.id.title);
            tv.setText(group);

            // TODO .. lets make this more efficient ;)
            // Default to image - change if journal
            for (int i = 0; i < children.get(groupPosition).size(); i++) {
                Artefact a = children.get(groupPosition).get(i);
                if (a.isJournal()) {
                    ImageView iv = (ImageView) convertView.findViewById(R.id.artefact_icon);
                    iv.setImageResource(R.drawable.ic_menu_compose);
                    break;
                }
            }

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int arg0, int arg1) {
            return true;
        }

        @Override
        public void onClick(View v) {

            v.getTag();
            if (DEBUG)
                Log.d(TAG, "onChildClick detected");
            Artefact a = (Artefact) v.getTag();
            ;

            switch (v.getId()) {
            case R.id.btnEdit:
                a.edit(mContext);
                break;
            case R.id.txtArtefactFileThumb:
                a.view(mContext);
                break;
            case R.id.btnDelete:
                a.delete(mContext);
                updateView();
                break;
            }
        }

    }
}
