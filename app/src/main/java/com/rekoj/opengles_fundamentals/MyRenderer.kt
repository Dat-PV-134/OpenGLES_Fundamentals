package com.rekoj.opengles_fundamentals

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES32
import android.opengl.GLES32.glClearColor
import android.opengl.GLES32.GL_COLOR_BUFFER_BIT
import android.opengl.GLES32.glClear
import android.opengl.GLES32.glViewport
import android.opengl.GLSurfaceView.Renderer
import com.rekoj.opengles_fundamentals.util.LoggerConfig
import com.rekoj.opengles_fundamentals.util.ShaderHelper
import com.rekoj.opengles_fundamentals.util.ShaderReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer(private val context: Context) : Renderer {
    // Dữ liệu đỉnh
    private val vertices: FloatArray = floatArrayOf(
        0.0f, 0.5f, 0.0f,   // z, y, z
        -0.5f, -0.5f, 0.0f,
        0.5f, -0.5f, 0.0f
    )

    // Các biến để lưu trữ id của program, vbo và vao
    private var program = 0
    private var VBO = 0
    private var VAO = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // glClearColor chỉ định giá trị màu rgba cho bộ đệm màu sắc của OpenGLES
        glClearColor(1.0f, 0.5f, 0.5f, 0f)

        // Biên dịch và liên kết Vertex Shader và Fragment Shader vào Open GL program
        val vertexShaderSource = ShaderReader.readTextFileFromResource(context, R.raw.vertex_shader)
        val fragmentShaderSource = ShaderReader.readTextFileFromResource(context, R.raw.fragment_shader)
        program = ShaderHelper.buildProgram(vertexShaderSource, fragmentShaderSource)
        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program)
        }
        // Sử dụng program
        GLES32.glUseProgram(program)

        // Tạo 2 biến IntBuffer để lưu trữ id của VAO và VBO
        val vaoBuffer = IntBuffer.allocate(1)
        val vboBuffer = IntBuffer.allocate(1)
        // Tạo VAO và lưu id lưu id vào vaoBuffer
        GLES32.glGenVertexArrays(1, vaoBuffer)
        // Tạo VBO và lưu id lưu id vào vboBuffer
        GLES32.glGenBuffers(1, vboBuffer)
        // Gán id của VAO và VBO vào 2 biến toàn cục đã tạo nhằm sử dụng sau
        VAO = vaoBuffer.get(0)
        VBO = vboBuffer.get(0)

        // Liên kết VAO để bắt đầu lưu trữ cách thức truy cập cũng như dữ liệu của VBO
        GLES32.glBindVertexArray(VAO)
        // Liên kết VBO và truyền dữ liệu đỉnh vào
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, VBO)
        // Tạo 1 vùng nhớ native để lưu trữ dữ liệu đỉnh
        val vertexBuffer: FloatBuffer = ByteBuffer
            .allocateDirect(vertices.size * Float.SIZE_BYTES) // Phân bổ vùng nhớ có kích thước = số lượng đỉnh * kích thước của 1 biến Float
            .order(ByteOrder.nativeOrder()) // Tùy vào kiến trúc của phần cứng mà vùng nhớ đc cung cấp sẽ được sắp xếp theo thứ tự khác nhau. Do đó cần sử dụng chung 1 cách sắp xếp
            .asFloatBuffer() // Chuyển ByteBuffer sang FloatBuffer để sử dụng
        // Truyền dữ liệu đỉnh vào FloatBuffer vừa tạo
        vertexBuffer.put(vertices)
        // Đặt position bằng 0 để bắt đầu đọc dữ liệu từ giá trị đầu tiên
        vertexBuffer.position(0)
        // Gửi dữ liệu đỉnh đến VBO
        // glBufferData(GLenum target, GLsizeiptr size, java.nio.Buffer data, GLenum usage);
        // target: Buffer object được chỉ định
        // size: Số byte cần thiết để lưu trữ dữ liệu đỉnh
        // data: Bộ đệm chứa dữ liệu đỉnh
        // usage: Cách sử dụng dữ liệu
        // Đối với GL_STATIC_DRAW: sửa đổi dữ liệu 1 lần và sử dụng nhiều lần
        // https://registry.khronos.org/OpenGL-Refpages/es3/html/glBufferData.xhtml
        GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, vertices.size * Float.SIZE_BYTES, vertexBuffer, GLES32.GL_STATIC_DRAW)

        // Xác định kiểu dữ liệu và cách thức đọc dữ liệu cho thuộc tính đỉnh có location = index
        // glVertexAttribPointer(GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, GLint offset);
        // indx: Location của thuộc tính trong VertexShader
        // type: Kiểu dữ liệu
        // normalized: true -> ánh xạ dữ liệu kiểu số nguyên sang phạm vi [-1, 1] và [0, 1] nếu là kiểu số nguyên ko dấu
        // stride: Số bytes cách nhau giữa 2 đỉnh
        // offset: Vị trí bắt đầu đọc dữ liệu
        GLES32.glVertexAttribPointer(0, 3, GLES32.GL_FLOAT, false, 3 * Float.SIZE_BYTES, 0)
        // Bật thuộc tính đỉnh có location = index để sử dụng
        GLES32.glEnableVertexAttribArray(0)

        // Hủy liên kết VBO và VAO khi không sử dụng nữa
        // 0: Hủy liên kết tất cả buffer đc liên kết trước đó
        GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0)
        GLES32.glBindVertexArray(0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // glViewport set phạm vi hiển thị của OpenGL
        glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // clear bộ đệm màu sắc để thay bằng màu mới đã chỉ định ở onSurfaceCreated
        glClear(GL_COLOR_BUFFER_BIT)

        // Liên kết VAO và vẽ tam giác
        GLES32.glBindVertexArray(VAO)
        // Kết xuất các hình nguyên thủy (điểm, đường, tam giác) từ mảng dữ liệu
        // glDrawArrays(GLenum mode, GLint first, GLsizei count);
        // mode: kiểu hình nguyên thủy
        // first: Vị trí bắt đầu của mảng dữ liệu đỉnh đã được bật
        // count: Số lượng đỉnh được vẽ
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        // Hủy liên kết VAO khi ko dùng đến nữa
        GLES32.glBindVertexArray(0)
    }
}