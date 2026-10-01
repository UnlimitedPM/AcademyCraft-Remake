package cn.academy.ability.client.md;

import cn.academy.ability.meltdowner.MeltdownerCategory;
import cn.academy.sound.HeldLoops;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les trois rayons miniers : leurs genres, leur rayon tenu, et leurs deux sortes d'etincelles.
 *
 * <p>Le minage lui-meme se relit dans {@code SkillCurvesTest}, avec les autres courbes des
 * competences. Ici, c'est ce qu'on <b>voit</b> — ce qui manquait au port, et ce que l'original
 * faisait entierement chez le client : trois entites de rayon, qui vivaient le temps du maintien
 * et crachaient leurs etincelles.
 *
 * <p>Rien n'y est invente : les nombres viennent des trois {@code RendererRayComposite} de
 * l'original — {@code BasicMineRayRender}, {@code ExpertRayRenderer} et {@code LuckRayRender} —
 * et de l'entite {@code EntityMineRayBase} dont les trois heritaient.
 */
class MineRaysTest {

    private static final long BIRTH = 100_000L;

    @Test
    @DisplayName("les trois rayons miniers sont ceux de l'original")
    void lesTroisRayonsMinierSontCeuxDeLoriginal() {
        // Le rayon de base EST le petit rayon : memes textures, memes nombres. Ce qui change,
        // c'est qu'il vit — les 233333 ticks de l'entite de l'original, soit trois heures — et
        // que c'est donc le client qui le tue, au relachement de la touche.
        assertEquals("mdray_mine_basic", MdRayKind.MINE_BASIC.name());
        assertEquals("textures/effects/mdray_small/blend_in.png",
                MdRayKind.MINE_BASIC.glowIn().getPath());
        assertEquals(0.03, MdRayKind.MINE_BASIC.innerRadius(), 1e-6);
        assertEquals(0.045, MdRayKind.MINE_BASIC.outerRadius(), 1e-6);
        assertEquals(0.3, MdRayKind.MINE_BASIC.glowWidth(), 1e-6);
        assertEquals(0.5, MdRayKind.MINE_BASIC.glowAlpha(), 1e-6);
        assertEquals(MdRayKind.HELD_TICKS, MdRayKind.MINE_BASIC.lifeTicks());
        assertTrue(MdRayKind.MINE_BASIC.lifeMs() > 3_600_000L, "plus d'une heure, donc jamais");
        assertEquals(200, MdRayKind.MINE_BASIC.blendInMs(), "deux dixiemes pour pousser");

        // Le rayon de l'expert a ses propres textures, et un coeur plus large — mais c'est le
        // DESSIN de l'original qui fait foi, et il rabaissait deux de ses nombres a chaque image :
        // l'opacite du coeur a 180 au lieu de 230, celle de la lueur a 50 % au lieu de 70.
        assertEquals("mdray_mine_expert", MdRayKind.MINE_EXPERT.name());
        assertEquals("textures/effects/mdray_expert/blend_in.png",
                MdRayKind.MINE_EXPERT.glowIn().getPath());
        assertEquals(0.045, MdRayKind.MINE_EXPERT.innerRadius(), 1e-6);
        assertEquals(180, MdRayKind.MINE_EXPERT.inner().a());
        assertEquals(0.056, MdRayKind.MINE_EXPERT.outerRadius(), 1e-6);
        assertEquals(0.5, MdRayKind.MINE_EXPERT.glowWidth(), 1e-6);
        assertEquals(0.5, MdRayKind.MINE_EXPERT.glowAlpha(), 1e-6);

        // Et celui de la chance est le meme, en dore : un coeur presque blanc et une gaine
        // violette, la ou les deux autres sont vertes.
        assertEquals("mdray_mine_luck", MdRayKind.MINE_LUCK.name());
        assertEquals("textures/effects/mdray_luck/blend_in.png",
                MdRayKind.MINE_LUCK.glowIn().getPath());
        assertEquals(0.04, MdRayKind.MINE_LUCK.innerRadius(), 1e-6);
        assertEquals(241, MdRayKind.MINE_LUCK.inner().r());
        assertEquals(229, MdRayKind.MINE_LUCK.inner().g());
        assertEquals(247, MdRayKind.MINE_LUCK.inner().b());
        assertEquals(0.05, MdRayKind.MINE_LUCK.outerRadius(), 1e-6);
        assertEquals(205, MdRayKind.MINE_LUCK.outer().r());
        assertEquals(166, MdRayKind.MINE_LUCK.outer().g());
        assertEquals(232, MdRayKind.MINE_LUCK.outer().b());
        assertEquals(0.45, MdRayKind.MINE_LUCK.glowWidth(), 1e-6);
        assertEquals(0.6, MdRayKind.MINE_LUCK.glowAlpha(), 1e-6);

        // Aucun des trois ne se dessine a la bille : ils partent des yeux de leur tireur, donc ils
        // se recollent a sa main — c'est le drapeau des rayons nes de lui.
        for (MdRayKind kind : new MdRayKind[] { MdRayKind.MINE_BASIC, MdRayKind.MINE_EXPERT,
                MdRayKind.MINE_LUCK }) {
            assertTrue(kind.viewOptimize(), kind.name() + " se recolle a la main de son tireur");
            assertEquals(0.0, kind.glowEndFix(), 1e-9, "sa lueur s'arrete a sa pointe");
            assertEquals(0.0, kind.sparkRate(), 1e-9,
                    "ses etincelles ne sont pas celles de MdRays : MineRayEffect les semme");
        }

        assertEquals(MdRayKind.MINE_BASIC, MdRayKind.byName("mdray_mine_basic"));
        assertEquals(8, MdRayKind.all().size(), "les huit genres sont connus");
    }

