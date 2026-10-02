#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 localPoint;
flat in vec2 halfSize;
flat in vec2 lens;
flat in float blur;
in vec4 vertexColor;

out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float RED_INDEX = 0.985;
const float BLUE_INDEX = 1.015;

const float DENSITY = 0.1;
const float REFRACTION = 2.8;
const float ZOOM = 0.11;
const float SQUIRCLE = 4.6;
const float CIRCLE = 2.0;
const float DISPERSION = 0.22;
const float SATURATION = 1.0;
const float BRIGHTNESS = 0.55;
const float BEND_PROFILE = 2.0;
const float BEND_AIM = -1.0;
const float BEND_REACH = 3.5;
const float FRESNEL_RANGE = 30.0;
const float FRESNEL_HARD = 0.2;
const float FRESNEL_GLOW = 0.2;
const vec4 BODY_TOP = vec4(0.1633, 0.1633, 0.2075, 0.7094);
const float SOFT_SHARE = 1.0;
const float FROST_SHARE = 0.5;
const int FROST_TAPS = 8;
const vec4 BODY_BOTTOM = vec4(0.1110, 0.1110, 0.1471, 0.7716);

float squircleDistance(vec2 point, vec2 extent, float radius, float power) {
    vec2 outside = abs(point) - extent + radius;
    vec2 corner = max(outside, 0.0);
    float reach = power <= 2.01
            ? length(corner)
            : pow(pow(corner.x, power) + pow(corner.y, power), 1.0 / power);
    return reach + min(max(outside.x, outside.y), 0.0) - radius;
}

// WHY: бикубическая выборка из четырёх билинейных (Sigg, Hadwiger, GPU Gems 2, гл. 20): линза
// WHY: растягивает кадр, и билинейная выборка давала на кромке видимые ступени пикселей
vec4 backdrop(vec2 uv) {
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 coord = uv * size - 0.5;
    vec2 base = floor(coord);
    vec2 f = coord - base;
    vec2 square = f * f;
    vec2 cube = square * f;
    vec2 w0 = (1.0 - 3.0 * f + 3.0 * square - cube) / 6.0;
    vec2 w1 = (4.0 - 6.0 * square + 3.0 * cube) / 6.0;
    vec2 w2 = (1.0 + 3.0 * f + 3.0 * square - 3.0 * cube) / 6.0;
    vec2 w3 = cube / 6.0;
    vec2 near = w0 + w1;
    vec2 far = w2 + w3;
    vec2 lower = (base + w1 / near - 1.0 + 0.5) / size;
    vec2 upper = (base + w3 / far + 1.0 + 0.5) / size;
    vec4 top = mix(texture(Sampler0, vec2(upper.x, lower.y)), texture(Sampler0, vec2(lower.x, lower.y)), near.x);
    vec4 bottom = mix(texture(Sampler0, vec2(upper.x, upper.y)), texture(Sampler0, vec2(lower.x, upper.y)), near.x);
    return mix(bottom, top, near.y);
}

float smoothFade(float travel) {
    float clamped = clamp(travel, 0.0, 1.0);
    return clamped * clamped * (3.0 - 2.0 * clamped);
}

// WHY: преломление по закону Снеллиуса на скруглённой кромке: угол падения растёт к краю по
// WHY: профилю, выход считается через asin(sin(a) / n), смещение это тангенс разницы углов
float bendAt(float depth, float thickness) {
    if (depth >= thickness) {
        return 0.0;
    }
    float share = 1.0 - depth / max(thickness, 1.0e-4);
    float incoming = asin(clamp(pow(share, BEND_PROFILE), 0.0, 1.0));
    float leaving = asin(clamp(sin(incoming) / REFRACTION, -1.0, 1.0));
    return -tan(leaving - incoming);
}

float rimGlow(float outside) {
    float base = 1.0 + outside / 1500.0 * pow(500.0 / FRESNEL_RANGE, 2.0) + FRESNEL_HARD;
    return clamp(pow(max(base, 0.0), 5.0), 0.0, 1.0);
}

