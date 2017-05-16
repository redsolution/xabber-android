package com.xabber.android.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.extension.file.FileManager;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;


public class LogFilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private File[] files;


    public void setFiles(File[] files) {
        if (files == null) {
            return;
        }

        this.files = files;
        Arrays.sort(files, Collections.reverseOrder());
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FileViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        FileViewHolder fileHolder = (FileViewHolder) holder;

        final File file = files[position];

        fileHolder.fileName.setText(file.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(holder.itemView.getContext(), holder.itemView);
                popup.inflate(R.menu.item_log_file);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_log_file_open:
                                viewFile(file, holder.itemView.getContext());
                                return true;

                            case R.id.action_log_file_send:
                                sendFile(file, holder.itemView.getContext());
                                return true;
                        }

                        return false;
                    }
                });
                popup.show();

            }
        });
    }

    private void viewFile(File file, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(FileManager.getFileUri(file), "text/plain");
        context.startActivity(intent);
    }

    private void sendFile(File file, Context context) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent .setType("vnd.android.cursor.dir/email");
        emailIntent .putExtra(Intent.EXTRA_STREAM, FileManager.getFileUri(file));
        emailIntent .putExtra(Intent.EXTRA_SUBJECT,
                file.getName().substring(0, file.getName().length() - 4).replaceAll("_"," ")
                        + " log file");
        context.startActivity(Intent.createChooser(emailIntent , "Send log file"));
    }

    @Override
    public int getItemCount() {
        if (files != null) {
            return files.length;
        }

        return 0;
    }

    private static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName;


        FileViewHolder(View itemView) {
            super(itemView);
            fileName = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}
