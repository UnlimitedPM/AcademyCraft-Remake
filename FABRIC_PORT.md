# Portage Fabric — état et plan

Minecraft 1.20.1, Fabric Loader 0.16.14, Fabric API 0.92.12.

## Pourquoi Fabric n'est pas un renommage

NeoForge 1.20.1 est un fork direct de Forge 47 : mêmes packages `net.minecraftforge`, donc le
code Forge s'y compile presque tel quel. Fabric est une autre architecture :

- pas de `DeferredRegister` ni de `RegistryObject` (`Registry.register` direct) ;
- pas de bus d'événements Forge : il faut brancher les callbacks Fabric API ;
- pas de capacités (`Capability`, `LazyOptional`, `IItemHandler`) ;
- pas d'API d'énergie, de fluides ni de configuration dans Fabric API ;
- un autre système de réseau ;
- des hooks de rendu différents (HUD, monde, écran).

## Audit du code actuel (447 classes principales)

| Dépendance | Fichiers |
|---|---|
| Utilisent directement `net.minecraftforge` | 226 |
| Dépendent de Forge directement ou par import transitif | 327 |
| Sans dépendance Forge détectée (imports) | 120 |
| Réseau Forge (`SimpleChannel`, `NetworkEvent`, `PacketDistributor`) | 72 |
| Capacités (`Capability`, `LazyOptional`, `IItemHandler`, énergie, fluides) | 55 |
| Événements (`SubscribeEvent`, bus Forge) | 53 |
| Hooks client Forge (overlay HUD, rendu monde, renderers) | 25 |
| Registres Forge | 18 |
| Configuration (`ForgeConfigSpec`) | 2 fichiers, mais centraux |

Les 120 classes « propres » ne suffisent pas : beaucoup référencent des classes du même package
qui, elles, touchent Forge. Le portage doit donc se faire par domaine, pas par fichier isolé.

## Palier livré dans cette branche

`fabric/` charge le mod et enregistre une première tranche de contenu vanilla :

- 20 objets matériaux/composants (`Item` simple) ;
- l'unité d'énergie (`EnergyUnitItem`) déplacée dans `common/` avec `EnergyItems` et
  `ImagEnergyItem` : premier vrai morceau partagé, avec `PlatformHooks` pour la capacité d'énergie
  externe ;
- 5 blocs vanilla (4 minerais + `machine_frame`) avec leurs block items ;
- le moteur à chat (`CatEngineBlock`, `CatEngineBlockEntity`, `CatEngineRenderer`) déplacé dans
  `common/` : premier bloc à block entity et premier rendu partagés. Le type de block entity est
  injecté par le chargeur, le rendu Fabric est enregistré par `AcademyCraftFabricClient` ;
- onze classes du réseau d'énergie passées dans `common/` : `EnergyNode`, `EnergyGenerator`,
  `EnergyReceiver`, `ImagNetworkData`, `NodeConnection`, `NodeType`, `WirelessBalancer`,
  `WirelessMachine`, `WirelessNode`, `BlockEntityScan`, `ConfigText` ;
- utilitaires neutres passés dans `common/` : `CubicCurve`, `ImprovedNoise`, `Plotter`,
  `SyncedInt`, `StructureLootTables`, `RenderBounds`, `MatrixStructure`, `WindgenStructure` ;
- stockage neutre : interfaces `ItemStorage` et `EnergyStorage`, `StorageSlot` commun,
  `SimpleItemStorage` (implementation sans Forge), adaptateurs `ForgeItemStorage`/`ForgeEnergyStorage`.
  Les 9 machines de `StoresItems` et les 10 menus n'importent plus `ItemStackHandler`/`SlotItemHandler` ;
  `MachineDrops` est commun ;
- échanges d'énergie neutres : `PlatformHooks.chargeExternalItem`, `extractExternalItem` et
  `pushEnergyToNeighbor` ; Forge les branche sur ses capabilities, Fabric renvoie 0 pour l'instant.
  SolarGen passe déjà par ces crochets ;
- `SimpleItemStorage` sait se sauvegarder en NBT ; `ForgeItemHandlerAdapter` expose un stockage
  commun comme `IItemHandler`. SolarGen n'utilise plus `ItemStackHandler` en interne ;
- `TooltipItem` extrait de `ModItems` vers `common/` ; Fabric enregistre `magnetic_coil` avec son
  infobulle ;
