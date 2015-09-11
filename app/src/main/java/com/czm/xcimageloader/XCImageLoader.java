package com.czm.xcimageloader;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;
import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
/**
 * Created by caizhiming on 2015/9/10.
 * 图片异步加载框架-XCImageLoader-图片加载器
 */
public class XCImageLoader {

    private static final String TAG = XCImageLoader.class.getSimpleName();
    /**
     * 单例模式
     */
    private static XCImageLoader mInstance;
    /**
     * Lru图片缓存对象
     */
    private LruCache<String, Bitmap> mLruCache;
    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEAFULT_THREAD_COUNT = 2;
    private int mThreadCount = DEAFULT_THREAD_COUNT;
    /**
     * 任务队列的调度方式-默认采用LIFO
     */
    private Type mType = Type.LIFO;
    public enum Type{
        FIFO,LIFO
    }
    /**
     * 是否开启磁盘缓存机制
     */
    private boolean mIsDiskCacheEnable = true;
    /**
     * 任务调度队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    /**
     * UI线程中的Handler
     */
    private Handler mUIHandler;
    /**
     * 信号量
     */
    private Semaphore mPoolThreadHandlerSemaphore = new Semaphore(0);
    private Semaphore mPoolTThreadSemaphore;
    public static XCImageLoader getInstance()
    {
        if (mInstance == null)
        {
            synchronized (XCImageLoader.class)
            {
                if (mInstance == null)
                {
                    mInstance = new XCImageLoader(DEAFULT_THREAD_COUNT,Type.LIFO);
                }
            }
        }
        return mInstance;
    }
    public static XCImageLoader getInstance(int threadCount,Type type)
    {
        if (mInstance == null)
        {
            synchronized (XCImageLoader.class)
            {
                if (mInstance == null)
                {
                    mInstance = new XCImageLoader(threadCount,type);
                }
            }
        }
        return mInstance;
    }
    private XCImageLoader(int threadCount,Type type){
        init(threadCount, type);
    }
    /**
     * 初始化信息
     * @param threadCount
     * @param type
     */
    private void init(int threadCount,Type type){
        initBackThread();
        //获取当前应用的最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        mLruCache = new LruCache<String,Bitmap>(maxMemory/8){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;
        mPoolTThreadSemaphore = new Semaphore(threadCount);
    }
    /**
     * 初始化后台轮询线程
     */
    private void initBackThread() {
        //后台轮询线程
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //从线程池中取出一个任务开始执行
                        mThreadPool.execute(getTaskFromQueue());
                        try {
                            mPoolTThreadSemaphore.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放信号量
                mPoolThreadHandlerSemaphore.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
    }
    /**
     * 从队列中取出一个任务来
     */
    private Runnable getTaskFromQueue() {
        if(mType == Type.FIFO){
            return mTaskQueue.removeFirst();
        }else if(mType == Type.LIFO){
            return mTaskQueue.removeLast();
        }
        return  null;
    }
    /**
     * 加载图片并显示到ImageView上
     */
    public void displayImage(final String path,final ImageView imageView
        ,final boolean isFromNet){
            imageView.setTag(path);
        if(mUIHandler == null){
            mUIHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    // 获取得到图片，为imageview回调设置图片
                    ImageHolder holder = (ImageHolder) msg.obj;
                    Bitmap bmp = holder.bitmap;
                    ImageView imageview = holder.imageView;
                    String path = holder.path;
                    // 将path与getTag存储路径进行比较，防止错乱
                    if (imageview.getTag().toString().equals(path))
                    {
                        if(bmp != null){
                            imageview.setImageBitmap(bmp);
                        }
                    }
                }
            };
        }
        // 根据path在缓存中获取bitmap
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null)
        {
            refreshBitmap(path, imageView, bm);
        }else{//如果没有LruCache，则创建任务并添加到任务队列中
            addTaskToQueue(createTask(path, imageView, isFromNet));
        }
    }
    /**
     * 根据参数，创建一个任务
     */
    private Runnable createTask(final String path, final ImageView imageView,
                                final boolean isFromNet)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                Bitmap bm = null;
                if (isFromNet)
                {
                    File file = getDiskCacheDir(imageView.getContext(),
                            Utils.makeMd5(path));
                    if (file.exists())// 如果在缓存文件中发现
                    {
                        Log.v(TAG, "disk cache image :" + path);
                        bm = loadImageFromLocal(file.getAbsolutePath(),
                                imageView);
                    } else
                    {
                        if (mIsDiskCacheEnable)// 检测是否开启硬盘缓存
                        {
                            boolean downloadState = ImageDownloadUtils
                                    .downloadImageByUrl(path, file);
                            if (downloadState)// 如果下载成功
                            {
                                Log.v(TAG,
                                        "download image :" + path
                                                + " to disk cache: "
                                                + file.getAbsolutePath());
                                bm = loadImageFromLocal(file.getAbsolutePath(),
                                        imageView);
                            }
                        } else
                        {// 直接从网络加载
                            bm = ImageDownloadUtils.downloadImageByUrl(path,
                                    imageView);
                        }
                    }
                } else
                {
                    bm = loadImageFromLocal(path, imageView);
                }
                // 3、把图片加入到缓存
                setBitmapToLruCache(path, bm);
                refreshBitmap(path, imageView, bm);
                mPoolTThreadSemaphore.release();
            }
        };
    }
    /**
     * 添加任务到任务队列中
     */
    private synchronized void addTaskToQueue(Runnable runnable)
    {
        mTaskQueue.add(runnable);
        try
        {
            if (mPoolThreadHandler == null)
                mPoolThreadHandlerSemaphore.acquire();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(24);
    }
    /**
     * 刷新图片到ImageView
     */
    private void refreshBitmap(final String path, final ImageView imageView,
                               Bitmap bm)
    {
        Message message = Message.obtain();
        ImageHolder holder = new ImageHolder();
        holder.bitmap = bm;
        holder.path = path;
        holder.imageView = imageView;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }
    /**
     * 获得缓存图片的地址
     */
    public File getDiskCacheDir(Context context, String uniqueName)
    {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState()))
        {//存在SD卡的情况
            cachePath = context.getExternalCacheDir().getPath();
        } else
        {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }
    /**
     * 将图片加入LruCache
     */
    protected void setBitmapToLruCache(String path, Bitmap bm)
    {
        if (getBitmapFromLruCache(path) == null)
        {
            if (bm != null)
                mLruCache.put(path, bm);
        }
    }
    /**
     * 根据path从缓存中获取bitmap
     */
    private Bitmap getBitmapFromLruCache(String key)
    {
        return mLruCache.get(key);
    }

    /**
     * 加载本地图片
     */
    private Bitmap loadImageFromLocal(final String path,
                                      final ImageView imageView)
    {
        Bitmap bm;
        // 加载图片
        // 图片的压缩
        // 1、获得图片需要显示的大小
        ImageUtils.ImageSize imageSize = ImageUtils.getImageViewSize(imageView);
        // 2、压缩图片
        bm = decodeSampledBitmapFromPath(path, imageSize.width,
                imageSize.height);
        return bm;
    }
    /**
     * 根据图片需要显示的宽和高对图片进行压缩
     */
    protected Bitmap decodeSampledBitmapFromPath(String path, int width,
                                                 int height)
    {
        // 获得图片的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = ImageUtils.calculateInSampleSize(options,
                width, height);

        // 使用获得到的InSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    /**
     * Image实体类Holder
     */
    private static class ImageHolder{
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
