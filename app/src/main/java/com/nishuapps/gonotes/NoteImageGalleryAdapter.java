package com.nishuapps.gonotes;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

public class NoteImageGalleryAdapter extends RecyclerView.Adapter<NoteImageGalleryAdapter.GalleryViewHolder> {

    private final ArrayList<String> imagePaths;

    public NoteImageGalleryAdapter(ArrayList<String> imagePaths) {
        this.imagePaths = imagePaths;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_image, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        String path = imagePaths.get(position);
        Glide.with(holder.itemView.getContext())
                .load(path.startsWith("content://") ? Uri.parse(path) : new File(path))
                .into(holder.zoomImageView);
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ZoomableImageView zoomImageView;

        GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            zoomImageView = itemView.findViewById(R.id.galleryZoomImage);
        }
    }
}