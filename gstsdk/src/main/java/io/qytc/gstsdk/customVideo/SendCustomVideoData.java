package io.qytc.gstsdk.customVideo;

import android.content.Context;
import android.graphics.SurfaceTexture;

import android.opengl.EGLContext;
import android.view.Surface;

import io.qytc.gstsdk.customVideo.OpenGLBaseModule.GLThread;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;


public class SendCustomVideoData implements GLThread.IGLSurfaceTextureListener {
    private static String TAG = "SendCustomVideoData";

    private String mVideoFile;
    private Context mContext;
    private TRTCCloud mTRTCCloud;
    private boolean mIsSending;

    private GLThread mGLThread;
    private MovieVideoFrameReader mPlayThread;

    public SendCustomVideoData(Context context) {
        mContext = context;
        mTRTCCloud = TRTCCloud.sharedInstance(mContext);
        mIsSending = false;
    }

    public synchronized void start(String videoFile) {
        if (mIsSending) return;

        //记录视频文件的路径，一会儿用来创建一个 MovieVideoFrameReader。
        mVideoFile = videoFile;

        //启动一个 OpenGL 线程，该线程用于定时 sendCustomVideoData()
        mGLThread = new GLThread();
        mGLThread.setListener(this);
        mGLThread.start();

        mIsSending = true;
    }

    public synchronized void stop() {
        if (!mIsSending) return;
        mIsSending = false;

        if (mPlayThread != null) mPlayThread.requestStop();

        if (mGLThread != null) mGLThread.stop();

    }

    /**
     * 承载视频画面的“画板（SurfaceTexture）”已经准备好了，需要我们创建一个 MovieVideoFrameReader，并与之关联起来。
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture) {

        mPlayThread = new MovieVideoFrameReader(mVideoFile, new Surface(surfaceTexture));
        mGLThread.setInputSize(mPlayThread.getVideoWidth(), mPlayThread.getVideoHeight());

        mPlayThread.start();

    }

    /**
     * 当 GLThread 线程关联的“画板”内容发生变更时，也就是有新的一帧视频渲染上来时，
     * GLThread 就会触发该回调。此时，我们就可以向 TRTC SDK 中 sendCustomVideoData()了。
     */
    @Override
    public int onTextureProcess(int textureId, EGLContext eglContext) {
        if (!mIsSending) return textureId;

        //将视频帧通过纹理方式塞给SDK
        TRTCCloudDef.TRTCVideoFrame videoFrame = new TRTCCloudDef.TRTCVideoFrame();
        videoFrame.texture = new TRTCCloudDef.TRTCTexture();
        videoFrame.texture.textureId = textureId;
        videoFrame.texture.eglContext14 = eglContext;
        videoFrame.width = mPlayThread.getVideoWidth();
        videoFrame.height = mPlayThread.getVideoHeight();
        videoFrame.pixelFormat = TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_Texture_2D;
        videoFrame.bufferType = TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_TEXTURE;
        mTRTCCloud.sendCustomVideoData(videoFrame);
        return textureId;
    }

    @Override
    public void onSurfaceTextureDestroy(SurfaceTexture surfaceTexture) {

    }

}
