package io.qytc.gstsdk.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.tencent.rtmp.ui.TXCloudVideoView;

import java.util.List;

import io.qytc.gstsdk.R;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {

    private static final String                 TAG = VideoAdapter.class.getSimpleName();
    private              List<TXCloudVideoView> mList;
    private VideoAdapterListener mVideoAdapterListener;

    public VideoAdapter(List<TXCloudVideoView> list, VideoAdapterListener videoAdapterListener) {
        mList = list;
        this.mVideoAdapterListener = videoAdapterListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.small_videoview_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
//        holder.name.setText((CharSequence) mList.get(position).getTag());

        TXCloudVideoView videoView = mList.get(position);
        int childCount = holder.remoteLayout.getChildCount();
        Log.d(TAG, "onBindViewHolder: "+childCount);

        ViewGroup parent = (ViewGroup)videoView.getParent();
        if(parent!=null){
            parent.removeAllViews();
        }
        holder.remoteLayout.addView(videoView);
        holder.mianLayout.setOnClickListener(view -> {
            Log.d(TAG, "onClick: " + mList.get(position));
            mVideoAdapterListener.OnClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

//        TextView     name;
        LinearLayout remoteLayout;
        FrameLayout  mianLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            name = itemView.findViewById(R.id.remote_name_tv);
            remoteLayout = itemView.findViewById(R.id.remote_video_layout);
            mianLayout = itemView.findViewById(R.id.remote_layout);
        }
    }

    public interface VideoAdapterListener{
        void OnClick(int position);
    }
}
