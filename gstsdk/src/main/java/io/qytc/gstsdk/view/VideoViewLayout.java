package io.qytc.gstsdk.view;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.qytc.gstsdk.R;
import io.qytc.gstsdk.adapter.VideoAdapter;

import com.tencent.rtmp.TXLog;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloudDef;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoViewLayout extends RelativeLayout {
    private final static String TAG = VideoViewLayout.class.getSimpleName();
    private Context mContext;
    private ArrayList<TXCloudVideoView> mVideoViewList;
    private ArrayList<RelativeLayout.LayoutParams> mFloatParamList;
    private ArrayList<LayoutParams> mGrid7ParamList;
    private LinearLayout mBigVideoView;

    private VideoAdapter mVideoAdapter;
    private RecyclerView mRecyclerView;
    private List<String> mList;

    public interface ITRTCVideoViewLayoutListener {
        void onEnableRemoteVideo(String userId, boolean enable);

        void onEnableRemoteAudio(String userId, boolean enable);

        void onChangeVideoFillMode(String userId, boolean adjustMode);
    }

    public VideoViewLayout(Context context) {
        super(context);
        initView(context);
    }

    public VideoViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public VideoViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }


    private void initView(Context context) {
        mContext = context;

        mList=new ArrayList<>();
        mList.add("江梦梁");
        mList.add("谭军");
        mList.add("操双平");
        mList.add("陈浩");

        mList.add("李畅");
        mList.add("蔡保成");
        mList.add("杨程");
        mList.add("代家彪");

        mList.add("胡书亮");
        mList.add("刘宗希");
        mList.add("钟宇锋");
        mList.add("彭斌");

        mList.add("张伟");
        mList.add("雄森");
        mList.add("刘虎");

        LayoutInflater.from(context).inflate(R.layout.room_show_view, this);
        mBigVideoView = findViewById(R.id.big_video_view);
        mRecyclerView=findViewById(R.id.recycler);
        LinearLayoutManager layoutManager=new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);
//        VideoAdapter videoAdapter=new VideoAdapter(mList);
//        mRecyclerView.setAdapter(videoAdapter);
    }
}