- ouverture de menu neutralisée : `PlatformHooks.openMenu` remplace les 11 appels à
  `NetworkHooks.openScreen` ; Forge installe l'implémentation, Fabric branche la sienne
  (`FabricPlatformHooks`, via `ExtendedScreenHandlerFactory`) ;
- générateur solaire entièrement dans `common/` (`SolarGenBlock`, `SolarGenBlockEntity`,
  `SolarGenMenu`) : le type de block entity et le type de menu sont injectés par le chargeur,
  Forge/NeoForge n'ajoutent qu'une sous-classe à capabilities (`ForgeSolarGenBlockEntity`) et un
  adaptateur `EnergyStorage` → `IEnergyStorage`. Fabric l'enregistre avec un
  `ExtendedScreenHandlerType` (position du bloc en données d'ouverture). Le raccordement au
  réseau passe par `PlatformHooks.ensureNodeLinked`, que Forge branche sur `NodeFinder` ;
- formeur de métal dans `common/` (`MetalFormerBlock`, `MetalFormerBlockEntity`,
  `MetalFormerMenu`, `MetalFormerMode`, `MetalFormerRecipes`), enregistré sur Fabric. Ses recettes
  ne nomment plus `ModItems`/`ModBlocks` : `AcademyContent.item("wafer")` les retrouve par
  identifiant dans la registry vanilla, ce qui marche sur les trois chargeurs. Les sons de machine
  (`MachineSounds`, `MachineLoops`) sont communs aussi ;
- éolienne dans `common/` : base productrice (altitude, colonne, bloc de chargement), rotor
  (hélice, zone balayée, colonne) et leurs menus. Les blocs voisins (`windgen_pillar`,
  `windgen_main`, `windgen_base`) se comparent par identifiant, plus par registre Forge. Fabric
  enregistre les trois blocs, les deux block entities, les deux menus et l'objet `windgen_fan`.
  Le rendu des pales reste côté Forge pour l'instant ;
- Matrix et nœuds sans fil dans `common/` (`MatrixBlock`, `MatrixBlockEntity`, `MatrixMenu`,
  `NodeBlock`, `NodeBlockEntity`, `NodeMenu`, `NodeFinder`) : les paquets de configuration
  d'écran (`MatrixConfigPacket`, `NodeConfigPacket`) restent côté Forge, `PlatformHooks` ne
  transporte que l'intention (`sendMatrixConfig`, `sendNodeConfig`). Fabric enregistre les quatre
  blocs, les deux types de block entity et les deux menus ;
- machines à phase dans `common/` : fuseur Imag et générateur de phase (blocs, block entities,
  menus, recettes du fuseur). La cuve est une `SimpleFluidTank` neutre (fluide + quantité +
  sauvegarde NBT) ; Forge la réexpose en `IFluidHandler` avec `ForgeFluidHandlerAdapter`. Fabric
  enregistre les deux machines et les objets `matter_unit`/`matter_unit_phase_liquid` ;
- moteur d'interface dans `common/` : `CGui*`, `CWidget`, `AcademyFonts`, `MarqueeText` et les
  huit écrans de machines. L'enregistrement des programmes de rendu reste par chargeur
  (`ForgeCGuiShaders`), et un access widener commun ouvre les états de rendu protégés de vanilla
  (`RenderType.create` est appelé par réflexion). Fabric enregistre les huit écrans avec
  `MenuScreens` ; sans programmes, les anneaux et icônes grisées s'abstiennent ;
- génération du monde : les `data/academy/worldgen/**` (configured/placed features) et les tables
  de butin des blocs sont partagées ; `FabricWorldgen` ajoute les quatre minerais à toutes les
  biomes de l'overworld avec `BiomeModifications`, comme le biome modifier Forge. Le lac de phase
  liquide attend le bloc de fluide ;
- réseau neutre : `Network` (codec + gestionnaires) et `AcademyPackets` déclarent les paquets une
  fois ; `ForgeNetworkTransport` les enregistre sur un `SimpleChannel`, `FabricNetworkTransport`
  sur un canal unique avec l'identifiant en tête. Quatre paquets sont déjà communs : onglet sans
  fil (demande + état), configuration du nœud et du Matrix ;
- première entité : `EntitySilbarn`, `SilbarnVisuals` et `SilbarnItem` dans `common/` ; le type
  d'entité est résolu par identifiant à la création du projectile, et le son de l'impact aussi
  (`AcademyContent.entityType`/`sound`). Le rendu (`SilbarnRenderer`, `ObjModels`, `ObjMesh`) est
  commun aussi ; les éclats passent par `PlatformHooks.spawnSilbarnFragments`, branché sur
  `SilbarnFrags` côté Forge. Fabric enregistre le rendu avec `EntityRendererRegistry` ;
- les 47 événements sonores de `ModSounds`, mêmes identifiants que `sounds.json` ;
- un onglet créatif `itemGroup.academy_tab` ;
- les assets du mod (textures, modèles, langues) sont repris de `forge/src/main/resources` ;
- les `data/` (recettes, succès, tags, worldgen) sont volontairement exclus tant que les objets
  référencés n'existent pas sous Fabric.

Vérifié : le client Fabric démarre, un monde se charge, le contenu est enregistré.

### Aptitudes, deuxième palier

- les données par joueur passent par `PlatformHooks.installAbilityData` /
  `PlatformHooks.abilityData(Player)`. Forge répond avec sa capacité (`AbilityCapability`),
  Fabric avec `FabricAbilityData`, une table en mémoire dont les entrées tombent à la
  déconnexion. **Limite assumée** : rien n'est encore sauvegardé sous Fabric ; l'étape suivante est
  un attachement persistant, et le système d'aptitudes n'est pas encore branché côté Fabric, donc
  aucun joueur n'en dépend aujourd'hui ;
- un crochet `categoryChanged` remplace l'appel direct au pré-réglage Forge dans le changement de
  catégorie ;
- `AcademySounds` est commun : il prend un `Supplier<SoundEvent>` (Forge lui passe son
  `RegistryObject`, qui en est un) ou un identifiant de son résolu par `AcademyContent.sound` ;
- `CONVERT_CATEGORY` rejoint les succès neutres, donc attribuables par `PlatformHooks` ;
- passent en `common/` : `develop/` (actions, niveaux, apprentissage), `preset/` (modèle, sans le
  `PresetTracker` Forge), `generic/`, `FactorItem`, `TargetingUtil`, et les compétences
  électromaster, meltdowner et téléporteur qui n'envoient aucun paquet de chargeur ;
- restent côté Forge les compétences liées à un paquet encore Forge (`BloodSplashPacket`,
  `ShiftTeleportPacket`, `TeleportCritPacket`, `VecWavePacket`, `SmokePuffPacket`) ou à une entité
  Forge (`EntityMdBall`, `EntityMagHook`). Les porter débloque le reste : c'est l'étape suivante.

### Machines et liquide, sous Fabric

Deux manques qui se voyaient en jeu, et qui sont réglés :

- **les modèles OBJ.** Quatre machines dessinées par un fichier `.obj` (génératrice solaire, machine
  à phase, pied et nacelle de l'éolienne) étaient posées par le chargeur de modèle de Forge
  (`"loader": "forge:obj"`), qui n'existe pas sous Fabric : elles ne se voyaient pas du tout.
  `FabricMachineModels` les déclare comme rendus de block entity (`FabricObjRenderer`), qui relisent
  le même fichier avec `ObjModels` et le posent aux coordonnées relevées dans les modèles de bloc.
  Le matrix passe par `MatrixRenderer`, déjà commun, et l'éolienne par `FabricWindgenMainRenderer`,
  qui dessine le corps puis les pales : `WindgenMainRenderer` est passé en `common/` lui aussi, son
  seul appel Forge (le signalement des blocs ombrés à Oculus) devenant le crochet
  `PlatformHooks.trackShadowCaster` ;
- **l'imag phase liquide** n'était pas déclaré du tout : l'unité de phase se posait comme un objet
  inerte. `FabricFluids` déclare le fluide, ses deux variantes, son bloc et l'unité qui le pose, et
  son rendu (l'image `block/black`) est enregistré dans l'initialiseur client. Le comportement du
  bloc — éteindre le feu — est commun (`PhaseLiquidDousing`). **Limite assumée** : la viscosité, la
  densité, la nage et la descente recopiée de la 1.12.2 sont des déclarations de `FluidType` Forge ;
  sous Fabric le corps suit le déplacement de l'eau.

### Ce qui tourne sous Fabric aujourd'hui

- **les aptitudes**, des deux côtés du réseau : `AbilityTicker` (tick, mort, dégâts, maintiens) est
  commun, `AbilityClientEvents` aussi (touches, présets, charges du client), et `FabricAbilityEvents`
  donne au ticker les événements de Fabric. Les effets se dessinent (`FabricClientRenders`) et le HUD
  est là (`AbilityHudOverlay`, sauf le lecteur média) ;
- **le contenu** : 46 recettes et 13 succès, les quatre facteurs d'induction, le grappin,
  l'installeur de terminal, le tutoriel, l'unité vide et l'unité de phase, les machines et le liquide ;
- **`/aim` et `/aimp`**, et les succès (`AcademyAdvancements`, déclencheurs posés par réflexion).

**Limites assumées, à traiter plus tard :**

- `ALLOW_DAMAGE` de Fabric ne laisse pas réécrire le montant : les marques de radiation et les
  veilles qui *réduisent* les dégâts ne s'appliquent pas encore (les refus, eux, passent) ;
- pas d'événement pour le réveil dans un lit, la protection de chute, ni le tick des vivants ;
- le **lecteur média** reste chez Forge : son moteur lit le flux sonore par
  `Sound#getStream(SoundBufferLibrary, Sound, boolean)`, une méthode que seul Forge ajoute. Il en va
  de même des écrans du terminal, qui l'utilisent ;
- les **machines développeur**, le **brouilleur** et le **développeur portable** ne sont pas
  enregistrés sous Fabric : leurs recettes et leurs succès sont exclus du jar, et les deux tags
  vanilla `mineable/pickaxe` et `needs_iron_tool` se plaignent de les nommer (avertissements sans
  conséquence) ;
- JEI et CraftTweaker sont des intégrations Forge ; Fabric demanderait REI/EMI, hors périmètre.

## Ordre de portage

1. **Socle de plateforme dans `common/`**
   - interface de registre, de config, de réseau, d'événements, de stockage d'objets/énergie/fluides ;
   - implémentations Forge/NeoForge existantes derrière ces interfaces ;
   - implémentations Fabric au fur et à mesure.

2. **Contenu**
   - tous les blocs/objets, y compris machines, outils, unité d'énergie, facteurs ;
   - block entities, menus, sons, entités, particules ;
   - onglet créatif complet.

3. **Stockage et capacités**
   - `IItemHandler` → inventaire maison ou `fabric-transfer-api-v1` ;
   - `IEnergyStorage` → interface maison (mêmes méthodes) ;
   - `IFluidHandler`/`FluidStack` → abstraction de fluide maison + rendu Fabric ;
   - `LazyOptional` → `Optional`/référence directe.

4. **Événements et données joueur**
   - bus Forge → registre d'événements maison alimenté par les callbacks Fabric ;
   - données d'aptitude, presets, terminal, tutoriel : attachement persistant au joueur
     (mixin NBT ou équivalent), car Fabric 1.20.1 n'a pas d'API d'attachement standard.

5. **Réseau**
   - `SimpleChannel` → `ServerPlayNetworking`/`ClientPlayNetworking` avec les mêmes messages ;
   - garder la logique de sérialisation dans `common/`.

6. **Configuration**
   - `ForgeConfigSpec` → abstraction `ConfigSpec` maison, lecture/écriture TOML ;
   - deux fichiers actuels : `Config` et `HudConfig`.

7. **Client**
   - overlays HUD Forge → `HudRenderCallback` ;
   - `RenderLevelStageEvent` → `WorldRenderEvents` ;
   - `EntityRenderersEvent` → `EntityRendererRegistry` ;
   - écrans/menus : mêmes classes vanilla, seul l'enregistrement change.

8. **Intégrations**
   - JEI Fabric (`jei-1.20.1-fabric`) ;
   - CraftTweaker n'a pas de version Fabric : l'intégration sera désactivée sous Fabric.

9. **Quilt**
   - livrer le jar Fabric ; Quilt Loader lit `fabric.mod.json`.

## Règles pour ne pas diverger

- Toute logique qui ne touche pas une API de chargeur va dans `common/`.
- Un module de chargeur ne contient que : point d'entrée, registres, callbacks, implémentations
  de services.
- Pas de copie maintenue à la main entre `forge/`, `neoforge/` et `fabric/`.
- Le filtre `prepareNeoForgeSources` de `neoforge/` disparaîtra quand `common/` portera les
  classes neutres.
