#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord;
in vec2 localPoint;
flat in vec2 halfSize;
flat in float blur;
flat in float corner;
in vec4 vertexColor;

out vec4 fragColor;

const float BLUR_LEVELS = 2.5;

float roundedDistance(vec2 point, vec2 extent, float radius) {
    vec2 outside = abs(point) - extent + radius;
    return length(max(outside, 0.0)) + min(max(outside.x, outside.y), 0.0) - radius;
}

void main() {
    vec4 color = texture(Sampler0, texCoord, blur * BLUR_LEVELS) * vertexColor;
    float radius = min(corner, min(halfSize.x, halfSize.y));
    float edge = roundedDistance(localPoint, halfSize, radius);
    float reach = 0.5 * max(fwidth(edge), 1.0e-3) + blur * min(halfSize.x, halfSize.y) * 0.12;
    color.a *= 1.0 - smoothstep(-reach, reach, edge);
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
