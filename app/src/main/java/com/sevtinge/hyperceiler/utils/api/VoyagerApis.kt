/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.api

import android.view.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.api.LazyClass.SystemProperties
import com.sevtinge.hyperceiler.utils.api.LazyClass.clazzMiuiBuild
import java.lang.reflect.*

@JvmInline
value class Args(val args: Array<out Any?>)

@JvmInline
value class ArgTypes(val argTypes: Array<out Class<*>>)

@Suppress("NOTHING_TO_INLINE")
inline fun args(vararg args: Any?) = Args(args)

@Suppress("NOTHING_TO_INLINE")
inline fun argTypes(vararg argTypes: Class<*>) = ArgTypes(argTypes)

typealias MethodCondition = Method.() -> Boolean

/**
 * 扩展函数 通过类或者对象获取单个属性
 * @param fieldName 属性名
 * @param isStatic 是否静态类型
 * @param fieldType 属性类型
 * @return 符合条件的属性
 * @throws IllegalArgumentException 属性名为空
 * @throws NoSuchFieldException 未找到属性
 */
fun Any.field(
    fieldName: String,
    isStatic: Boolean = false,
    fieldType: Class<*>? = null
): Field {
    if (fieldName.isBlank()) throw IllegalArgumentException("Field name must not be empty!")
    var c: Class<*> = if (this is Class<*>) this else this.javaClass
    do {
        c.declaredFields
            .filter { isStatic == it.isStatic }
            .firstOrNull { (fieldType == null || it.type == fieldType) && (it.name == fieldName) }
            ?.let { it.isAccessible = true;return it }
    } while (c.superclass?.also { c = it } != null)
    throw NoSuchFieldException("Name: $fieldName,Static: $isStatic, Type: ${if (fieldType == null) "ignore" else fieldType.name}")
}

/**
 * 判断运行模块的机型是否是平板，仅支持小米设备
 * @return 一个 Boolean 值，true 代表是平板，false 代表不是平板
 * @author Voyager
 */
val IS_TABLET by lazy {
    getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_TABLET") ?: false
}

/**
 * 函数调用，适用于其他一些需要判断的情况，仅支持小米设备的判断
 * 2024-04-20 更新对非小米设备的判断方式，仅防止闪退
 * @return 一个 Boolean 值，true 代表是平板，false 代表不是平板
 */
fun isPad(): Boolean {
    return try {
        clazzMiuiBuild.getField("IS_TABLET").getBoolean(null)
    } catch(_: Throwable) {
        false
    }
}

/**
 * 是否为国际版系统
 */
val IS_INTERNATIONAL_BUILD by lazy {
    getStaticObjectOrNullAs<Boolean>(clazzMiuiBuild, "IS_INTERNATIONAL_BUILD") ?: false
}

val IS_HYPER_OS by lazy {
    invokeStaticMethodBestMatch(
        SystemProperties, "getInt", null, "ro.mi.os.version.code", -1
    ) != -1
}

/**
 * 扩展函数 通过遍历方法数组 返回符合条件的方法数组
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun Array<Method>.findAllMethods(condition: MethodCondition): Array<Method> {
    return this.filter { it.condition() }.onEach { it.isAccessible = true }.toTypedArray()
}

/**
 * 通过条件获取方法数组
 * @param clz 类
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun findAllMethods(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    var c = clz
    val arr = ArrayList<Method>()
    arr.addAll(c.declaredMethods.findAllMethods(condition))
    if (findSuper) {
        while (c.superclass?.also { c = it } != null) {
            arr.addAll(c.declaredMethods.findAllMethods(condition))
        }
    }
    return arr
}

/**
 * 通过条件获取方法数组
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun findAllMethods(
    clzName: String,
    classLoader: ClassLoader = EzXHelper.classLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    return findAllMethods(loadClass(clzName, classLoader), findSuper, condition)
}

/**
 * 模糊查找组件调用
 */
object BlurDraw {
    fun getValueByFields(target: Any, fieldNames: List<String>, clazz: Class<*>? = null): Any? {
        var targetClass = clazz ?: target.javaClass
        while (targetClass != Any::class.java) {
            for (fieldName in fieldNames) {
                try {
                    val field = targetClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    val value = field.get(target)
                    if (value is Window) {
                        // Log.i("BlurPersonalAssistant Window field name: $fieldName")
                        return value
                    }
                } catch (e: NoSuchFieldException) {
                    // This field doesn't exist in this class, skip it
                } catch (e: IllegalAccessException) {
                    // This field isn't accessible, skip it
                }
            }
            targetClass = targetClass.superclass ?: break
        }
        return null
    }
}
