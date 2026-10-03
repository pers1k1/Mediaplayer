#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 Local;
in ivec2 HalfSize;
in ivec2 Corner;

out vec2 localPoint;
flat out vec2 halfSize;
flat out vec2 corner;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    localPoint = Local;
    halfSize = vec2(HalfSize) / 4.0;
    corner = vec2(Corner) / 4.0;
    vertexColor = Color;
}
