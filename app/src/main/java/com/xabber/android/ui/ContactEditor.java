package com.xabber.android.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.dialog.ContactDeleteDialogFragment;

public class ContactEditor extends ContactViewer {

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, ContactEditor.class)
                .setAccount(account).setUser(user).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_alias:
                editAlias();
                return true;

            case R.id.action_edit_groups:
                startActivity(GroupEditor.createIntent(this, getAccount(), getBareAddress()));
                return true;

            case R.id.action_remove_contact:
                ContactDeleteDialogFragment.newInstance(getAccount(), getBareAddress())
                        .show(getFragmentManager(), "CONTACT_DELETE");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void editAlias() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_alias);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getBareAddress());
        input.setText(rosterContact.getName());
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    RosterManager.getInstance().setName(getAccount(), getBareAddress(), input.getText().toString());
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
