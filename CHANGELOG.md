# Changelog

## 0.1.2 - le formeur et ses recettes / the former and its recipes

### English

All of 0.1.1, plus two things.

- **The metal former's mode arrows work.** They were wired into nothing: the engine looked up a page widget through the view, and during the screen's build the view does not know the pages yet — they are only placed in the tree afterwards. Every screen that wires something at that moment was concerned, not just this one.
- **The metal former knows its recipes again.** It had the original's four; it now has all seventeen explicit ones — cutting silicon into wafers and pieces, etching a data chip into a calc chip, plating iron and constraint ingot, refining its four ores — plus the **refining rule**: every ore that smelts comes back at double. That covers the mod's ores, vanilla's, and any other mod's metals, because it reads the game's own smelting recipes instead of a hand-written list. The original's two exceptions are kept: redstone gives a block, lapis gives twelve.
- A flaky in-game test (a thrown iron block landing) was given a wider budget, so the suite stops reporting a failure that was only impatience.
- **Tests**: 158 in-game tests, 825 unit tests.

### Francais

Tout le 0.1.1, plus deux choses.

- **Les fleches de mode du formeur de metal fonctionnent.** Elles se cablaient dans le vide : le moteur cherchait un widget de page a travers la vue, et pendant la construction de l'ecran la vue ne connait pas encore les pages — elles ne sont posees dans l'arbre qu'apres. Tous les ecrans qui cablent quelque chose a ce moment-la etaient concernes, pas seulement celui-ci.
- **Le formeur connait ses recettes.** Il n'en avait que les quatre de l'original ; il a maintenant les dix-sept explicites — tailler le silicium en tranches et en morceaux, graver une puce de donnees en puce de calcul, plaquer le fer et le lingot de contrainte, raffiner ses quatre minerais — plus la **regle de raffinage** : tout minerai qui fond revient au double. Cela couvre les minerais du mod, ceux de vanilla et ceux des metaux des autres mods, parce qu'elle lit les recettes de fonte du jeu au lieu d'une liste ecrite a la main. Les deux exceptions de l'original sont gardees : le redstone donne un bloc, le lapis en donne douze.
- Un test en jeu instable (le lancer d'un bloc de fer qui retombe) a recu un budget plus large : la suite ne signale plus un echec qui n'etait que de l'impatience.
- **Des tests** : 158 tests en jeu, 825 tests unitaires.

## 0.1.1 - le developpeur ouvre la porte / the developer opens the door

Tout le 0.1.0, plus ce que le tester en vrai a fait sortir. Rien n'a ete retire.

### English

All of 0.1.0, plus what testing it as a real player brought out. Nothing was removed.

- **A first ability can be obtained now.** The port had no way in: the developer opened on an empty tree, and the only door was a debug command nobody knows. Its **LEARN** button gives a first ability outright, without waiting for the machine's energy — with an **induction factor** in the inventory, that factor decides the kind and is consumed; without one, the kind is drawn at random. `/learn` in chat does exactly the same thing.
- **The developer's console** says what the original's says: Academy OS booting, the failure, the verdict, and the command to type.
- **Two interface engine fixes**, both of which were hiding something the player could not see: a button that overflows its parent is clickable now — the LEARN button never was, because it is twice as wide as its panel — and a text is only drawn in a box that has a size, so every line of a skill's page (its name, its experience, its conditions, its cost) was invisible.
- **The README explains how to play**: how to get an ability, where to put the skills you learn, and which keys do what — left click, right click, R and F for the four slots, C for presets, N for the preset editor, V to turn the ability on and off, F4 for the debug panel, Left Alt for the terminal.
- **Tests**: 157 in-game tests and around 825 unit tests, two of which pin the new click rule.

### Francais

Tout le 0.1.0, plus ce que le tester en vrai a fait sortir. Rien n'a ete retire.

- **Une premiere aptitude s'obtient maintenant.** Le port n'avait aucune porte d'entree : le developpeur s'ouvrait sur un arbre vide, et la seule commande etait une commande de debogage que personne ne connait. Son bouton **LEARN** donne la premiere aptitude tout de suite, sans attendre l'energie de la machine — avec un **facteur d'induction** dans l'inventaire, c'est lui qui decide du type et il est consomme ; sans facteur, le type est tire au sort. La commande `/learn` fait exactement la meme chose.
- **La console du developpeur** dit ce que dit celle de l'original : le demarrage d'Academy OS, l'echec, le verdict, et la commande a taper.
- **Deux corrections du moteur d'interface**, qui cachaient toutes les deux quelque chose que le joueur ne pouvait pas voir : un bouton qui deborde de son parent se clique maintenant — celui de LEARN ne se cliquait pas, il fait deux fois la largeur de son panneau — et un texte ne se dessine que dans une boite qui a une taille, donc toutes les lignes d'une page de competence (son nom, son experience, ses conditions, son cout) etaient invisibles.
- **Le README explique comment jouer** : obtenir une aptitude, ranger les competences apprises, et ce que fait chaque touche — clic gauche, clic droit, R et F pour les quatre emplacements, C pour les presets, N pour l'editeur, V pour allumer et eteindre, F4 pour le panneau de debogage, Alt gauche pour le terminal.
- **Des tests** : 157 tests en jeu et environ 825 tests unitaires, dont deux qui figent la nouvelle regle du clic.

## 0.1.0 - premiere version de test / first test build

### English

First public build. It exists to be tested: everything below works, the rest is listed in the README.

- **The original's GUI engine**, re-implemented inside the mod, with the original's own layout documents reused as they are. The ten machine screens (solar, wind, phase, imag fusor, metal former, wireless node and matrix, ability interferer, developer) and the ability developer's skill tree are drawn by it.
- **The four ability categories and their 35 skills**, plus the three generic courses: control points, overload, per-skill experience, charges, cooldowns, key bindings, presets, and the ability HUD.
- **The Imag energy network**: solar, wind and phase generators, the wireless nodes and the matrix, the imag fusor, the metal former, the ability interferer.
- **The data terminal**, its applications (settings, skill tree, media player, about) and the tutorial.
- **The world content**: the mod's ores, the Imag phase liquid, and the advancements.
- **Seven bugs of the original fixed**, found while reading it: a knockback that was applied twice, a search that could pick the wrong target, and a few misspelled interface labels.
- **Tests**: around 820 unit tests, 156 in-game tests and an offline asset validator, so the numbers of the original stay the numbers of the original.

### Francais

Premiere version publique. Elle existe pour etre testee : tout ce qui suit fonctionne, le reste est liste dans le README.

- **Le moteur d'interface de l'original**, reimplemente dans le mod, avec les documents de mise en page de l'original reutilises tels quels. Les dix ecrans de machine (solaire, eolien, de phase, fusor d'imag, formeur de metal, noud sans fil et matrice, interfereur d'aptitude, developpeur) et l'arbre du developpeur d'aptitude sont dessines par lui.
- **Les quatre categories d'aptitudes et leurs 35 competences**, avec les trois cursus generiques : points de controle, surcharge, experience par competence, charges, recharges, touches, presets et affichage tete haute.
- **Le reseau d'energie imag** : generateurs solaire, eolien et de phase, nouds sans fil et matrice, fusor d'imag, formeur de metal, interfereur d'aptitude.
- **Le terminal de donnees**, ses applications (reglages, arbre de competences, lecteur media, a propos) et le tutoriel.
- **Le contenu de monde** : les minerais du mod, le fluide de phase imag et les succes.
- **Sept bugs de l'original corriges**, trouves en le lisant : un recul applique deux fois, une recherche qui pouvait viser la mauvaise cible, et quelques libelles d'interface mal orthographies.
- **Des tests** : environ 820 tests unitaires, 156 tests en jeu et un validateur de ressources, pour que les nombres de l'original restent ceux de l'original.