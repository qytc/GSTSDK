package io.qytc.gstsdk.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.liteav.TXLiteAVCode;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.qytc.gstsdk.R;
import io.qytc.gstsdk.adapter.VideoAdapter;
import io.qytc.gstsdk.common.ThirdLoginConstant;
import io.qytc.gstsdk.customVideo.RenderVideoFrame;
import io.qytc.gstsdk.dialog.BeautySettingPanel;
import io.qytc.gstsdk.dialog.MoreDialog;
import io.qytc.gstsdk.dialog.SettingDialog;
import io.qytc.gstsdk.view.VideoViewLayout;

public class RoomActivity extends Activity implements View.OnClickListener, SettingDialog.ISettingListener, MoreDialog.IMoreListener, VideoViewLayout.ITRTCVideoViewLayoutListener, BeautySettingPanel.IOnBeautyParamsChangeListener {
    private final static String TAG = RoomActivity.class.getSimpleName();

    private boolean bBeautyEnable = true, bEnableVideo = true, bEnableAudio = true;
    private int iDebugLevel = 0;

    private TextView tvRoomId;

    private ImageButton ivShowMode, ivBeauty, ivCamera, ivVoice, ivLog,swatch_camera_iv;
    //    private SettingDialog      settingDlg;
//    private MoreDialog         moreDlg;
    private LinearLayout mLocalVideo;
    private TXCloudVideoView bigVideoView;

    private RecyclerView       mRecyclerView;
    private BeautySettingPanel mBeautyPannelView;
    private ImageButton        hangupBtn;

    private VideoAdapter videoAdapter;

    private TRTCCloudDef.TRTCParams trtcParams;     /// TRTC SDK 视频通话房间进入所必须的参数
    private TRTCCloud               trtcCloud;              /// TRTC SDK 实例对象
    private TRTCCloudListenerImpl   trtcListener;    /// TRTC SDK 回调监听

    private int                    mBeautyLevel    = 5;
    private int                    mWhiteningLevel = 3;
    private int                    mRuddyLevel     = 2;
    private int                    mBeautyStyle    = TRTCCloudDef.TRTC_BEAUTY_STYLE_SMOOTH;
    private int                    mSdkAppId       = -1;
    private int                    mAppScene       = TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL;
    private String                 mAnchor;
    private int                    roomId;
    private List<TXCloudVideoView> mList           = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //应用运行时，保持屏幕高亮，不锁屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.room_activity);

        //获取前一个页面得到的进房参数
        Intent intent = getIntent();
        roomId = intent.getIntExtra(ThirdLoginConstant.ROOMID, 0);
        String selfUserId = intent.getStringExtra(ThirdLoginConstant.USERID);
        String userSig = intent.getStringExtra(ThirdLoginConstant.USERSIGN);
//        mAnchor = intent.getStringExtra(ThirdLoginConstant.ANCHOR);
        mSdkAppId = intent.getIntExtra(ThirdLoginConstant.SDKAPPID, -1);

        trtcParams = new TRTCCloudDef.TRTCParams(mSdkAppId, selfUserId, userSig, roomId, "", "");
        trtcParams.role = TRTCCloudDef.TRTCRoleAnchor;
//        settingDlg = new SettingDialog(this, this, mAppScene);
//        moreDlg = new MoreDialog(this, this);
        //初始化 UI 控件
        initView();