    @Test
    @DisplayName("le son du rayon est celui de sa boucle")
    void leSonDuRayonEstCeluiDeSaBoucle() {
        // Le rayon de l'original ne jouait aucun son : c'est le contexte client qui ouvrait une
        // boucle, `md.mine_loop`, tant que la touche restait enfoncee. Le port la tient dans
        // `HeldLoops`, avec sa competence, et le genre du rayon rappelle le meme nom et le meme
        // volume. Ce test est le seul lien entre les deux tables : sans lui, elles peuvent
        // diverger en silence.
        assertEquals("md.mine_loop", MdRayKind.MINE_BASIC.sound());
        assertEquals(0.3f, MdRayKind.MINE_BASIC.soundVolume(), 1e-6, "une boucle d'entretien");

        for (String skill : new String[] { "mine_ray_basic", "mine_ray_expert", "mine_ray_luck" }) {
            HeldLoops.Loop loop = HeldLoops.forSkill(skill);
            assertNotNull(loop, skill + " fait tourner une boucle");
            assertEquals(MdRayKind.MINE_BASIC.sound(), loop.event());
            assertEquals(MdRayKind.MINE_BASIC.soundVolume(), loop.volume(), 1e-6);
            assertFalse(loop.hasStartup(), "la mise en route, c'est la competence qui la joue");
        }
    }

    @Test
    @DisplayName("chaque rayon minier sait quelle competence il dessine")
    void chaqueRayonMinierSaitQuelleCompetenceIlDessine() {
        // C'est la seule chose que le port a traduite du contexte de l'original, qui choisissait
        // son entite a la construction de son contexte client. Le test se ferme sur lui-meme :
        // le nom rendu par le genre doit etre celui de la competence, dans les deux sens.
        assertEquals(MdRayKind.MINE_BASIC, MineRayEffect.kindOf(MeltdownerCategory.MINE_RAY_BASIC));
        assertEquals(MdRayKind.MINE_EXPERT, MineRayEffect.kindOf(MeltdownerCategory.MINE_RAY_EXPERT));
        assertEquals(MdRayKind.MINE_LUCK, MineRayEffect.kindOf(MeltdownerCategory.MINE_RAY_LUCK));

        assertEquals(MeltdownerCategory.MINE_RAY_BASIC.getName(),
                MineRayEffect.skillOf(MdRayKind.MINE_BASIC));
        assertEquals(MeltdownerCategory.MINE_RAY_EXPERT.getName(),
                MineRayEffect.skillOf(MdRayKind.MINE_EXPERT));
        assertEquals(MeltdownerCategory.MINE_RAY_LUCK.getName(),
                MineRayEffect.skillOf(MdRayKind.MINE_LUCK));

        // Une competence qui n'est pas un rayon minier n'a pas de rayon, et un genre qui n'est pas
        // un rayon minier n'a pas de competence : c'est ce qui empeche le crochet client de semer
        // le rayon d'une autre competence.
        assertNull(MineRayEffect.kindOf(MeltdownerCategory.MELTDOWNER));
        assertNull(MineRayEffect.kindOf(MeltdownerCategory.LIGHT_SHIELD));
        assertNull(MineRayEffect.skillOf(MdRayKind.SMALL));
        assertNull(MineRayEffect.skillOf(MdRayKind.MELTDOWNER));
    }