float shapePower(float radius) {
    float smallest = min(halfSize.x, halfSize.y) * 2.0;
    bool circular = radius >= smallest * 0.49 && abs(halfSize.x - halfSize.y) * 2.0 <= smallest * 0.02;
    return circular ? CIRCLE : SQUIRCLE;
}

vec3 graded(vec3 glass) {
    float grey = dot(glass, LUMA);
    return clamp(mix(vec3(grey), glass, SATURATION) * BRIGHTNESS, 0.0, 1.0);
}

vec3 dispersed(vec2 spot, vec2 offset) {
    float spread = DISPERSION * 20.0;
    vec3 glass;
    glass.r = backdrop(spot + offset * (1.0 - (RED_INDEX - 1.0) * spread)).r;
    glass.g = backdrop(spot + offset).g;
    glass.b = backdrop(spot + offset * (1.0 - (BLUE_INDEX - 1.0) * spread)).b;
    return glass;
}

// WHY: на ходу острова стекло становится матовым: к преломлённой выборке добавляется кольцо из
// WHY: восьми соседних, и то, что видно сквозь стекло, размывается вместе с кромкой и содержимым
vec3 frosted(vec2 spot, vec2 offset, float reach, vec2 screen) {
    vec3 glass = dispersed(spot, offset);
    if (reach < 0.5) {
        return graded(glass);
    }
    for (int tap = 0; tap < FROST_TAPS; tap++) {
        float angle = float(tap) * 6.2831853 / float(FROST_TAPS);
        glass += texture(Sampler0, spot + offset + vec2(cos(angle), sin(angle)) * reach / screen).rgb;
    }
    return graded(glass / float(FROST_TAPS + 1));
}

void main() {
    float radius = lens.x;
    float edge = squircleDistance(localPoint, halfSize, radius, shapePower(radius));
    float smallest = min(halfSize.x, halfSize.y);
    float feather = max(1.0, fwidth(edge)) + blur * smallest * SOFT_SHARE;
    if (edge > feather * 0.5) {
        discard;
    }
    vec2 slope = vec2(dFdx(edge), dFdy(edge));
    float steep = length(slope);
    vec2 normal = steep < 1.0e-5 ? vec2(0.0) : slope / steep;
    float reveal = clamp(0.5 - edge / feather, 0.0, 1.0);

    vec2 screen = vec2(textureSize(Sampler0, 0));
    float depth = max(0.0, -edge);
    float thickness = max(1.0, lens.y);
    vec2 offset = normal * bendAt(depth, thickness) * BEND_AIM * BEND_REACH * thickness / screen;
    vec2 centre = gl_FragCoord.xy + vec2(-localPoint.x, localPoint.y);
    float zoom = ZOOM * smoothFade(depth / max(1.0, halfSize.x));
    vec2 spot = mix(gl_FragCoord.xy / screen, centre / screen, zoom);

    float rise = clamp((localPoint.y + halfSize.y) / max(1.0, halfSize.y * 2.0), 0.0, 1.0);
    vec4 body = mix(BODY_TOP, BODY_BOTTOM, rise);
    float bodyAlpha = clamp(body.a * DENSITY * reveal, 0.0, 1.0);
    vec3 seen = frosted(spot, offset, blur * smallest * FROST_SHARE, screen);
    vec3 blended = mix(seen, body.rgb, bodyAlpha);
    float sheen = edge >= 0.0 ? 0.0 : rimGlow(edge) * FRESNEL_GLOW * 0.7 * min(1.0, steep) * reveal;
    blended = mix(blended, mix(vec3(1.0), body.rgb, bodyAlpha * 0.5), clamp(sheen, 0.0, 1.0));

    fragColor = vec4(blended * vertexColor.rgb * ColorModulator.rgb, reveal * vertexColor.a * ColorModulator.a);
}
