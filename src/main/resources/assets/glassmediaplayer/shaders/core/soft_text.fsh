#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// WHY: ванильный шейдер умножает на покрытие и цвет, и прозрачность буквы, поэтому сглаженный край
// WHY: темнеет в чёрную обводку, а порог 0.1 срезает мягкий край. Здесь цвет буквы постоянный,
// WHY: покрытие идёт только в прозрачность, без порога; степень чуть меньше единицы добавляет
// WHY: тонким штрихам плотности, которую сглаживание на малом кегле у них отнимает
const float COVERAGE_POWER = 0.85;

void main() {
    float coverage = pow(texture(Sampler0, texCoord0).r, COVERAGE_POWER);
    float alpha = coverage * vertexColor.a * ColorModulator.a;
    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb * ColorModulator.rgb, alpha);
}
