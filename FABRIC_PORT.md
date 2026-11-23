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
- les 47 événements sonores de `ModSounds`, mêmes identifiants que `sounds.json` ;
- un onglet créatif `itemGroup.academy_tab` ;
- les assets du mod (textures, modèles, langues) sont repris de `forge/src/main/resources` ;
- les `data/` (recettes, succès, tags, worldgen) sont volontairement exclus tant que les objets
  référencés n'existent pas sous Fabric.

Vérifié : le client Fabric démarre, un monde se charge, le contenu est enregistré.

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
