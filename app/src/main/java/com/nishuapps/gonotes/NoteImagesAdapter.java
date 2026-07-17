package com.nishuapps.gonotes;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

// Adapter for showing multiple note images stacked vertically (Google Keep style)
public class NoteImagesAdapter extends RecyclerView.Adapter<NoteImagesAdapter.ImageViewHolder> {
	
	public interface OnImageActionListener {
		void onImageClicked(String path, int position);
		void onRemoveClicked(String path, int position);
	}
	
	private final ArrayList<String> imagePaths;
	private final OnImageActionListener listener;
	
	public NoteImagesAdapter(ArrayList<String> imagePaths, OnImageActionListener listener) {
		this.imagePaths = imagePaths;
		this.listener = listener;
	}
	
	@NonNull
	@Override
	public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_image, parent, false);
		return new ImageViewHolder(view);
	}
	
	@Override
	public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
		String path = imagePaths.get(position);
		
		// Quality is preserved: Glide loads a high-quality version that fits the screen.
		// The original file on disk is never resized or compressed.
		Glide.with(holder.itemView.getContext())
				.load(path)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.override(1080)
				.centerInside()
				.into(holder.imageView);
		
		holder.imageView.setOnClickListener(v -> {
			if (listener != null) listener.onImageClicked(path, holder.getAdapterPosition());
		});
		
		holder.removeButton.setOnClickListener(v -> {
			if (listener != null) listener.onRemoveClicked(path, holder.getAdapterPosition());
		});
	}
	
	@Override
	public int getItemCount() {
		return imagePaths.size();
	}
	
	static class ImageViewHolder extends RecyclerView.ViewHolder {
		ImageView imageView;
		TextView removeButton;
		
		ImageViewHolder(View itemView) {
			super(itemView);
			imageView = itemView.findViewById(R.id.imageNoteItem);
			removeButton = itemView.findViewById(R.id.buttonRemoveThisImage);
		}
	}
}