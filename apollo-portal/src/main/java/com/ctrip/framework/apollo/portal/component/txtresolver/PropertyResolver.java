package com.ctrip.framework.apollo.portal.component.txtresolver;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;

import com.google.common.base.Strings;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * normal property file resolver.
 * update comment and blank item implement by create new item and delete old item.
 * update normal key/value item implement by update.
 */
@Component("propertyResolver")
public class PropertyResolver implements ConfigTextResolver {

  private static final String KV_SEPARATOR = "=";
  private static final String ITEM_SEPARATOR = "\n";

  @Override
  public ItemChangeSets resolve(long namespaceId, String configText, List<ItemDTO> baseItems) {
    //创建已经保存过的Item Map,以lineNum为键
    Map<Integer, ItemDTO> oldLineNumMapItem = BeanUtils.mapByKey("lineNum", baseItems);
    //创建已经保存过的Item Map,以key为键
    Map<String, ItemDTO> oldKeyMapItem = BeanUtils.mapByKey("key", baseItems);

    //remove comment and blank item map.
    oldKeyMapItem.remove("");
    //1. 分行
    String[] newItems = configText.split(ITEM_SEPARATOR);
    //2. 防御代码 判断是否有重复key
    if (isHasRepeatKey(newItems)) {
      throw new BadRequestException("config text has repeat key please check.");
    }
    //3. 处理新旧Item对象, 并比较其不同放入ItemChangeSets
    ItemChangeSets changeSets = new ItemChangeSets();
    Map<Integer, String> newLineNumMapItem = new HashMap<>();//use for delete blank and comment item
    int lineCounter = 1;
    for (String newItem : newItems) {
      newItem = newItem.trim();
      newLineNumMapItem.put(lineCounter, newItem);
      ItemDTO oldItemByLine = oldLineNumMapItem.get(lineCounter);

      //是否是注释行 行首是[#]或者[!]
      if (isCommentItem(newItem)) {

        handleCommentLine(namespaceId, oldItemByLine, newItem, lineCounter, changeSets);

      //是否是空白行
      } else if (isBlankItem(newItem)) {

        handleBlankLine(namespaceId, oldItemByLine, lineCounter, changeSets);

        //normal item
      } else {
        handleNormalLine(namespaceId, oldKeyMapItem, newItem, lineCounter, changeSets);
      }

      lineCounter++;
    }

    //4. 经过上面几步,keyMapOldItem保留的是,需要删除的普通item, 即 老的比新的多出那几行
    // 还有就是注释行和空行
    // 添加被删除item记录
    deleteCommentAndBlankItem(oldLineNumMapItem, newLineNumMapItem, changeSets);
    deleteNormalKVItem(oldKeyMapItem, changeSets);

    return changeSets;
  }

  private boolean isHasRepeatKey(String[] newItems) {
    Set<String> keys = new HashSet<>();
    int lineCounter = 1;
    int keyCount = 0;
    for (String item : newItems) {
      if (!isCommentItem(item) && !isBlankItem(item)) {
        keyCount++;
        String[] kv = parseKeyValueFromItem(item);
        if (kv != null) {
          keys.add(kv[0].toLowerCase());
        } else {
          throw new BadRequestException("line:" + lineCounter + " key value must separate by '='");
        }
      }
      lineCounter++;
    }

    return keyCount > keys.size();
  }

  private String[] parseKeyValueFromItem(String item) {
    int kvSeparator = item.indexOf(KV_SEPARATOR);
    if (kvSeparator == -1) {
      return null;
    }

    String[] kv = new String[2];
    kv[0] = item.substring(0, kvSeparator).trim();
    kv[1] = item.substring(kvSeparator + 1, item.length()).trim();
    return kv;
  }

  private void handleCommentLine(Long namespaceId, ItemDTO oldItemByLine, String newItem, int lineCounter, ItemChangeSets changeSets) {
    String oldComment = oldItemByLine == null ? "" : oldItemByLine.getComment();
    //create comment. implement update comment by delete old comment and create new comment
    //老的此行不是注释, 或者新老注释不相同
    //创建注释ItemDTO到变化集的新增项,
    //另外, 更新注释配置, 通过删除+添加的方式.
    if (!(isCommentItem(oldItemByLine) && newItem.equals(oldComment))) {
      changeSets.addCreateItem(buildCommentItem(0l, namespaceId, newItem, lineCounter));
    }
  }

