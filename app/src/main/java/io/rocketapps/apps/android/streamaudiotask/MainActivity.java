package io.rocketapps.apps.android.streamaudiotask;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.rocketapps.apps.android.streamaudiotask.design.PlayPauseView;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mp;
    private String TAG = "PLAY_AUDIO";


    private VideoView myVideoView;
    private String urlStream;

    private int finishedParts = 0;
    private PlayPauseView btnPlay;
    private boolean isFetchingSong = false;
    private boolean songIsFetched = false;
    private ArrayList<ChunkClass> mChunks;
    private int nextChunkArrive = 0;
    private File downloadingMediaFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        myVideoView = (VideoView) findViewById(R.id.myVideoView);
        btnPlay = (PlayPauseView) findViewById(R.id.playBtn);
        mp = new MediaPlayer();


        btnPlay.setMediaPlayer(mp);

        btnPlay.setClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFetchingSong && songIsFetched) {
                    return;
                } else if (!songIsFetched) {
                    isFetchingSong = true;
                    btnPlay.startFetching();
                    startTasks();
                } else if (btnPlay.getState() == PlayPauseView.BUTTON_STATE.STATE_PLAYING) {
                    btnPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PAUSE);
//                    mp.seekTo(mp.getDuration() - 10000);//TODO for testing only, its painful waiting song to end
                    mp.pause();
                } else if (btnPlay.getState() == PlayPauseView.BUTTON_STATE.STATE_PAUSE) {
                    btnPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PLAYING);
                    mp.start();
                }

            }
        });

        MediaController mc = new MediaController(this);

        //myVideoView.setMediaController(mc);
        urlStream = "http://pubcache1.arkiva.de/test/hls_index.m3u8";
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                myVideoView.setVideoURI(Uri.parse(urlStream));
//            }
//        });

    }

    public void startTasks() {

        new FetchAudio().execute();
    }


    class FetchAudio extends AsyncTask<Object, Object, File> {

        @Override
        protected File doInBackground(Object... params) {
            int count;

            try {

                String baseUrl = "http://pubcache1.arkiva.de/test/";
                String hlsUrl = baseUrl + "hls_index.m3u8";

                String[] ext = getHLSFile(hlsUrl).split("\n");

                String bestaudio = "";
                for (String s : ext) {
                    if (s.contains("EXTM3U")) {
                        continue;
                    }
                    if (s.contains("TYPE=AUDIO")) {
                        s = s.replace("#EXT-X-MEDIA:", "");
                        s = s.replace("\"", "");
                        Map<String, String> audio = splitToMap(s, ",", "=");
                        Log.d(TAG, s);
                        bestaudio = audio.get("URI");
                        Log.d(TAG, audio.get("URI"));
                    } else {
                        if (!bestaudio.equals("")) {
                            break;
                        }
                    }
                }

                hlsUrl = baseUrl + bestaudio;
                ext = getHLSFileForBestQuality(hlsUrl).split("\n");

                mChunks = getAllChunks(ext);
                nextChunkArrive = 0;


                for(int i = 0; i < mChunks.size(); i = i +2){
                    mChunks.get(i).setFilename("filename_"+i+".mp3");
                    mChunks.get(i).setPos(i);
                    ChunkClass c2 = null;
                    if(mChunks.size() - 1 > i + 1) {
                        mChunks.get(i + 1).setFilename("filename_" + (i + 1) + ".mp3");
                        mChunks.get(i + 1).setPos(i + 1);
                        c2 = mChunks.get(i + 1);
                    }
                    downloadAudio(mChunks.get(i), c2);
                }

                return null;
//                playSong(cDir.getPath() + "/" + "piece_1.mp3");
            } catch (Exception e) {
                e.printStackTrace();

            }

            return null;
        }

        @Override
        protected void onPostExecute(File downloadingMediaFile) {
            super.onPostExecute(downloadingMediaFile);
            if (downloadingMediaFile == null)
                return;


        }
    }


    private ArrayList<ChunkClass> getAllChunks(String[] ext) {
        ArrayList<ChunkClass> m = new ArrayList<>();
        int count = 0;
        while (count < ext.length) {
            String s = ext[count];
            if(s.contains("EXT-X-ENDLIST")){
                break;
            }else if (!s.contains("#EXTINF")) {
                count++;
                continue;
            }else{
                count ++;
                ChunkClass cc = new ChunkClass();
                s = ext[count];
                String[] timer = s.split(":")[1].split("@");
                cc.setOffset(Integer.parseInt(timer[1]));
                cc.setLength(Integer.parseInt(timer[0]));
                count ++;
                s = ext[count];
                cc.setName(s);
                m.add(cc);
                count ++;
            }

        }

        return m;
    }

    private File downloadAudio(final ChunkClass chunkClass, final ChunkClass chunkClass1) throws Exception {

        int count;
        final String _url = "http://pubcache1.arkiva.de/test/"+chunkClass.getName();



        Thread t1 = new Thread(new Runnable() {

            @Override
            public void run() {
                int count = 0;
                int total = 0;
                Log.i(TAG, "Started T"+chunkClass.getPos()+".:" + total);

                Log.i(TAG, "Starting T1.");
                try {
                    URL url = new URL(_url);
                    URLConnection con = url.openConnection();
                    con.setRequestProperty("Range", "bytes="+chunkClass.getOffset()+"-"+(chunkClass.getLength() + chunkClass.getOffset()));

                    HttpURLConnection connection = (HttpURLConnection) con;
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int lenghtOfFile = connection.getContentLength();
                    Log.d(TAG, connection.getHeaderField("Content-Range"));
                    Log.d(TAG, String.valueOf(lenghtOfFile));

                    final InputStream input = connection.getInputStream();

                    File cDir = getApplication().getExternalFilesDir(null);
                    String filename = chunkClass.getFilename();
                    final File downloadingMediaFile = new File(getApplicationContext().getCacheDir(), filename);
                    if (downloadingMediaFile.exists()) {
                        downloadingMediaFile.delete();
                    }


                    final OutputStream output = new FileOutputStream(downloadingMediaFile);

                    final byte data[] = new byte[1024];

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();

                    chunkClass.setFile(downloadingMediaFile);
                    chunkClass.setFullPath(downloadingMediaFile.getAbsolutePath());
                    threadsHaveFinish(chunkClass);



                } catch (Exception e) {

                }

                Log.i(TAG, "Finished T"+chunkClass.getPos()+".:" + total);
            }
        });


        Thread t2 = new Thread(new Runnable() {

            @Override
            public void run() {
                int total = 0;
                int count = 0;
                Log.i(TAG, "Started T"+chunkClass1.getPos()+".:" + total);


                try {
                    URL url = new URL(_url);
                    URLConnection con = url.openConnection();
                    con.setRequestProperty("Range", "bytes="+chunkClass1.getOffset()+"-"+(chunkClass1.getLength() + chunkClass1.getOffset()));

                    HttpURLConnection connection = (HttpURLConnection) con;
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int lenghtOfFile = connection.getContentLength();
                    Log.d(TAG, connection.getHeaderField("Content-Range"));
                    Log.d(TAG, String.valueOf(lenghtOfFile));

                    final InputStream input = connection.getInputStream();

                    File cDir = getApplication().getExternalFilesDir(null);
                    String filename = chunkClass1.getFilename();
                    final File downloadingMediaFile = new File(getApplicationContext().getCacheDir(), filename);
                    if (downloadingMediaFile.exists()) {
                        downloadingMediaFile.delete();
                    }


                    final OutputStream output = new FileOutputStream(downloadingMediaFile);

                    final byte data[] = new byte[1024];

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();


                    chunkClass1.setFile(downloadingMediaFile);
                    chunkClass1.setFullPath(downloadingMediaFile.getAbsolutePath());

                    threadsHaveFinish(chunkClass1);

//                    threadsHaveFinish(1, downloadingMediaFile);

                } catch (Exception e) {

                }

                Log.i(TAG, "Finishing T"+chunkClass1.getPos()+".:" + total);
            }
        });

        t1.start();
        if(chunkClass1 != null) {
            t2.start();
        }


        return null;
    }

    private void threadsHaveFinish(ChunkClass chunk) {
        if(mChunks == null){
            return;
        }
        nextChunkArrive ++;
        if(nextChunkArrive == mChunks.size()) {
           try{
               String filename = "full_song.mp3";
               downloadingMediaFile = new File(getApplicationContext().getCacheDir(), filename);
               if (downloadingMediaFile.exists()) {
                   downloadingMediaFile.delete();
               }

               OutputStream output = new FileOutputStream(downloadingMediaFile);
                for(ChunkClass c : mChunks){
                    Log.d(TAG, c.getFullPath());
                    File file = new File(c.getFullPath());
                    int size = (int) file.length() - 1;
                    byte[] bytes = new byte[size];

                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();

                    output.write(bytes, 0, size);

                    file.delete();
                }
               output.flush();
               output.close();

               Log.d(TAG, String.valueOf(downloadingMediaFile.length()));


               playSong(downloadingMediaFile);
           }catch (Exception e){
                e.printStackTrace();
           }
        }

    }



    public static Map<String, String> splitToMap(String source, String entriesSeparator, String keyValueSeparator) {
        Map<String, String> map = new HashMap<String, String>();
        String[] entries = source.split(entriesSeparator);
        for (String entry : entries) {
            if (!TextUtils.isEmpty(entry) && entry.contains(keyValueSeparator)) {
                String[] keyValue = entry.split(keyValueSeparator);
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }


    private void playSong(File mediaFile) {
//    private void playSong(String songPath) {
        Log.d(TAG, String.valueOf(mediaFile.getAbsolutePath()));
        try {


            FileInputStream fileInputStream = new FileInputStream(mediaFile);
            Log.d(TAG, String.valueOf(fileInputStream.available()));


            mp.reset();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            mp.setDataSource(urlStream);
            mp.setDataSource(fileInputStream.getFD());
            fileInputStream.close();
//            mp.setDataSource(getApplicationContext(), Uri.parse(songPath));
//            mp.setDataSource("http://pubcache1.arkiva.de/test/hls_index.m3u8");

            // Setup listener so next song starts automatically
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                public void onCompletion(MediaPlayer mp) {
                    btnPlay.setState(PlayPauseView.BUTTON_STATE.STATE_COMPLETED);
                    songIsFetched = false;
                    isFetchingSong = false;
                    if(downloadingMediaFile != null){
                        downloadingMediaFile.delete();
                    }

//                    nextSong();
                }

            });
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    btnPlay.setState(PlayPauseView.BUTTON_STATE.STATE_PLAYING);
                    mp.start();
                }
            });

            mp.prepareAsync();


        } catch (IOException e) {
            e.printStackTrace();
            Log.v(getString(R.string.app_name), e.getMessage());
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        isFetchingSong = false;
        songIsFetched = true;

    }

    private void nextSong() {

    }


    public String getHLSFile(String _url) throws Exception {
        int count;

        URL url = new URL(_url);
        URLConnection con = url.openConnection();

        HttpURLConnection connection = (HttpURLConnection) con;

//                connection.setChunkedStreamingMode(0);
//                connection.setAllowUserInteraction(false);
//                con.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.connect();


        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "Server returned HTTP " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());

        }

        int lenghtOfFile = connection.getContentLength();


        InputStream input = new BufferedInputStream(url.openStream());
        long total = 0;

        String tt = "";
        byte _data[] = new byte[1024];

        while ((count = input.read(_data)) != -1) {
            tt += new String(_data);
        }

        return tt;

    }


    public String getHLSFileForBestQuality(String _url) throws Exception {
        int count;

        URL url = new URL(_url);
        URLConnection con = url.openConnection();

        HttpURLConnection connection = (HttpURLConnection) con;

//                connection.setChunkedStreamingMode(0);
//                connection.setAllowUserInteraction(false);
//                con.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.connect();


        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.d(TAG, "Server returned HTTP " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());

        }

        int lenghtOfFile = connection.getContentLength();


        InputStream input = new BufferedInputStream(url.openStream());
        long total = 0;

        String tt = "";
        byte _data[] = new byte[1024];

        while ((count = input.read(_data)) != -1) {
            tt += new String(_data);
        }

        return tt;

    }



}
