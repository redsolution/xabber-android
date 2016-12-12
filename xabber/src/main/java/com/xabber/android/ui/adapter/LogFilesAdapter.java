package com.xabber.android.ui.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xabber.android.data.extension.file.FileManager;

import java.io.File;
import java.util.Arrays;


public class LogFilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private File[] files;


    public void setFiles(File[] files) {
        this.files = files;
        Arrays.sort(files);
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
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(FileManager.getFileUri(file), "text/plain");
                holder.itemView.getContext().startActivity(intent);
            }
        });
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
