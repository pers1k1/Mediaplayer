package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;

// WHY: каждая общая часть острова стоит в двух местах сразу, в таблетке и в карточке, и рисуется
// WHY: между ними по доле формы: обложка растёт, название едет и меняет кегль, полоса утолщается
record IslandFlight(IslandScene scene, float pillX, float pillY, float cardX, float cardY, float share, float blur) {

    float x(float pillOffset, float cardOffset) {
        return Anim.lerp(pillX + pillOffset, cardX + cardOffset, share);
    }

    float y(float pillOffset, float cardOffset) {
        return Anim.lerp(pillY + pillOffset, cardY + cardOffset, share);
    }

    float mix(float pill, float card) {
        return Anim.lerp(pill, card, share);
    }

    IslandMeasure measure() {
        return scene.measure();
    }
}
