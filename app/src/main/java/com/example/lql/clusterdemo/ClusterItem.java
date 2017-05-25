package com.example.lql.clusterdemo;

import com.amap.api.maps.model.LatLng;

/**
 * Created by yiyi.qi on 16/10/10.
 */
//这是聚合点的bean
public interface ClusterItem {

    /**
     * 返回聚合元素的地理位置
     *
     * @return
     */
     LatLng getPosition();
}