    @Test
    @DisplayName("le rayon tenu suit le regard et s'en va au relachement")
    void leRayonTenuSuitLeRegardEtSEnVaAuRelachement() {
        MdRays.clear();
        MdRays.hold(MdRayKind.MINE_BASIC, new Vec3(0, 0, 0), new Vec3(0, 0, 15), BIRTH);

        assertEquals(1, MdRays.live().size(), "un seul rayon tenu a la fois");
        assertEquals(MdRayKind.MINE_BASIC, MdRays.heldKind());

        MdRays.LiveRay ray = MdRays.live().get(0);
        // Il pousse comme les autres rayons : deux dixiemes de seconde avant d'etre entier.
        assertEquals(7.5, ray.drawnLength(BIRTH + 100), 1e-9);
        assertEquals(15.0, ray.drawnLength(BIRTH + 200), 1e-9);

        // Et il suit : le regard a tourne, le rayon suit, longueur comprise.
        MdRays.moveHeld(new Vec3(3, 1, 0), new Vec3(3, 1, 15));
        assertEquals(3.0, ray.from()[0], 1e-9);
        assertEquals(15.0, ray.length(), 1e-9);

        MdRays.moveHeld(new Vec3(3, 1, 0), new Vec3(3, 1, 5));
        assertEquals(5.0, ray.length(), 1e-9, "meme racourci d'un coup");

        // Sa vie ne le tue pas : une minute de maintien plus tard, il est toujours la.
        MdRays.tick(BIRTH + 60_000);
        assertEquals(1, MdRays.live().size());

        MdRays.releaseHeld();
        assertTrue(MdRays.live().isEmpty(), "le relachement l'emporte sur-le-champ");
        assertNull(MdRays.heldKind());
    }

    @Test
    @DisplayName("ouvrir un rayon tenu ferme le precedent, et quitter le monde les oublie")
    void ouvrirUnRayonTenuFermeLePrecedent() {
        // Le joueur ne peut tenir qu'un rayon minier a la fois — mais il peut changer de touche
        // sans relacher, et deux entites se superposeraient. L'original n'en avait qu'une, celle
        // de son contexte : c'est elle qu'on refait.
        MdRays.clear();
        MdRays.hold(MdRayKind.MINE_BASIC, Vec3.ZERO, new Vec3(0, 0, 15), BIRTH);
        MdRays.hold(MdRayKind.MINE_LUCK, Vec3.ZERO, new Vec3(0, 0, 15), BIRTH);

        assertEquals(1, MdRays.live().size());
        assertEquals(MdRayKind.MINE_LUCK, MdRays.heldKind());

        // Et quitter un monde emporte tout : un rayon tenu ne meurt jamais tout seul, donc sans
        // cet oubli il se dessinerait encore dans le monde suivant.
        MdRays.clear();
        assertTrue(MdRays.live().isEmpty());
        assertNull(MdRays.heldKind(), "le prochain maintien en rouvrira un");
    }