  private void handleBlankLine(Long namespaceId, ItemDTO oldItem, int lineCounter, ItemChangeSets changeSets) {
    if (!isBlankItem(oldItem)) {
      changeSets.addCreateItem(buildBlankItem(0l, namespaceId, lineCounter));
    }
  }

  private void handleNormalLine(Long namespaceId, Map<String, ItemDTO> keyMapOldItem, String newItem,
                                int lineCounter, ItemChangeSets changeSets) {
    //1. 根据等号拆分成2个字符串数组[key, value]                              
    String[] kv = parseKeyValueFromItem(newItem);

    if (kv == null) {
      throw new BadRequestException("line:" + lineCounter + " key value must separate by '='");
    }

    String newKey = kv[0];
    String newValue = kv[1].replace("\\n", "\n"); //handle user input \n
    //2. 根据key名字 旧的item 
    ItemDTO oldItem = keyMapOldItem.get(newKey);

    if (oldItem == null) {//new item 旧的item没有此key即为新item
      changeSets.addCreateItem(buildNormalItem(0l, namespaceId, newKey, newValue, "", lineCounter));
    } else if (!newValue.equals(oldItem.getValue()) || lineCounter != oldItem.getLineNum()) {//update item
      //新旧值不相等 或 行号(位置)有变化, 即为updated item
      changeSets.addUpdateItem(
          buildNormalItem(oldItem.getId(), namespaceId, newKey, newValue, oldItem.getComment(),
              lineCounter));
    }
    keyMapOldItem.remove(newKey);//移除老的ItemDTO 对象
  }

  private boolean isCommentItem(ItemDTO item) {
    return item != null && "".equals(item.getKey())
        && (item.getComment().startsWith("#") || item.getComment().startsWith("!"));
  }

  private boolean isCommentItem(String line) {
    return line != null && (line.startsWith("#") || line.startsWith("!"));
  }

  private boolean isBlankItem(ItemDTO item) {
    return item != null && "".equals(item.getKey()) && "".equals(item.getComment());
  }

  private boolean isBlankItem(String line) {
    return  Strings.nullToEmpty(line).trim().isEmpty();
  }

  private void deleteNormalKVItem(Map<String, ItemDTO> baseKeyMapItem, ItemChangeSets changeSets) {
    //surplus item is to be deleted
    for (Map.Entry<String, ItemDTO> entry : baseKeyMapItem.entrySet()) {
      changeSets.addDeleteItem(entry.getValue());
    }
  }

  private void deleteCommentAndBlankItem(Map<Integer, ItemDTO> oldLineNumMapItem,
                                         Map<Integer, String> newLineNumMapItem,
                                         ItemChangeSets changeSets) {

    for (Map.Entry<Integer, ItemDTO> entry : oldLineNumMapItem.entrySet()) {
      int lineNum = entry.getKey();
      ItemDTO oldItem = entry.getValue();
      String newItem = newLineNumMapItem.get(lineNum);

      //1. old is blank by now is not
      //2.old is comment by now is not exist or modified
      if ((isBlankItem(oldItem) && !isBlankItem(newItem))
          || isCommentItem(oldItem) && (newItem == null || !newItem.equals(oldItem.getComment()))) {
        changeSets.addDeleteItem(oldItem);
      }
    }
  }

  private ItemDTO buildCommentItem(Long id, Long namespaceId, String comment, int lineNum) {
    return buildNormalItem(id, namespaceId, "", "", comment, lineNum);
  }

  private ItemDTO buildBlankItem(Long id, Long namespaceId, int lineNum) {
    return buildNormalItem(id, namespaceId, "", "", "", lineNum);
  }

  private ItemDTO buildNormalItem(Long id, Long namespaceId, String key, String value, String comment, int lineNum) {
    ItemDTO item = new ItemDTO(key, value, comment, lineNum);
    item.setId(id);
    item.setNamespaceId(namespaceId);
    return item;
  }
}
