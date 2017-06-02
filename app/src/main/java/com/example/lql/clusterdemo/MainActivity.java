package com.example.lql.clusterdemo;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.ClusterMarkerOverlay;
import com.example.lql.clusterdemo.demo.RegionItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements  EasyPermissions.PermissionCallbacks
        ,ClusterMarkerOverlay.ClusterRender ,AMap.OnMapLoadedListener ,ClusterClickListener {

    final int RC_CAMERA_AND_WIFI=0x1;

    MapView mapView;
    private AMap aMap;
    MyLocationStyle myLocationStyle;

    private int clusterRadius = 100; //半径

    private Map<Integer, Drawable> mBackDrawAbles = new HashMap<Integer, Drawable>();

    private ClusterOverlay mClusterOverlay;
    private MapView testmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        initView(savedInstanceState);
    }

    private void initView(Bundle savedInstanceState) {

        mapView= (MapView) findViewById(R.id.test_map);
        mapView.onCreate(savedInstanceState);
        aMap=mapView.getMap();

        if(null==aMap){
            aMap=mapView.getMap();
            UiSettings uiSettings =  aMap.getUiSettings();
            uiSettings.setLogoBottomMargin(-50);//隐藏logo
            uiSettings.setTiltGesturesEnabled(false);// 禁用倾斜手势。
            uiSettings.setRotateGesturesEnabled(false);// 禁用旋转手势。
            uiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_CENTER);//放大缩小按钮放在屏幕中间

            setMapCustomStyleFile(MainActivity.this);
        }
        chackPermission();
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 必须回调MapView的onSaveInstanceState()方法
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁资源
        mClusterOverlay.onDestroy();
        mapView.onDestroy();
    }



    /**
     * 定义所需要的权限
     */
    String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE };

    /**
     * 检查权限
     */
    private void chackPermission(){
        if (EasyPermissions.hasPermissions(this, perms)) {
            Location();
        } else {
            EasyPermissions.requestPermissions(this, "拍照需要摄像头权限", RC_CAMERA_AND_WIFI, perms);
        }
    }


    /**
     * 定位初始化地图
     */
    private void  Location(){
            myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE) ;//定位一次，且将视角移动到地图中心点。
            myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
            aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
            aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
            aMap.setOnMapLoadedListener(this);
//            addPoint();
            addLine();
    }


    private void addLine(){

        List<LatLng> latLngs = new ArrayList<LatLng>();
        latLngs.add(new LatLng(31.2593572798,121.1443197727));
        latLngs.add(new LatLng(31.2583300886,121.1432147026));
        latLngs.add(new LatLng(31.2570827699,121.1426997185));
        latLngs.add(new LatLng(31.2553309931,121.1422920227));

        latLngs.add(new LatLng(31.2531481053,121.1407470703));
        latLngs.add(new LatLng(31.2518823742,121.1393952370));
        latLngs.add(new LatLng(31.2512036418,121.1384296417));
        latLngs.add(new LatLng(31.2489656247,121.1373138428));

        latLngs.add(new LatLng(31.2470577650,121.1367774010));
        latLngs.add(new LatLng(31.2460671303,121.1362838745));
        latLngs.add(new LatLng(31.2428016314,121.1337089539));
        latLngs.add(new LatLng(31.2345823478,121.1267781258));

        latLngs.add(new LatLng(31.2303623484,121.1230659485));
        latLngs.add(new LatLng(31.2248026704,121.1222076416));


        Polyline polyline = aMap.addPolyline(new PolylineOptions().
                addAll(latLngs).width(10).color(Color.argb(255, 255 ,0 ,0)));

    }

