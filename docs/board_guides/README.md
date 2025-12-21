# Arduino Board Guides

Comprehensive setup guides for all supported Arduino boards with Ardunakon.

## Board Guides

| Board | Guide | Built-in Wireless |
|-------|-------|-------------------|
| Arduino UNO R4 WiFi | [UNO_R4_WIFI.md](UNO_R4_WIFI.md) | ✅ BLE + WiFi |
| Arduino UNO Q | [UNO_Q.md](UNO_Q.md) | ✅ Bluetooth 5.1 |
| Arduino GIGA R1 | [GIGA_R1.md](GIGA_R1.md) | ✅ BLE + WiFi |
| Classic UNO / Nano | [CLASSIC_UNO_NANO.md](CLASSIC_UNO_NANO.md) | ❌ External |
| Arduino Mega 2560 | [MEGA_2560.md](MEGA_2560.md) | ❌ External |
| Leonardo / Micro | [LEONARDO_MICRO.md](LEONARDO_MICRO.md) | ❌ External |
| Arduino Due / Zero | [DUE_ZERO.md](DUE_ZERO.md) | ❌ External |

## Motor Driver Guides

| Driver Type | Guide | Use Case |
|-------------|-------|----------|
| Brushed ESC (60A-120A) | [BRUSHED_ESC.md](BRUSHED_ESC.md) | RC cars, large robots |
| L298N (included in board guides) | See individual board guides | Small motors |

## Quick Reference

### Boards with Built-in Wireless
- UNO R4 WiFi, UNO Q, GIGA R1 - No external module needed

### Boards Requiring External Bluetooth
- Classic UNO, Nano, Mega - Use HC-05/06 or HM-10
- Leonardo, Micro - Use Serial1 on D0/D1
- Due, Zero - 3.3V logic (no voltage divider needed)

---

*For module compatibility details, see [COMPATIBILITY.md](../../arduino_sketches/COMPATIBILITY.md)*
