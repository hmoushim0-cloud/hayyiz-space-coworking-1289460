package sa.factory.hayyizspacecoworki.data.repository

import sa.factory.hayyizspacecoworki.domain.repository.HayyizSpaceRepository
import sa.factory.hayyizspacecoworki.hardware.HardwareSensorDriver
import sa.factory.hayyizspacecoworki.model.HayyizSpaceItem
import sa.factory.hayyizspacecoworki.model.HayyizSpaceOrder
import sa.factory.hayyizspacecoworki.model.HayyizSpaceTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HayyizSpaceRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : HayyizSpaceRepository {

    private val _catalog = MutableStateFlow<List<HayyizSpaceItem>>(listOf(
        HayyizSpaceItem("1", "إدارة حسابات السوشيال ميديا", "إدارة محتوى يومية لـ10 عملاء مع تحليل الأداء عبر لوحة تحكم موحدة", 150.0, "معتمد", "إدارة السوشيال ميديا"),
        HayyizSpaceItem("2", "تفعيل الوصول عبر RFID", "دمج بطاقات الوصول 13.56MHz مع نظام TOTP للمصادقة الثنائية", 280.0, "فوري", "الوصول الآمن"),
        HayyizSpaceItem("3", "تدريب على TOTP", "تدريب فريق العمل على استخدام رموز المرور لمرة واحدة عبر تطبيق الهاتف", 95.0, "جديد", "تنظيم المهام")
    ))
    override val catalogFlow: Flow<List<HayyizSpaceItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<HayyizSpaceItem>>(emptyList())
    override val activeCart: StateFlow<List<HayyizSpaceItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<HayyizSpaceTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            HayyizSpaceTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: HayyizSpaceItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: HayyizSpaceItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): HayyizSpaceOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = HayyizSpaceOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
