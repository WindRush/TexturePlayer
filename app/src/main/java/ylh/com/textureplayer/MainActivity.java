package ylh.com.textureplayer;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import ylh.com.textureplayer.videoview.TextureVideoPlayer;


public class MainActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "yangLihai_";

    private TextureVideoPlayer mVideoPlayer;
    private String[] videoUrls = new String[]{"http://ourtimespicture.bs2dl.yy.com/anim_prod_1475891583483.mp4",
                        "http://tb-video.bdstatic.com/tieba-smallvideo/1252235_2665d2901cd54360927926cd9e288c19.mp4",
            "http://tb-video.bdstatic.com/tieba-smallvideo/1252235_886e6a7283e308144d8e2c8117798fa1.mp4"};

    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.activity_main);

        Button play = (Button) findViewById(R.id.btn_play);
        Button pause = (Button) findViewById(R.id.btn_pause);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoPlayer.setUrl(videoUrls[0]);
                mVideoPlayer.play();
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoPlayer.pause();
            }
        });

        mVideoPlayer = (TextureVideoPlayer) findViewById(R.id.media_player);
        mVideoPlayer.setVideoMode(TextureVideoPlayer.CENTER_MODE);
        mVideoPlayer.setOnVideoPlayingListener(new TextureVideoPlayer.OnVideoPlayingListener() {
            @Override
            public void onVideoSizeChanged(int vWidth, int vHeight) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoPlayer.getLayoutParams();
                params.width = getResources().getDisplayMetrics().widthPixels;
                params.height = (int) ((float)params.width/(float)vWidth*(float)vHeight);
//                mVideoPlayer.setLayoutParams(params);
            }

            @Override
            public void onStart() {
                Toast.makeText(mContext,"onstart",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPlaying(int duration, int percent) {
                Log.e(TAG,"playing_"+"总时长："+duration+",播放进度:"+percent);
            }

            @Override
            public void onPause() {
                Toast.makeText(mContext,"onpause",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRestart() {
                Toast.makeText(mContext,"onrestart",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPlayingFinish() {
                Toast.makeText(mContext,"onplayingfinish",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTextureDestory() {
                if (mVideoPlayer != null){
                    mVideoPlayer.pause();
                }
            }
        });

    }


    @Override
    public void onClick(View v) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoPlayer.mState == TextureVideoPlayer.VideoState.pause){
            if (mVideoPlayer!=null){
                if (!mVideoPlayer.isPlaying()){
                    mVideoPlayer.pause();
                }
            }
        }
    }
}