        //创建 TRTC SDK 实例
        trtcListener = new TRTCCloudListenerImpl(this);
        trtcCloud = TRTCCloud.sharedInstance(this);
        trtcCloud.setListener(trtcListener);
        trtcCloud.setConsoleEnabled(true);
        videoAdapter = new VideoAdapter(mList, mVideoAdapterListener);
        mRecyclerView.setAdapter(videoAdapter);
        //开始进入视频通话房间
        enterRoom();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trtcCloud.setListener(null);
        TRTCCloud.destroySharedInstance();
    }

    @Override
    public void onBackPressed() {
        exitRoom();
    }

    /**
     * 初始化界面控件，包括主要的视频显示View，以及底部的一排功能按钮
     */
    private void initView() {
        mLocalVideo = findViewById(R.id.videoView);
        mLocalVideo.removeAllViews();
//        mLocalVideo.setUserId(trtcParams.userId);
        bigVideoView = new TXCloudVideoView(this);
        bigVideoView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        bigVideoView.setUserId(trtcParams.userId);
        mLocalVideo.addView(bigVideoView);
//        mList.add(bigVideoView);

        tvRoomId = findViewById(R.id.roomNo_tv);
        tvRoomId.setText("房间号：" + roomId);

        mRecyclerView = findViewById(R.id.recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);

        swatch_camera_iv=findViewById(R.id.swatch_camera_iv);
        swatch_camera_iv.setOnClickListener(this);

        ivVoice = findViewById(R.id.mute_button);
        hangupBtn = findViewById(R.id.hangup_button);
        ivCamera = findViewById(R.id.camera_button);

        ivVoice.setOnClickListener(this);
        hangupBtn.setOnClickListener(this);
        ivCamera.setOnClickListener(this);
    }


    /**
     * 设置视频通话的视频参数：需要 SettingDialog 提供的分辨率、帧率和流畅模式等参数
     */
    private void setTRTCCloudParam() {

        // 大画面的编码器参数设置
        // 设置视频编码参数，包括分辨率、帧率、码率等等，这些编码参数来自于 SettingDialog 的设置
        // 注意（1）：不要在码率很低的情况下设置很高的分辨率，会出现较大的马赛克
        // 注意（2）：不要设置超过25FPS以上的帧率，因为电影才使用24FPS，我们一般推荐15FPS，这样能将更多的码率分配给画质
        TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();
        encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720;// settingDlg.getResolution();
        encParam.videoFps = 20;// settingDlg.getVideoFps();
        encParam.videoBitrate = 1500;// settingDlg.getVideoBitrate();

        if (Build.MODEL.equals("Hi3798MV200")) {
            encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_LANDSCAPE;// TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT : ;
        } else {
            encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;// TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT : ;
        }

        trtcCloud.setVideoEncoderParam(encParam);

        TRTCCloudDef.TRTCNetworkQosParam qosParam = new TRTCCloudDef.TRTCNetworkQosParam();
        qosParam.controlMode = TRTCCloudDef.VIDEO_QOS_CONTROL_SERVER;//settingDlg.getQosMode();
        qosParam.preference = TRTCCloudDef.VIDEO_QOS_CONTROL_SERVER; //settingDlg.getQosPreference();
        trtcCloud.setNetworkQosParam(qosParam);
        trtcCloud.setPriorRemoteVideoStreamType(TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
    }

    /**
     * 加入视频房间：需要 TRTCNewViewActivity 提供的  TRTCParams 函数
     */
    private void enterRoom() {
        // 预览前配置默认参数
        setTRTCCloudParam();

        // 开启视频采集预览
//        if (trtcParams.role == TRTCCloudDef.TRTCRoleAnchor) {

        startLocalVideo(bigVideoView, true);
//        }

        trtcCloud.setBeautyStyle(TRTCCloudDef.TRTC_BEAUTY_STYLE_SMOOTH, 5, 5, 5);

        if (trtcParams.role == TRTCCloudDef.TRTCRoleAnchor) {
            trtcCloud.startLocalAudio();
        }

        setVideoFillMode(true);
//        setVideoRotation(moreDlg.isVideoVertical());
        enableAudioHandFree(true);
        //enableGSensor(moreDlg.isEnableGSensorMode());
        enableAudioVolumeEvaluation(true);

        trtcCloud.enterRoom(trtcParams, mAppScene);
    }

    /**
     * 退出视频房间
     */
    private void exitRoom() {
        bigVideoView.stop(true);
        startLocalVideo(bigVideoView, false);

        if (trtcCloud != null) {
            trtcCloud.exitRoom();
        }

        finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.mute_button) {
            onEnableAudio();
        } else if (view.getId() == R.id.hangup_button) {
            exitRoom();
        } else if (view.getId() == R.id.camera_button) {
            onEnableVideo();
        }else if(view.getId()== R.id.swatch_camera_iv){
            trtcCloud.switchCamera();
        }
    }

    /**
     * 点击开启或关闭美颜
     */
    private void onChangeBeauty() {
        bBeautyEnable = !bBeautyEnable;
        mBeautyPannelView.setVisibility(bBeautyEnable ? View.VISIBLE : View.GONE);

    }

    /**
     * 开启/关闭视频上行
     */
    private void onEnableVideo() {
        bEnableVideo = !bEnableVideo;
        startLocalVideo(bigVideoView, bEnableVideo);
        ivCamera.setImageResource(bEnableVideo ? R.mipmap.camera_unfocus : R.mipmap.camera_focus);
    }

    /**
     * 开启/关闭音频上行
     */
    private void onEnableAudio() {
        bEnableAudio = !bEnableAudio;
        trtcCloud.muteLocalAudio(bEnableAudio);
        ivVoice.setImageResource(bEnableAudio ? R.mipmap.mute_focus : R.mipmap.mute_unfocus);
    }

    /**
     * 点击打开仪表盘浮层，仪表盘浮层是SDK中覆盖在视频画面上的一系列数值状态
     */
    private void onChangeLogStatus() {

        iDebugLevel = (iDebugLevel + 1) % 3;
        ivLog.setImageResource((0 == iDebugLevel) ? R.mipmap.log2 : R.mipmap.log);

        trtcCloud.showDebugView(iDebugLevel);
    }

    /*
     * 打开更多参数设置面板
     */
    private void onShowMoreDlg() {
//        moreDlg.setRole(trtcParams.role);
//        moreDlg.show(beingLinkMic, mAppScene);
    }

    @Override
    public void onComplete() {
        setTRTCCloudParam();
//        setVideoFillMode(settingDlg.isVideoVertical());
//        moreDlg.updateVideoFillMode(settingDlg.isVideoVertical());
    }

    private TXCloudVideoView getVideoView(String userId) {
        for (TXCloudVideoView videoView : mList) {
            if (videoView.getUserId().equals(userId)) {
                return videoView;
            }
        }

        return null;
    }

    /**
     * SDK内部状态回调
     */
    static class TRTCCloudListenerImpl extends TRTCCloudListener implements TRTCCloudListener.TRTCVideoRenderListener {

        private WeakReference<RoomActivity>       mContext;

        public TRTCCloudListenerImpl(RoomActivity activity) {
            super();
            mContext = new WeakReference<>(activity);
        }

        /**
         * 加入房间
         */
        @Override
        public void onEnterRoom(long elapsed) {
            Log.d(TAG, "onEnterRoom: 加入房间");
            final RoomActivity activity = mContext.get();
            if (activity != null) {
                Toast.makeText(activity, "加入房间成功", Toast.LENGTH_SHORT).show();
                if (Build.MODEL.equals("Hi3798MV200")) {
                    activity.trtcCloud.setLocalViewRotation(0);
                    activity.trtcCloud.setVideoEncoderRotation(0);
                    activity.enableVideoEncMirror(true);
                } else {
                    activity.setLocalViewMirrorMode(TRTCCloudDef.TRTC_VIDEO_MIRROR_TYPE_DISABLE);
                    activity.enableVideoEncMirror(false);
                }
            }
        }

        /**
         * 离开房间
         */
        @Override
        public void onExitRoom(int reason) {
            Log.d(TAG, "onExitRoom: ");

        }

        /**
         * ERROR 大多是不可恢复的错误，需要通过 UI 提示用户
         */
        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.d(TAG, "sdk callback onError");
            RoomActivity activity = mContext.get();
            if (activity == null) return;

            if (errCode == TXLiteAVCode.ERR_ROOM_REQUEST_TOKEN_HTTPS_TIMEOUT ||
                    errCode == TXLiteAVCode.ERR_ROOM_REQUEST_IP_TIMEOUT ||
                    errCode == TXLiteAVCode.ERR_ROOM_REQUEST_ENTER_ROOM_TIMEOUT) {
                Toast.makeText(activity, "进房超时，请检查网络或稍后重试:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                activity.exitRoom();
                return;
            }

            if (errCode == TXLiteAVCode.ERR_ROOM_REQUEST_TOKEN_INVALID_PARAMETER ||
                    errCode == TXLiteAVCode.ERR_ENTER_ROOM_PARAM_NULL ||
                    errCode == TXLiteAVCode.ERR_SDK_APPID_INVALID ||
                    errCode == TXLiteAVCode.ERR_ROOM_ID_INVALID ||
                    errCode == TXLiteAVCode.ERR_USER_ID_INVALID ||
                    errCode == TXLiteAVCode.ERR_USER_SIG_INVALID) {
                Toast.makeText(activity, "进房参数错误:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                activity.exitRoom();
                return;
            }

            if (errCode == TXLiteAVCode.ERR_ACCIP_LIST_EMPTY ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_UNPACKING_ERROR ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_TOKEN_ERROR ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_ALLOCATE_ACCESS_FAILED ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_GENERATE_SIGN_FAILED ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_TOKEN_TIMEOUT ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_INVALID_COMMAND ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_GENERATE_KEN_ERROR ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_GENERATE_TOKEN_ERROR ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_DATABASE ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_BAD_ROOMID ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_BAD_SCENE_OR_ROLE ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_ROOMID_EXCHANGE_FAILED ||
                    errCode == TXLiteAVCode.ERR_SERVER_INFO_STRGROUP_HAS_INVALID_CHARS ||
                    errCode == TXLiteAVCode.ERR_SERVER_ACC_TOKEN_TIMEOUT ||
                    errCode == TXLiteAVCode.ERR_SERVER_ACC_SIGN_ERROR ||
                    errCode == TXLiteAVCode.ERR_SERVER_ACC_SIGN_TIMEOUT ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_INVALID_ROOMID ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_CREATE_ROOM_FAILED ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_SIGN_ERROR ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_SIGN_TIMEOUT ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_ADD_USER_FAILED ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_FIND_USER_FAILED ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_SWITCH_TERMINATION_FREQUENTLY ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_LOCATION_NOT_EXIST ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_ROUTE_TABLE_ERROR ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_INVALID_PARAMETER) {
                Toast.makeText(activity, "进房失败，请稍后重试:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                activity.exitRoom();
                return;
            }

            if (errCode == TXLiteAVCode.ERR_SERVER_CENTER_ROOM_FULL ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_REACH_PROXY_MAX) {
                Toast.makeText(activity, "进房失败，房间满了，请稍后重试:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                activity.exitRoom();
                return;
            }

            if (errCode == TXLiteAVCode.ERR_SERVER_CENTER_ROOM_ID_TOO_LONG) {
                Toast.makeText(activity, "进房失败，roomID超出有效范围:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                activity.exitRoom();
                return;
            }

            if (errCode == TXLiteAVCode.ERR_SERVER_ACC_ROOM_NOT_EXIST ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_ROOM_NOT_EXIST) {
                Toast.makeText(activity, "进房失败，请确认房间号正确:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                activity.exitRoom();
                return;
            }

            if (errCode == TXLiteAVCode.ERR_SERVER_INFO_SERVICE_SUSPENDED) {
                Toast.makeText(activity, "进房失败，请确业务是否欠费:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                activity.exitRoom();
                return;
            }

            if (errCode == TXLiteAVCode.ERR_SERVER_INFO_PRIVILEGE_FLAG_ERROR ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_NO_PRIVILEDGE_CREATE_ROOM ||
                    errCode == TXLiteAVCode.ERR_SERVER_CENTER_NO_PRIVILEDGE_ENTER_ROOM) {
                Toast.makeText(activity, "进房失败，无权限进入房间:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                activity.exitRoom();
                return;
            }

            if (errCode <= TXLiteAVCode.ERR_SERVER_SSO_SIG_EXPIRED &&
                    errCode >= TXLiteAVCode.ERR_SERVER_SSO_INTERNAL_ERROR) {
                // 错误参考 https://cloud.tencent.com/document/product/269/1671#.E5.B8.90.E5.8F.B7.E7.B3.BB.E7.BB.9F
                Toast.makeText(activity, "进房失败，userSig错误:" + errCode + "[" + errMsg + "]", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onError: " + errCode);
                activity.exitRoom();
                return;
            }

            Toast.makeText(activity, "onError: " + errMsg + "[" + errCode + "]", Toast.LENGTH_SHORT).show();
        }

        /**
         * WARNING 大多是一些可以忽略的事件通知，SDK内部会启动一定的补救机制
         */
        @Override
        public void onWarning(int warningCode, String warningMsg, Bundle extraInfo) {
            Log.d(TAG, "sdk callback onWarning");
        }

        /**
         * 有新的用户加入了当前视频房间
         */
        @Override
        public void onUserEnter(String userId) {
            Log.d(TAG, "onUserEnter: " + userId);
            RoomActivity activity = mContext.get();

            if (activity != null) {
                TXCloudVideoView videoView = new TXCloudVideoView(activity);
                videoView.setUserId(userId);
                videoView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                activity.mList.add(videoView);
                activity.videoAdapter.notifyDataSetChanged();

            }
        }

        /**
         * 有用户离开了当前视频房间
         */
        @Override
        public void onUserExit(String userId, int reason) {
            Log.d(TAG, "onUserExit: ");
            RoomActivity activity = mContext.get();
            if (activity != null) {
                //停止观看画面
                activity.trtcCloud.stopRemoteView(userId);
                activity.trtcCloud.stopRemoteSubStreamView(userId);
                for (int i = 0; i < activity.mList.size(); i++) {
                    if (activity.mList.get(i).getUserId().equals(userId)) {
                        activity.mList.remove(i);
                        activity.videoAdapter.notifyDataSetChanged();
                        return;
                    }
                }
            }
        }

        /**
         * 有用户屏蔽了画面
         */
        @Override
        public void onUserVideoAvailable(final String userId, boolean available) {
            RoomActivity activity = mContext.get();
            if (activity != null) {

                if (available) {
                    final TXCloudVideoView renderView = activity.getVideoView(userId);
                    if (renderView != null) {
                        Log.d(TAG, "onUserVideoAvailable: " + renderView.getUserId());
                        // 启动远程画面的解码和显示逻辑，FillMode 可以设置是否显示黑边
                        activity.trtcCloud.setRemoteViewFillMode(userId, TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT);
                        activity.trtcCloud.startRemoteView(userId, renderView);
                    }
                } else {
                    activity.trtcCloud.stopRemoteView(userId);
                }
            }
        }

        public void onUserSubStreamAvailable(final String userId, boolean available) {
            Log.d(TAG, "onUserSubStreamAvailable: ");
            RoomActivity activity = mContext.get();
//            if (activity != null) {
//                VideoStream userStream = new VideoStream();
//                userStream.userId = userId;
//                userStream.streamType = TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SUB;
//                if (available) {
//                    final TXCloudVideoView renderView = activity.mLocalVideo.onMemberEnter(userId + TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SUB);
//                    if (renderView != null) {
//                        // 启动远程画面的解码和显示逻辑，FillMode 可以设置是否显示黑边
//                        activity.trtcCloud.setRemoteSubStreamViewFillMode(userId, TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT);
//                        activity.trtcCloud.startRemoteSubStreamView(userId, renderView);
//
//                        activity.runOnUiThread(() -> renderView.setUserId(userId + TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SUB));
//                    }
//                    activity.mVideosInRoom.add(userStream);
//                } else {
//                    activity.trtcCloud.stopRemoteSubStreamView(userId);
//                    activity.mLocalVideo.onMemberLeave(userId + TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_SUB);
//                    activity.mVideosInRoom.remove(userStream);
//                }
//                activity.updateCloudMixtureParams();
//            }
        }

        /**
         * 有用户屏蔽了声音
         */
        @Override
        public void onUserAudioAvailable(String userId, boolean available) {
            Log.d(TAG, "onUserAudioAvailable: ");
            RoomActivity activity = mContext.get();
//            if (activity != null) {
//                if (available) {
//                    final TXCloudVideoView renderView = activity.mLocalVideo.onMemberEnter(userId + TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
//                    if (renderView != null) {
//                        renderView.setVisibility(View.VISIBLE);
//                    }
//                }
//            }
        }

        /**
         * 首帧渲染回调
         */
        @Override
        public void onFirstVideoFrame(String userId, int streamType, int width, int height) {
            Log.d(TAG, "onFirstVideoFrame: ");
            RoomActivity activity = mContext.get();
//            if (activity != null) {
//                activity.mLocalVideo.freshToolbarLayoutOnMemberEnter(userId + TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
//            }
        }

        @Override
        public void onConnectOtherRoom(final String userID, final int err, final String errMsg) {

        }

        @Override
        public void onDisConnectOtherRoom(final int err, final String errMsg) {

        }

        @Override
        public void onNetworkQuality(TRTCCloudDef.TRTCQuality localQuality, ArrayList<TRTCCloudDef.TRTCQuality> remoteQuality) {

        }

        @Override
        public void onRenderVideoFrame(String s, int i, TRTCCloudDef.TRTCVideoFrame trtcVideoFrame) {
            Log.d(TAG, "onRenderVideoFrame: ");
        }
    }

    @Override
    public void onEnableRemoteVideo(final String userId, boolean enable) {
//        if (enable) {
//            final TXCloudVideoView renderView = mLocalVideo.getCloudVideoViewByUseId(userId + TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
//            if (renderView != null) {
//                trtcCloud.setRemoteViewFillMode(userId, TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT);
//                trtcCloud.startRemoteView(userId, renderView);
//                runOnUiThread(() -> {
//                    renderView.setUserId(userId + TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
//                    mLocalVideo.freshToolbarLayoutOnMemberEnter(userId);
//                });
//            }
//        } else {
//            trtcCloud.stopRemoteView(userId);
//        }
    }

    @Override
    public void onEnableRemoteAudio(String userId, boolean enable) {
        trtcCloud.muteRemoteAudio(userId, !enable);
    }

    @Override
    public void onChangeVideoFillMode(String userId, boolean adjustMode) {
        trtcCloud.setRemoteViewFillMode(userId, adjustMode ? TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT : TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FILL);
    }

    @Override
    public void onSwitchCamera(boolean bCameraFront) {
        trtcCloud.switchCamera();
    }

    @Override
    public void onFillModeChange(boolean bFillMode) {
        setVideoFillMode(bFillMode);
    }

    @Override
    public void onVideoRotationChange(boolean bVertical) {
//        setVideoRotation(bVertical);
    }

    @Override
    public void onEnableAudioCapture(boolean bEnable) {
        enableAudioCapture(bEnable);
    }

    @Override
    public void onEnableAudioHandFree(boolean bEnable) {
        enableAudioHandFree(bEnable);
    }

    @Override
    public void onMirrorLocalVideo(int localViewMirror) {
        setLocalViewMirrorMode(localViewMirror);
    }

    @Override
    public void onMirrorRemoteVideo(boolean bMirror) {
        enableVideoEncMirror(bMirror);
    }

    @Override
    public void onEnableGSensor(boolean bEnable) {
        enableGSensor(bEnable);
    }

    @Override
    public void onEnableAudioVolumeEvaluation(boolean bEnable) {
        enableAudioVolumeEvaluation(bEnable);
    }

    @Override
    public void onEnableCloudMixture(boolean bEnable) {

    }

    @Override
    public void onClickButtonGetPlayUrl() {

    }

    @Override
    public void onClickButtonLinkMic() {

    }

    @Override
    public void onChangeRole(int role) {
        if (trtcCloud != null) {
            trtcCloud.switchRole(role);
        }
//        if (role == TRTCCloudDef.TRTCRoleAnchor) {
//            startLocalVideo(true);
//            if (moreDlg.isEnableAudioCapture()) {
//                trtcCloud.startLocalAudio();
//            }
//        } else {
//            startLocalVideo(false);
//            trtcCloud.stopLocalAudio();
//            TXCloudVideoView localVideoView = mLocalVideo.getCloudVideoViewByUseId(trtcParams.userId);
//            if (localVideoView != null) {
//                localVideoView.setVisibility(View.GONE);
//            }
//        }
    }

    @Override
    public void onBeautyParamsChange(BeautySettingPanel.BeautyParams params, int key) {
        switch (key) {
            case BeautySettingPanel.BEAUTYPARAM_BEAUTY:
                mBeautyStyle = params.mBeautyStyle;
                mBeautyLevel = params.mBeautyLevel;
                if (trtcCloud != null) {
                    trtcCloud.setBeautyStyle(mBeautyStyle, mBeautyLevel, mWhiteningLevel, mRuddyLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_WHITE:
                mWhiteningLevel = params.mWhiteLevel;
                if (trtcCloud != null) {
                    trtcCloud.setBeautyStyle(mBeautyStyle, mBeautyLevel, mWhiteningLevel, mRuddyLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_BIG_EYE:
                if (trtcCloud != null) {
                    trtcCloud.setEyeScaleLevel(params.mBigEyeLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_FACE_LIFT:
                if (trtcCloud != null) {
                    trtcCloud.setFaceSlimLevel(params.mFaceSlimLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_FILTER:
                if (trtcCloud != null) {
                    trtcCloud.setFilter(params.mFilterBmp);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_GREEN:
                if (trtcCloud != null) {
                    trtcCloud.setGreenScreenFile(params.mGreenFile);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_MOTION_TMPL:
                if (trtcCloud != null) {
                    trtcCloud.selectMotionTmpl(params.mMotionTmplPath);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_RUDDY:
                mRuddyLevel = params.mRuddyLevel;
                if (trtcCloud != null) {
                    trtcCloud.setBeautyStyle(mBeautyStyle, mBeautyLevel, mWhiteningLevel, mRuddyLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_FACEV:
                if (trtcCloud != null) {
                    trtcCloud.setFaceVLevel(params.mFaceVLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_FACESHORT:
                if (trtcCloud != null) {
                    trtcCloud.setFaceShortLevel(params.mFaceShortLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_CHINSLIME:
                if (trtcCloud != null) {
                    trtcCloud.setChinLevel(params.mChinSlimLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_NOSESCALE:
                if (trtcCloud != null) {
                    trtcCloud.setNoseSlimLevel(params.mNoseScaleLevel);
                }
                break;
            case BeautySettingPanel.BEAUTYPARAM_FILTER_MIX_LEVEL:
                if (trtcCloud != null) {
                    trtcCloud.setFilterConcentration(params.mFilterMixLevel / 10.f);
                }
                break;
        }
    }

    private void setVideoFillMode(boolean bFillMode) {
        if (bFillMode) {
            trtcCloud.setLocalViewFillMode(TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FILL);
        } else {
            trtcCloud.setLocalViewFillMode(TRTCCloudDef.TRTC_VIDEO_RENDER_MODE_FIT);
        }
    }

    private void enableAudioCapture(boolean bEnable) {
        if (bEnable) {
            trtcCloud.startLocalAudio();
        } else {
            trtcCloud.stopLocalAudio();
        }
    }

    private void enableAudioHandFree(boolean bEnable) {
        if (bEnable) {
            trtcCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);
        } else {
            trtcCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE);
        }
    }

    private void enableVideoEncMirror(boolean bMirror) {
        trtcCloud.setVideoEncoderMirror(bMirror);
    }

    private void setLocalViewMirrorMode(int mirrorMode) {
        trtcCloud.setLocalViewMirror(mirrorMode);
    }

    private void enableGSensor(boolean bEnable) {
        if (bEnable) {
            trtcCloud.setGSensorMode(TRTCCloudDef.TRTC_GSENSOR_MODE_UIFIXLAYOUT);
        } else {
            trtcCloud.setGSensorMode(TRTCCloudDef.TRTC_GSENSOR_MODE_DISABLE);
        }
    }

    private void enableAudioVolumeEvaluation(boolean bEnable) {
//        if (bEnable) {
//            trtcCloud.enableAudioVolumeEvaluation(300);
//            mLocalVideo.showAllAudioVolumeProgressBar();
//        } else {
//            trtcCloud.enableAudioVolumeEvaluation(0);
//            mLocalVideo.hideAllAudioVolumeProgressBar();
//        }
    }

    private void startLocalVideo(TXCloudVideoView localVideoView, boolean enable) {
        Log.d(TAG, "startLocalVideo: " + enable);
        if (enable) {
            //启动SDK摄像头采集和渲染
            trtcCloud.startLocalPreview(true, localVideoView);
        } else {
            trtcCloud.stopLocalPreview();
        }
    }

    private VideoAdapter.VideoAdapterListener mVideoAdapterListener = new VideoAdapter.VideoAdapterListener() {
        @Override
        public void OnClick(int position) {
            mLocalVideo.removeAllViews();
            TXCloudVideoView videoView = mList.get(position);
            ViewGroup parent = (ViewGroup) videoView.getParent();
            if (parent != null) {
                parent.removeAllViews();
            }
            mLocalVideo.addView(videoView);

            ViewGroup parent1 = (ViewGroup) bigVideoView.getParent();
            if (parent1 != null) {
                parent1.removeAllViews();
            }

            mList.remove(position);
            mList.add(position, bigVideoView);
            videoAdapter.notifyDataSetChanged();

            bigVideoView = videoView;
        }
    };
}
