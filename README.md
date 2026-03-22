# GroupMaster

Minecraft мод для управления группами игроков с системой бонусов, наследованием и веб-панелью.

**Minecraft:** 1.21 · **Java:** 21 · **Лоадеры:** Fabric, Forge, NeoForge

## Возможности

- **Группы** — создание, иерархия с наследованием, приоритеты
- **Бонусы** — data-driven система с триггерами, условиями, действиями, стакингом и таргетами
- **Веб-панель** — управление через браузер с REST API и SSE-обновлениями в реальном времени
- **Хранение** — SQLite с автоматическими миграциями

## Команды

Все команды требуют permission level 2.

### Группы

```
/gm group create <id> <displayName> [parent] [priority]
/gm group delete <id>
/gm group list
/gm group info <id>
/gm group setparent <id> <parent|none>
```

### Игроки

```
/gm player assign <player> <group>
/gm player remove <player>
/gm player info <player>
```

### Бонусы

```
/gm bonus add <group> <trigger> <actionType> <actionValue>
/gm bonus addoverride <group> <trigger> <actionType> <actionValue>
/gm bonus addtarget <group> <trigger> <target> <actionType> <actionValue>
/gm bonus condition <bonusId> <conditionJson>
/gm bonus interval <bonusId> <ticks>
/gm bonus stacks <bonusId> <maxStacks> <stackMode> <resetOn>
/gm bonus remove <id>
/gm bonus list <group>
/gm bonus effective <group>
/gm bonus types
```

## Система бонусов

### Триггеры

| Триггер | Описание |
|---------|----------|
| `on_join` | При входе игрока в группу |
| `on_leave` | При выходе из группы |
| `tick` | Каждые N тиков (настраивается через `interval`) |
| `on_attack` | При атаке сущности |
| `on_damaged` | При получении урона |
| `on_kill` | При убийстве сущности |
| `on_death` | При смерти игрока |
| `on_eat` | При употреблении еды |

### Действия

| Действие | Значение | Пример |
|----------|----------|--------|
| `effect` | Эффект зелья | `{"effect":"minecraft:speed","amplifier":1,"duration":10}` |
| `attribute` | Модификатор атрибута | `{"attribute":"minecraft:generic.max_health","amount":4.0,"operation":"add_value"}` |
| `burn` | Поджог | `{"duration":3}` |
| `command` | Серверная команда | `{"command":"give {player} minecraft:diamond 1"}` |
| `message` | Сообщение в чат | `{"message":"Вы стали сильнее!"}` |
| `set_group` | Смена группы | `{"group":"paladin_lv2"}` |

### Условия

Условия задаются в формате JSON:

```json
{"type":"is_day"}
{"type":"health_below","value":0.5}
{"type":"in_dimension","dimension":"minecraft:the_nether"}
{"type":"and","conditions":[{"type":"is_night"},{"type":"sneaking"}]}
```

Доступные типы: `always`, `is_day`, `is_night`, `in_sunlight`, `in_water`, `in_rain`, `on_fire`, `sneaking`, `health_below`, `health_above`, `in_dimension`. Композитные: `and`, `or`, `not`.

### Таргеты

- `self` — действие применяется к игроку группы (по умолчанию)
- `victim` — действие применяется к другому игроку (PvP)

### Стакинг

- **maxStacks** — максимум стаков (0 = без ограничений)
- **stackMode** — `each` (применяется при каждом стаке) или `threshold` (применяется при достижении максимума)
- **resetOn** — `never`, `on_death`, `on_leave`

### Наследование и override

Бонусы наследуются по цепочке групп от корня к потомку. Бонус с `override=true` в дочерней группе заменяет родительский бонус с тем же ключом (`actionType:mergeKey`).

## Веб-панель

Запускается автоматически на порту 8080. Конфигурация: `config/groupmaster/web.json`.

При первом запуске генерируется токен авторизации — он выводится в лог сервера.

### REST API

**Группы:**
- `GET /api/groups` — список групп
- `POST /api/groups` — создать группу
- `GET /api/groups/{id}` — информация о группе
- `PUT /api/groups/{id}` — обновить группу
- `DELETE /api/groups/{id}` — удалить группу
- `GET /api/groups/{id}/players` — игроки группы
- `GET /api/groups/{id}/tree` — дерево иерархии

**Игроки:**
- `GET /api/players` — список игроков
- `POST /api/players/{uuid}/assign` — назначить группу
- `POST /api/players/{uuid}/remove` — убрать из группы

**Бонусы:**
- `GET /api/bonuses` — все бонусы
- `GET /api/bonuses/group/{groupId}` — бонусы группы
- `GET /api/bonuses/group/{groupId}/effective` — эффективные бонусы (с наследованием)
- `POST /api/bonuses/group/{groupId}` — создать бонус
- `DELETE /api/bonuses/{id}` — удалить бонус
- `GET /api/bonuses/types` — типы действий
- `GET /api/bonuses/conditions` — типы условий

**События:** `GET /api/events` — SSE-поток обновлений

Все запросы требуют заголовок `Authorization: Bearer <token>`.

## Структура проекта

```
common/     — общий код (модели, команды, бонусы, хранилище, веб-сервер)
fabric/     — Fabric-специфичный код
forge/      — Forge-специфичный код
neoforge/   — NeoForge-специфичный код
```
