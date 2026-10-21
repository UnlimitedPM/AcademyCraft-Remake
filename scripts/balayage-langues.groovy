// Balayage des interfaces : ouvre chaque machine dans chaque langue et releve les textes qui ne
// tiennent pas dans leur boite.
//
// A quoi ca sert : la largeur d'un libelle vient des documents de l'original, ecrits pour l'anglais.
// Une traduction plus longue deborde, et c'est le joueur qui le voit. Ce script le dit avant lui :
// il ouvre les neuf machines dans les cinq langues, mesure chaque texte (widget par widget : la
// largeur du texte contre celle de sa boite) et ecrit son rapport dans run/balayage.txt.
//
// COMMENT LE LANCER (developpement seulement) :
//   1. lancer le client de developpement et charger un monde (l'ecran de titre suffit : le script
//      charge « Test World » lui-meme s'il n'y a pas de monde) ;
//   2. lancer ce script dans le DebugBridge (le pont ecoute en WebSocket sur 9876) ;
//   3. lire run/balayage.txt une minute et demie plus tard.
// Il pose ses blocs devant le joueur et les remet en place ensuite.
//
// Deux pieges payes ici :
//  - `mc.level.setBlockAndUpdate` ecrit sur le niveau CLIENT : le serveur ne voit rien, donc son
//    `use()` ne s'execute pas et aucun ecran ne s'ouvre. Il faut poser le bloc par le SERVEUR.
//  - la fenetre du jeu n'a pas le focus (on pilote depuis un terminal) : sans
//    `pauseOnLostFocus = false`, Minecraft ouvre son ecran de pause et le balayage ne mesure que lui.
//  - et `mc.options.languageCode = lang` ne change RIEN a lui seul : c'est le LanguageManager qui
//    porte la langue choisie, et c'est lui qu'il faut appeler avant de recharger les ressources.
def mc = net.minecraft.client.Minecraft.getInstance()
if (mc.level == null) { mc.execute { mc.createWorldOpenFlows().loadLevel(null, "Test World") }; Thread.sleep(20000) }
def server = mc.getSingleplayerServer()
def sp = server.getPlayerList().getPlayers().get(0)
def sl = sp.serverLevel()
def base = sp.blockPosition()
def out = new File(mc.gameDirectory, "balayage.txt")
out.text = ""

def blocks = [
  "academy:metal_former", "academy:imag_fusor", "academy:node_basic",
  "academy:ability_interferer", "academy:dev_normal", "academy:phase_gen",
  "academy:solar_gen", "academy:windgen_base", "academy:matrix"
]
def langs = ["en_us", "fr_fr", "de_de", "es_es", "zh_cn"]

/** La langue du joueur, remise a la fin : le balayage ne doit pas la lui changer. */
def initialLanguage = mc.getLanguageManager().getSelected()

def log = { String line -> out.append(line + "\n"); println line }

def body = {
  def blockRegistry = sl.registryAccess().registryOrThrow(
      net.minecraft.core.registries.Registries.BLOCK)

  def walkReport
  walkReport = { w, depth, found ->
    if (w == null || depth > 8 || found.size() > 60) return
    try {
      if (w.text != null && w.text.content != null && !w.text.content.isEmpty() && w.transform.doesDraw) {
        int cell = cn.academy.client.gui.AcademyFonts.cell(w.text.fontSize * w.absScale)
        int tw = cn.academy.client.gui.AcademyFonts.width(mc.font, w.text.content, cell)
        int box = (int) Math.round(w.absW)
        if (tw > box) found << (w.name + " boite=" + box + " texte=" + tw + " [" + w.text.content + "]")
      }
    } catch (Throwable t) { }
    for (def child : w.children()) walkReport(child, depth + 1, found)
  }

  def measure = { screen ->
    def view = null
    for (Class c = screen.getClass(); c != null; c = c.superclass) {
      try { def f = c.getDeclaredField("view"); f.setAccessible(true); view = f.get(screen); break }
      catch (NoSuchFieldException e) { }
    }
    def found = []
    if (view != null) walkReport(view.root, 0, found)
    return found
  }

  for (lang in langs) {
    if (mc.getLanguageManager().getSelected() != lang) {
      // `options.languageCode` ne suffit pas : c'est le LanguageManager qui porte la langue
      // choisie, et `reloadResourcePacks` la relit (`onResourceManagerReload`). Sans
      // `setSelected`, le balayage mesurait cinq fois la meme langue — la mienne.
      mc.execute {
        mc.getLanguageManager().setSelected(lang)
        mc.options.languageCode = lang
        mc.reloadResourcePacks()
      }
      Thread.sleep(5000)
    }
    log("=== " + lang)
    int i = 0
    for (id in blocks) {
      i++
      def pos = base.offset(2, 0, -4 + i)
      def block = blockRegistry.get(net.minecraft.resources.ResourceLocation.tryParse(id))
      if (block == null) { log("  " + id + " : bloc inconnu"); continue }

      // JAMAIS SUR UN BLOC EXISTANT. Un emplacement occupe, et la machine qu'on y pose
      // remplace ce qui s'y trouvait — une partie d'une autre machine, par exemple. C'est
      // arrive : le balayage a pose un bloc sur la base d'une eolienne du joueur, la base a
      // pose sa moitie haute dans le pilier du dessus, et la colonne s'est retrouvee trouee —
      // l'eolienne ne tournait plus (`complete` faux). Quatre cases d'air au-dessus, donc.
      def clear = true
      for (int dy = 0; dy <= 3; dy++) {
        if (!sl.getBlockState(pos.above(dy)).isAir()) clear = false
      }
      if (!clear) { log("  " + id + " : emplacement occupe, saute"); continue }

      server.execute { sl.setBlockAndUpdate(pos, block.defaultBlockState()) }
      Thread.sleep(250)
      def hit = new net.minecraft.world.phys.BlockHitResult(
        net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false)
      mc.execute { mc.setScreen(null); mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit) }
      Thread.sleep(700)
      def screen = mc.screen
      if (screen == null || screen.getClass().getSimpleName().equals("PauseScreen")) {
        log("  " + id + " : aucun ecran (" + (screen == null ? "null" : "pause") + ")")
      } else {
        def overflows = measure(screen)
        log("  " + id + " -> " + screen.getClass().getSimpleName() + " : " + overflows.size() + " debordement(s)")
        for (def line : overflows) log("      " + line)
      }
      server.execute { sl.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()) }
      Thread.sleep(200)
    }
  }

  // Et on rend sa langue au joueur.
  mc.execute {
    mc.getLanguageManager().setSelected(initialLanguage)
    mc.options.languageCode = initialLanguage
    mc.reloadResourcePacks()
  }
  Thread.sleep(3000)
  log("FIN (langue rendue : " + initialLanguage + ")")
}
mc.execute { mc.options.pauseOnLostFocus = false }
Thread.sleep(300)
Thread.start {
  try { body() } catch (Throwable t) { log("ERREUR " + t) }
}
println "BALAYAGE LANCE"
