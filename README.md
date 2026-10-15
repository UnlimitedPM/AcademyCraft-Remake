<p align="center">
  <img src="src/main/resources/academy_logo.png" alt="AcademyCraft" width="120">
</p>

<h1 align="center">AcademyCraft Remake</h1>

<p align="center">
  <b>AcademyCraft for Minecraft 1.20.1</b> — the 1.12.2 classic, rebuilt: superpowers, an Imag energy<br>
  network, ten machines and a skill tree.
</p>

<p align="center">
  Minecraft 1.20.1 &nbsp;·&nbsp; Forge 47.4.20+ &nbsp;·&nbsp; Java 17 &nbsp;·&nbsp; GPLv3 &nbsp;·&nbsp; <b>0.1.0 (test build)</b>
</p>

---

## English

**AcademyCraft Remake** is a port of [AcademyCraft](https://github.com/LambdaInnovation/AcademyCraft) (Minecraft 1.12.2) to **Minecraft 1.20.1 / Forge**. Superpowers, an energy network, machines, a skill tree: the mod is the same, the game under it is not.

The original drew its interfaces with LambdaLib2's own GUI engine, and the port re-implements that engine inside the mod. The original's layout documents are reused as they are, so the screens look like the real thing instead of a look-alike.

### Download and install

1. Install **Forge 47.4.20 or later** for Minecraft 1.20.1.
2. Download **`academy-0.1.0.jar`** from the [Releases](../../releases) page.
3. Drop it into your `mods` folder and start the game.

Nothing else to install. The port works on a server too, but it is a **test build**: keep a backup of your world.

### What is in this first version

| | |
|---|---|
| **Abilities** | the four categories and their 35 skills — electromaster (railgun, thunder clap, magnet manipulation...), meltdowner (rays, jet engine...), teleporter (marking, shift, flesh ripping...), vecmanip (directional shock, plasma cannon...) — plus the three generic courses, with control points, overload, per-skill experience, charges and cooldowns |
| **Skill tree** | the Ability Developer machine: the original's tree, its icons, its descriptions, its levels, its costs, and the magnetic coil that resets a category |
| **Energy** | the Imag network: solar, wind and phase generators, the wireless nodes and the matrix, the imag fusor, the metal former |
| **Terminal** | the data terminal and its apps (settings, skill tree, media player, about) plus the tutorial |
| **Interface** | the ability HUD, key hints, notifications, presets, the F4 debug console, and the ten machine screens on the rebuilt engine |
| **World** | the mod's ores, the Imag phase liquid, the advancements |

### What is still missing

Honest list, so you know what you are testing:

- **Metal former REFINE recipes** — the mode exists but is empty. In the original it was the ore multiplier: 2 to 4 times more from the mod's ores, and twice as much from every vanilla and metal ore.
- **Wireless tab** — SSID, password and the node list have no screen yet; the nodes find a matrix by themselves.
- **Frequency transmitter** — the item is decorative for now.
- **Languages** — only English ships today. The original also had Japanese, Korean, Russian, Simplified and Traditional Chinese.
- **Media player** — it plays its three built-in tracks, but without covers and without a progress bar.
- **Per-skill configuration** — you cannot disable a skill or tune its cost yet.
- **JEI and CraftTweaker** integration, and **EU (IC2) / RF conversion** blocks.

### Building from source

Needs **JDK 17** (Gradle 8.1 and Forge 1.20.1 refuse newer ones):

```
./gradlew build        # jar in build/libs/academy-<version>.jar
./gradlew runClient    # a development client
./gradlew test         # unit tests
./gradlew runGameTestServer   # in-game tests, headless
```

On Windows, run `gradlew.bat`. If Java 17 is not your default, set `JAVA_HOME` first — the project also pins it in `gradle.properties` (`org.gradle.java.home`).

### Credits

The original **AcademyCraft** was made by **WeAthFolD** and **Lambda Innovation**, with [LambdaLib2](https://github.com/LambdaInnovation/LambdaLib2). Its assets, its layout documents, its numbers and its behaviour are what this port is built from — thank you for a mod we are still playing.

### License

**GPLv3**, like the original. See `LICENSE.txt`.

---

## Français

**AcademyCraft Remake** est le portage d'[AcademyCraft](https://github.com/LambdaInnovation/AcademyCraft) (Minecraft 1.12.2) vers **Minecraft 1.20.1 / Forge**. Les superpouvoirs, le réseau d'énergie, les machines et l'arbre de compétences sont les mêmes ; le jeu en dessous, non.

L'original dessinait ses interfaces avec le moteur d'interface de LambdaLib2 ; le port réimplémente ce moteur dans le mod. Les documents de mise en page de l'original sont réutilisés tels quels, donc les écrans ressemblent au vrai mod et non à une imitation.

### Télécharger et installer

1. Installez **Forge 47.4.20 ou plus** pour Minecraft 1.20.1.
2. Téléchargez **`academy-0.1.0.jar`** depuis la page [Releases](../../releases).
3. Posez-le dans votre dossier `mods` et lancez le jeu.

Rien d'autre à installer. Le port fonctionne aussi sur un serveur, mais c'est une **version de test** : gardez une sauvegarde de votre monde.

### Ce que contient cette première version

Les quatre catégories d'aptitudes et leurs 35 compétences (electromaster, meltdowner, teleporter, vecmanip) et les trois cursus génériques, avec points de contrôle, surcharge, expérience par compétence, charges et recharges ; l'arbre du développeur d'aptitude avec ses icônes, ses descriptions, ses niveaux et ses coûts ; le réseau d'énergie imag (générateurs solaire, éolien et de phase, nœuds et matrice sans fil, fusor d'imag, formeur de métal) ; le terminal de données et ses applications, plus le tutoriel ; l'affichage tête haute, les préset, la console F4, et les dix écrans de machine sur le moteur reconstruit ; les minerais du mod, le fluide de phase imag et les succès.

### Ce qu'il manque encore

- **Les recettes REFINE du formeur de métal** : le mode existe mais il est vide. Chez l'original, c'était le multiplicateur de minerais (2 à 4 fois plus sur les minerais du mod, et le double sur tous les minerais vanilla et des métaux).
- **L'onglet sans fil** : SSID, mot de passe et liste des nœuds n'ont pas encore d'écran.
- **L'émetteur de fréquence** : l'objet est décoratif pour le moment.
- **Les langues** : l'anglais seulement. L'original en avait six.
- **Le lecteur média** : il joue ses trois morceaux, mais sans pochettes ni barre de progression.
- **La configuration par compétence**, l'intégration **JEI** et **CraftTweaker**, et les blocs de conversion **EU (IC2) / RF**.

### Compiler depuis les sources

Il faut **JDK 17** (Gradle 8.1 et Forge 1.20.1 refusent les plus récents). Les commandes sont celles de la section anglaise : `gradlew build`, `gradlew runClient`, `gradlew test`, `gradlew runGameTestServer`. Sous Windows, `gradlew.bat` ; si Java 17 n'est pas votre Java par défaut, posez `JAVA_HOME` (le projet l'épingle aussi dans `gradle.properties`).

### Crédits

L'**AcademyCraft** d'origine a été fait par **WeAthFolD** et **Lambda Innovation**, avec [LambdaLib2](https://github.com/LambdaInnovation/LambdaLib2). Ses ressources, ses documents de mise en page, ses nombres et son comportement sont ce sur quoi ce portage est bâti — merci pour un mod auquel on joue encore.

### Licence

**GPLv3**, comme l'original. Voir `LICENSE.txt`.