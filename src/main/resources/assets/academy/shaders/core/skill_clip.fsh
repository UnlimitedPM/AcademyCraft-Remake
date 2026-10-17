#version 150

// L'icone d'un noeud, ECRETEE sur la forme de son disque.
//
// L'original obtenait cela par un tour de profondeur : il ecrivait la profondeur avec la forme du
// disque (test d'alpha a 0,3), puis dessinait l'icone avec un test EQUAL — seuls les pixels ou le
// disque avait ecrit passaient. Le port ne pouvait pas faire cela avec ses batches, alors il le fait
// ici : Sampler1 est le disque, et son canal alpha multiplie l'icone.
//
// C'est ce qui manquait et ce que le joueur voyait : « l'image des competences deborde des cercles »,
// avec « des lignes noires entre la couleur du cercle et les bordures du carre de l'image » — les
// coins de l'icone, carres, sortaient du cercle.
//
// MaskScale et MaskOffset placent l'icone dans le disque : l'icone fait quatorze unites au milieu du
// disque qui en fait vingt-trois, donc ses coordonnees de texture vont de 5/23 a 18/23 de celles du
// disque. Grey eteint l'icone d'une competence non apprise, comme le faisait le programme mono de
// l'original.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
uniform vec2 MaskScale;
uniform vec2 MaskOffset;
uniform float Grey;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 icon = texture(Sampler0, texCoord0);
    float mask = texture(Sampler1, texCoord0 * MaskScale + MaskOffset).a;
    // Le disque sert de POCHOIR, pas de voile : on rejette ce qui tombe hors de lui, et l'icone
    // garde son opacite. Multiplier l'alpha par celui du disque l'assombrissait, parce que le
    // disque lui-meme est a moitie translucide : le joueur : « maintenant l'icone est assombrie ».
    // C'est le test d'alpha de l'original, au meme seuil : GL_GREATER, 0.3.
    if (mask < 0.3) {
        discard;
    }
    if (Grey > 0.5) {
        icon.rgb = vec3((icon.r + icon.g + icon.b) / 3.0);
    }
    fragColor = icon * vertexColor * ColorModulator;
}
