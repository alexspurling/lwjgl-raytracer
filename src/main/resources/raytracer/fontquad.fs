/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */

out vec4 color;

/* This comes interpolated from the vertex shader */
in vec2 texcoord;

/* The texture we are going to sample */
uniform sampler2D tex;

void main(void) {
  /* Well, simply sample the texture */
  color = texture2D(tex, texcoord);
}