//    /**
//     * 添加点
//     */
//    private void addPoint(){
//        //点击可以动态添加点
//        aMap.setOnMapClickListener(new AMap.OnMapClickListener() {
//            @Override
//            public void onMapClick(LatLng latLng) {
//                double lat = Math.random() + 39.474923;
//                double lon = Math.random() + 116.027116;
//                LatLng latLng1 = new LatLng(lat, lon, false);
//                RegionItem regionItem = new RegionItem(latLng1,
//                        "test");
//                mClusterOverlay.addClusterItem(regionItem);
//            }
//        });
//    }
//

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    //成功
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
        Location();
    }

    //失败
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied
        Toast.makeText(this, "该应该需要开启定位及获取SD卡相关权限，请手动开启", Toast.LENGTH_SHORT).show();
    }


    /**
     * 初始化地图（勿动）李清林
     * @param context
     */
    private void setMapCustomStyleFile(Context context) {
        String styleName = "style_json.json";
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        String filePath = null;
        try {
            inputStream = context.getAssets().open(styleName);
            byte[] b = new byte[inputStream.available()];
            inputStream.read(b);

            filePath = context.getFilesDir().getAbsolutePath();
            File file = new File(filePath + "/" + styleName);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            outputStream = new FileOutputStream(file);
            outputStream.write(b);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();

                if (outputStream != null)
                    outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        aMap.setCustomMapStylePath(filePath + "/" + styleName);

        aMap.showMapText(true);//是否显示地理位置名称

    }

    /**
     * 地图加载完成之后的回调
     */
    @Override
    public void onMapLoaded() {
        new Thread() {
            public void run() {
                List<ClusterItem> items = new ArrayList<ClusterItem>();
                //随机10000个点
//                for (int i = 0; i < 10000; i++) {
//                    double lat = Math.random() + 39.474923;
//                    double lon = Math.random() + 116.027116;
//                    LatLng latLng = new LatLng(lat, lon, false);
//                    RegionItem regionItem = new RegionItem(latLng, "test" + i);
//                    items.add(regionItem);
//                }

                LatLng latLng1 = new LatLng( 38.0461800000 , 114.6140400000,  false);
                LatLng latLng2 = new LatLng( 38.0443300000 , 114.6073100000,  false);
                LatLng latLng3 = new LatLng( 38.0141300000 , 114.5598600000,  false);
                LatLng latLng4 = new LatLng( 38.0352950000 , 114.6704720000 , false);
                LatLng latLng5 = new LatLng( 37.9901110000 , 114.6876740000 , false);
                LatLng latLng6 = new LatLng( 39.6544940000 , 118.1688190000 , false);
                LatLng latLng7 = new LatLng( 39.6304729827 , 118.1289666620 , false);
                LatLng latLng8 = new LatLng( 39.9960460000 , 118.7169020000 , false);
                LatLng latLng9 = new LatLng( 39.7494320000 , 118.7091430000 , false);
                LatLng latLng10 = new LatLng( 40.0166770000 , 118.7535150000 ,  false);

                LatLng latLng11 = new LatLng( 39.9722700000 , 116.7762090000 ,  false);
                LatLng latLng12 = new LatLng( 39.1441170000 , 116.6423350000 ,  false);
                LatLng latLng13 = new LatLng( 39.1077020000 , 116.3645280000 ,  false);
                LatLng latLng14 = new LatLng( 39.1014920000 , 116.4732270000 ,  false);
                LatLng latLng15 = new LatLng( 39.5200990000 , 116.6999510000 ,  false);
                LatLng latLng16 = new LatLng( 39.5846250000 , 116.7607720000 ,  false);
                LatLng latLng17 = new LatLng( 37.6948730000 , 116.2599780000 ,  false);
                LatLng latLng18 = new LatLng( 38.8755350000 , 116.4722320000 ,  false);
                LatLng latLng19 = new LatLng( 36.5877910000 , 114.5070920000 ,  false);
                LatLng latLng20 = new LatLng( 40.0456370000 , 119.5531600000 ,  false);

                LatLng latLng21 = new LatLng( 39.9678620044 , 119.5698771699 ,  false);
                LatLng latLng22 = new LatLng( 39.9376034482 , 119.5458578617 ,  false);
                LatLng latLng23 = new LatLng( 40.0456370000 , 119.5531600000 ,  false);
                LatLng latLng24 = new LatLng( 38.0444748802 , 116.7127498185 ,  false);
                LatLng latLng25 = new LatLng( 38.7020624096 , 116.1097408463 ,  false);
                LatLng latLng26 = new LatLng( 38.3204504273 , 116.8651637885 ,  false);
                LatLng latLng27 = new LatLng( 38.0585830000 , 117.2381180000 ,  false);
                LatLng latLng28 = new LatLng( 38.0658010844 , 116.5728435979 ,  false);
                LatLng latLng29 = new LatLng( 37.8720459038 , 116.5440219180 ,  false);
                LatLng latLng30 = new LatLng( 37.8665776175 , 116.5610717036 ,  false);


                LatLng latLng31 = new LatLng( 38.3231604273 , 116.8636327885 , false);
                LatLng latLng32 = new LatLng( 38.2781152530 , 117.7740563118 , false);
                LatLng latLng33 = new LatLng( 37.6306052257 , 116.3969213267 , false);
                LatLng latLng34 = new LatLng( 37.7223453840 , 115.7865732550 , false);
                LatLng latLng35 = new LatLng( 38.2378324802 , 115.5042831752 , false);
                LatLng latLng36 = new LatLng( 37.5374340524 , 115.5813681419 , false);
                LatLng latLng37 = new LatLng( 37.7756573582 , 115.8075229675 , false);
                LatLng latLng38 = new LatLng( 37.7374387570 , 115.6284924279 , false);
                LatLng latLng39 = new LatLng( 40.7783126534 , 114.8737683907 , false);
                LatLng latLng40 = new LatLng( 38.6999692482 , 115.7812863022 , false);


                LatLng latLng41 = new LatLng( 38.8973220147 , 115.4724232215 ,false);
                LatLng latLng42 = new LatLng( 39.3392018779 , 115.9136276197 ,false);
                LatLng latLng43 = new LatLng( 39.4767115200 , 115.9924796580 ,false);
                LatLng latLng44 = new LatLng( 40.9598936605 , 117.9490676414 ,false);

                RegionItem regionItem1 = new RegionItem(latLng1, "test1" );
                RegionItem regionItem2 = new RegionItem(latLng2, "test2" );
                RegionItem regionItem3 = new RegionItem(latLng3, "test3" );
                RegionItem regionItem4 = new RegionItem(latLng4, "test4" );
                RegionItem regionItem5 = new RegionItem(latLng5, "test5" );
                RegionItem regionItem6 = new RegionItem(latLng6, "test6" );
                RegionItem regionItem7 = new RegionItem(latLng7, "test7" );
                RegionItem regionItem8 = new RegionItem(latLng8, "test8" );
                RegionItem regionItem9 = new RegionItem(latLng9, "test9" );
                RegionItem regionItem10 = new RegionItem(latLng10, "test10" );


                RegionItem regionItem21 = new RegionItem(latLng21, "test21" );
                RegionItem regionItem22 = new RegionItem(latLng22, "test22" );
                RegionItem regionItem23 = new RegionItem(latLng23, "test23" );
                RegionItem regionItem24 = new RegionItem(latLng24, "test24" );
                RegionItem regionItem25 = new RegionItem(latLng25, "test25" );
                RegionItem regionItem26 = new RegionItem(latLng26, "test26" );
                RegionItem regionItem27 = new RegionItem(latLng27, "test27" );
                RegionItem regionItem28 = new RegionItem(latLng28, "test28" );
                RegionItem regionItem29 = new RegionItem(latLng29, "test29" );
                RegionItem regionItem30 = new RegionItem(latLng30, "test30" );

                RegionItem regionItem11 = new RegionItem(latLng11, "test11");
                RegionItem regionItem12 = new RegionItem(latLng12, "test12");
                RegionItem regionItem13 = new RegionItem(latLng13, "test13");
                RegionItem regionItem14 = new RegionItem(latLng14, "test14");
                RegionItem regionItem15 = new RegionItem(latLng15, "test15");
                RegionItem regionItem16 = new RegionItem(latLng16, "test16");
                RegionItem regionItem17 = new RegionItem(latLng17, "test17");
                RegionItem regionItem18 = new RegionItem(latLng18, "test18");
                RegionItem regionItem19 = new RegionItem(latLng19, "test19");
                RegionItem regionItem20 = new RegionItem(latLng20, "test20");

                RegionItem regionItem31 = new RegionItem(latLng31, "test31" );
                RegionItem regionItem32 = new RegionItem(latLng32, "test32" );
                RegionItem regionItem33 = new RegionItem(latLng33, "test33" );
                RegionItem regionItem34 = new RegionItem(latLng34, "test34" );
                RegionItem regionItem35 = new RegionItem(latLng35, "test35" );
                RegionItem regionItem36 = new RegionItem(latLng36, "test36" );
                RegionItem regionItem37 = new RegionItem(latLng37, "test37" );
                RegionItem regionItem38 = new RegionItem(latLng38, "test38" );
                RegionItem regionItem39 = new RegionItem(latLng39, "test39" );
                RegionItem regionItem40 = new RegionItem(latLng40, "test40" );


                RegionItem regionItem41 = new RegionItem(latLng41, "test41" );
                RegionItem regionItem42 = new RegionItem(latLng42, "test42" );
                RegionItem regionItem43 = new RegionItem(latLng43, "test43" );
                RegionItem regionItem44 = new RegionItem(latLng44, "test44" );



                items.add(regionItem1);
                items.add(regionItem2);
                items.add(regionItem3);
                items.add(regionItem4);
                items.add(regionItem5);
                items.add(regionItem6);
                items.add(regionItem7);
                items.add(regionItem8);
                items.add(regionItem9);
                items.add(regionItem10);

                items.add(regionItem11);
                items.add(regionItem12);
                items.add(regionItem13);
                items.add(regionItem14);
                items.add(regionItem15);
                items.add(regionItem16);
                items.add(regionItem17);
                items.add(regionItem18);
                items.add(regionItem19);
                items.add(regionItem20);


                items.add(regionItem21);
                items.add(regionItem22);
                items.add(regionItem23);
                items.add(regionItem24);
                items.add(regionItem25);
                items.add(regionItem26);
                items.add(regionItem27);
                items.add(regionItem28);
                items.add(regionItem29);
                items.add(regionItem30);



                items.add(regionItem31);
                items.add(regionItem32);
                items.add(regionItem33);
                items.add(regionItem34);
                items.add(regionItem35);
                items.add(regionItem36);
                items.add(regionItem37);
                items.add(regionItem38);
                items.add(regionItem39);
                items.add(regionItem40);


                items.add(regionItem41);
                items.add(regionItem42);
                items.add(regionItem43);
                items.add(regionItem44);



                mClusterOverlay = new ClusterOverlay(aMap, items,
                        dp2px(getApplicationContext(), clusterRadius),
                        getApplicationContext());
//                mClusterOverlay.setClusterRenderer(MainActivity.this);
                mClusterOverlay.setOnClusterClickListener(MainActivity.this);

            }

        }.start();
    }

    @Override
    public Drawable getDrawAble(int clusterNum) {
        int radius = dp2px(getApplicationContext(), 80);
        if (clusterNum == 1) {
            Drawable bitmapDrawable = mBackDrawAbles.get(1);
            if (bitmapDrawable == null) {
                bitmapDrawable =
                        getApplication().getResources().getDrawable(
                                R.drawable.icon_openmap_mark);
                mBackDrawAbles.put(1, bitmapDrawable);
            }

            return bitmapDrawable;
        } else if (clusterNum < 5) {

            Drawable bitmapDrawable = mBackDrawAbles.get(2);
            if (bitmapDrawable == null) {
                bitmapDrawable = new BitmapDrawable(null, drawCircle(radius,
                        Color.argb(159, 210, 154, 6)));
                mBackDrawAbles.put(2, bitmapDrawable);
            }

            return bitmapDrawable;
        } else if (clusterNum < 10) {
            Drawable bitmapDrawable = mBackDrawAbles.get(3);
            if (bitmapDrawable == null) {
                bitmapDrawable = new BitmapDrawable(null, drawCircle(radius,
                        Color.argb(199, 217, 114, 0)));
                mBackDrawAbles.put(3, bitmapDrawable);
            }

            return bitmapDrawable;
        } else {
            Drawable bitmapDrawable = mBackDrawAbles.get(4);
            if (bitmapDrawable == null) {
                bitmapDrawable = new BitmapDrawable(null, drawCircle(radius,
                        Color.argb(235, 215, 66, 2)));
                mBackDrawAbles.put(4, bitmapDrawable);
            }

            return bitmapDrawable;
        }
    }


    private Bitmap drawCircle(int radius, int color) {

        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        RectF rectF = new RectF(0, 0, radius * 2, radius * 2);
        paint.setColor(color);
        canvas.drawArc(rectF, 0, 360, true, paint);
        return bitmap;
    }

    @Override
    public void onClick(Marker marker, List<ClusterItem> clusterItems) {
        if(clusterItems.size()==1){
            RegionItem regionItem=(RegionItem)clusterItems.get(0);
            Toast.makeText(this, "点击的是"+regionItem.getTitle(), Toast.LENGTH_SHORT).show();
        }else{
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (ClusterItem clusterItem : clusterItems) {
                builder.include(clusterItem.getPosition());
            }
            LatLngBounds latLngBounds = builder.build();
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));
        }

    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


}