    @Test
    @DisplayName("seules les etincelles du bloc tombent")
    void seulesLesEtincellesDuBlocTombent() {
        // L'original donnait a ses etincelles de bloc la gravite de sa `Rigidbody`, 0,01 bloc par
        // tick au carre, et la laissait a zero partout ailleurs — celles du rayon continuent donc
        // droit devant elles, et seules celles du bloc tombent.
        MdSparks.clear();
        long now = 5_000L;
        MdSparks.spawn(new Vec3(0, 64, 0), Vec3.ZERO, MineRayEffect.BLOCK_SPARK_GRAVITY,
                MdSparks.PLAIN, now, new Random(1));
        MdSparks.spawn(new Vec3(0, 64, 0), Vec3.ZERO, now, new Random(2));

        MdSparks.tick(now + 50);
        MdSparks.tick(now + 100);

        MdSparks.Spark falling = MdSparks.live().get(0);
        MdSparks.Spark floating = MdSparks.live().get(1);
        assertEquals(63.97, falling.pos()[1], 1e-9, "0,01 puis 0,02 sous la position de depart");
        assertEquals(64.0, floating.pos()[1], 1e-9, "et celle du rayon ne bouge pas en hauteur");
        assertEquals(MdSparks.PLAIN, floating.texture(), "les deux rayons ordinaires crachent la bille");
    }

    @Test
    @DisplayName("le rayon de la chance a sa propre etincelle")
    void leRayonDeLaChanceASaPropreEtincelle() {
        // L'original remplacait la texture des etincelles de son rayon de la chance par une etoile
        // a quatre branches — la seule entite de tout le plasma a le faire. Le reste du port se
        // contente de la bille, et c'est pour cela qu'un genre doit dire laquelle des deux.
        assertEquals("textures/effects/md_particle.png", MdSparks.PLAIN.getPath());
        assertEquals("textures/effects/md_particle_luck.png", MdSparks.LUCK.getPath());

        assertEquals(MdSparks.LUCK, MineRayEffect.sparkTexture(MdRayKind.MINE_LUCK));
        assertEquals(MdSparks.PLAIN, MineRayEffect.sparkTexture(MdRayKind.MINE_BASIC));
        assertEquals(MdSparks.PLAIN, MineRayEffect.sparkTexture(MdRayKind.MINE_EXPERT));

        // Et les deux autres en lachent plus souvent que lui : une chance sur deux contre trois
        // sur cinq, comme leurs entites.
        assertEquals(0.5, MineRayEffect.sparkChance(MdRayKind.MINE_BASIC), 1e-9);
        assertEquals(0.6, MineRayEffect.sparkChance(MdRayKind.MINE_EXPERT), 1e-9);
        assertEquals(0.6, MineRayEffect.sparkChance(MdRayKind.MINE_LUCK), 1e-9);
    }

    @Test
    @DisplayName("les etincelles du bloc sont celles de l'original")
    void lesEtincellesDuBlocSontCellesDeLoriginal() {
        // Trois par tick, dans la boite du bloc un peu debordante — de -0,2 a 1,2 bloc —, et
        // poussees a six centimetres par tick : c'est le `ranged(-.06, .06)` de l'original.
        assertEquals(3, MineRayEffect.BLOCK_SPARKS);
        assertEquals(-0.2, MineRayEffect.BLOCK_OFFSET_MIN, 1e-9);
        assertEquals(1.2, MineRayEffect.BLOCK_OFFSET_MAX, 1e-9);
        assertEquals(0.06, MineRayEffect.BLOCK_SPARK_SPEED, 1e-9);
        assertEquals(0.01, MineRayEffect.BLOCK_SPARK_GRAVITY, 1e-9);

        // Le rayon, lui, dessine quinze blocs et creuse a dix ou vingt : deux nombres differents,
        // et c'est l'original qui les voulait tels. Ses etincelles se posent sur les dix premiers.
        assertEquals(15.0, MineRayEffect.BEAM_LENGTH, 1e-9);
        assertEquals(10.0, MineRayEffect.SPARK_REACH, 1e-9);
        assertEquals(0.03, MineRayEffect.SPARK_SPEED, 1e-9, "trois centimetres, pas un et demi");
    }
}
