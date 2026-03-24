package moe.ore.xposed.utils;

import android.annotation.TargetApi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ChooseUtils {

    private static final Method startMethodTracingMethod;
    private static final Method stopMethodTracingMethod;
    private static final Method getMethodTracingModeMethod;
    private static final Method getRuntimeStatMethod;
    private static final Method getRuntimeStatsMethod;
    private static final Method countInstancesOfClassMethod;
    private static final Method countInstancesOfClassesMethod;

    private static Method getInstancesOfClassesMethod;

    static {
        try {
            Class<?> c = Class.forName("dalvik.system.VMDebug");
            startMethodTracingMethod = c.getDeclaredMethod("startMethodTracing", String.class,
                    Integer.TYPE, Integer.TYPE, Boolean.TYPE, Integer.TYPE);
            stopMethodTracingMethod = c.getDeclaredMethod("stopMethodTracing");
            getMethodTracingModeMethod = c.getDeclaredMethod("getMethodTracingMode");
            getRuntimeStatMethod = c.getDeclaredMethod("getRuntimeStat", String.class);
            getRuntimeStatsMethod = c.getDeclaredMethod("getRuntimeStats");

            countInstancesOfClassMethod = c.getDeclaredMethod("countInstancesOfClass",
                    Class.class, Boolean.TYPE);


            countInstancesOfClassesMethod = c.getDeclaredMethod("countInstancesOfClasses",
                    Class[].class, Boolean.TYPE);

            //android 9.0以上才有这个方法
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                getInstancesOfClassesMethod = c.getDeclaredMethod("getInstancesOfClasses",
                        Class[].class, Boolean.TYPE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据Class获取当前进程全部的实例
     *
     * @param clazz 需要查找的Class
     * @return 当前进程的全部实例。
     */
    @TargetApi(28)
    public static <T> ArrayList<T> choose(Class<T> clazz) throws Exception {
        return (ArrayList<T>) choose(clazz, false);
    }

    /**
     * 根据Class获取当前进程全部的实例
     *
     * @param clazz      需要查找的Class
     * @param assignable 是否包含子类的实例
     * @return 当前进程的全部实例。
     */
    @TargetApi(28)
    public static synchronized ArrayList<Object> choose(Class clazz, boolean assignable) throws Exception {
        ArrayList<Object> resut = null;
        Object[][] instancesOfClasses = getInstancesOfClasses(new Class[]{clazz}, assignable);
        if (instancesOfClasses != null) {
            resut = new ArrayList<>();
            for (Object[] instancesOfClass : instancesOfClasses) {
                resut.addAll(Arrays.asList(instancesOfClass));
            }
        }
        return resut;
    }

    @TargetApi(28)
    private static Object[][] getInstancesOfClasses(Class<?>[] classes,
                                                    boolean assignable)
            throws Exception {
        return (Object[][]) getInstancesOfClassesMethod.invoke(
                null, new Object[]{classes, assignable});
    }

    public static void startMethodTracing(String filename, int bufferSize, int flags,
                                          boolean samplingEnabled, int intervalUs) throws Exception {
        startMethodTracingMethod.invoke(null, filename, bufferSize, flags, samplingEnabled,
                intervalUs);
    }

    public static void stopMethodTracing() throws Exception {
        stopMethodTracingMethod.invoke(null);
    }

    public static int getMethodTracingMode() throws Exception {
        return (int) getMethodTracingModeMethod.invoke(null);
    }

    /**
     * String gc_count = VMDebug.getRuntimeStat("art.gc.gc-count");
     * String gc_time = VMDebug.getRuntimeStat("art.gc.gc-time");
     * String bytes_allocated = VMDebug.getRuntimeStat("art.gc.bytes-allocated");
     * String bytes_freed = VMDebug.getRuntimeStat("art.gc.bytes-freed");
     * String blocking_gc_count = VMDebug.getRuntimeStat("art.gc.blocking-gc-count");
     * String blocking_gc_time = VMDebug.getRuntimeStat("art.gc.blocking-gc-time");
     * String gc_count_rate_histogram = VMDebug.getRuntimeStat("art.gc.gc-count-rate-histogram");
     * String blocking_gc_count_rate_histogram =VMDebug.getRuntimeStat("art.gc.gc-count-rate-histogram");
     */
    public static String getRuntimeStat(String statName) throws Exception {
        return (String) getRuntimeStatMethod.invoke(null, statName);
    }

    /**
     * 获取当前进程的状态信息
     */
    public static Map<String, String> getRuntimeStats() throws Exception {
        return (Map<String, String>) getRuntimeStatsMethod.invoke(null);
    }

    public static long countInstancesofClass(Class<?> c, boolean assignable) throws Exception {
        return (long) countInstancesOfClassMethod.invoke(null, new Object[]{c, assignable});
    }

    public static long[] countInstancesofClasses(Class<?>[] classes, boolean assignable)
            throws Exception {
        return (long[]) countInstancesOfClassesMethod.invoke(
                null, new Object[]{classes, assignable});
    }


}