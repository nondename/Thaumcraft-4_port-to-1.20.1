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

- ~~**Падение Duplicate registration flux_gas — Codex (2026-10-05): common/blocks/ModOres.java, проверка регистрации в mod/build.gradle, docs/CRUCIBLE_SLICE.md.** Коммит 9551a3b повторно зарегистрировал существующие flux_gas/flux_goo. Удалить дубли, сохранить действующую механику ModAlchemy и проверить runtime загрузку. Пересечение заявки taint — только исправление подтверждённого падения.~~ Готово: дубли регистрации и Creative убраны; ModAlchemy/FluxBlock сохранены. build + 48 GameTest прошли, runClient завершил загрузку реестров и текстур без ошибки. Gradle guard подтверждён на сломанном/исправленном состоянии; docs/CRUCIBLE_SLICE.md.

- **Блоки порчи — Claude (2026-10-05): common/blocks/{BlockTaint,BlockTaintFibres,BlockFluxGoo,BlockFluxGas}.java, ModOres.java.** Перенос базовых блоков порчи из legacy (1.12.2) в modern (1.20.1). Только регистрация и базовые свойства, без механик распространения.

- ~~**Третий проход книги — Codex (2026-10-05): client/BookSkin.java, ResearchTreeScreen.java, ResearchEntryScreen.java, ThaumonomiconScreen.java; research/tree.json и отсутствующие textures/items деталей жезлов, scripts/export_modern_research.py, mod/build.gradle (проверка иконок при сборке) и docs/THAUMONOMICON_UI.md.** Кожаный ванильный переплёт, крупные контрастные иконки и исправление missing textures по новым скринам.~~ Готово: полный ванильный переплёт, крупные контрастные значки/текст, UV зеркалирование, 22 исправленных пути, 3 предметные иконки. build + проверка 193 PNG в JAR и локализации; docs/THAUMONOMICON_UI.md. Игровую оценку выполняет пользователь.

- ~~**Второй вариант книги по скринам — Codex (2026-10-05): client/BookSkin, ResearchTreeScreen, ResearchEntryScreen, ThaumonomiconScreen, свои lang ключи и docs/THAUMONOMICON_UI.md.** Компактный силуэт, облегчённые закладки, подписи без пересечения сгиба, встроенный возврат и оформление последней страницы. Серверные механики не трогаю.~~ Готово: ванильная бумага, компактный масштаб, закладки, заголовки и подписи на отдельных страницах, встроенный возврат и карточка конца записи. build и 917 EN/RU ключей проверены; оценка в игре пользователем.

- ~~**Оформление таумономикона — Codex (2026-10-05): client/ResearchTreeScreen, ResearchEntryScreen, ThaumonomiconScreen, новый BookSkin.java, свои lang ключи и docs/THAUMONOMICON_UI.md.** По утверждённому варианту пользователя: бумага и обложка Minecraft вместо деревянной рамки, закладки, читаемые состояния и перелистывание. Механики исследований не меняю.~~ Готово: общая бумажная BookSkin, обложка/сгиб/закладки, состояния узлов и стрелки страниц, память карты; build и 913 EN/RU ключей проверены. Игровая оценка внешнего вида — пользователем, docs/THAUMONOMICON_UI.md.

- ~~**Пьедестал по игровым скринам — Codex (2026-10-05): models/block и item/arcane_pedestal.json, textures/block/pedestal*, при необходимости ClientRegistration/InfusionDeviceBlock, scripts/verify-survival-resources.ps1 и docs/INFUSION_ASSET_FIXES.md.** Исправить UV прозрачного атласа, миниатюру и частицы разрушения. Прежний claim закрыт; механики алтаря не меняю.~~ Готово: закрытые UV 18 граней, общая модель блока/миниатюры, совпадающая коллизия; opaque particle и явные atlas sprites. build + 48 GameTest и проверка пикселей/PNG прошли. Клиент визуально не запускался; docs/INFUSION_ASSET_FIXES.md.

- ~~**Дополнение к аудиту исследований: ModNetwork.java, новый ResearchActionMessage, client/ResearchScreen.java, scripts/export_modern_research.py.** Стандартный пакет кнопки хранит byte и обрезает действия 1000/2000; нужен отдельный серверный пакет с проверкой открытого меню. Экспорт должен сохранять скрытые условия дерева.~~ Готово: новый проверяемый C2S пакет, протокол 3, экспорт метаданных дерева.

