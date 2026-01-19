package com.ae2addon.highlighter.server;

import appeng.api.stacks.AEKey;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通过反射访问AE2内部数据来定位样板接口
 * 使用反射而非Mixin以获得更好的版本兼容性
 */
public class PatternProviderLocator {
    
    /**
     * 根据物品和CPU查找样板接口位置
     * 
     * @param player 请求的玩家
     * @param targetWhat 目标物品 (AEKey)
     * @param cpuSerial CPU序列号
     * @return 样板接口的位置列表
     */
    public static List<BlockPos> findProviderPositions(ServerPlayer player, AEKey targetWhat, int cpuSerial) {
        List<BlockPos> positions = new ArrayList<>();
        
        try {
            // 获取玩家当前打开的容器
            var container = player.containerMenu;
            if (container == null) {
                Ae2PatternHighlighter.LOGGER.warn("Player has no open container");
                return positions;
            }
            
            // 检查是否是AE2的合成状态菜单
            String containerClassName = container.getClass().getName();
            Ae2PatternHighlighter.LOGGER.info("Container class: {}", containerClassName);
            
            if (!containerClassName.contains("CraftingStatusMenu") && 
                !containerClassName.contains("CraftingCPUMenu")) {
                Ae2PatternHighlighter.LOGGER.warn("Container is not a crafting menu: {}", containerClassName);
                return positions;
            }
            
            // 尝试直接访问AE2的CraftingCPUMenu或CraftingStatusMenu
            // CraftingStatusMenu 继承自 CraftingCPUMenu
            // 字段在CraftingCPUMenu中: private CraftingCPUCluster cpu
            Object cpu = getFieldValueByName(container, "cpu");
            
            if (cpu == null) {
                // 打印所有字段用于调试
                Ae2PatternHighlighter.LOGGER.warn("Could not find 'cpu' field. Available fields:");
                Class<?> clazz = container.getClass();
                while (clazz != null && clazz != Object.class) {
                    for (var field : clazz.getDeclaredFields()) {
                        Ae2PatternHighlighter.LOGGER.warn("  {}.{}: {}", clazz.getSimpleName(), field.getName(), field.getType().getSimpleName());
                    }
                    clazz = clazz.getSuperclass();
                }
                return positions;
            }
            
            Ae2PatternHighlighter.LOGGER.info("Found CPU: {}", cpu.getClass().getName());
            
            // CraftingCPUCluster中有 public final CraftingCpuLogic craftingLogic
            Object cpuLogic = getFieldValueByName(cpu, "craftingLogic");
            if (cpuLogic == null) {
                Ae2PatternHighlighter.LOGGER.warn("Could not find craftingLogic in CPU");
                return positions;
            }
            
            Ae2PatternHighlighter.LOGGER.info("Found CPU logic: {}", cpuLogic.getClass().getName());
            
            // CraftingCpuLogic中有 private ExecutingCraftingJob job
            Object job = getFieldValueByName(cpuLogic, "job");
            if (job == null) {
                Ae2PatternHighlighter.LOGGER.info("No active job in CPU (this is normal if nothing is crafting)");
                return positions;
            }
            
            Ae2PatternHighlighter.LOGGER.info("Looking for patterns that produce: {}", 
                targetWhat.getDisplayName().getString());
            
            // 获取tasks映射
            Object tasks = getFieldValueByName(job, "tasks");
            if (tasks == null) {
                Ae2PatternHighlighter.LOGGER.warn("Could not find tasks in job");
                return positions;
            }
            
            // 遍历tasks获取pattern details，只处理输出匹配物品的pattern
            if (tasks instanceof Map<?, ?> taskMap) {
                Ae2PatternHighlighter.LOGGER.info("Found {} tasks, filtering by item", taskMap.size());
                for (Object patternDetails : taskMap.keySet()) {
                    // 检查这个pattern是否输出我们要找的物品
                    if (!patternProducesItem(patternDetails, targetWhat)) {
                        continue; // 跳过不匹配的pattern
                    }
                    
                    // 通过CraftingService获取提供此样板的providers
                    List<BlockPos> providerPositions = findProvidersForPattern(cpu, patternDetails);
                    positions.addAll(providerPositions);
                }
            }
            
        } catch (Exception e) {
            Ae2PatternHighlighter.LOGGER.error("Error finding pattern providers", e);
        }
        
        return positions;
    }
    
