# WoonTime plugins

Два независимых плагина для Paper 1.21.4 / Java 21:

- **WoonPrefixes** — префиксы с MiniMessage-градиентами, сохранением владения/выбора, отображением в чате и PlaceholderAPI.
- **WoonCases** — регистрируемые блоки кейсов, открытие меню DeluxeMenus, частицы, звуки, анимация и выдача префиксов.

## Сборка

Из корня репозитория: `mvn -B clean package`

JAR-файлы собираются отдельно:
- `WoonPrefixes/target/WoonPrefixes-1.0.0.jar`
- `WoonCases/target/WoonCases-1.0.0.jar`

GitHub Actions загружает их двумя раздельными artifacts: `WoonPrefixes-1.0.0` и `WoonCases-1.0.0`.

## Установка

1. Установи оба JAR в папку `plugins`.
2. Для меню кейса установи DeluxeMenus.
3. Для плейсхолдеров установи PlaceholderAPI.
4. Перезапусти сервер.

## Префиксы

Стандартные ID:
- `punisher` — Каратель (красный градиент)
- `invincible` — Непобедим (розовый градиент)
- `king` — Король (синий градиент)

Команды:
- `/prefixes list`
- `/prefixes select <id>`
- `/prefixes give <игрок> <id>` — администратор
- `/prefixes remove <игрок> <id>` — администратор

PlaceholderAPI:
- `%woonprefixes_selected%` — выбранный префикс без MiniMessage-разметки
- `%woonprefixes_selected_mini%` — выбранный префикс с MiniMessage-разметкой
- `%woonprefixes_has_punisher%`, `%woonprefixes_has_invincible%`, `%woonprefixes_has_king%` — проверка владения

## Кейсы

Встань на блок, который должен стать кейсом, и выполни:
`/wooncases setblock prefixcase ENDER_CHEST`

Команда поставит указанный материал под ногами и зарегистрирует координаты. ПКМ по блоку откроет DeluxeMenus меню с ID `prefixcase` (по умолчанию) и запустит анимацию открытия. Награда выбирается из `WoonCases/config.yml`.

- `/wooncases remove <id>` — удалить регистрацию кейса
- `/wooncases reload` — перезагрузить конфигурацию

Меню DeluxeMenus должно быть создано отдельно и иметь ID `prefixcase` (или укажи свой ID в настройке `menu`). Награды кейса должны совпадать с ID префиксов в WoonPrefixes.
