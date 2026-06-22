package com.changan.common.utils;

import cn.hutool.core.collection.CollUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TreeUtils {

    /**
     * 构建树形结构
     *
     * @param allNodes       所有节点列表
     * @param idGetter       获取节点ID的函数
     * @param parentIdGetter 获取父节点ID的函数
     * @param childrenSetter 设置子节点列表的函数
     * @param <T>            节点类型
     * @return 根节点列表
     */
    public static <T> List<T> buildTree(
            List<T> allNodes,
            Function<T, Long> idGetter,
            Function<T, Long> parentIdGetter,
            BiConsumer<T, List<T>> childrenSetter) {

        if (CollUtil.isEmpty(allNodes)) {
            return Collections.emptyList();
        }

        // 1.按 parentId 分组
        Map<Long, List<T>> parentIdMap = allNodes.stream()
                .filter(node -> parentIdGetter.apply(node) != null)
                .collect(Collectors.groupingBy(parentIdGetter));

        // 2. 找出根节点
        List<T> roots = parentIdMap.getOrDefault(0L, Collections.emptyList());

        // 3. 递归填充子节点
        for (T root : roots) {
            fillChildren(root, parentIdMap, idGetter, childrenSetter);
        }
        return roots;
    }

    private static <T> void fillChildren(
            T parent,
            Map<Long, List<T>> parentIdMap,
            Function<T, Long> idGetter,
            BiConsumer<T, List<T>> childrenSetter) {

        // 获取id
        Long nodeId = idGetter.apply(parent);
        // 找出所有子节点
        List<T> children = parentIdMap.getOrDefault(nodeId, Collections.emptyList());
        // 设置子节点
        childrenSetter.accept(parent, children);
        for (T child : children) {
            fillChildren(child, parentIdMap, idGetter, childrenSetter);
        }
    }
}
