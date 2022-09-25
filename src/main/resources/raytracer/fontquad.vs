/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */

layout (location=0) in vec3 position;
layout (location=1) in vec2 inTexCoord;

/* Write interpolated texture coordinate to fragment shader */
out vec2 texcoord;

void main(void) {

  gl_Position = vec4(position, 1.0);

  texcoord = inTexCoord;
}
