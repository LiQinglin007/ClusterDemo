package com.example.lql.clusterdemo.demo;

import com.amap.api.maps.model.LatLng;
import com.example.lql.clusterdemo.ClusterItem;


/**
 * Created by yiyi.qi on 16/10/10.
 */

public class RegionItem implements ClusterItem {

    @Override
    public String toString() {
        return "RegionItem{" +
                "mLatLng=" + mLatLng +
                ", mTitle='" + mTitle + '\'' +
                '}';
    }

    private LatLng mLatLng;
    private String mTitle;

    public RegionItem(LatLng latLng, String title) {
        mLatLng=latLng;
        mTitle=title;
    }

    @Override
    public LatLng getPosition() {
        // TODO Auto-generated method stub
        return mLatLng;
    }

    public String getTitle(){
        return mTitle;
    }

}
