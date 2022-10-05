package play.spring

/**
 * 排序规则:
 * ```
 *  1. 按bean依赖顺序
 *  2. 按order
 *  3. 按beanName
 * ```
 *
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see javax.annotation.Priority
 * @see play.Order
 * @author LiangZengle
 */
interface OrderedSmartInitializingSingleton {

  fun afterSingletonsInstantiated()
}
