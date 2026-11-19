package cn.academy.neoforge;

import cn.academy.AcademyCraftCommon;
import net.minecraftforge.fml.common.Mod;

/**
 * Point d'entree NeoForge 1.20.1.
 *
 * <p>NeoForge 1.20.1 (47.x) est un fork direct de Forge 47 : il conserve les packages
 * net.minecraftforge, seule l'implementation change. Le code Forge pourra donc etre repris ici
 * avec tres peu de modifications, ce qui en fait le premier chargeur a porter.
 */
@Mod(AcademyCraftCommon.MOD_ID)
public final class AcademyCraftNeoForge {
    public AcademyCraftNeoForge() {
        AcademyCraftCommon.init();
    }
}
