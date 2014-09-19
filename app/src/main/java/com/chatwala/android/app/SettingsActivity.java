package com.chatwala.android.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import com.chatwala.android.R;
import com.chatwala.android.camera.ChatwalaActivity;
import com.chatwala.android.files.FileManager;
import com.chatwala.android.messages.MessageManager;
import com.chatwala.android.ui.SettingsRow;
import com.chatwala.android.util.DeliveryMethod;

/**
 * Created with IntelliJ IDEA.
 * User: Eliezer
 * Date: 5/15/2014
 * Time: 5:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsActivity extends BaseChatwalaActivity {
    private static final String BLOW_AWAY_STORAGE_TAG = "blow_away_storage";
    private static final String MARK_ALL_AS_READ_TAG = "mark_all_as_read";

    private SettingsRow messagePreviewRow;
    private SettingsRow deliveryMethodRow;
    /*private SettingsRow refreshIntervalRow;
    private SettingsRow maxDiskUsageRow;*/

    private boolean previewOnSelectInitialFired = false;
    private boolean deliveryOnSelectInitialFired = false;
    /*private boolean refreshOnSelectInitialFired = false;
    private boolean diskOnSelectInitialFired = false;*/

    private SettingsRow editProfilePicRow;
    private SettingsRow markAllAsReadRow;
    private SettingsRow blowAwayStorageRow;

    private SettingsRow termsRow;
    private SettingsRow privacyRow;
    private SettingsRow feedbackRow;
    private SettingsRow versionRow;

    private abstract static class OnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}
    }

    private static class MessageRefreshIntervals {
        public static final long ONE_MINUTE = 60 * 60 * 1000;
        public static final long FIVE_MINUTES = ONE_MINUTE * 5;
        public static final long TEN_MINUTES = FIVE_MINUTES * 2;
        public static final long THIRTY_MINUTES = TEN_MINUTES * 3;
        public static final long ONE_HOUR = THIRTY_MINUTES * 2;
        public static final long TWO_HOURS = ONE_HOUR * 2;
    }

    private static class MaxDiskSpaceValues {
        public static final int TEN_MEG = 10;
        public static final int FIFTY_MEG = 50;
        public static final int ONE_HUNDRED_MEG = 100;
        public static final int FIVE_HUNDRED_MEG = 500;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        messagePreviewRow = (SettingsRow) findViewById(R.id.settings_row_preview);
        deliveryMethodRow = (SettingsRow) findViewById(R.id.settings_row_delivery);
        /*refreshIntervalRow = (SettingsRow) findViewById(R.id.settings_row_refresh);
        maxDiskUsageRow = (SettingsRow) findViewById(R.id.settings_row_disk_usage);*/

        markAllAsReadRow = (SettingsRow) findViewById(R.id.settings_row_mark_all_as_read);
        blowAwayStorageRow = (SettingsRow) findViewById(R.id.settings_row_blow_away_storage);
        editProfilePicRow = (SettingsRow) findViewById(R.id.settings_row_profile_pic);

        termsRow = (SettingsRow) findViewById(R.id.settings_row_terms);
        privacyRow = (SettingsRow) findViewById(R.id.settings_row_privacy);
        feedbackRow = (SettingsRow) findViewById(R.id.settings_row_feedback);
        versionRow = (SettingsRow) findViewById(R.id.settings_row_version);

        /**
         * values corresponding to i can be found in res/values.settings_arrys.xml
         */
        messagePreviewRow.setSelection(AppPrefs.shouldShowPreview() ? 0 : 1);
        messagePreviewRow.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!previewOnSelectInitialFired) {
                    previewOnSelectInitialFired = true;
                    return;
                }
                AppPrefs.setShouldShowPreview(i == 0);
            }
        });

        deliveryMethodRow.setSelection(AppPrefs.getDeliveryMethod().getMethod());
        deliveryMethodRow.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!deliveryOnSelectInitialFired) {
                    deliveryOnSelectInitialFired = true;
                    return;
                }
                switch(i) {
                    case 0:
                        AppPrefs.setDeliveryMethod(DeliveryMethod.SMS);
                        break;
                    case 1:
                        AppPrefs.setDeliveryMethod(DeliveryMethod.CWSMS);
                        break;
                    case 2:
                        AppPrefs.setDeliveryMethod(DeliveryMethod.EMAIL);
                        break;
                    case 3:
                        AppPrefs.setDeliveryMethod(DeliveryMethod.FB);
                        break;
                    case 4:
                        AppPrefs.setDeliveryMethod(DeliveryMethod.TOP_CONTACTS);
                        break;
                }
            }
        });

        /*long refreshInterval = AppPrefs.getMessageRefreshInterval();
        if(refreshInterval == MessageRefreshIntervals.ONE_MINUTE) {
            refreshIntervalRow.setSelection(0);
        }
        else if(refreshInterval == MessageRefreshIntervals.FIVE_MINUTES) {
            refreshIntervalRow.setSelection(1);
        }
        else if(refreshInterval == MessageRefreshIntervals.TEN_MINUTES) {
            refreshIntervalRow.setSelection(2);
        }
        else if(refreshInterval == MessageRefreshIntervals.THIRTY_MINUTES) {
            refreshIntervalRow.setSelection(3);
        }
        else if(refreshInterval == MessageRefreshIntervals.ONE_HOUR) {
            refreshIntervalRow.setSelection(4);
        }
        else if(refreshInterval == MessageRefreshIntervals.TWO_HOURS) {
            refreshIntervalRow.setSelection(5);
        }
        refreshIntervalRow.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!refreshOnSelectInitialFired) {
                    refreshOnSelectInitialFired = true;
                    return;
                }
                switch(i) {
                    case 0:
                        AppPrefs.setMessageRefreshInterval(MessageRefreshIntervals.ONE_MINUTE);
                        break;
                    case 1:
                        AppPrefs.setMessageRefreshInterval(MessageRefreshIntervals.FIVE_MINUTES);
                        break;
                    case 2:
                        AppPrefs.setMessageRefreshInterval(MessageRefreshIntervals.TEN_MINUTES);
                        break;
                    case 3:
                        AppPrefs.setMessageRefreshInterval(MessageRefreshIntervals.THIRTY_MINUTES);
                        break;
                    case 4:
                        AppPrefs.setMessageRefreshInterval(MessageRefreshIntervals.ONE_HOUR);
                        break;
                    case 5:
                        AppPrefs.setMessageRefreshInterval(MessageRefreshIntervals.TWO_HOURS);
                        break;
                }
            }
        });

        int maxDiskSpace = AppPrefs.getMaxDiskSpace();
        if(maxDiskSpace == MaxDiskSpaceValues.TEN_MEG) {
            maxDiskUsageRow.setSelection(0);
        }
        else if(maxDiskSpace == MaxDiskSpaceValues.FIFTY_MEG) {
            maxDiskUsageRow.setSelection(1);
        }
        else if(maxDiskSpace == MaxDiskSpaceValues.ONE_HUNDRED_MEG) {
            maxDiskUsageRow.setSelection(2);
        }
        else if(maxDiskSpace == MaxDiskSpaceValues.FIVE_HUNDRED_MEG) {
            maxDiskUsageRow.setSelection(3);
        }
        maxDiskUsageRow.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!diskOnSelectInitialFired) {
                    diskOnSelectInitialFired = true;
                    return;
                }

                switch(i) {
                    case 0:
                        AppPrefs.setMaxDiskSpace(MaxDiskSpaceValues.TEN_MEG);
                        break;
                    case 1:
                        AppPrefs.setMaxDiskSpace(MaxDiskSpaceValues.FIFTY_MEG);
                        break;
                    case 2:
                        AppPrefs.setMaxDiskSpace(MaxDiskSpaceValues.ONE_HUNDRED_MEG);
                        break;
                    case 3:
                        AppPrefs.setMaxDiskSpace(MaxDiskSpaceValues.FIVE_HUNDRED_MEG);
                        break;
                }
            }
        });*/

        markAllAsReadRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsConfirmationDialog frag =SettingsConfirmationDialog.newInstance(
                        R.string.mark_all_as_read_confirmation_title, R.string.mark_all_as_read_confirmation_message);
                frag.show(getSupportFragmentManager(), MARK_ALL_AS_READ_TAG);
            }
        });

        blowAwayStorageRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsConfirmationDialog frag =SettingsConfirmationDialog.newInstance(
                        R.string.blow_away_storage_confirmation_title, R.string.blow_away_storage_confirmation_message);
                frag.show(getSupportFragmentManager(), BLOW_AWAY_STORAGE_TAG);
            }
        });

        editProfilePicRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingsActivity.this, ProfilePicActivity.class));
            }
        });

        termsRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("http://www.chatwala.com/tos");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        privacyRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("http://www.chatwala.com/privacy");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        feedbackRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingsActivity.this, FeedbackActivity.class));
            }
        });

        versionRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ShowVersionDialog().show(getSupportFragmentManager(), "dialog");
            }
        });
    }

    private void onConfirmationDialogPositive(String tag) {
        if(BLOW_AWAY_STORAGE_TAG.equals(tag)) {
            FileManager.clearMessageStorage();
            Toast.makeText(this, R.string.blow_away_storage_confirmation, Toast.LENGTH_SHORT).show();
        }
        else if(MARK_ALL_AS_READ_TAG.equals(tag)) {
            MessageManager.markAllAsRead();
            Toast.makeText(this, R.string.mark_all_as_read_confirmation, Toast.LENGTH_SHORT).show();
        }
    }

    private void onConfirmationDialogNegative(String tag) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, ChatwalaActivity.class));
        finish();
    }

    public static class ShowVersionDialog extends DialogFragment {
        public ShowVersionDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle onSavedInstanceState) {
            String versionName;
            try {
                versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = "unknown";
            }
            String display = getString(R.string.version_information_version) + " " + versionName + "\n" +
                    getString(R.string.version_information_server) + " " + EnvironmentVariables.get().getDisplayString();
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.version_information_title)
                    .setMessage(display)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                        }
                    })
                    .setCancelable(true)
                    .create();
        }
    }

    public static class SettingsConfirmationDialog extends DialogFragment {
        private SettingsActivity activity;

        public SettingsConfirmationDialog() {}

        public static SettingsConfirmationDialog newInstance(int titleRes, int messageRes) {
            SettingsConfirmationDialog frag = new SettingsConfirmationDialog();
            Bundle args = new Bundle();
            args.putInt("title", titleRes);
            args.putInt("message", messageRes);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            this.activity = (SettingsActivity) activity;
        }

        @Override
        public Dialog onCreateDialog(Bundle onSavedInstanceState) {
            String message = getString(getArguments().getInt("message"));
            message = String.format(message, FileManager.getMessageStorageUsageInMb(),
                    FileManager.getTotalStorageSpaceInGb());
            Dialog d = new AlertDialog.Builder(getActivity())
                    .setTitle(getArguments().getInt("title"))
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            activity.onConfirmationDialogPositive(getTag());
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            activity.onConfirmationDialogNegative(getTag());
                            dialog.dismiss();
                        }
                    })
                    .setCancelable(false)
                    .create();
            d.setCancelable(false);
            return d;
        }
    }
}
