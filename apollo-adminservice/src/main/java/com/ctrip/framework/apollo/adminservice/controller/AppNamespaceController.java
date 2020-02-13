package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.service.AppNamespaceService;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AppNamespaceController {

  private final AppNamespaceService appNamespaceService;
  private final NamespaceService namespaceService;

  public AppNamespaceController(
      final AppNamespaceService appNamespaceService,
      final NamespaceService namespaceService) {
    this.appNamespaceService = appNamespaceService;
    this.namespaceService = namespaceService;
  }

  @PostMapping("/apps/{appId}/appnamespaces")
  public AppNamespaceDTO create(@RequestBody AppNamespaceDTO appNamespace,
                                @RequestParam(defaultValue = "false") boolean silentCreation) {

    //把API层的Object格式[DTO] 再变回common entity
    AppNamespace entity = BeanUtils.transform(AppNamespace.class, appNamespace);
    //可能是已经保存进入数据库了?
    AppNamespace managedEntity = appNamespaceService.findOne(entity.getAppId(), entity.getName());

    if (managedEntity == null) {
      //全新的appNamespace
      if (StringUtils.isEmpty(entity.getFormat())){
        entity.setFormat(ConfigFileFormat.Properties.getValue());//赋值默认的Format为[PROPERTIES]
      }
      //调用service层 保存entity
      entity = appNamespaceService.createAppNamespace(entity);
    } else if (silentCreation) {//??
      appNamespaceService.createNamespaceForAppNamespaceInAllCluster(appNamespace.getAppId(), appNamespace.getName(),
          appNamespace.getDataChangeCreatedBy());

      entity = managedEntity;
    } else {
      throw new BadRequestException("app namespaces already exist.");
    }

    return BeanUtils.transform(AppNamespaceDTO.class, entity);
  }

  @DeleteMapping("/apps/{appId}/appnamespaces/{namespaceName:.+}")
  public void delete(@PathVariable("appId") String appId, @PathVariable("namespaceName") String namespaceName,
      @RequestParam String operator) {
    AppNamespace entity = appNamespaceService.findOne(appId, namespaceName);
    if (entity == null) {
      throw new BadRequestException("app namespace not found for appId: " + appId + " namespace: " + namespaceName);
    }
    appNamespaceService.deleteAppNamespace(entity, operator);
  }

  @GetMapping("/appnamespaces/{publicNamespaceName}/namespaces")
  public List<NamespaceDTO> findPublicAppNamespaceAllNamespaces(@PathVariable String publicNamespaceName, Pageable pageable) {

    List<Namespace> namespaces = namespaceService.findPublicAppNamespaceAllNamespaces(publicNamespaceName, pageable);

    return BeanUtils.batchTransform(NamespaceDTO.class, namespaces);
  }

  @GetMapping("/appnamespaces/{publicNamespaceName}/associated-namespaces/count")
  public int countPublicAppNamespaceAssociatedNamespaces(@PathVariable String publicNamespaceName) {
    return namespaceService.countPublicAppNamespaceAssociatedNamespaces(publicNamespaceName);
  }

  @GetMapping("/apps/{appId}/appnamespaces")
  public List<AppNamespaceDTO> getAppNamespaces(@PathVariable("appId") String appId) {

    List<AppNamespace> appNamespaces = appNamespaceService.findByAppId(appId);

    return BeanUtils.batchTransform(AppNamespaceDTO.class, appNamespaces);
  }
}
