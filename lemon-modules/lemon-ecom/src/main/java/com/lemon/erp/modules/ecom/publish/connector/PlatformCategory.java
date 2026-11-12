package com.lemon.erp.modules.ecom.publish.connector;

/**
 * 平台类目（亚马逊为 Product Type）
 *
 * @param parentId 上级类目 ID，顶级为 null
 * @param leaf     是否叶子类目（只有叶子类目可以刊登）
 * @param path     完整路径（如 Home &gt; Kitchen &gt; Cups），没有为 null
 */
public record PlatformCategory(String id, String name, String parentId, boolean leaf, String path) {
}
