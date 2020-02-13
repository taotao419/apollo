package com.ctrip.framework.apollo.biz.repository;


import com.ctrip.framework.apollo.biz.entity.Cluster;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface ClusterRepository extends PagingAndSortingRepository<Cluster, Long> {

  /**
   * 按照Spring data 定义的规则，查询方法以find|read|get开头
   * 涉及条件查询时，条件的属性用条件关键字连接，要注意的是：条件属性以首字母大写其余字母小写为规定。
   * 条件的属性名称与个数要与参数的位置与个数一一对应
   * 定义一个Entity实体类
      class User｛
        private String firstname;
        private String lastname;
      ｝
      使用And条件连接时，应这样写：
      findByLastnameAndFirstname(String lastname,String firstname);
   */
  List<Cluster> findByAppIdAndParentClusterId(String appId, Long parentClusterId);

  List<Cluster> findByAppId(String appId);

  Cluster findByAppIdAndName(String appId, String name);

  List<Cluster> findByParentClusterId(Long parentClusterId);
}
