# Кто что сейчас делает

Над репозиторием работает несколько агентов параллельно, на разных машинах и в
разных рабочих копиях. Этот файл — единственный способ не наступить друг другу
на руки. **Перед тем как взять задачу — посмотри сюда и допиши строку.**

Правила простые:

1. **Заявляй директории, а не задачи.** Столкновения происходят по файлам.
2. **Заявляй до начала работы**, не после. Заявка стоит одну строку и один пуш.
3. **Снимай заявку, когда закончил** — перечёркиванием, чтобы осталась история.
4. **Не коммить чужие файлы.** `git add -A` в общей рабочей копии затянет чужую
   незакоммиченную работу. Добавляй свои пути явно.
5. Если нужно тронуть чужую область — сначала допиши сюда, что и зачем.

---

## Активные заявки

- **Падение C2 в runClient — Codex (2026-10-04): mod/build.gradle, docs/CLIENT_RUNTIME.md.** По hs_err_pid17428.log: Temurin 17.0.15 падает в C2 CompilerThread2 при компиляции FluidState.tick. Обход только для клиентского dev-запуска Windows; игровой код и файлы других агентов не меняю.

- ~~**Тигель — Codex (2026-10-04): новые common/alchemy/**, регистрация Thaumcraft.java, ItemWand.java (котёл), ModOres.java (Creative), новые ресурсы тигля/рецептов, client/CrucibleClient.java, CrucibleTests.java, docs/{CRUCIBLE_SLICE,METALS_SLICE}.md.**~~ Нагрев/вода/предметы/аспекты, получение таумия по оригиналу, сохранение и серверная авторитетность. Capability и сканер другого агента не меняются. Готово: тигель, вода/нагрев/аспекты, три JSON-рецепта, семя пустоты, NBT, очистка, HUD и компаратор. build + 26 GameTest прошли. World flux, мехи/нитор, трубы и research gates остаются ограничениями первого этапа, см. docs/CRUCIBLE_SLICE.md.


- ~~**Металлы TC4 — Codex (2026-10-04): новый common/items/ModMetals.java, Thaumcraft.java (регистрация), ModOres.java (Creative), новые recipes/*thaumium*, recipes/*void*, assets/{models,textures,blockstates} металлов, lang/en_us.json и ru_ru.json (только новые ключи), forge/minecraft tags, новый MetalsTests.java, docs/METALS_SLICE.md.**~~ Слитки/самородки таумия и пустотного металла, таумиевый блок, оригинальные текстуры, преобразование 9:1. Тигель отдельный будущий этап; чужие capability не меняю. Готово: 5 объектов, 6 рецептов и открытия в книге, оригинальные PNG, EN/RU и Forge tags. build + 22 GameTest прошли; первичное получение слитков/сканирование отложено до тигля и аспектов, см. docs/METALS_SLICE.md.


- ~~**Creative и стол исследований — Codex (2026-10-04): ModOres.java (наконечники/иконка), client/WandModelVariants.java, models/item/wand*.json и wand_variants/*_sceptre.json, ResearchTableEntity.java, models/item/wand_base.json, common/research/ResearchTableBlock.java и ResearchMenu.java, client/ResearchScreen.java, blockstates/table.json, новые models/block/research_table_*.json, textures/block/research_table.png и textures/gui/guiresearchtable2.png, новый GameTest и docs/WAND_AUDIT.md.**~~ По скринам пользователя: 6 базовых наконечников, наклон жезла, оригинальный двухблочный стол и GUI; общие knowledge-файлы другого агента не трогаю. Готово: 6 базовых caps, GUI наклон +45°, отдельный скипетр, двухблочная модель, оригинальный GUI, инструменты/инвентарь и ориентация. build и 21 GameTest прошли; неперенесённая головоломка записана в docs/WAND_AUDIT.md.


- ~~**Iron Cap — Codex (2026-10-04): textures/item/wand_cap_iron.png, docs/WAND_AUDIT.md.**~~ Восстановление оригинальной предметной иконки; старая иконка стартового слайса отличается от TC4.


- ~~**Атлас моделей жезлов — Codex (2026-10-04): assets/minecraft/atlases/blocks.json, docs/WAND_AUDIT.md.**~~ Исправление пропущенной регистрации 15 модельных PNG в атласе; проверка запуска из основной игровой копии.


- ~~**Проверка всех жезлов и наконечников — Codex (2026-10-04): новый client/WandModelVariants.java, models/item/wand*.json и models/item/wand_variants/**, textures/models/wand_{rod,cap}_*.png, новый modernTest/java/thaumcraft/gametest/WandVariantsTests.java, комментарий ArcaneWandRecipe.java.**~~ Назначено пользователем: модели всех 54 сочетаний обычных стержней/активных наконечников, исходные модельные текстуры TC4, проверка рецептов/ёмкости/расхода и инертных деталей; область пересекается с заявкой MiMo. Таумометр и эссенция не меняются. Готово: 54 NBT-модели обычных жезлов, 15 исходных модельных PNG, cap scale 1.2; 26 предметных моделей/PNG/локализаций проверены. build и 20 GameTest прошли; ограничения и сценарий клиента в docs/WAND_AUDIT.md.


- ~~**Внешний вид арканного верстака — Codex (2026-10-04): `client/ArcaneWorkbenchScreen.java`, новые `client/ArcaneWorkbenchRendering.java` и `client/ArcaneWorkbenchRenderer.java`, `common/blocks/ArcaneWorkbench{Block,BlockEntity}.java`, `models/block/arcane_workbench.json`, `textures/block/arcane_workbench.png`.**~~ Готово: 6 частей ModelArcaneWorkbench и UV атласа 128×64, исходная worktable.png без изменений (SHA256 совпадает с mod/thaumcraft_src), item-модель наследует новый block-модель; тёмный фон мира, альфа GUI и тултипы; вставленный жезл на столе и синхронизация BE для наблюдателей. `build` и 18 GameTest прошли. В клиенте проверить стол/иконку, GUI, вставку/извлечение жезла и видимость у второго игрока. Общие ClientRegistration и ресурсы эссенции не менялись.

- ~~**Арканный верстак — Codex (2026-10-04): `common/blocks/ArcaneWorkbench*.java`, `client/ArcaneWorkbenchScreen.java`, новый `modernTest/java/thaumcraft/gametest/WorkbenchSafetyTests.java`, корректировка проверок результата в `WandWorkbenchTests.java`.**~~ Готово: по одному списанию ингредиентов, точная оплата и обновление после оплаты, сохранение/дроп только реальных входов, независимое превью всех пользователей с их скидкой, закрытие одного не отключает остальных. Shift-клик требует места для полного результата; остатки рецептов возвращаются один раз. Исправлены передача BlockPos при открытии GUI и смещение фона относительно leftPos/topPos. `build` и 18 GameTest прошли (8 новых: обычный/Shift-клик, полный инвентарь, результат ×4, вёдра, два пользователя/скидки, NBT/reopen, разрушение). Визуальная проверка GUI и реальный multiplayer остаются за игровым клиентом. Таумометр, эссенция, capability и ресурсы не менялись.

- ~~**Анимация зарядки — Codex (2026-10-04): `mod/src/modern/java/thaumcraft/client/WandChargeAnimation.java` (новый), `common/items/tools/ItemWand.java` (client extension и продолжение использования).**~~ Готово: first-person WAVE из legacy `ItemWandRenderer`, плавное поднятие и зеркалирование для левой руки; изменения vis/координат узла не перезапускают использование и переоснащение. `build` и 10 GameTest прошли. Визуально проверить обе руки, удержание более 10 секунд, отпускание ПКМ и отведение взгляда. Луч узел→жезл по-прежнему отложен; модели/PNG деталей жезлов не менялись.

- **`mod/src/modernTest/**`, `mod/build.gradle` (секция `dependencies` с тестовыми
  зависимостями), `.github/workflows/port-1.20.1.yml` — MiMo (2026-10-03).**
  Этап 0: настоящие unit-тесты в `modernTest` (сейчас `gradlew test` прогоняет
  0 тестов — JUnit не подключён), первые guard-тесты, ассерт на ванильные рецепты
  в GameTest и прогон `runGameTestServer` в CI.

- **`mod/src/modern/java/thaumcraft/common/lib/capabilities/IThaumometerKnowledge.java`, `mod/src/modern/java/thaumcraft/common/lib/capabilities/ThaumometerKnowledge.java`, `mod/src/modern/java/thaumcraft/common/commands/AspectCommands.java`, `mod/src/modern/java/thaumcraft/client/ResearchTreeScreen.java` — ChatGPT (2026-10-02).** Первый persistent/synced research-state: ключи исследований, тестовая выдача и блокировка дерева по прогрессу.

- **Руды: `mod/src/modern/java/thaumcraft/common/blocks/ModOres.java`, `mod/src/modern/java/thaumcraft/common/items/ModItems.java`, `mod/src/modern/java/thaumcraft/common/world/InfusedOreFeature.java` + новые `CinnabarOreFeature.java`/`AmberOreFeature.java`, `mod/src/modern/java/thaumcraft/common/config/ConfigAspects.java`, `mod/src/modern/resources/data/thaumcraft/{loot_tables,worldgen,recipes}/**`, `mod/src/modern/resources/data/minecraft/tags/blocks/**`, `mod/src/modern/resources/assets/thaumcraft/{blockstates,models,textures,lang}/**` — MiMo (2026-10-03).** Сверка руд с оригиналом 1.7.10 (`decompiled/`): киноварь и янтарь (meta 0/7) со всеми генерацией/лутом/плавкой, предметы «Ртуть»/«Янтарь», точечные баги (hardness 3.0→1.5, опыт, лут Fortune, тег инструмента), аспекты ванильных руд.

- **Таумометр: `mod/src/modern/java/thaumcraft/common/items/tools/{ItemThaumometer,ThaumometerTargets,ScanInteractions}.java`, `mod/src/modern/java/thaumcraft/common/config/EntityAspects.java`, `mod/src/modern/java/thaumcraft/common/config/ConfigAspects.java` (только ведра/вода/лава), `mod/src/modern/java/thaumcraft/client/ThaumometerItemRenderer.java`, `mod/src/modern/resources/data/thaumcraft/recipes/thaumometer.json` — MiMo (2026-10-03).** Сверка с оригиналом 1.7.10: рецепт (золото вместо железа), дальность луча 5, сканирование игроков, нормализация аспектов узла при выдаче, звук/отмена кликов, строка типа узла в HUD. Файлы capability ChatGPT (`IThaumometerKnowledge`, `ThaumometerKnowledge`) трогать не планируется; если понадобится — допишу сюда до правки.

- **Очки откровения: новые `mod/src/modern/java/thaumcraft/common/items/armor/**`, `mod/src/modern/java/thaumcraft/api/nodes/IRevealer.java`, `mod/src/modern/java/thaumcraft/api/IVisDiscountGear.java`, правки `mod/src/modern/java/thaumcraft/client/AuraNodeRenderer.java`, `mod/src/modern/java/thaumcraft/common/items/ModItems.java`, `mod/src/modern/java/thaumcraft/common/config/ConfigAspects.java`, `mod/src/modern/java/thaumcraft/Thaumcraft.java` (одна строка enqueueWork), `mod/src/modern/resources/data/thaumcraft/recipes/goggles.json`, `mod/src/modern/resources/assets/thaumcraft/{models/item/gogglesrevealing.json,lang/**}` — MiMo (2026-10-03).** Первый предмет брони в modern-слайсе: по оригиналу `decompiled/` (ItemGoggles, TileNodeRenderer#174-199, ConfigAspects#677) — IRevealer-подсветка узлов сквозь стены, редкость, ремонт золотом, скидка 5%, аспект SENSES 4, рецепт и локализация.

- **Звуковая/поведенческая сверка: `mod/src/modern/java/thaumcraft/common/items/tools/ItemWand.java` (одна строка категории звука), `mod/src/modern/resources/data/thaumcraft/advancements/recipes/**`, `mod/src/modern/resources/assets/thaumcraft/sounds.json`, `mod/src/modern/resources/assets/thaumcraft/sounds/**` — MiMo (2026-10-03).** Сверка с `decompiled/`: категория звуков wand/cameraticks = master (как в оригинальном `sounds.json` и у zap от другого агента), advance-манифест книги рецептов очков по образцу thaumometer/wand, точечная правка reveal-ветки таумометра в `AuraNodeRenderer` (main-hand по оригиналу `inventory.getCurrentItem()`).

- **Жезлы и арканный верстак: новые `mod/src/modern/java/thaumcraft/api/wands/**`, `mod/src/modern/java/thaumcraft/common/items/wands/**`, `mod/src/modern/java/thaumcraft/common/crafting/**`, `mod/src/modern/java/thaumcraft/common/blocks/ArcaneWorkbench*.java`, `mod/src/modern/java/thaumcraft/client/ArcaneWorkbenchScreen.java`, правки `common/items/tools/ItemWand.java` (полная переработка), `common/items/ModItems.java`, `common/blocks/ModOres.java`, `client/WandHud.java`, `client/ClientRegistration.java`, `client/ThaumonomiconScreen.java` (переименование iron_wand_cap), `common/nodes/AuraNodeBlockEntity.java` (только ветка подпитки жезлом), `Thaumcraft.java`, `mod/src/modern/resources/data/thaumcraft/recipes/**`, `mod/src/modern/resources/data/thaumcraft/advancements/recipes/**` (переименования iron_wand_cap), `mod/src/modern/resources/data/thaumcraft/loot_tables/blocks/arcane_workbench.json` (новый), `mod/src/modern/resources/assets/thaumcraft/{textures,lang,models,blockstates}/**`, `docs/PLAYABLE_SCAN_SLICE.md` (отчёт слайса) — MiMo (2026-10-04).** Порт по `decompiled/`: реестры `WandCap`/`WandRod`/`IWandRodOnUpdate` (+`StaffRod` только данные), предметы wand_cap ×9 / wand_rod ×17 (посохи — предметы без механики), жезл с NBT rod/cap и ёмкостью rod×100, имя `%CAP %ROD %OBJ`, оригинальный тултип, `getConsumptionModifier` со скидкой очков (`IVisDiscountGear` — только броня, baubles нет), подпитка от узла (удержание ПКМ, ray по bounds 0.3–0.7), блок/TE/меню/GUI арканного верстака (`BlockTable` meta 15), арканные рецепты с висом (шапки/палочки/комбо жезлов, очки возвращаем в арканный вид), рецепт стола. Исследования-гейты в рецептах — пустые до готовности дерева (записано в отчёте). Файлы ChatGPT (`IThaumometerKnowledge` и др.) — read-only; чужие незакоммиченные файлы (`ItemShard`, `ConfigAspects`, `ConfigRecipesArcaneSlice`, `ThaumometerHands`) не трогаю и не коммичу.

---

## Снятые заявки

### ~~Research flow, wand HUD и legacy assets — Codex (2026-10-02)~~

`mod/src/modern/**`, `mod/gradle.properties`, `docs/PLAYABLE_SCAN_SLICE.md`,
`scripts/export_modern_research.py`: модель палочки, HUD vis, оригинальная
навигация исследований, research browser и пакет legacy-ассетов. Зафиксировано
коммитом `d400596d`; локальный `gradlew build` после исправления импорта `Level`
прошёл успешно.

### ~~Первые шаги 0.3.0 — Codex (2026-10-02)~~

`mod/src/modern/**`, `mod/gradle.properties`, `docs/PLAYABLE_SCAN_SLICE.md`:
двуручный хват таумометра при свободной второй руке, ориентация/размер/цвет линзы,
сканирование 41 типа сущностей с отдельным сохранением, аспекты из рецептов,
железная деревянная палочка и превращение полки в читаемый Таумономикон.
`compileJava jar` успешно; новые тесты не запускались. Визуальная и игровая
проверка 0.3.0 остаётся за клиентом, сценарии перечислены в документации.

### ~~Игровая альфа 0.2.0 — Codex (2026-10-02)~~

`mod/src/modern/**`, `mod/src/modernTest/**`, `mod/build.gradle`,
`mod/gradle.properties`, `docs/PLAYABLE_SCAN_SLICE.md`: руды и осколки,
рецепты выживания, стол с объединением аспектов, сканирование предметов,
исправление атласа и подключения рендера. Собран jar 0.2.0-survival-alpha.
Четыре серверных GameTest прошли; визуальная проверка клиента ещё нужна.
Ветка `port/1.20.1-bootstrap`.

### ~~Сканер thaumometer — Codex (2026-10-02)~~

Добавлены рендер модели и линзы сканера, звуковой тик, синхронизация знаний
игрока, сохранение открытых аспектов и изученных предметов. Сканирование блоков
разрешает цель в её реальный `ItemStack` (включая жидкие блоки через ведро),
чтобы выбор аспектов и повторное сканирование соответствовали модели Thaumcraft
1.12.2. Старые записи `scannedBlocks` переносятся при чтении сохранения.
`compileJava processResources` прошла; игровой запуск пока проверил пользователь.

### ~~Точные теги ItemStack — Codex (2026-10-02)~~

`ThaumcraftApi` хранит теги предмета без NBT как fallback, а для ItemStack с
NBT — отдельный тег по копии данных стека. При чтении точное совпадение
проверяется первым. Свежая `compileJava` прошла.

### ~~Ванильные аспекты предметов — Codex (2026-10-02)~~

Перенесены прямые аспекты инструментов, еды, моб-дропа, ингредиентов зелий,
семян и вёдер в `mod/src/modern/java/thaumcraft/common/config/ConfigAspects.java`.
Источником служит сохранённая реализация 1.12.2. Рецептно-вычисляемые сложные
теги оставлены до переноса этой механики. Свежая `compileJava` прошла.

### ~~Ветка исследований TT — Claude (сессия аудита 2026-07-28)~~

**Заявлено:** 2026-07-28. Закрывает первый пункт `KNOWN_ISSUES` — без записей
Таумономикона бо́льшая часть рецептов модуля не срабатывает.

**Сделано в 1.1.25.0:** вкладка `TT_CATEGORY` и ветка семи фокусов. Остальные
поддеревья (`DARK_QUARTZ`, `GASEOUS_LIGHT`, `SPELL_CLOTH` и одиночки от ключей
TC4) закрыты позже; порядок и дерево — в `KNOWN_ISSUES`.

Директории и файлы:

- `mod/src/main/java/thaumcraft/common/config/research/ConfigResearchTinkerer.java` (новый)
- `mod/src/main/java/thaumcraft/common/config/research/TinkererResearchItem.java` (новый)
- `mod/src/test/java/thaumcraft/common/config/research/TinkererResearchStaticGuardTest.java` (новый)
- `mod/src/main/java/thaumcraft/common/config/research/ConfigResearch.java` (только две строки вызова)
- `mod/src/main/resources/assets/thaumcraft/textures/misc/r_enchanting.png` (иконка вкладки)
- строки `ttresearch.*` в обоих `lang`

### ~~Модуль Thaumic Tinkerer — Claude (сессия TT-порта)~~

**Заявлено:** 2026-07-28. **Снято:** 2026-07-30 — перенос закончен, все 79
объектов на месте.

Директории и файлы:

- `mod/src/main/java/thaumcraft/common/items/tinkerer/**`
- `mod/src/main/java/thaumcraft/common/blocks/tinkerer/**`
- `mod/src/main/java/thaumcraft/common/tiles/tinkerer/**`
- `mod/src/main/java/thaumcraft/common/lib/tinkerer/**`
- `mod/src/main/java/thaumcraft/common/lib/enchantment/tinkerer/**`
- `mod/src/main/java/thaumcraft/common/lib/world/dim/bedrock/**`
- `mod/src/main/java/thaumcraft/common/items/wands/foci/Focus*.java` (семь фокусов TT)
- `mod/src/main/java/thaumcraft/common/config/ConfigTinkerer.java`
- `mod/src/test/java/thaumcraft/common/items/wands/foci/ThaumicTinkererFociStaticGuardTest.java`
- `scripts/**` — генераторы документации TT
- `TT_OBJECT_REFERENCE.md`, `TT_PORT_QUEUE.md`, `THAUMIC_TINKERER_PLAN.md`

Общие файлы, которые приходится трогать (правки точечные, конфликтуют редко —
но лучше предупредить перед своей правкой):

- `ConfigItems.java`, `ConfigBlocks.java` — только добавление своих полей в конец
- `CommonProxy.java`, `ClientProxy.java` — только свои GUI id и ветки `switch`
- `lang/en_us.lang`, `lang/ru_ru.lang` — только ключи `*.tinkerer.*`, `*.kami.*`, `ttmisc.*`
- `CHANGELOG.md`, `KNOWN_ISSUES.md` — своя запись сверху

**Состояние модуля:** 79 из 79 объектов перенесены (1.1.42.0). Работа
перешла из переноса в починку: открытые расхождения — в `KNOWN_ISSUES.md` и
в разделе «Открыто» файла `THAUMIC_TINKERER_PLAN.md`.

---

## Не моё — не трогаю

- **Генерация мира** (`common/lib/world/**`, `common/lib/events/EventHandlerWorld.java`,
  `common/lib/world/biomes/**`) — отдельное направление другого агента. Правило
  «не коммитить чужое» остаётся: чужая незакоммиченная работа в общей рабочей
  копии не должна попасть в чужой коммит.
- **Клиентский рендер TC4** (`client/renderers/**`, `client/fx/**`) — отдельное
  направление. (Пять guard-тестов оттуда, которые тут числились падающими, на
  2026-07-28 зелёные — пункт в `KNOWN_ISSUES` снят.)
