package com.neo.lrucache.lrucachedemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private LruCache<String, Bitmap> mMemoryCache;

    private Button button;
    private ImageView imageView;

    private static String url = "http://img1.imgtn.bdimg.com/it/u=4063154249,2505929094&fm=27&gp=0.jpg";
    private static String url2 = "http://img5.imgtn.bdimg.com/it/u=664218364,1651852111&fm=27&gp=0.jpg";

    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSIONS = 1;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button);
        imageView = (ImageView) findViewById(R.id.imageView);

        /*获取到可用内存的最大值，使用内存超出这个值会引起OutOfMemory异常,LruCache通过构造函数传入缓存值，以KB为单位。
          使用最大可用内存值的1/8作为缓存的大小。*/
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap bitmap) {
                /*重写此方法来衡量每张图片的大小，默认返回图片数量。*/
                return bitmap.getByteCount() / 1024;
            }
        };

        /*动态请求权限*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSIONS);
                requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSIONS);
            }
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadBitmap(url,imageView);
            }
        });

    }

    /**
     * 缓存图片
     * @param key
     * @param bitmap
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * 获取缓存的图片
     * @param key
     * @return
     */
    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * 加载图片
     * @param key
     * @param imageView
     */
    public void loadBitmap(String key, ImageView imageView) {
        final Bitmap bitmap = getBitmapFromMemCache(key);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.mipmap.ic_launcher);
            BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            task.execute(key);
        }
    }

    /**
     * 异步加载网络图片
     */
    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        public ImageView mImageView;

        public BitmapWorkerTask(ImageView mImageView) {
            this.mImageView = mImageView;
        }

        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            try {
                bitmap = getBitmapFromURL(params[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            addBitmapToMemoryCache(params[0], bitmap);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mImageView.setImageBitmap(bitmap);
        }
    }

    /**
     * URL 转 Bitmap
     */
    public static Bitmap getBitmapFromURL(String path) throws MalformedURLException {

        Bitmap b = null;
        URL url = new URL(path);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                b = bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("Neo","b=="+b);
        return b;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSIONS: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "允许读写存储！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "未允许读写存储！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            case REQUEST_CODE_READ_EXTERNAL_STORAGE_PERMISSIONS: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "允许读写存储！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "未允许读写存储！", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
}
