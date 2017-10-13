package ylh.com.textureplayer.videoview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

/**
 * Created by yangLiHai on 2016/11/3.
 */

public class TextureVideoPlayer extends TextureView implements TextureView.SurfaceTextureListener{

    private String TEXTUREVIDEO_TAG = "yangLiHai_video";

    private String url;

    public VideoState mState;

    private MediaPlayer mMediaPlayer;

    private int mVideoWidth;//视频宽度
    private int mVideoHeight;//视频高度

    public static final int CENTER_CROP_MODE = 1;//中心裁剪模式
    public static final int CENTER_MODE = 2;//一边中心填充模式

    public int mVideoMode = 0;

    //回调监听
    public interface OnVideoPlayingListener {
        void onVideoSizeChanged(int vWidth, int vHeight);
        void onStart();
        void onPlaying(int duration, int percent);
        void onPause();
        void onRestart();
        void onPlayingFinish();
        void onTextureDestory();
    }

    //播放状态
    public enum VideoState{
        init,palying,pause
    }


    private OnVideoPlayingListener listener;
    public void setOnVideoPlayingListener(OnVideoPlayingListener listener){
        this.listener = listener;
    }

    public TextureVideoPlayer(Context context) {
        super(context);
        init();
    }

    public TextureVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TextureVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init(){
        setSurfaceTextureListener(this);
    }


    public void setUrl(String url){
        this.url = url;
    }

    public void play(){
        if (mMediaPlayer==null ) return;

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mState = VideoState.palying;
            if (listener!=null) listener.onStart();
            getPlayingProgress();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TEXTUREVIDEO_TAG , e.toString());
        }

    }

    public void pause(){
        if (mMediaPlayer==null) return;

        if (mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
            mState = VideoState.pause;
            if (listener!=null) listener.onPause();
        }else{
            mMediaPlayer.start();
            mState = VideoState.palying;
            if (listener!=null) listener.onRestart();
            getPlayingProgress();
        }
    }

    public void stop(){
        if (mMediaPlayer.isPlaying()){
            mMediaPlayer.stop();
//            mMediaPlayer.release();
        }
    }

    //播放进度获取
    private void getPlayingProgress(){
        mProgressHandler.sendEmptyMessage(0);
    }

    private Handler mProgressHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0){
                if (listener!=null && mState == VideoState.palying){
                    listener.onPlaying(mMediaPlayer.getDuration(),mMediaPlayer.getCurrentPosition());
                    sendEmptyMessageDelayed(0,100);
                }
            }
        }
    };

    public boolean isPlaying(){
        return mMediaPlayer.isPlaying();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TEXTUREVIDEO_TAG,"onsurfacetexture available");

        if (mMediaPlayer==null){
            mMediaPlayer = new MediaPlayer();

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //当MediaPlayer对象处于Prepared状态的时候，可以调整音频/视频的属性，如音量，播放时是否一直亮屏，循环播放等。
                    mMediaPlayer.setVolume(1f,1f);
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return false;
                }
            });

            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    //此方法获取的是缓冲的状态
                    Log.e(TEXTUREVIDEO_TAG,"缓冲中:"+percent);
                }
            });

            //播放完成的监听
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mState = VideoState.init;
                    if (listener!=null) listener.onPlayingFinish();
                }
            });

            mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    mVideoHeight = mMediaPlayer.getVideoHeight();
                    mVideoWidth = mMediaPlayer.getVideoWidth();
                    updateTextureViewSize(mVideoMode);
                    if (listener!=null){
                        listener.onVideoSizeChanged(mVideoWidth,mVideoHeight);
                    }
                }
            });


        }

        //拿到要展示的图形界面
        Surface mediaSurface = new Surface(surface);
        //把surface
        mMediaPlayer.setSurface(mediaSurface);
        mState = VideoState.palying;

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        updateTextureViewSize(mVideoMode);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mMediaPlayer.pause();
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        if (listener!=null)listener.onTextureDestory();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void setVideoMode(int mode){
        mVideoMode=mode;
    }

    /**
     *
     * @param mode Pass {@link #CENTER_CROP_MODE} or {@link #CENTER_MODE}. Default
     * value is 0.
     */
    public void updateTextureViewSize(int mode){
        if (mode==CENTER_MODE){
            updateTextureViewSizeCenter();
        }else if (mode == CENTER_CROP_MODE){
            updateTextureViewSizeCenterCrop();
        }
    }

    //重新计算video的显示位置，裁剪后全屏显示
    private void updateTextureViewSizeCenterCrop(){

        float sx = (float) getWidth() / (float) mVideoWidth;
        float sy = (float) getHeight() / (float) mVideoHeight;

        Matrix matrix = new Matrix();
        float maxScale = Math.max(sx, sy);

        //第1步:把视频区移动到View区,使两者中心点重合.
        matrix.preTranslate((getWidth() - mVideoWidth) / 2, (getHeight() - mVideoHeight) / 2);

        //第2步:因为默认视频是fitXY的形式显示的,所以首先要缩放还原回来.
        matrix.preScale(mVideoWidth / (float) getWidth(), mVideoHeight / (float) getHeight());

        //第3步,等比例放大或缩小,直到视频区的一边超过View一边, 另一边与View的另一边相等. 因为超过的部分超出了View的范围,所以是不会显示的,相当于裁剪了.
        matrix.postScale(maxScale, maxScale, getWidth() / 2, getHeight() / 2);//后两个参数坐标是以整个View的坐标系以参考的

        setTransform(matrix);
        postInvalidate();
    }

    //重新计算video的显示位置，让其全部显示并据中
    private void updateTextureViewSizeCenter(){

        float sx = (float) getWidth() / (float) mVideoWidth;
        float sy = (float) getHeight() / (float) mVideoHeight;

        Matrix matrix = new Matrix();

        //第1步:把视频区移动到View区,使两者中心点重合.
        matrix.preTranslate((getWidth() - mVideoWidth) / 2, (getHeight() - mVideoHeight) / 2);

        //第2步:因为默认视频是fitXY的形式显示的,所以首先要缩放还原回来.
        matrix.preScale(mVideoWidth / (float) getWidth(), mVideoHeight / (float) getHeight());

        //第3步,等比例放大或缩小,直到视频区的一边和View一边相等.如果另一边和view的一边不相等，则留下空隙
        if (sx >= sy){
            matrix.postScale(sy, sy, getWidth() / 2, getHeight() / 2);
        }else{
            matrix.postScale(sx, sx, getWidth() / 2, getHeight() / 2);
        }

        setTransform(matrix);
        postInvalidate();
    }


}
