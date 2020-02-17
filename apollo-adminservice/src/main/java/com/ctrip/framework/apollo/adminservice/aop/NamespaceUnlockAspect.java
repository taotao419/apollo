package com.ctrip.framework.apollo.adminservice.aop;


import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.service.ItemService;
import com.ctrip.framework.apollo.biz.service.NamespaceLockService;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * 如果是一个回滚操作 则解锁namespace 
 * unlock namespace if is redo operation.
 * --------------------------------------------
 * For example: If namespace has a item K1 = v1
 * --------------------------------------------
 * First operate: change k1 = v2 (lock namespace)
 * Second operate: change k1 = v1 (unlock namespace)
 */
@Aspect
@Component
public class NamespaceUnlockAspect {

  private Gson gson = new Gson();

  private final NamespaceLockService namespaceLockService;
  private final NamespaceService namespaceService;
  private final ItemService itemService;
  private final ReleaseService releaseService;
  private final BizConfig bizConfig;

  public NamespaceUnlockAspect(
      final NamespaceLockService namespaceLockService,
      final NamespaceService namespaceService,
      final ItemService itemService,
      final ReleaseService releaseService,
      final BizConfig bizConfig) {
    this.namespaceLockService = namespaceLockService;
    this.namespaceService = namespaceService;
    this.itemService = itemService;
    this.releaseService = releaseService;
    this.bizConfig = bizConfig;
  }

  //AOP 符合PreAcquireNamespaceLock注解 且 函数参数类型匹配的函数执行完毕后 触发如下Advice
  //create item
  @After("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, item, ..)")
  public void requireLockAdvice(String appId, String clusterName, String namespaceName,
                                ItemDTO item) {
    tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
  }

  //update item
  @After("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, itemId, item, ..)")
  public void requireLockAdvice(String appId, String clusterName, String namespaceName, long itemId,
                                ItemDTO item) {
    tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
  }

  //update by change set
  @After("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, changeSet, ..)")
  public void requireLockAdvice(String appId, String clusterName, String namespaceName,
                                ItemChangeSets changeSet) {
    tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
  }

  //delete item
  @After("@annotation(PreAcquireNamespaceLock) && args(itemId, operator, ..)")
  public void requireLockAdvice(long itemId, String operator) {
    Item item = itemService.findOne(itemId);
    if (item == null) {
      throw new BadRequestException("item not exist.");
    }
    tryUnlock(namespaceService.findOne(item.getNamespaceId()));
  }

  private void tryUnlock(Namespace namespace) {
    if (bizConfig.isNamespaceLockSwitchOff()) {
      return;
    }

    if (!isModified(namespace)) {//如果没有改动则
      namespaceLockService.unlock(namespace.getId());
    }

  }

  boolean isModified(Namespace namespace) {
    //1. 获得当前Namespace的最近的有效Release对象
    Release release = releaseService.findLatestActiveRelease(namespace);
    //2. 获得当前Namespace的Items
    List<Item> items = itemService.findItemsWithoutOrdered(namespace.getId());
    //3. 如果无 Release 对象，判断是否有普通的 Item 配置项。若有，则代表修改过。
    // 就是说发布出去的[release]和还未发布的[item] 是否都为空. 否?则说明改过.
    if (release == null) {
      return hasNormalItems(items);
    }
    //4. 比较
    Map<String, String> releasedConfiguration = gson.fromJson(release.getConfigurations(), GsonType.CONFIG);
    Map<String, String> configurationFromItems = generateConfigurationFromItems(namespace, items);

    MapDifference<String, String> difference = Maps.difference(releasedConfiguration, configurationFromItems);

    return !difference.areEqual();

  }

  private boolean hasNormalItems(List<Item> items) {
    for (Item item : items) {
      if (!StringUtils.isEmpty(item.getKey())) {
        return true;
      }
    }

    return false;
  }

  private Map<String, String> generateConfigurationFromItems(Namespace namespace, List<Item> namespaceItems) {

    Map<String, String> configurationFromItems = Maps.newHashMap();
    //获得父 Namespace
    Namespace parentNamespace = namespaceService.findParentNamespace(namespace);
    //parent namespace
    if (parentNamespace == null) {
      //如无父 Namespace,使用自己配置
      generateMapFromItems(namespaceItems, configurationFromItems);
    } else {//child namespace
      //若有父 Namespace ，说明是灰度发布，合并父 Namespace 的配置 + 自己的配置项
      Release parentRelease = releaseService.findLatestActiveRelease(parentNamespace);
      if (parentRelease != null) {
        configurationFromItems = gson.fromJson(parentRelease.getConfigurations(), GsonType.CONFIG);
      }
      generateMapFromItems(namespaceItems, configurationFromItems);
    }

    return configurationFromItems;
  }

  private Map<String, String> generateMapFromItems(List<Item> items, Map<String, String> configurationFromItems) {
    for (Item item : items) {
      String key = item.getKey();
      if (StringUtils.isBlank(key)) {
        continue;
      }
      //把父namespace的配置里面的item 替换成子Namespace的item
      configurationFromItems.put(key, item.getValue());
    }

    return configurationFromItems;
  }

}
