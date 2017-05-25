package com.example.lql.clusterdemo;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.AlphaAnimation;
import com.amap.api.maps.model.animation.Animation;
import com.example.lql.clusterdemo.demo.RegionItem;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by yiyi.qi on 16/10/10.
 * 整体设计采用了两个线程,一个线程用于计算组织聚合数据,一个线程负责处理Marker相关操作
 */
public class ClusterOverlay implements AMap.OnCameraChangeListener,
        AMap.OnMarkerClickListener {
    private AMap mAMap;
    private Context mContext;
    private List<ClusterItem> mClusterItems;  //坐标点数组
    private List<Cluster> mClusters;//聚合点数组
    private int mClusterSize;   //聚合范围的大小
    private ClusterClickListener mClusterClickListener;
    private ClusterRender mClusterRender;
    private List<Marker> mAddMarkers = new ArrayList<Marker>();
    private double mClusterDistance;  //聚合距离
    private LruCache<Integer, BitmapDescriptor> mLruCache;
    private LruCache<String, BitmapDescriptor> mLruCacheName;
    private HandlerThread mMarkerHandlerThread = new HandlerThread("addMarker");
    private HandlerThread mSignClusterThread = new HandlerThread("calculateCluster");
    private Handler mMarkerhandler; //更新marker
    private Handler mSignClusterHandler;  //计算marker
    private float mPXInMeters;
    private boolean mIsCanceled = false;

    /**
     * 构造函数
     *
     * @param amap
     * @param clusterSize 聚合范围的大小（指点像素单位距离内的点会聚合到一个点显示）
     * @param context
     */
    public ClusterOverlay(AMap amap, int clusterSize, Context context) {
        this(amap, null, clusterSize, context);
    }

    /**
     * 构造函数,批量添加聚合元素时,调用此构造函数
     *
     * @param amap
     * @param clusterItems 聚合元素
     * @param clusterSize 聚合范围的大小（指点像素单位距离内的点会聚合到一个点显示）
     * @param context
     */
    public ClusterOverlay(AMap amap, List<ClusterItem> clusterItems,
                          int clusterSize, Context context) {
        //默认最多会缓存80张图片作为聚合显示元素图片,根据自己显示需求和app使用内存情况,可以修改数量
        mLruCache = new LruCache<Integer, BitmapDescriptor>(80) {
            protected void entryRemoved(boolean evicted, Integer key, BitmapDescriptor oldValue,
                                        BitmapDescriptor newValue) {
                oldValue.getBitmap().recycle();
            }
        };

        mLruCacheName = new LruCache<String, BitmapDescriptor>(80) {
            protected void entryRemoved(boolean evicted, Integer key, BitmapDescriptor oldValue, BitmapDescriptor newValue) {
                oldValue.getBitmap().recycle();
            }
        };
        if (clusterItems != null) {
            mClusterItems = clusterItems;
        } else {
            mClusterItems = new ArrayList<ClusterItem>();
        }
        mContext = context;
        mClusters = new ArrayList<Cluster>();
        this.mAMap = amap;
        mClusterSize = clusterSize;
        mPXInMeters = mAMap.getScalePerPixel();
        mClusterDistance = mPXInMeters * mClusterSize;
        amap.setOnCameraChangeListener(this);
        amap.setOnMarkerClickListener(this);
        initThreadHandler();//初始化线程
        assignClusters();//把点进行聚合
    }

    /**
     * 设置聚合点的点击事件
     *
     * @param clusterClickListener
     */
    public void setOnClusterClickListener(ClusterClickListener clusterClickListener) {
        mClusterClickListener = clusterClickListener;
    }

    /**
     * 添加一个聚合点
     *
     * @param item
     */
    public void addClusterItem(ClusterItem item) {
        Message message = Message.obtain();
        message.what = 1;
        message.obj = item;
        mSignClusterHandler.sendMessage(message);
    }

    /**
     * 设置聚合元素的渲染样式，不设置则默认为气泡加数字形式进行渲染
     *
     * @param render
     */
    public void setClusterRenderer(ClusterRender render) {
        mClusterRender = render;
    }

    /**
     * 销毁资源
     */
    public void onDestroy() {
        mIsCanceled = true;
        mSignClusterHandler.removeCallbacksAndMessages(null);
        mMarkerhandler.removeCallbacksAndMessages(null);
        mSignClusterThread.quit();
        mMarkerHandlerThread.quit();
        for (int i = 0; i <mAddMarkers.size() ; i++) {
            mAddMarkers.get(i).remove();
        }
        mAddMarkers.clear();
        mLruCache.evictAll();
    }

    //初始化Handler
    private void initThreadHandler() {
        mMarkerHandlerThread.start();
        mSignClusterThread.start();
        mMarkerhandler = new MarkerHandler(mMarkerHandlerThread.getLooper());
        mSignClusterHandler = new SignClusterHandler(mSignClusterThread.getLooper());
    }

    @Override
    public void onCameraChange(CameraPosition arg0) {

    }

    @Override
    public void onCameraChangeFinish(CameraPosition arg0) {
        //完成缩放的时候
        mPXInMeters = mAMap.getScalePerPixel();
        mClusterDistance = mPXInMeters * mClusterSize;
        //重新对点进行聚合
        assignClusters();
    }

    //点击事件
    @Override
    public boolean onMarkerClick(Marker arg0) {
        if (mClusterClickListener == null) {
            return true;
        }
       Cluster cluster= (Cluster) arg0.getObject();
        if(cluster!=null){
            mClusterClickListener.onClick(arg0,cluster.getClusterItems());
            return true;
        }
        return false;
    }


    /**
     * 将聚合元素添加至地图上
     */
    private void addClusterToMap(List<Cluster> clusters) {

        ArrayList<Cluster> mList =new ArrayList<>();
        mList.addAll(clusters);

        ArrayList<Marker> removeMarkers = new ArrayList<>();
        //已经添加过的聚合元素
        removeMarkers.addAll(mAddMarkers);
        //做一个隐藏的动画
        AlphaAnimation alphaAnimation=new AlphaAnimation(1, 0);
        MyAnimationListener myAnimationListener=new MyAnimationListener(removeMarkers);
        for (Marker marker : removeMarkers) {
            marker.setAnimation(alphaAnimation);
            marker.setAnimationListener(myAnimationListener);
            marker.startAnimation();
        }

        //然后再把所有的聚合元素重新添加
        for (int i = 0; i <clusters.size() ; i++) {
            addSingleClusterToMap(clusters.get(i));
        }

    }

    private AlphaAnimation mADDAnimation=new AlphaAnimation(0, 1);

    /**
     * 将单个聚合元素添加至地图显示
     * @param cluster
     */
    private void addSingleClusterToMap(Cluster cluster) {
        Cluster mCluster = cluster;
        LatLng latlng = cluster.getCenterLatLng();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.anchor(0.5f, 0.5f) .icon(getBitmapDes(cluster)).position(latlng);

        Marker marker = mAMap.addMarker(markerOptions);
        marker.setAnimation(mADDAnimation);
        marker.setObject(cluster);

        marker.startAnimation();
        cluster.setMarker(marker);
        mAddMarkers.add(marker);
    }


    /**
     * 这个貌似是处理多个坐标点
     */
    private void calculateClusters() {
        mIsCanceled = false;
        mClusters.clear();
        //判断现在地图上的区域是不是应该包含这个点，如果包含，就把点加到聚合数据里边，然后去通知mMarkerhandler更新一下。

        LatLngBounds visibleBounds = mAMap.getProjection().getVisibleRegion().latLngBounds;  //由可视区域的四个顶点形成的经纬度范围
        for (int i = 0; i < mClusterItems.size(); i++) {
            if (mIsCanceled) {
                return;
            }
            LatLng latlng = mClusterItems.get(i).getPosition();
            if (visibleBounds.contains(latlng)) {
                //判断坐标是否可以依附某个聚合点   不可以返回null
                Cluster cluster = getCluster(latlng,mClusters);
                if (cluster != null) {
                    cluster.addClusterItem(mClusterItems.get(i));
                } else {
                    //没有可以依附的聚合点的时候，就创建一个新的聚合点，加到聚合点数组里边
                    cluster = new Cluster(latlng);
                    mClusters.add(cluster);
                    //然后把这个坐标加到聚合点里边
                    cluster.addClusterItem(mClusterItems.get(i));
                }
            }
        }

        //复制一份数据，规避同步
        //创建一个新的聚合点数组
        List<Cluster> clusters = new ArrayList<Cluster>();
        //把现有的聚合点加进去
        clusters.addAll(mClusters);
        //给后给控制marher的headler发送消息。把所有的聚合点信息发过去
        Message message = Message.obtain();
        message.what = 0;
        message.obj = clusters;
        if (mIsCanceled) {
            return;
        }
        mMarkerhandler.sendMessage(message);
    }

    /**
     * 对点进行聚合
     */
    private void assignClusters() {
        mIsCanceled = true;
        mSignClusterHandler.removeMessages(0);//先把队列里边的消息移除
        mSignClusterHandler.sendEmptyMessage(0);//然后再发消息
    }

    /**
     * 在已有的聚合基础上，对添加的单个元素进行聚合
     * @param clusterItem
     */
    private void calculateSingleCluster(ClusterItem clusterItem) {
        LatLngBounds visibleBounds = mAMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng latlng = clusterItem.getPosition();
        if (!visibleBounds.contains(latlng)) {
            return;
        }
        Cluster cluster = getCluster(latlng,mClusters);
        if (cluster != null) {
            cluster.addClusterItem(clusterItem);
            Message message = Message.obtain();
            message.what = 2;
            message.obj = cluster;
            mMarkerhandler.removeMessages(2);
            mMarkerhandler.sendMessageDelayed(message, 5);

        } else {
            cluster = new Cluster(latlng);
            mClusters.add(cluster);
            cluster.addClusterItem(clusterItem);
            Message message = Message.obtain();
            message.what = 1;
            message.obj = cluster;
            mMarkerhandler.sendMessage(message);

        }
    }

    /**
     * 根据一个点获取是否可以依附的聚合点，没有则返回null
     *
     * @param latLng
     * @return
     */
    private Cluster getCluster(LatLng latLng,List<Cluster>clusters) {
        for (Cluster cluster : clusters) {
            LatLng clusterCenterPoint = cluster.getCenterLatLng();
            double distance = AMapUtils.calculateLineDistance(latLng, clusterCenterPoint);
            if (distance < mClusterDistance) {
                return cluster;
            }
        }
        return null;
    }


    /**
     * 获取每个聚合点的绘制样式
     */
    private BitmapDescriptor getBitmapDes(Cluster mCluster) {
        Cluster mCluster1 = mCluster;
        BitmapDescriptor bitmapDescriptor;
        if(mCluster.getClusterCount() > 1){//当数量》1设置个数
            bitmapDescriptor = mLruCache.get(mCluster.getClusterCount());
            if (bitmapDescriptor == null) {
                TextView textView = new TextView(mContext);
                String tile = String.valueOf(mCluster.getClusterCount());
//                textView.setText("附近有\n"+tile+"个"+"\n坐标");
                textView.setText(tile);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                if (mClusterRender != null && mClusterRender.getDrawAble(mCluster.getClusterCount()) != null) {
                    textView.setBackgroundDrawable(mClusterRender.getDrawAble(mCluster.getClusterCount()));
                } else {
                    textView.setBackgroundResource(R.drawable.yuan);
                }
                bitmapDescriptor = BitmapDescriptorFactory.fromView(textView);
                mLruCache.put(mCluster.getClusterCount(), bitmapDescriptor);
            }
        }else{//否则，设置名称
            RegionItem mRegionItem= (RegionItem) mCluster.getClusterItems().get(0);
            bitmapDescriptor = mLruCacheName.get(mRegionItem.getTitle());
            if (bitmapDescriptor == null) {
                TextView textView = new TextView(mContext);
                textView.setText(mRegionItem.getTitle());
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                if (mClusterRender != null && mClusterRender.getDrawAble(mCluster.getClusterCount()) != null) {
                    textView.setBackgroundDrawable(mClusterRender.getDrawAble(mCluster.getClusterCount()));
                } else {
                    textView.setBackgroundResource(R.drawable.yuan);
                }
                bitmapDescriptor = BitmapDescriptorFactory.fromView(textView);
                mLruCacheName.put(mRegionItem.getTitle(), bitmapDescriptor);
            }
        }
        return bitmapDescriptor;
    }

    /**
     * 更新已加入地图聚合点的样式
     */
    private void updateCluster(Cluster cluster) {
        Marker marker = cluster.getMarker();
        marker.setIcon(getBitmapDes(cluster));
    }


//-----------------------辅助内部类用---------------------------------------------

    /**
     * marker渐变动画，动画结束后将Marker删除
     */
    class MyAnimationListener implements Animation.AnimationListener {
        private  List<Marker> mRemoveMarkers ;

        MyAnimationListener(List<Marker> removeMarkers) {
            mRemoveMarkers = removeMarkers;
        }

        @Override
        public void onAnimationStart() {

        }

        @Override
        public void onAnimationEnd() {
            for(Marker marker:mRemoveMarkers){
                marker.remove();
            }
            mRemoveMarkers.clear();
        }
    }

    /**
     * 处理market添加，更新等操作
     */
    class MarkerHandler extends Handler {

        MarkerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case 0://接收到在当前区域内应该显示的所有的聚合点，把聚合点加到地图上
                    List<Cluster> clusters = (List<Cluster>) message.obj;
                    addClusterToMap(clusters);
                    break;
                case 1://接收单个聚合点
                    Cluster cluster = (Cluster) message.obj;
                    addSingleClusterToMap(cluster);
                    break;
                case 2:
                    Cluster updateCluster = (Cluster) message.obj;
                    updateCluster(updateCluster);
                    break;
            }
        }
    }

    /**
     * 处理聚合点算法线程
     */
    class SignClusterHandler extends Handler {

        SignClusterHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    calculateClusters();
                    break;
                case 1:
                    ClusterItem item = (ClusterItem) message.obj;
                    mClusterItems.add(item);
                    Log.e("###","添加单个聚合点"+((RegionItem)item).getTitle());
                    calculateSingleCluster(item);
                    break;
            }
        }
    }
}