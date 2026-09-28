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
in vec2 TexCoord;
in ivec2 Local;
in ivec2 HalfSize;
in vec3 Blur;
in float Corner;

out vec2 texCoord;
out vec2 localPoint;
flat out vec2 halfSize;
flat out float blur;
flat out float corner;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = TexCoord;
    localPoint = vec2(Local) / 4.0;
    halfSize = vec2(HalfSize) / 4.0;
    blur = Blur.x;
    corner = Corner;
    vertexColor = Color;
}
