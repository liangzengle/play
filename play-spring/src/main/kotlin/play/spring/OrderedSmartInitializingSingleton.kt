package play.spring

/**
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see javax.annotation.Priority
 * @see play.Order
 * @author LiangZengle
 */
interface OrderedSmartInitializingSingleton {

  fun afterSingletonsInstantiated()
}