- ~~**Аудит исследований — Codex (2026-10-05): common/research/**, client/ResearchTreeScreen.java, новый рецепт пополнения чернил в common/crafting и ModRecipes.java, progression/tree JSON, свои lang ключи, новые GameTest и docs/RESEARCH_AUDIT.md.** По поручению пользователя: таумономикон, стол, записки и серверные условия; скрытые родители/связанные открытия и чернила. Пересечение старых claims — только перечисленные файлы, сканирование и генерацию не меняю.~~ Готово: серверная передача действий записки, конечные/пополняемые чернила, скрытые требования и связанная этикетка, полная глава исследований и рецепты; build + 48 GameTest, 909 EN/RU ключей проверены. Остатки исходной мини-игры и клиентская проверка — docs/RESEARCH_AUDIT.md.

- ~~**Команды TC4 — Codex (2026-10-05): common/commands/**, capabilities IThaumometerKnowledge/ThaumometerKnowledge (точное начисление административных аспектов, set/add warp), новые data/thaumcraft/commands/research_links.json, свои lang ключи, новые CommandsTests и docs/COMMANDS.md.** Явное поручение пользователя: синтаксис/алиасы/research/aspect/warp по сохранённому CommandThaumcraft; debug вынести отдельно. Пересечение старого claims знаний — только указанный API, сканирование не меняю.~~ Готово: original research/player/all/reset/list, aspect, warp set/add PERM/TEMP, /tc /thaum и OP2; 17 скрытых связей, точные начисления, debug отдельно, EN/RU. build + 44 GameTest прошли; docs/COMMANDS.md. Реальный сетевой клиент вручную не проверялся.

- ~~**Аудит нитора/алюментума/узла в банке — Codex (2026-10-05): common/alchemy/ModAlchemy.java и NitorBlock.java, новые AlumentumItem/AlumentumProjectile и ModAlchemyEntities, регистрация Thaumcraft.java/ClientRegistration.java, новый клиент NitorRenderer/ресурсы, новые тесты и docs/ALCHEMY_MECHANICS_AUDIT.md.** Проверка рецептов/исследований по сохранённым исходникам; узел в банке пока аудит, расширение переноса ожидает ответа пользователя. Старые claims пересекаются только в указанных строках; генерация мира read-only.~~ Готово: рецепты/исследования нитора и алюментума сверены; добавлены бросок/взрыв алюментума, Creative и mobGriefing. build + 40 GameTest прошли. Узел в банке отсутствует — полный перенос вынесен в уточнение объёма; docs/ALCHEMY_MECHANICS_AUDIT.md.

- ~~**Банки/пьедестал/Salis Mundus — Codex (2026-10-05): common/infusion/ModInfusion.java и InfusionDeviceBlock.java, common/sounds/ModSounds.java, sounds.json + новые jar*.ogg, models/item/arcane_pedestal.json, textures/item/salis_mundus.png.mcmeta, docs/INFUSION_ASSET_FIXES.md.** По скринам пользователя: звуки банки, инвентарная миниатюра пьедестала, наличие/анимация соли. Пересечение прежнего claims sounds — только jar; генерацию/книгу не трогаю.~~ Готово: оригинальная анимация соли, jar1–4 при установке/разрушении/переливании, отдельная инвентарная модель пьедестала по сохранённому port-reference. build + 37 GameTest прошли; модели/локализация/хэши проверены. Клиентская оценка — пользователем; docs/INFUSION_ASSET_FIXES.md.

- ~~**Русская локализация — Codex (2026-10-05): mod/src/modern/resources/assets/thaumcraft/lang/{ru_ru,en_us}.json, только строки UI в client/ResearchEntryScreen.java при необходимости; scripts/export_modern_research.py (экспорт страниц), новый scripts/verify-modern-localization.ps1 и docs/LOCALIZATION.md.** По поручению пользователя: сверка сохранённого русского оригинала TC4 и доступных modern ключей. Пересечение старых claims lang — только перевод, механики и реестры не меняю.~~ Готово: 900 EN/RU ключей, 728 UI/страниц проверены; названия сверены с русским оригиналом, пропущенная страница восстановлена, пути IMG скрыты; build прошла, 900 RU ключей подтверждены в JAR. Игровую вычитку переносов не выполняла; docs/LOCALIZATION.md.

- ~~**Survival 1–4 — Codex (2026-10-05), пользователь включил инфузию:** common/research/** (заметки/серверное завершение), capabilities IThaumometerKnowledge/ThaumometerKnowledge (расход очков), ModNetwork (новый C2S), client ResearchTreeScreen/ResearchScreen/ResearchEntryScreen; common/alchemy/** (исследовательские условия, нитор/алюментум), common/crafting/** и ArcaneWorkbenchMenu.java (условия/рецепты деталей), новый common/infusion/**; регистрация Thaumcraft.java/ModOres.java; ConfigAspects.initModItems, новые ресурсы/рецепты и только свои lang ключи, новые GameTest и docs/SURVIVAL_PROGRESSION.md. Пересечение со старыми claims знаний — явное поручение пользователя; сканер/генерацию мира не меняю. Пункты 5/6 отложены.~~ Готово: исследования/заметки и серверные условия; рецепты старших обычных жезлов, нитор/алюментум, перегонка/банки/пузырьки и инфузия. build + 37 GameTest после совмещения с 4bff3a2 прошли; 27 моделей проверены скриптом. Ограничения записок, нестабильности, искажения и клиентской проверки — docs/SURVIVAL_PROGRESSION.md. Пункты 5/6 не выполнялись.

- ~~**Флюкс тигля — Codex (2026-10-05): common/alchemy/{ModAlchemy,CrucibleEntity,FluxBlock,FluxSpill}.java, новые ресурсы flux_gas/flux_goo, lang (только флюкс), новый FluxTests.java, docs/CRUCIBLE_SLICE.md.**~~ Готово: выброс газа/жидкого флюкса при очистке, разрушении и переполнении; конечные объёмы, движение и оригинальные PNG. build + 28 GameTest прошли. Полный taint/мобы/Vis Exhaustion отдельный этап; ограничения в CRUCIBLE_SLICE.md.

- ~~**Внешний вид тигля — Codex (2026-10-04): models/block/crucible*.json, client/CrucibleClient.java, новый client/CrucibleRenderer.java, docs/CRUCIBLE_SLICE.md.**~~ Готово: оригинальные PNG по назначению, закрытое дно/внутренние стенки, cutout-корпус отдельно от прозрачной воды, точный уровень/окраска воды. build прошла; визуальная проверка пользователем. Таумономикон следующим этапом.

- ~~**Падение C2 в runClient — Codex (2026-10-04): mod/build.gradle, docs/CLIENT_RUNTIME.md.**~~ По hs_err_pid17428.log: Temurin 17.0.15 падает в C2 CompilerThread2 при компиляции FluidState.tick. Готово: C1 для клиентского dev-запуска Windows, переключатель safeClientJit; build + 26 GameTest прошли, параметр проверен в Forge client run config. Вход в клиентский мир после обхода ещё требует проверки пользователем.

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
