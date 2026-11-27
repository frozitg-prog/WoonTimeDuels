# WoonTime plugins

Два независимых плагина для Paper 1.21.4 / Java 21:
- **WoonPrefixes** — префиксы с MiniMessage-градиентами и PlaceholderAPI.
- **WoonCases** — блоки кейсов, DeluxeMenus, эффекты открытия и выдача префиксов.

## Сборка
Из корня: `mvn -B package`

JAR-файлы появятся отдельно:
- `WoonPrefixes/target/WoonPrefixes-1.0.0.jar`
- `WoonCases/target/WoonCases-1.0.0.jar`

Оба JAR также публикуются отдельными GitHub Actions artifacts.

Требования сервера: Paper 1.21.4, Java 21. Для меню кейсов рекомендуется DeluxeMenus; для плейсхолдеров префиксов — PlaceholderAPI.
