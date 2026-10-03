#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 localPoint;
flat in vec2 halfSize;
flat in vec2 corner;
in vec4 vertexColor;

out vec4 fragColor;

float roundedDistance(vec2 point, vec2 extent, float radius) {
    vec2 outside = abs(point) - extent + radius;
    return length(max(outside, 0.0)) + min(max(outside.x, outside.y), 0.0) - radius;
}

void main() {
    float radius = min(corner.x, min(halfSize.x, halfSize.y));
    float edge = roundedDistance(localPoint, halfSize, radius);
    float reach = 0.5 * max(fwidth(edge), 1.0e-3) + corner.y;
    float cover = 1.0 - smoothstep(-reach, reach, edge);
    if (cover <= 0.0) {
        discard;
    }
    fragColor = vertexColor * ColorModulator * vec4(1.0, 1.0, 1.0, cover);
}