    /**
     * 根据serial找到对应的AEKey (物品)
     */
    private static Object findAEKeyBySerial(Object container, long itemSerial) {
        try {
            // 从CraftingStatusMenu获取status
            Object status = getFieldValueByName(container, "status");
            if (status == null) {
                Ae2PatternHighlighter.LOGGER.debug("No status in container");
                return null;
            }
            
            // status.getEntries() 返回 List<CraftingStatusEntry>
            Object entries = callMethod(status, "getEntries");
            if (entries instanceof java.util.List<?> entryList) {
                for (Object entry : entryList) {
                    // 获取serial
                    Object serial = callMethod(entry, "getSerial");
                    if (serial instanceof Long serialLong && serialLong == itemSerial) {
                        // 找到了！获取what
                        return callMethod(entry, "getWhat");
                    }
                }
            }
        } catch (Exception e) {
            Ae2PatternHighlighter.LOGGER.debug("Error finding AEKey by serial: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 检查pattern是否输出指定的物品
     */
    private static boolean patternProducesItem(Object patternDetails, Object targetWhat) {
        try {
            // IPatternDetails.getPrimaryOutput() 返回 GenericStack
            Object primaryOutput = callMethod(patternDetails, "getPrimaryOutput");
            if (primaryOutput != null) {
                // GenericStack.what() 返回 AEKey
                Object outputWhat = callMethod(primaryOutput, "what");
                if (outputWhat != null && outputWhat.equals(targetWhat)) {
                    Ae2PatternHighlighter.LOGGER.debug("Pattern matches! Output: {}", outputWhat);
                    return true;
                }
            }
            
            // 也检查所有输出 getOutputs()
            Object outputs = callMethod(patternDetails, "getOutputs");
            if (outputs instanceof Object[] outputArray) {
                for (Object output : outputArray) {
                    Object outputWhat = callMethod(output, "what");
                    if (outputWhat != null && outputWhat.equals(targetWhat)) {
                        Ae2PatternHighlighter.LOGGER.debug("Pattern matches via getOutputs! Output: {}", outputWhat);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Ae2PatternHighlighter.LOGGER.debug("Error checking pattern output: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 查找提供特定样板的所有provider位置
     */
    private static List<BlockPos> findProvidersForPattern(Object cpu, Object patternDetails) {
        List<BlockPos> positions = new ArrayList<>();
        
        try {
            Ae2PatternHighlighter.LOGGER.info("Finding providers for pattern: {}", patternDetails.getClass().getSimpleName());
            
            // CraftingCPUCluster直接有getGrid()方法
            Object grid = callMethod(cpu, "getGrid");
            Ae2PatternHighlighter.LOGGER.info("  getGrid() result: {}", grid != null ? grid.getClass().getSimpleName() : "null");
            
            if (grid == null) {
                // 备用：尝试通过getNode获取
                Object gridNode = callMethod(cpu, "getNode");
                Ae2PatternHighlighter.LOGGER.info("  getNode() result: {}", gridNode != null ? gridNode.getClass().getSimpleName() : "null");
                if (gridNode != null) {
                    grid = callMethod(gridNode, "getGrid");
                    Ae2PatternHighlighter.LOGGER.info("  Grid from node: {}", grid != null ? grid.getClass().getSimpleName() : "null");
                }
            }
            
            if (grid == null) {
                Ae2PatternHighlighter.LOGGER.warn("  Could not find grid");
                return positions;
            }
            
            // 获取CraftingService - 使用IGrid.getCraftingService()
            Object craftingService = callMethod(grid, "getCraftingService");
            if (craftingService == null) {
                craftingService = callMethodWithClass(grid, "getService", 
                        "appeng.api.networking.crafting.ICraftingService");
            }
            
            Ae2PatternHighlighter.LOGGER.info("  CraftingService: {}", craftingService != null ? craftingService.getClass().getSimpleName() : "null");
            
            if (craftingService == null) {
                return positions;
            }
            
            // 获取providers
            Object providers = callMethod(craftingService, "getProviders", patternDetails);
            Ae2PatternHighlighter.LOGGER.info("  Providers result: {}", providers != null ? providers.getClass().getSimpleName() : "null");
            
            if (providers == null) {
                return positions;
            }
            
            // 遍历providers获取位置
            if (providers instanceof Iterable<?> providerIterable) {
                int providerCount = 0;
                for (Object provider : providerIterable) {
                    providerCount++;
                    Ae2PatternHighlighter.LOGGER.info("    Provider {}: {}", providerCount, provider.getClass().getSimpleName());
                    BlockPos pos = getProviderPosition(provider);
                    if (pos != null && !positions.contains(pos)) {
                        positions.add(pos);
                        Ae2PatternHighlighter.LOGGER.info("    Found position: {}", pos);
                    }
                }
                Ae2PatternHighlighter.LOGGER.info("  Total providers: {}, positions found: {}", providerCount, positions.size());
            }
            
        } catch (Exception e) {
            Ae2PatternHighlighter.LOGGER.error("Error finding providers for pattern", e);
        }
        
        return positions;
    }
    
    /**
     * 获取provider的BlockPos
     */
    private static BlockPos getProviderPosition(Object provider) {
        try {
            Ae2PatternHighlighter.LOGGER.info("      Getting position from provider: {}", provider.getClass().getName());
            
            // PatternProviderLogic有一个host字段 (PatternProviderLogicHost)
            Object host = getFieldValueByName(provider, "host");
            Ae2PatternHighlighter.LOGGER.info("        host field: {}", host != null ? host.getClass().getSimpleName() : "null");
            
            if (host != null) {
                // 检查是否是Part - Part有getHost()方法返回IPartHost
                String hostClassName = host.getClass().getName();
                if (hostClassName.contains("Part")) {
                    // Part的位置来自于PartHost
                    Object partHost = callMethod(host, "getHost");
                    Ae2PatternHighlighter.LOGGER.info("        Part.getHost(): {}", partHost != null ? partHost.getClass().getSimpleName() : "null");
                    
                    if (partHost != null) {
                        // IPartHost.getLocation() 返回 DimensionalBlockPos
                        Object location = callMethod(partHost, "getLocation");
                        Ae2PatternHighlighter.LOGGER.info("        IPartHost.getLocation(): {}", location);
                        
                        if (location != null) {
                            // DimensionalBlockPos.getPos()
                            Object pos = callMethod(location, "getPos");
                            Ae2PatternHighlighter.LOGGER.info("        DimensionalBlockPos.getPos(): {}", pos);
                            if (pos instanceof BlockPos blockPos) {
                                return blockPos;
                            }
                        }
                        
                        // 备用: 直接获取BlockEntity
                        Object be = callMethod(partHost, "getBlockEntity");
                        if (be != null) {
                            Object pos = getFieldValueByName(be, "worldPosition");
                            Ae2PatternHighlighter.LOGGER.info("        PartHost BE worldPosition: {}", pos);
                            if (pos instanceof BlockPos blockPos) {
                                return blockPos;
                            }
                        }
                    }
                }
                
                // PatternProviderLogicHost.getBlockEntity() - 但host可能本身就是BlockEntity
                Object blockEntity = host;
                
                // 尝试getBlockEntity()，如果没有就使用host本身
                Object beFromMethod = callMethod(host, "getBlockEntity");
                if (beFromMethod != null) {
                    blockEntity = beFromMethod;
                }
                Ae2PatternHighlighter.LOGGER.info("        BlockEntity: {}", blockEntity.getClass().getSimpleName());
                
                // 尝试 getBlockPos()
                Object pos = callMethod(blockEntity, "getBlockPos");
                Ae2PatternHighlighter.LOGGER.info("        getBlockPos(): {}", pos);
                
                if (pos == null) {
                    // 尝试直接访问worldPosition字段（Minecraft的BlockEntity使用这个）
                    pos = getFieldValueByName(blockEntity, "worldPosition");
                    Ae2PatternHighlighter.LOGGER.info("        worldPosition field: {}", pos);
                }
                
                if (pos instanceof BlockPos blockPos) {
                    return blockPos;
                }
            }
            
            // 备用：尝试通过mainNode
            Object mainNode = getFieldValueByName(provider, "mainNode");
            Ae2PatternHighlighter.LOGGER.info("        mainNode field: {}", mainNode != null ? mainNode.getClass().getSimpleName() : "null");
            
            if (mainNode != null) {
                Object node = callMethod(mainNode, "getNode");
                Ae2PatternHighlighter.LOGGER.info("        getNode(): {}", node != null ? node.getClass().getSimpleName() : "null");
                
                if (node != null) {
                    // InWorldGridNode有getLocation()方法返回BlockPos
                    Object pos = callMethod(node, "getLocation");
                    Ae2PatternHighlighter.LOGGER.info("        getLocation(): {}", pos);
                    
                    if (pos instanceof BlockPos blockPos) {
                        return blockPos;
                    }
                    
                    // 备用：尝试通过location字段
                    pos = getFieldValueByName(node, "location");
                    Ae2PatternHighlighter.LOGGER.info("        location field: {}", pos);
                    
                    if (pos instanceof BlockPos blockPos) {
                        return blockPos;
                    }
                }
            }
            
        } catch (Exception e) {
            Ae2PatternHighlighter.LOGGER.error("Error getting position from provider", e);
        }
        
        Ae2PatternHighlighter.LOGGER.warn("      Could not get position from provider");
        return null;
    }
    
    /**
     * 通过多个可能的字段名尝试获取字段值
     */
    private static Object getFieldValueByName(Object obj, String... fieldNames) {
        if (obj == null) return null;
        
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (String fieldName : fieldNames) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (NoSuchFieldException ignored) {
                    // 尝试下一个字段名
                } catch (Exception e) {
                    Ae2PatternHighlighter.LOGGER.debug("Error accessing field {}: {}", fieldName, e.getMessage());
                }
            }
            clazz = clazz.getSuperclass();
        }
        
        return null;
    }
    
    /**
     * 调用无参数方法
     */
    private static Object callMethod(Object obj, String... methodNames) {
        if (obj == null) return null;
        
        Class<?> clazz = obj.getClass();
        for (String methodName : methodNames) {
            try {
                Method method = findMethod(clazz, methodName);
                if (method != null) {
                    method.setAccessible(true);
                    return method.invoke(obj);
                }
            } catch (Exception e) {
                Ae2PatternHighlighter.LOGGER.debug("Error calling method {}: {}", methodName, e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 调用带参数的方法
     */
    private static Object callMethod(Object obj, String methodName, Object... args) {
        if (obj == null) return null;
        
        Class<?> clazz = obj.getClass();
        try {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                    method.setAccessible(true);
                    return method.invoke(obj, args);
                }
            }
        } catch (Exception e) {
            Ae2PatternHighlighter.LOGGER.debug("Error calling method {} with args: {}", methodName, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 调用需要类参数的方法 (如getService(Class))
     */
    private static Object callMethodWithClass(Object obj, String methodName, String... classNames) {
        if (obj == null) return null;
        
        for (String className : classNames) {
            try {
                Class<?> paramClass = Class.forName(className);
                Method method = obj.getClass().getMethod(methodName, Class.class);
                method.setAccessible(true);
                return method.invoke(obj, paramClass);
            } catch (Exception ignored) {
                // 尝试下一个类名
            }
        }
        
        // 尝试无参数版本
        return callMethod(obj, methodName);
    }
    
    /**
     * 在类层级中查找方法
     */
    private static Method findMethod(Class<?> clazz, String methodName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException ignored) {
                // 检查接口
                for (Class<?> iface : clazz.getInterfaces()) {
                    try {
                        return iface.getDeclaredMethod(methodName);
                    } catch (NoSuchMethodException ignored2) {}
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
