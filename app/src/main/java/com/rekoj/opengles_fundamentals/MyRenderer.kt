package com.rekoj.opengles_fundamentals

import android.opengl.GLES32.glClearColor
import android.opengl.GLES32.GL_COLOR_BUFFER_BIT
import android.opengl.GLES32.glClear
import android.opengl.GLES32.glViewport
import android.opengl.GLSurfaceView.Renderer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer : Renderer {
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // glClearColor chỉ định giá trị màu rgba cho bộ đệm màu sắc của OpenGLES
        glClearColor(1.0f, 0.5f, 0.5f, 0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // glViewport set phạm vi hiển thị của OpenGL
        glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // clear bộ đệm màu sắc để thay bằng màu mới đã chỉ định ở onSurfaceCreated
        glClear(GL_COLOR_BUFFER_BIT)
    }
}