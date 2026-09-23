#!/usr/bin/env python3
"""Extrait une police d'une collection .ttc vers un .ttf simple.

Pourquoi cet outil existe : l'original dessine tout son texte avec une police du
systeme (Microsoft YaHei par defaut), et cette police n'existe chez Windows que sous
forme de collection, `msyh.ttc`. Or le chargeur de police de Minecraft appelle
`stbtt_InitFont(info, buffer)` **a l'offset 0** (voir `TrueTypeGlyphProviderDefinition`) :
sur un .ttc, l'offset 0 tombe sur l'entete de collection, pas sur une police, donc le
chargement echoue.

Le format d'une collection est simple : un entete `ttcf`, puis un decalage par police,
chaque decalage pointant sur une table de repertoires de la forme habituelle d'un .ttf.
Les tables sont partagees et leurs decalages sont absolus dans le fichier de collection.
Extraire une police = recopier son repertoire en redecalant ses tables a la suite.

Usage : python scripts/ttc-to-ttf.py <source.ttc> <dest.ttf> [index]
"""

import struct
import sys


def extraire(source, destination, index=0):
    with open(source, "rb") as fichier:
        donnees = fichier.read()

    if donnees[:4] != b"ttcf":
        # Deja une police simple : rien a faire, on recopie.
        with open(destination, "wb") as fichier:
            fichier.write(donnees)
        return len(donnees), 1

    nb_polices = struct.unpack(">I", donnees[8:12])[0]
    if not 0 <= index < nb_polices:
        raise SystemExit(
            "index %d hors bornes : la collection en contient %d" % (index, nb_polices)
        )

    debut = struct.unpack(">I", donnees[12 + 4 * index : 16 + 4 * index])[0]
    entete = donnees[debut : debut + 12]
    nb_tables = struct.unpack(">H", donnees[debut + 4 : debut + 6])[0]

    tables = []
    for i in range(nb_tables):
        enregistrement = donnees[debut + 12 + i * 16 : debut + 12 + (i + 1) * 16]
        tag, somme, decalage, longueur = struct.unpack(">4sIII", enregistrement)
        tables.append((tag, somme, decalage, longueur))

    # Les tables sont remises a la suite du repertoire, alignees sur 4 octets, et le
    # repertoire pointe desormais sur ces nouvelles places.
    position = len(entete) + nb_tables * 16
    corps = bytearray()
    nouveau_repertoire = bytearray()
    for tag, somme, decalage, longueur in tables:
        while position % 4:
            corps += b"\0"
            position += 1
        nouveau_repertoire += struct.pack(">4sIII", tag, somme, position, longueur)
        corps += donnees[decalage : decalage + longueur]
        position += longueur

    with open(destination, "wb") as fichier:
        fichier.write(entete)
        fichier.write(bytes(nouveau_repertoire))
        fichier.write(bytes(corps))

    return position, nb_polices


def main():
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    index = int(sys.argv[3]) if len(sys.argv) > 3 else 0
    taille, nb_polices = extraire(sys.argv[1], sys.argv[2], index)
    print(
        "%s -> %s : %d octets extraits (police %d sur %d)"
        % (sys.argv[1], sys.argv[2], taille, index, nb_polices)
    )


if __name__ == "__main__":
    main()
